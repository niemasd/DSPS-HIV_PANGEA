package individualBasedModel;

import java.io.*;
import java.util.*;


/**
 * Class to represent an individual virus.  One virus per host. Virus holds
 * Things like viral load, and time of infection (can be used to calculate age of virus). 
 * @author Emma
 * @created 22 October 2013
 * 28 mar 2014 - added way to specify initial VL in constructor for first virus. 
 * 23 Sept 14 - Implementing a 2 week latency period so that transmissions are not infintesimally close to each other (Matthew Hall request)
 */

public class Virus {
	
	static double[]		popDist = getSetpt();
	static double		heritability;
	static boolean 		acute=true;
	
	protected Host 		myHost;		//The host that 'owns' this virus.
	protected long		hostUid;	//The UID of the host that 'owns' this virus
	protected double	infectionTime;	//when the virus infected the host
	protected double	viralLoad;		//viral load of this virus
	protected double	recoveryTime; //when the virus recovers (when asymptomatic phase ends)
	protected double 	treatmentTime; //when virus was treated
	
	List<Double>		transmissionTimes = new ArrayList<Double>(); //when this virus infected other hosts
	
	protected boolean	isMigrant;
	
	
	// constructors
	public Virus(Host myHost, double time) {  //for first infection
		this.myHost 	= myHost;
		this.hostUid   = myHost.getUid();
		this.infectionTime = time;
		
		Random rand = new Random();
		int i = rand.nextInt(popDist.length+1);
		this.viralLoad = popDist[i];				//simply randomly pick a starting viral load from dist
		this.isMigrant = false;
	}
	
	public Virus(Host myHost, double time, double initVL) { //for first infection where initial VL is specified!
		this.myHost 	= myHost;
		this.hostUid   = myHost.getUid();
		this.infectionTime = time;
		
		this.viralLoad = initVL;
		this.isMigrant = false;
	}
	
	public Virus(Host myHost, double time, Virus vir) {  //for infection from other host (provide their virus object)
		this.myHost 	= myHost;
		this.hostUid   = myHost.getUid();
		this.infectionTime = time;
		
		Random rand = new Random();
		int i = rand.nextInt(popDist.length);
		this.viralLoad = heritability*vir.getViralLoad() + (1-heritability)*popDist[i]; //from Alizon
		this.isMigrant = vir.isMigrant;
	}
	
	//setters
	
	public void setRecoveryTime(double time){
		recoveryTime = time;
	}
	
	public void setTreatmentTime(double time){
		treatmentTime = time;
	}
	
	//getters
	public double getViralLoad(){
		return viralLoad;
	}
	
	public double getInfectionTime(){
		return infectionTime;
	}
	
	public double getRecoveryTime(){
		return recoveryTime;
	}
	
	public double getTreatmentTime(){
		return treatmentTime;
	}
	
	public boolean getIsMigrant(){
		return isMigrant;
	}
	
	/**
	 * This euqation comes from Fraser et al 2007
	 * @return
	 */
	public double getTransmissionRisk(){ 			//should be modified in future to look at time of virus
		double trueVL = Math.pow(10, viralLoad);
		//using the values that Maximized likelihood of Fraser's equation:
		double bMax = 0.317, bk = 1.02, b50 = 13938.0;
		double ans = (bMax*Math.pow(trueVL, bk))/(Math.pow(trueVL, bk)+Math.pow(b50, bk)) /100;
		return ans;
	}
	
	/**
	 * Returns Fraser et al 2007 but includes check if in acute phase, in which case,
	 * Fraser et al said Acute phase lasts 0.24 years and rate is 2.76/yaer which for 100 contacts is
	 * 0.276/contact  -see Fraser et al 2007 SI Text
	 * 26 Aug 14 - If host is treated, then transmission risk is different! chcek for this.
	 * 23 Sept 14 - Implementing a 2 week latency period so that transmissions are not infintesimally close to each other (Matthew Hall request)
	 * @param currentTime
	 * @return
	 */
	public double getTransmissionRisk (double currentTime) {
		if(currentTime-infectionTime <= 0.038){ //if infection is less than 14 days, transmission risk is 0
			return 0.0;
		}
		
		if(acute){
			if(currentTime-infectionTime <= 0.24){
				return 0.276;
			}
		} 
		
		//else if not acute, or if not in acute phase, then do as normal.
		double trueVL;
		if(myHost.state == InfectionState.TREATED) //if host treated, VL is low!
			trueVL = 50;
		 else
			trueVL = Math.pow(10, viralLoad);
		
		//using the values that Maximized likelihood of Fraser's equation:
		double bMax = 0.317, bk = 1.02, b50 = 13938.0;
		double ans = (bMax*Math.pow(trueVL, bk))/(Math.pow(trueVL, bk)+Math.pow(b50, bk)) /100;
		return ans;
	}
	
	/**
	 * This equation also comes from Fraser et al 2007
	 * According to the equation, this actually models when the asymptomatic phase ends, according to VL.
	 * Currently (28 Mar 14) this is taken as 'death' time, but since recovery is death in this model, is implemented under
	 * the 'recovery' event. 
	 * It returns the 'age' at which the person should die/recover (age of the infection).
	 * 
	 * 26 Aug 14 - If host is treated, then recovery risk is different! chcek for this.
	 * @param time
	 */
	public double getAsymptomaticEndAge(){  
	
		double trueVL;
		if(myHost.state == InfectionState.TREATED) //if host treated, VL is low!
			trueVL = 50;
		 else
			trueVL = Math.pow(10, viralLoad);
		
		//using the values that Maximized likelihood of Fraser's equation:
		double dMax = 25.4, dk = 0.41, d50 = 3058.0;
		double ans = (dMax*Math.pow(d50, dk))/(Math.pow(trueVL, dk)+Math.pow(d50, dk));
		return ans;
	}
	
	//other methods
	
	public void recordInfection(double time){
		transmissionTimes.add((Double)time);
	}
	
	public String getVirusLifeInfo(){
		return ""+viralLoad+","+infectionTime+","+recoveryTime+","+(recoveryTime-infectionTime)+","+transmissionTimes.size();
	}
	
	public List<String> getVirusTransmissionInfo(){
		List<String> result = new ArrayList<String>();
		for(Double d: transmissionTimes){
			result.add(""+myHost.getName()+","+viralLoad+","+(recoveryTime-infectionTime)+","+((double)d-infectionTime));
		}
		return result;
	}
	
	
	//to get setpt dist from file
	private static double[] getSetpt() throws IllegalArgumentException
    {
        FileReader f;
        BufferedReader b = null;
        StringTokenizer s;
        double setpt[] = new double[8483];

        String line, tre;
        int counter =0;

        try
        {
            f = new FileReader(new File("C:\\Users\\Room65-Admin\\Desktop\\Emma\\viralLoads.txt"));
            b = new BufferedReader(f);
            // get the name of the system & store in it's head node
            while((line = b.readLine()) != null)
            {
                s = new StringTokenizer(line);
                try //if it's a blank line... see if there's more text until file is empty
                {
                    while(s.hasMoreTokens())
                    {
                        tre = s.nextToken();
                        setpt[counter] = Double.parseDouble(tre);
                        counter++;
                    }
                }
                catch(NoSuchElementException pe)
                {
                    System.out.println("Blank line in setpt file.. trying next line...");
                }
            }
        }
        catch(FileNotFoundException e)
        { // if file not found, ERROR!
            System.out.println("File not found");
            throw new IllegalArgumentException("File not found!");
        }
        catch(IOException e)
        { // if prob reading file, ERROR!
            System.out.println("Problem Reading File");
            throw new IllegalArgumentException("Problem Reading File");
        }
        catch(NoSuchElementException pe)
        { // if prob tokenizing file, ERROR!
            System.out.println("File may not be a valid format!!");
            throw new IllegalArgumentException("File may not be a valid formats!");
        }
        finally
        {
            try
            { b.close();
            }catch(IOException e)
            { // if prob closing file, ERROR!
                System.out.println("Problem closing file");
                throw new IllegalArgumentException("Problem closing file");
            }
            catch(NullPointerException e)
            {
                System.out.println("File not found");
                throw new IllegalArgumentException("File not found!");
            }
        }

 System.out.println("Sept file read in successfully. "+setpt.length+" vls read.");
        
        return setpt;

    }


}
