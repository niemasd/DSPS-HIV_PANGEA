package individualBasedModel;

import io.Parameter;
//import networks.NetworkNode;



import java.util.*;

import math.Distributions;

import org.apache.commons.math3.*;
import org.apache.commons.math3.distribution.*;

//import math.MersenneTwisterFast;

/**
 * Class to represent a deme.  A deme contains one or more hosts.
 * Each host within the deme has the same parameters.
 * @author sam
 * @created 15 June 2013
 * @version 4 July 2013
 * @version 24 July 2013
 * @version 5  Sept 2013
 * @version 6  Sept 2013
 * @version 24 Sept 2013 - added Deme.generateEvent in order to generate events from each deme
 * @version 26 Sept 2013 - using hostStates
 * @version 27 Sept 2013 - include SI
 * @version 2  Oct  2013 - implements NetworkNode interface (networks package)
 * @version 3  Oct  2013 - actually changed mind about NetworkNode; use use networks package to generate connectivity patterns and integrate though population
 * @author Emma
 * @version 12 Nov 2013 - implemented turning off/on heritability/virus use in events. Added function to get all recovered hosts.
 * @version 3 June 2014 - EBH - Added birth/death events/calculations
 * @version 9 June 2014 - EBH - add a way to specify whether gender is turned on in XML
 */
//public class Deme implements NetworkNode {
public class Deme {

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// class variables and methods
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private static long demeCounter 	= -1;

	private static long nextDemeUID() {
		demeCounter++;
		return (demeCounter);
	}
	
	private static boolean gender = false;
	private static boolean orientation = false;
	private static boolean usingDemeTypes = false;
	private static Population thisPopulation = null; //enables the demes to call the Pop to generate a random deme from the deme groups!
	
	//25 Aug 14 - EBH - set up a way to turn 'treatment' on and off depending on time... 
	private static boolean treatment = false;
	
	/**
	 * set whether gender is true for this run
	 */
	static void setGender(boolean g){
		gender = g;
	}
	
	/**
	 * set whether orientation is true for this run
	 */
	static void setOrientation (boolean o) {
		orientation = o;
	}
	
	/**
	 * set whether demeTypes are being used to classify demes and describe migration
	 */
	static void setUsingDemeTypes (boolean o) {
		usingDemeTypes = o;
	}
	
	/**
	 * 25 aug 14 - EBH - turn and on and off treatment - will make hazard 0 if treatment is off!
	 * @param o
	 */
	static void setTreatment (boolean o) {
		treatment = o;
	}
	
	/**
	 * reset deme counter between multiple replicate runs of DiscreteSpatialPhyloSimulator if necessary
	 */
	static void resetDemeCounter() {
		demeCounter = -1;
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// instance variables & methods
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	// instance variables
	protected String 			name 		= null;
	protected long 				uid;
	protected boolean			heritable 	= false;  //heritable/virus turned off by default
	
	// parameters for between demes
	protected DemeType			demeType	= DemeType.MIGRATION_OF_INFECTEDS;		// migration
																					// NETWORK if allow direct cross deme infection
	protected List<Deme>		neighbours	= null;
	private	  String[]			neighbourDemeNames  = null;							// use if setting from parameters xml file
	protected double[]			migrationParameters = null;
	protected double[]			cumProbBetweenDemes = null;							// only not null if NETWORK
	protected double			totalMigration 		= 0;
	
	
	// parameters for individuals within deme
	protected ModelType			modelType 	= ModelType.SIR;						// SIR or SEIR
	protected double[]			infectionParameters;
	
	protected int numberOfHosts				= 10;									// would normally have numberOfHosts = 1 if NETWORK
	protected List<Host> hosts;
	protected List<Host> deadHosts;													//holds a list of hosts that have died (for later access)#
	
	// 3 june 2014 - EBH - set parameters for birth-death model
	protected double[]			birthDeathParameters = {0,0};
	// 12 June 2014 - EBH - set so that birthDeath can either be growth or can be stable - by default it's 'Growth'
	protected boolean			birthDeathGrowth = true;
	protected int				initialNumberOfHosts;
	
	// 9 June 2014 - EBH - set ability to state how many male and female in each deme
	protected int[] 			maleFemale;
	//and what orientation this deme is
	protected OrientationChoices orientationChoice;
	
	// 5 Aug 2014 - EBH - set up parameters to enable demeGroups
	protected String 		demeGroup;
	protected String[]		neighbourDemeGroups;
	
	// experimental 6 sept 2013
	//protected int[] hostStates = new int[5];
	
	//EBH 25 Aug 14 - trying to introduce new state - Treated
	protected int[] 			hostStates = new int[6];
	protected double			treatmentParameter = 0;
	
	//EBH 16 dec 14 - set so can exponentially grow.
	protected boolean			demeOn = true;
	protected int				maxNumberOfHosts;
	protected boolean			exponentialGrowth = false;
	protected GenderType		genderOnly = null;//allow way to say that all new births should be of one gender, if gender turned on
	

	//////////////////////////////////
	// constructors
	
	public Deme() {
		this.uid  = nextDemeUID();
		this.name = "" + uid;
		
		for (int i = 0; i < hostStates.length; i++) {
			hostStates[i] = 0;
		}
	}
	
	public Deme(String name) {
		this.name = name;
		this.uid  = nextDemeUID();
		
		for (int i = 0; i < hostStates.length; i++) {
			hostStates[i] = 0;
		}
	}
	
	//////////////////////////////////
	// setters
	
	// set methods for between demes
	
	public void setDemeType(DemeType demeType) {
		this.demeType = demeType;
	}
	
	public void setNeighbours(List<Deme> neighbours) {
		this.neighbours = neighbours;
	}
	
	void addNeighbour(Deme neighb) {
		if (this.neighbours == null) {
			this.neighbours = new ArrayList<Deme>();
		}
		this.neighbours.add(neighb);
	}
	
	/**
	 * Emma Hodcroft 12 Nov 13 - to turn off/on use of Heritable/Virus obj
	 */
	public void setHeritable(boolean h){
		heritable = h;
	}
	
	/**
	 * 6 Aug 14 - EBH - set link to Population so that can use to pick deme from a group of demes
	 */
	public void setPopulation(Population p) {
		thisPopulation = p;
	}
	
	/**
	 * if NETWORK then migrationParameters converted to cumulative probability of infection of neighbouring demes
	 * note if probabilities sum to 1 then never attempt to transmit to own deme, e.g. if numberOfHosts = 1
	 * @param migrationParameters
	 */
	public void setMigrationParameters(double[] migrationParameters) {
		this.migrationParameters = migrationParameters;
		
		if (demeType == DemeType.INFECTION_OVER_NETWORK) {

			cumProbBetweenDemes = new double[migrationParameters.length];
			
			cumProbBetweenDemes[0] = migrationParameters[0];
			for (int i = 1; i < migrationParameters.length; i++) {
				cumProbBetweenDemes[i] = cumProbBetweenDemes[i-1] + migrationParameters[i];
			}
			totalMigration 		= 0;				// this is a component of the migration hazard, but if INFECTION_OVER_NETWORK, then individuals dont actually move
		} else {
			// demeType = MIGRATION_OF_INFECTEDS
			totalMigration = 0;
			for (int i = 0; i < migrationParameters.length; i++) {
				totalMigration += migrationParameters[i];
			}
		}
	}
	

	// set methods for Hosts in this deme
		
	public void setModelType(ModelType modelType) {
		this.modelType = modelType;
	}
	
	
	
	public void setInfectionParameters(double[] infectionParameters) {
		if (modelType == ModelType.SI) {
			if (infectionParameters.length == 1) {
				this.infectionParameters = infectionParameters;
			} else {
				System.out.println("Deme.setInfectionParameters: WARNING cant set SI infection parameters");
			}
		} else if (modelType == ModelType.SIR || modelType == ModelType.SIRT) {
			if (infectionParameters.length == 2) {
				this.infectionParameters = infectionParameters;
			} else {
				System.out.println("Deme.setInfectionParameters: WARNING cant set SIR infection parameters");
			}
		} else if (modelType == ModelType.SEIR) {
			if (infectionParameters.length == 3) {
				this.infectionParameters = infectionParameters;
			} else {
				System.out.println("Deme.setInfectionParameters: WARNING cant set SEIR infection parameters");
			}
		} else {
			System.out.println("Deme.setInfectionParameters: WARNING unknown modelType");
			this.infectionParameters = infectionParameters;
		}
		
	}
	
	public void setBirthDeathParameters(double[] birthDeathParameters) {
		this.birthDeathParameters = birthDeathParameters;
	}
	
	public void setTreatmentParameter(double treatmentParameter) {
		this.treatmentParameter = treatmentParameter;
	}
	
	/**
	 * use to set the number of male and female for each deme - IF gender turned on!
	 * @param numMaleFemale
	 */
	public void setMaleFemale(int[] numMaleFemale) {
		if(gender) {
			this.maleFemale = numMaleFemale;
			if(hosts != null){
//System.out.println("We are fixing gender after hosts are set!!");
				setGender();
			} // if hosts are null, just set the parameter.
		} else {
			System.out.println("WARNING! Cannot specify gender without turning on 'Gender' in General parameters in the XML!");
		}
		
	}
	
	/**
	 * used to set the gender of already existing hosts as specified by maleFemale
	 * If the number of males/females specified is < numhosts, then remaining hosts are randomly assigned gender 
	 * (should have been already done in setHosts)
	 * If the number of males/females specified is > numHosts, then gender is allocated, males first, until run out of hosts
	 */
	public void setGender() {
		int numMale = maleFemale[0];
		int numFemale = maleFemale[1];
		
		for (Host h : hosts) {
			if(numMale > 0) {
				h.setGender(GenderType.MALE);
				numMale--;
			} else if(numFemale > 0) {
				h.setGender(GenderType.FEMALE);
				numFemale--;
			} else { //if both numMale and numFemale are 0, but hosts still remain, assign randomly!
				setRandomGender(h);
			}
		}
	}
	
	/**
	 * This randomly generates a gender for each host in the deme
	 */
	public void setRandomGender() {
		for(Host h : hosts) {
			setRandomGender(h);
		}
	}
	
	/**
	 * This randomly generates a gender for a host
	 * @param h
	 */
	public void setRandomGender(Host h) {
		double fast = Distributions.randomUniform();
		if(fast <= 0.5) {
			h.setGender(GenderType.MALE);
		} else {
			h.setGender(GenderType.FEMALE);
		}
	}
	
	/**
	 * When a new host is born, this looks at the gender rules of the deme and assigns accordingly
	 * @param h
	 */
	public void setHostGender(Host h) {
		if(maleFemale == null) //if user did not specify gender for deme or population, randomly set
			setRandomGender(h);
		else if(genderOnly != null){ //if set that all new births should be one gender only
			h.setGender(genderOnly);
		}
		else {							// else specify gender based on the original number of males/females
			int numMales = 0, numFemales = 0;
			for(Host h2 : hosts){
				if(h2.getGender() == GenderType.MALE)
					numMales++;
				else if(h2.getGender() == GenderType.FEMALE)
					numFemales++;
			}
//System.out.println("We have "+numMales+" males and "+numFemales+" females, and are supposed to have "+maleFemale[0]+" and "+maleFemale[1]);
			if(numMales < maleFemale[0] && numFemales < maleFemale[1]) //if have too few males & females, decide randomly
				setRandomGender(h);
			else if(numMales < maleFemale[0]) //if just have too few males, make male
				h.setGender(GenderType.MALE);
			else if(numFemales < maleFemale[1]) //if just have too few females, make female
				h.setGender(GenderType.FEMALE);
			else //if have enough of both, then random
				setRandomGender(h);
		}
	}
	
	
	/**
	 * use to set multiple hosts per deme
	 * @param numberOfHosts
	 */
	public void setHosts(int numberOfHosts) {
		this.numberOfHosts = numberOfHosts;
		hosts			   = new ArrayList<Host>();
			
		//create hosts without gender or orientation
		for (int i = 0; i < numberOfHosts; i++) {
			hosts.add(new Host(this));
		}
		
		//if gender is on, figure out if already know genders or not
		if(gender){
			if(maleFemale != null) { //if not null, then assign by gender already specified! (see setGender)
//System.out.println("We are setting gender by specified!");
				setGender();
			} else { //if is null, assign random gender (either permanent, or until maleFemale is set)
				setRandomGender();
//System.out.println("We are setting gender by random!");
			}
			
			if(orientation){
				if(orientationChoice != null) {  //if not null, then assign by what has already been specified! (see setHostOrientation)
					setHostOrientation();
				} else {  //if is null, set all to be bisexual - will be changed later if specified by user
					
					orientationChoice = OrientationChoices.BISEXUAL; //set this now, but will be changed if set by user.
					setHostOrientation();
				}
			}
		}
		
		countHostStates();
	}
	
	/**
	 * use to set just one host per deme - useful if DemeType = NETWORK
	 */
	public void setHost() {
		this.numberOfHosts  = 1;
		hosts				= new ArrayList<Host>();
		hosts.add(new Host(this));
		
		countHostStates();
	}
	
	/**
	 * sets the deme's orientation, and if hosts are created, calls function to set hosts' orientation
	 */
	public void setOrientationChoice(OrientationChoices oCh) {
		if(orientation){
			orientationChoice = oCh;
			if(hosts != null){
				setHostOrientation();
			} //if they are null, just set choice, and wait.
		} else {
			System.out.println("WARNING! Cannot specify orientation without turning on 'Orientation' in General parameters in the XML!");
		}
	}
	
	/**
	 * Used to set the orientation of already existing hosts by the orientation specified in 'orientation'
	 */
	public void setHostOrientation() {
		for (Host h : hosts) {
			setHostOrientation(h);
		}
	}
	
	/**
	 * sets a host's orientation to the deme's orientation, for an individual host
	 * @param h
	 */
	public void setHostOrientation(Host h) {
		h.setOrientation(orientationChoice);
	}
	
	/**
	 * EBH - 3 June 14 - modified so that number of hosts is updated
	 * @param host
	 */
	public void addHost(Host host) {
		if (hosts == null) {
			hosts = new ArrayList<Host>();
		}
		
		if (!hosts.contains(host)) {
			host.myDeme = this;
			hosts.add(host);
			
			countHostStates();
			numberOfHosts++;
		}
		
	}
	
	/**
	 * set the first host in this deme to be INFECTED
	 */
	public void setIndexCase() {
		if (hosts == null) {
			hosts = new ArrayList<Host>();
		}
		Host h = hosts.get(0);
		h.setState(InfectionState.INFECTED);
		if(heritable){
			h.setVirus(new Virus(h, 0));
	System.out.println("initial Viral load: "+h.getVirus().getViralLoad());
		}
		countHostStates();
		thisPopulation.addInfectedHostToList(h);
	}
	
	/**
	 * Same as above but allows initial viral load to be set by user
	 * @param host
	 * @author Emma Hodcroft - 28 Mar 2014
	 */
	public void setIndexCase(double initVL) {
		if (hosts == null) {
			hosts = new ArrayList<Host>();
		}
		Host h = hosts.get(0);
		h.setState(InfectionState.INFECTED);
		if(heritable){
			h.setVirus(new Virus(h, 0, initVL));
	System.out.println("initial Viral load (set by user): "+h.getVirus().getViralLoad());
		}
		countHostStates();
		thisPopulation.addInfectedHostToList(h);
	}
	
	/**
	 * EBH - 3 June 14 - modified so that number of hosts is updated
	 * @param host
	 */
	public void removeHost(Host host) {
		if (hosts == null) {
			hosts = new ArrayList<Host>();
		}
		
		if (hosts.contains(host)) {
			host.myDeme = null;
			hosts.remove(host);
			
			countHostStates();
			numberOfHosts--;
		}
	}
	
	/**
	 * EBH - 3 June 14 - added so that we can keep track of dead hosts
	 * @param host
	 */
	public void addToDeadHosts(Host host) {
		if (deadHosts == null){
			deadHosts = new ArrayList<Host>();
		}
		
		if(deadHosts.contains(host)) {
			host.myDeme = null;
			deadHosts.add(host);
		}
	}
	
	//////////////////////////////////
	// getters

	public String getName() {
		
		if (this.name == null) {
			this.name = ""+uid;
		}
		
		return this.name;
	}
	
	/**
	 * returns any host in the list from self
	 * @return
	 */
	protected Host getHost() {
		
		if (numberOfHosts > 1) {
			int j = Distributions.randomInt(numberOfHosts);
			return ( hosts.get(j) );
			
		} else if (numberOfHosts == 1) {
			return hosts.get(0);
			
		} else {
			return null;
			
		}
				
	}
	
	/**
	 * returns any host which has state = INFECTED
	 * @return
	 */
	protected Host getInfectedHost() {
		List<Host> infectedHosts = new ArrayList<Host>();
		
		if(modelType == ModelType.SIRT){  //if has treatment, get also ones that are TREATED
			for (Host h : hosts) {
				if (h.getState().equals(InfectionState.INFECTED) || h.getState().equals(InfectionState.TREATED) ) {
					infectedHosts.add(h);
				}
			}
			
		} else {  //if doesn't have treatment, get only ones that are infected
			for (Host h : hosts) {
				if (h.getState().equals(InfectionState.INFECTED)) {
					infectedHosts.add(h);
				}
			}
		}
		
		if (infectedHosts.size() > 1) {
			int choice = Distributions.randomInt(infectedHosts.size());
			return infectedHosts.get(choice);
		} else if (infectedHosts.size() == 1) {
			return infectedHosts.get(0);
		} else {
			return null;
		}
	}
	
	/**
	 * returns any host which has state = EXPOSED
	 * @return
	 */
	protected Host getExposedHost() {
		List<Host> exposedHosts = new ArrayList<Host>();
		for (Host h : hosts) {
			if (h.getState().equals(InfectionState.EXPOSED)) {
				exposedHosts.add(h);
			}
		}
		
		if (exposedHosts.size() > 1) {
			int choice = Distributions.randomInt(exposedHosts.size());
			return exposedHosts.get(choice);
		
		} else if (exposedHosts.size() == 1) {
			return exposedHosts.get(0);
		} else {
			return null;
		}
	}
	
	/**
	 * returns all hosts which are infected - use this for sampling
	 * @return
	 */
	protected List<Host> getInfectedHosts() {
		List<Host> infectedHosts = new ArrayList<Host>();
		
		if(modelType == ModelType.SIRT){  //if has treatment, get also ones that are TREATED
			for (Host h : hosts) {
				if (h.getState().equals(InfectionState.INFECTED) || h.getState().equals(InfectionState.TREATED) ) {
					infectedHosts.add(h);
				}
			}
		} else {  //if doesn't have treatment, just return those infected
			for (Host h : hosts) {
				if (h.getState().equals(InfectionState.INFECTED)) {
					infectedHosts.add(h);
				}
			}
		}
		return infectedHosts;
	}
	
	/**
	 * Emma Hodcroft 12 Nov 13 - gets all recovered hosts (used for getting info about viruses at the end)
	 * @return
	 */
	protected List<Host> getRecoveredHosts(boolean includeDead) {
		List<Host> recoveredHosts = new ArrayList<Host>();
		for (Host h : hosts) {
			if (h.getState().equals(InfectionState.RECOVERED)) {
				recoveredHosts.add(h);
			}
		}
		
		if(includeDead){
			List<Host> deadRecoveredHosts = getDeadRecoveredHosts();
			if(recoveredHosts != null)
				recoveredHosts.addAll(deadRecoveredHosts);
		}
		return recoveredHosts;
	}
	
	/**
	 * Emma Hodcroft 12 Nov 13 - gets ALL recovered hosts (used for getting info about viruses at the end)
	 * Both ALIVE and DEAD recovered hosts... so get full history of infection
	 * @return
	 */
	protected List<Host> getDeadRecoveredHosts() {
		List<Host> deadRecoveredHosts = new ArrayList<Host>();
		if(deadHosts != null) {
			for (Host h : deadHosts) {
				if (h.getState().equals(InfectionState.RECOVERED)) {
					deadRecoveredHosts.add(h);
				}
			}
		}
		return deadRecoveredHosts;
	}
	
	/**
	 * returns any host which has state = IS (InfectionState)
	 * @param IS
	 * @return
	 */
	protected Host getHost(InfectionState IS) {
		List<Host> selectedHosts = new ArrayList<Host>();
		for (Host h : hosts) {
			if (h.getState().equals(IS)) {
				selectedHosts.add(h);
			}
		}
		
		int choice = Distributions.randomInt(selectedHosts.size());
		return selectedHosts.get(choice);
	}
	
	
	protected Deme getAnotherDeme() {
		// TO DO - decide how to choose between self and contacts
		// System.out.println("Deme.getHost - NOT IMPLEMENTED FOR NETWORK YET");
		
		// migration parameters are probability of infection between demes
		// choose between demes
		// if max value of cumProbBetweenDemes is 1 then never choose own deme (e.g. numberOfHosts = 1)
		int choice = Distributions.weightedChoice(cumProbBetweenDemes);
		
		return (neighbours.get(choice));
	}
	
	/**
	 * returns any host apart from the input host
	 * if DemeType = NETWORK then this can be from a different deme
	 * @param notThisHost
	 * @return
	 * EBH - 3 June 14 - added condition so that if chosen deme has 0 individuals, chose another deme!
	 * EBH - 4 June 14 - modified above so that it checks both chosen deme OR self (depending on which was chosen). Chosen neighbour
	 * must have at least 1 host, and if chosing own deme, must have 2 hosts (because cannot infect self!). If not, must choose new deme.
	 */
	protected Host getHost(Host notThisHost) {
		if(usingDemeTypes)  //if using deme types, redirect to the other getHost method!! DOES NOT WORK WITH MIGRATION!!
			return getHost(notThisHost, true);
//System.out.println("Demetype=mig_of_inf?: "+(demeType==DemeType.MIGRATION_OF_INFECTEDS));
//System.out.println("neighbors=null?: "+(neighbours==null));
//System.out.println("neighbors size?: "+neighbours.size());
//for(int i=0; i<neighbourDemeNames.length; i++) {System.out.println("\tneighbors list: "+neighbourDemeNames[i]);}

		Host anotherHost = null;
		if ( (demeType == DemeType.MIGRATION_OF_INFECTEDS) || (neighbours == null) || (neighbours.size() == 0) ) {
			if ( containsHost(notThisHost) && (numberOfHosts > 1) ) {
				anotherHost = getHost();
				while (anotherHost.equals(notThisHost)) {
					anotherHost = getHost();
				}
			}
			
		} else if (demeType == DemeType.INFECTION_OVER_NETWORK ) {
			// TO DO - decide how to choose between self and contacts
			// System.out.println("Deme.getHost - NOT IMPLEMENTED FOR NETWORK YET");
			
			// migration parameters are probability of infection between demes
			// choose between demes
			// if max value of cumProbBetweenDemes is 1 then never choose own deme (e.g. numberOfHosts = 1)

			boolean tryAgain = false;
			int orientCounter = 0;
			
			do {
			
			int choice = Distributions.weightedChoice(cumProbBetweenDemes);
/*String str = "(";
for(int pp=0; pp<cumProbBetweenDemes.length; pp++){
	str = str+cumProbBetweenDemes[pp]+",";
} */
			//EBH 5 June 2014 - this code forces to chose a deme that has at least 1 host if infect over network, at least 2 if own deme!
			int numHost = 0;
			if(choice < neighbours.size() )  numHost = neighbours.get(choice).numberOfHosts;
			else numHost = numberOfHosts-1;  //because cannot infect itself!
			
			int counter = 0;

			while(numHost == 0) {
				choice = Distributions.weightedChoice(cumProbBetweenDemes);
				if(choice < neighbours.size() )  numHost = neighbours.get(choice).numberOfHosts;
				else numHost = numberOfHosts-1;
				counter++;
				if(counter > 100) { System.out.println("ERROR: Cannot find deme with alternate host after 100 tries!"); numHost=1; }
			}
			
//System.out.println("\tChoice is: "+choice+"   neighbors size: "+neighbours.size()+"  cumprob: "+str+")");			
			if (choice < neighbours.size()) {

				// if another deme
				anotherHost = neighbours.get(choice).getHost();
				if(orientation){
					tryAgain = !notThisHost.isCorrectPartner(anotherHost); //if is not preferred partner, try again
				}
//System.out.println("We have made it here but anotherHost is null? "+(anotherHost==null)+" Deme name? "+(neighbours.get(choice).name));
				
			} else {
				// if own deme
				if ( containsHost(notThisHost) && (numberOfHosts > 1) ) {
					anotherHost = getHost();
					while (anotherHost.equals(notThisHost)) {
						anotherHost = getHost();
					}
					if(orientation){
						tryAgain = !notThisHost.isCorrectPartner(anotherHost); //if is not preferred partner, try again
					}
				}
				
			}
			orientCounter++;
			if(orientCounter > 100) { System.out.println("ERROR: Cannot find host to match sexual orientation after 100 tries!"); tryAgain=false; }
		} while (tryAgain == true);
		}
		
		return anotherHost;
		
	}
	
	/**
	 * This version of the getHost(notThisHost) function works for the demeGroups parameter...
	 * It will select what 'group' the new host will come from (or within-deme) using the 'Migration Params'
	 * then randomly select a deme in that group. (Then will do usual tests for orientation, gender, etc)
	 * 
	 * 6 Aug 14 - EBH
	 * @param notThisHost
	 * @param marker
	 * @return
	 */
	public Host getHost(Host notThisHost, boolean marker)
	{
		Host anotherHost = null;
		if ( (demeType == DemeType.MIGRATION_OF_INFECTEDS) ) {
			System.out.println("ERROR: MIGRATION DOES NOT YET WORK WITH DEMEGROUPS!!");
			return null;
			
		} else if( (neighbours == null) || (neighbours.size() == 0) ) {
			if ( containsHost(notThisHost) && (numberOfHosts > 1) ) {
				anotherHost = getHost();
				while (anotherHost.equals(notThisHost)) {
					anotherHost = getHost();
				}
			}
			
		} else if (demeType == DemeType.INFECTION_OVER_NETWORK ) {
			// migration parameters are probability of infection between demes
			// choose between demes
			// if max value of cumProbBetweenDemes is 1 then never choose own deme (e.g. numberOfHosts = 1)

			boolean tryAgain = false;
			int orientCounter = 0;
int chooseOut = 0;
int chooseIn = 0;
			
			do {
			
			/*EBH 5 June 2014 - this code forces to chose a deme that has at least 1 host if infect over network, at least 2 if own deme!
			 * EBH 6 Aug 2014 - modified so that it works with the deme groups
			 */
			int numHost = 0;
			Deme chosenDeme = null;
			int choice = 0;
			int counter = 0;
			
/*String str = "(";
for(int pp=0; pp<cumProbBetweenDemes.length; pp++){
	str = str+cumProbBetweenDemes[pp]+",";
} 
System.out.println("Cum probs is: "+str);*/
			
			while(numHost == 0) { //keep finding new deme until find one that's got at least 1 host
				
				choice = Distributions.weightedChoice(cumProbBetweenDemes);
//System.out.println("\tChoice is: "+choice);
				if(choice < neighbourDemeGroups.length ) {  // chosen to go outside deme!
					String chosenGroupString = neighbourDemeGroups[choice];
//System.out.println("***I am deme of group: "+demeGroup);
//System.out.println("***We want a host from group: "+chosenGroupString);
					chosenDeme = thisPopulation.getDemeFromGroup(chosenGroupString, this);
//System.out.println("***Population has returned: "+chosenDeme.toString());
					numHost = chosenDeme.numberOfHosts;
//System.out.println("\t\tChosenDeme: "+chosenDeme.toString()+" has "+chosenDeme.numberOfHosts+" hosts");

chooseOut++;
					
				} else { //chosen to stay inside deme
					numHost = numberOfHosts-1; //because cannot infect self, so there must be at least 1 other host in own deme
chooseIn++;
				}
				
				counter++;
				if(counter > 100) { System.out.println("ERROR: Cannot find deme with alternate host after 100 tries!"); numHost=1; }
			}

			
//System.out.println("\tChoice is: "+choice+"   neighbors size: "+neighbours.size()+"  cumprob: "+str+")");		
			
			//Now test to ensure host is notThisHost and that Orientation&Gender (if enabled) correctly match
			//
			if (choice < neighbourDemeGroups.length ) { //chosen to go to outside deme

				anotherHost = chosenDeme.getHost();
				if(orientation){
					boolean keepTrying = !notThisHost.isCorrectPartner(anotherHost); //if is not preferred partner, try 3 times in same deme, then try again completely
					int count = 0;
					while(keepTrying && count<3){
						anotherHost = chosenDeme.getHost();
						keepTrying = !notThisHost.isCorrectPartner(anotherHost);
						count++;
					}
					tryAgain = keepTrying;  //if after 3 tries still not right, then start over completely
//if(tryAgain) System.out.println("\t\tTrying again because orientation is wrong.");
				}
				
			} else {
				// if own deme
				if ( containsHost(notThisHost) && (numberOfHosts > 1) ) {
					anotherHost = getHost();
					while (anotherHost.equals(notThisHost)) {
						anotherHost = getHost();
					}
					if(orientation){
						boolean keepTrying = !notThisHost.isCorrectPartner(anotherHost);
						if(numberOfHosts > 2) {  //if there are more than 2 hosts, try 3 times - if just 2 hosts, don't try 3 times (will get same host every time)
							int count = 0;
							while(keepTrying && count<3) {
								while(anotherHost.equals(notThisHost))
									anotherHost = getHost();
								keepTrying = !notThisHost.isCorrectPartner(anotherHost);
								count++;
							}
						} 
						tryAgain = keepTrying; //if after 3 tries still not right, then start over completely
					}
				} else { //this should never happen because in step 1, should only have picked own deme if there is more than one host other than self - but just in case!
					tryAgain = true;
				}
				
			}
			orientCounter++;
			if(orientCounter > 100) { System.out.println("ERROR: Cannot find host to match sexual orientation after 100 tries!"); tryAgain=false; }
			
		} while (tryAgain == true);
//System.out.println("Chose out: "+chooseOut+"  Chose in: "+chooseIn);		

		}
		return anotherHost;
	}
	
	
	public List<Host> getHosts() {
		return hosts;
	}
	
	
	protected boolean containsHost(Host aHost) {
		return( hosts.contains(aHost) );
	}

	
	////////////////////////////////////////////////////
	// Set & Get Deme Parameters
	
	public List<String[]> getDemeParameterList() {
		List<String[]> params = new ArrayList<String[]>();
		params.add(new String[] {"DemeUID", ""+uid} );
		params.add(new String[] {"DemeName",getName()} );
		params.add(new String[] {"NumberOfHosts",""+hosts.size() } );
		if(birthDeathGrowth==false) {
			params.add(new String[] {"InitialNumberOfHosts", ""+initialNumberOfHosts} ); }
		if(exponentialGrowth==true) {
			params.add(new String[] {"MaxNumberOfHosts", ""+maxNumberOfHosts} ); 
			params.add(new String[] {"DemeStatus",""+demeOn} );}
		params.add(new String[] {"ModelType",""+modelType} );
		
		//record gender, if applicable
		if(gender){
			String[] numM = new String[2];
			String[] numF = new String[2];
			numM[0] = "NumMaleHosts";
			numF[0] = "NumFemaleHosts";
			int numMales = 0, numFemales = 0;
			for(Host h : hosts){
				if(h.getGender() == GenderType.MALE){
					numMales++;
				} else { numFemales++; }
			}
			numM[1] = ""+numMales;
			numF[1] = ""+numFemales;
			params.add(numM);
			params.add(numF);
			if(genderOnly != null)
				params.add(new String[] {"GenderOnly",""+genderOnly} );
			
			if(orientation){
				params.add(new String[] {"Orientation",""+orientationChoice} );
			}
		}
		
		String[] bd = new String[birthDeathParameters.length + 1];
		bd[0] = "BirthDeathParameters";
		for (int i = 0; i < birthDeathParameters.length; i++) {
			bd[i+1] = ""+birthDeathParameters[i];
		}
		params.add(bd);
		
		if(birthDeathParameters[0] != 0 && birthDeathParameters[1] != 0){ //if birth death params aren't zero, show growth type
			String[] bdType = new String[2];
			bdType[0] = "BirthDeathGrowthType";
			if(birthDeathGrowth == true)
				bdType[1] = "Growth";
			else {
				if(exponentialGrowth == false)
					bdType[1] = "Stable";
				else
					bdType[1] = "Exponential";
			}
			params.add(bdType);
		}
		
		if(treatmentParameter != 0) {
			String[] tType = new String[2];
			tType[0] = "TreatmentParameter";
			tType[1] = ""+treatmentParameter;
			params.add(tType);
		}
		
		String[] ip = new String[infectionParameters.length + 1];
		ip[0] = "InfectionParameters";
		for (int i = 0; i < infectionParameters.length; i++) {
			ip[i+1] = ""+infectionParameters[i];
		}
		params.add(ip);
		
		params.add(new String[]{"DemeType",""+demeType} );
		
		if (neighbours != null) {
			String[] np = new String[neighbours.size() + 1];
			np[0] = "Neighbours";
			for (int i = 0; i < neighbours.size(); i++) {
				np[i+1] = neighbours.get(i).getName();
			}
			params.add(np);
		}
		
		if (migrationParameters != null) {
			String[] mp = new String[migrationParameters.length + 1];
			mp[0] = "MigrationParameters";
			for (int i = 0; i < migrationParameters.length; i++) {
				mp[i+1] = ""+migrationParameters[i];
			}
			params.add(mp);
		}
		
		return params;
	}
	/**
	 * 16 Dec 14- EBH - modfiied so that can set demeStatus (off) and maxhostsperdeme
	 * @param params
	 */
	public void setDemeParameters(List<Parameter> params) {
		
		// specific to this deme
		if (params.get(0).getParentTag().equals("Deme")) {
			
			for (Parameter p : params) {
				if (p.getId().equals("DemeUID")) {
					uid = Integer.parseInt(p.getValue());
//System.out.println("set params for "+uid);
					
				} else if (p.getId().equals("DemeName")) {
					name = p.getValue();
					
				} else if (p.getId().equals("Neighbours")) {
					neighbourDemeNames = p.getValue().split(",");
					
					
				} else if (p.getId().equals("MigrationParameters")) {
					String[] mpTxt = p.getValue().split(",");
					double[] mp	   = new double[mpTxt.length];
					for ( int i = 0; i < mpTxt.length; i++) {
						mp[i]		= Double.parseDouble(mpTxt[i]);
					}
					setMigrationParameters(mp);
					
				} else if (p.getId().equals("DemeGroup")) {
					demeGroup = p.getValue().toLowerCase();
					
				} else if (p.getId().equals("NeighbourDemeGroups")) {
					String[] temp = p.getValue().toLowerCase().split(",");
					for(int i=0; i<temp.length; i++){
						temp[i] = temp[i].trim();
					}
					neighbourDemeGroups = temp;
				} else if (p.getId().equals("DemeStatus")) {
					String stat = p.getValue().trim().toLowerCase();
					if(stat.equals("off"))
						demeOn = false; 
				} else if (p.getId().equals("GenderOnly")) {  //sets all new births to be only this gender if gender turned on
					String gen = p.getValue().trim().toLowerCase();
System.out.println("In deme "+name+" genderOnly is set to: "+gen);
					if(gen.equals("female") || gen.equals("f") )
						genderOnly = GenderType.FEMALE;
					else if(gen.equals("male") || gen.equals("m") )
						genderOnly = GenderType.MALE;
					else {
						System.out.println("***GenderOnly specification not recognised! Specify Male or Female. Allowing random gender for this deme!");
					}
				}
			}
			
		}
		
		// specific to this deme, or for when all demes are set the same
		if  ( params.get(0).getParentTag().equals("Deme") || params.get(0).getParentTag().equals("PopulationStructure") ) {
			
			for (Parameter p : params) {
				
				if (p.getId().equals("NumberOfHosts")) {
					setHosts( Integer.parseInt(p.getValue() ));
				
				} else if (p.getId().equals("NumberOfHostsPerDeme")) {
					setHosts( Integer.parseInt(p.getValue() ));	
					initialNumberOfHosts = Integer.parseInt(p.getValue() );  //save to use if birthDeathType = stable
					
				} else if (p.getId().equals("MaxHostsPerDeme")) {
					if(p.getValue().equals("exp"))
						maxNumberOfHosts = Integer.MAX_VALUE;  //if exp, need to grow this deme manually (kind of)
					else
						maxNumberOfHosts = Integer.parseInt(p.getValue() ); //save to use if birthDeathType = exponential
					
				} else if (p.getId().equals("NumberOfMaleFemaleHosts")) { 
					String[] mftxt = p.getValue().split(",");
					int[] mf    = new int[mftxt.length];
					for ( int i = 0; i < mftxt.length; i++) {
						mf[i] = Integer.parseInt(mftxt[i]);
					}
					setMaleFemale(mf);
					
				} else if (p.getId().equals("OrientationChoice")) {
					String oChoice = p.getValue().trim().toLowerCase();
					if(oChoice.equals("hetero") | oChoice.equals("het") | oChoice.equals("heterosexual"))
						setOrientationChoice(OrientationChoices.HETEROSEXUAL);
					else if(oChoice.equals("homo") | oChoice.equals("homosexual"))
						setOrientationChoice(OrientationChoices.HOMOSEXUAL);
					else
						setOrientationChoice(OrientationChoices.BISEXUAL);
				
				} else if (p.getId().equals("ModelType")) {
					setModelType( ModelType.valueOf( p.getValue() ) );
				
				} else if (p.getId().equals("InfectionParameters")) {
					String[] iptxt = p.getValue().split(",");
					double[] ip    = new double[iptxt.length];
					for ( int i = 0; i < iptxt.length; i++) {
						ip[i] = Double.parseDouble(iptxt[i]);
					}
					setInfectionParameters(ip);
				
				} else if (p.getId().equals("BirthDeathParameters")) {
					String[] bdtxt = p.getValue().split(",");
					double[] bd    = new double[bdtxt.length];
					for( int i = 0; i < bdtxt.length; i++) {
						bd[i] = Double.parseDouble(bdtxt[i]);
					}
					setBirthDeathParameters(bd);
					
				} else if (p.getId().equals("TreatmentParameter")) {
					double treat = Double.parseDouble(p.getValue());
					setTreatmentParameter(treat);
					
				} else if (p.getId().equals("BirthDeathType")) { //defaults to 'growth' so check for 'stable'
					String bDType = p.getValue().trim().toLowerCase();
					if(bDType.equals("stable")){
						birthDeathGrowth = false;
					} else if (bDType.equals("exponential")) {
						exponentialGrowth = true;
						birthDeathGrowth = false; 
					}
					
				} else if (p.getId().equals("DemeType")) {
					setDemeType( DemeType.valueOf( p.getValue() ) );
					this.setMigrationParameters(migrationParameters);
				
				} else if (p.getId().equals("ProbabilityInfectionAnyOtherDeme") ||  p.getId().equals("ProbabilityMigrationAnyOtherDeme")) {
					// expect a single number as all neighbouring demes treated equally
					// divide this by number of neighbours
					double probBetween = Double.parseDouble(p.getValue());
					int    numNeighb   = neighbours.size();
					if (numNeighb < 1) {
						System.out.println("Deme.setDemeParameters - WARNING cant set probability infection because no neighbours to infect");
					} else {
						double p_per_neighb = probBetween/(double)numNeighb;
						double[] migrationParameters = new double[numNeighb];
						for (int i = 0; i < numNeighb; i++) {
							migrationParameters[i] = p_per_neighb;
						}
						this.setMigrationParameters(migrationParameters);
					}
				}
				
			}
			
		
								
				//} else {
				//	System.out.println("Deme.setDemeParamers - sorry dont understand "+p.getId()+" "+p.getValue());
				//}
			//}
			
		} else {
			System.out.println("Deme.setDemeParameters - WARNING attempted to set non-deme parameters for this deme, but didnt do anything");
		}
		
		
	}
	
	public void setNeighbours(Population thePopulation) {
		
		if (neighbourDemeNames != null) {
			System.out.println("Deme.setNeighbours - setting neighbours from neighbour names - "+name);
			List<Deme> nd  		= new ArrayList<Deme>();
			List<Deme> allDemes = thePopulation.getDemes();
			for ( String ndn : neighbourDemeNames ) {
				Deme tempDeme	= new Deme( ndn );
				if ( allDemes.contains( tempDeme ) ) {
					nd.add( allDemes.get( allDemes.indexOf( tempDeme ) ) );
				}
			}
			neighbours = nd;
			
		} else {
			//System.out.println("Deme.setNeighbours - WARNING cant set neighbouring demes from the the population because havent set neighbourDemeNames");
			if ( (neighbours != null) && (neighbours.size() > 0) ) {
//				System.out.println("Deme.setNeighbours - setting neighbour names from already set demes");
				neighbourDemeNames = new String[neighbours.size()];
				for (int i = 0; i < neighbours.size(); i++) {
					neighbourDemeNames[i] = neighbours.get(i).getName();
				}
			}
		}
	}
	
	
	////////////////////////////////////////////////////////////////////////////////////
	// EVENT GENERATORS - EXPERIMENTAL
	////////////////////////////////////////////////////////////////////////////////////
	
	// experimental 6 Sept 2013
	private void countHostStates() {
		int S = 0;
		int E = 0;
		int I = 0;
		int R = 0;
		int M = 0;
		int T = 0;
		for (Host h : hosts) {
			if (h.state == InfectionState.SUSCEPTIBLE) {
				S++;
			}else if (h.state == InfectionState.EXPOSED) {
				E++;
			} else if (h.state == InfectionState.INFECTED) {
				I++;
			} else if (h.state == InfectionState.RECOVERED) {
				R++;
			} else if (h.state == InfectionState.IMMUNE) {
				M++;
			} else if (h.state == InfectionState.TREATED) {
				T++;
			}
		}
		
		hostStates[0] = S;
		hostStates[1] = E;
		hostStates[2] = I;
		hostStates[3] = R;
		hostStates[4] = M;
		hostStates[5] = T;
		
	}
	
	
	// INCREMENTORS
	protected void incrementS() {
		hostStates[0] = hostStates[0] + 1;
	}
	
	protected void incrementE() {
		hostStates[1] = hostStates[1] + 1;
	}
	
	protected void incrementI() {
		hostStates[2] = hostStates[2] + 1;
	}
	
	protected void incrementR() {
		hostStates[3] = hostStates[3] + 1;
	}
	
	protected void incrementM() {
		hostStates[4] = hostStates[4] + 1;
	}
	
	protected void incrementT() {
		hostStates[5] = hostStates[5] + 1;
	}
	
	// DECREMENTORS
	protected void decrementS() {
		hostStates[0] = hostStates[0] - 1;
		
		if (hostStates[0] < 0) {
			hostStates[0] = 0;
		}
	}
	
	protected void decrementE() {
		hostStates[1] = hostStates[1] - 1;
		
		if (hostStates[1] < 0) {
			hostStates[1] = 0;
		}
	}
	
	protected void decrementI() {
		hostStates[2] = hostStates[2] - 1;
		
		if (hostStates[2] < 0) {
			hostStates[2] = 0;
		}
	}
	
	protected void decrementR() {
		hostStates[3] = hostStates[3] - 1;
		
		if (hostStates[3] < 0) {
			hostStates[3] = 0;
		}
	}
	
	protected void decrementM() {
		hostStates[4] = hostStates[4] - 1;
		
		if (hostStates[4] < 0) {
			hostStates[4] = 0;
		}
	}
	
	protected void decrementT() {
		hostStates[5] = hostStates[5] - 1;
		
		if(hostStates[5] < 0) {
			hostStates[5] = 0;
		}
	}
	
	
	public int numberSusceptible() {
		return hostStates[0];
	}
	
	public int numberExposed() {
		return hostStates[1];
	}
	
	public int numberInfected() {
		return (hostStates[2]);
	}
	
	public int numberRecovered() {
		return hostStates[3];
	}
	
	public int numberImmune() {
		return hostStates[4];
	}
	
	public int numberTreated() {
		return hostStates[5];
	}
	
	/**
	 * counts the number of hosts in each state, then returns current state of hosts number of S, I, R or S, E, I, R as appropriate in this deme
	 * @return
	 */
	public int[] hostStates() {
		//countHostStates();
		
		int S = numberSusceptible();
		int E = numberExposed();
		int I = numberInfected();
		int R = numberRecovered();
		int M = numberImmune();
		int T = numberTreated();
		
		if (modelType == ModelType.SI) {
			int[] hstates = {S, I};
			return hstates;
		} else if (modelType == ModelType.SIR) {
			int[] hstates = {S, I, R};
			return hstates;
		} else if (modelType == ModelType.SIRT) {
			int[] hstates = {S, I, R, T};
			return hstates;
		} else if (modelType == ModelType.SEIR) {
			int[] hstates = {S, E, I, R};
			return hstates;
		} else {
			int[] hstates = {S, E, I, R, M};
			return hstates;
		}
		
	}
	
	/**
	 * counts the number of hosts in each state, then returns the number in each category of S, E, I, R, M
	 * @return
	 */
	public int[] getSEIRM() {
		countHostStates();
		
		int S = numberSusceptible();
		int E = numberExposed();
		int I = numberInfected();
		int R = numberRecovered();
		int M = numberImmune();
		
		int[] hstates = {S, E, I, R, M};
		return hstates;
	}
	
	/**
	 * returns header corresponding to hostStates, e.g. for writing to csv file
	 * @return
	 */
	public String[] hostStatesHeader() {
		if (modelType == ModelType.SI) {
			String[] hstates = {"S-"+uid, "I-"+uid};
			return hstates;
		} else if (modelType == ModelType.SIR) {
			String[] hstates = {"S-"+uid, "I-"+uid, "R-"+uid};
			return hstates;
		} else if (modelType == ModelType.SIRT) {
			String[] hstates = {"S-"+uid, "I-"+uid, "R-"+uid, "T-"+uid};
			return hstates;
		} else if (modelType == ModelType.SEIR) {
			String[] hstates = {"S-"+uid, "E-"+uid, "I-"+uid, "R-"+uid};
			return hstates;
		} else {
			String[] hstates = {"S-"+uid, "E-"+uid, "I-"+uid, "R-"+uid, "M-"+uid};
			return hstates;
		}
	}
	
	private boolean hasEvent() {
		// how many of my hosts are active ?
		return (( numberExposed() + numberInfected() ) > 0 );
	}
	
	private double generateExposedToInfectedHazard() {
		double h = 0;
		if (modelType == ModelType.SEIR) {
			h = infectionParameters[0]*(double)numberExposed();
		}
		return h;
	}
	
	private double generateInfectOtherHazard() {
		double h = 0;
		if ((modelType == ModelType.SIR) || (modelType == ModelType.SI))  {
			h = infectionParameters[0]*(double)numberInfected();
		} else if (modelType == ModelType.SIRT){
			h = infectionParameters[0]* ( (double)numberInfected() + (double)numberTreated() );
		} else if (modelType == ModelType.SEIR) {
			h = infectionParameters[1]*(double)numberInfected();
		} else {
			System.out.println("Deme.generateInfectionHazard: WARNING unknown modelType");
		}
		return h;
	}
	
	private double generateRecoveryHazard() {
		double h = 0;
		if (modelType == ModelType.SI) {
			// do nothing
		} else if (modelType == ModelType.SIR) {
			h = infectionParameters[1]*(double)numberInfected();
		} else if (modelType == ModelType.SIRT) {
			h = infectionParameters[1]* ( (double)numberInfected() + (double)numberTreated() );
		} else if (modelType == ModelType.SEIR) {
			h = infectionParameters[2]*(double)numberInfected();
		} else {
			System.out.println("Deme.generateInfectionHazard: WARNING unknown modelType");
		}
		return h;
	}
	
	private double generateMigrationHazard() {
		double h = 0;
		if (demeType.equals(DemeType.MIGRATION_OF_INFECTEDS)) {
			//for (int i = 0; i < migrationParameters.length; i++) {
			//	h += migrationParameters[i];
			//}
			h = (double)totalMigration * (double)numberOfHosts;				// migration to any deme (but this one) x hosts
		}
		return h;
	}
	
	private double generateTreatmentHazard() {
		double h = 0;
		if(treatment){
			h = treatmentParameter * (double)numberInfected() ;
			
		} //if treatment not true, hazard is 0!
		return h;
	}
	
	private double generateBirthHazard() {
		if(birthDeathGrowth == true)
			return birthDeathParameters[0]*(double)numberOfHosts;
		else if(birthDeathGrowth == false && exponentialGrowth == false)//it is stable, so have risk inverse to the number of hosts currently occupying
			return birthDeathParameters[0]*( (double)initialNumberOfHosts-(double)numberOfHosts );
		else //is exponential, so risk inverse to number of hosts currently occupying vs the max number allowed
			return birthDeathParameters[0]*( (double)maxNumberOfHosts-(double)numberOfHosts );
	}
	
	private double generateDeathHazard() {
		return birthDeathParameters[1]*(double)numberOfHosts;
	}
	
	Hazard generateHazards() {
		if(this.demeOn==false) //if deme off, has no hazard - do not pick to do anything!
		{
			Hazard h = new Hazard(this);
			h.setExposedToInfectedHazard( 0 );
			h.setInfectOtherHazard( 0 );
			h.setMigrationHazard( 0 );
			h.setRecoveryHazard( 0 );
			h.setTreatmentHazard( 0 );
			h.setBirthHazard( 0 );
			h.setDeathHazard( 0 );
			h.getTotalHazard();	
			return h;
		}
		
		Hazard h = new Hazard(this);
		h.setExposedToInfectedHazard( generateExposedToInfectedHazard() );
		h.setInfectOtherHazard( generateInfectOtherHazard() );
		h.setMigrationHazard( generateMigrationHazard() );
		h.setRecoveryHazard( generateRecoveryHazard() );
		h.setTreatmentHazard( generateTreatmentHazard() );
		h.setBirthHazard( generateBirthHazard() );
		h.setDeathHazard( generateDeathHazard() );
		h.getTotalHazard();										// calculates total hazard
		return h;
	}
	
	/**
	 * Updated so that Birth/Death is included in event generation
	 * @Author EBH - 3 June 2014
	 * @param h
	 * @param currentTime
	 * @param actionTime
	 * @return
	 */
	Event generateEvent(Hazard h, double currentTime, double actionTime) {
		
/*if(this.uid == 3187)	{	
System.out.println("INSIDE THE DEME the hazards are:"
		+"\r\t Exposed Hazard is: "+generateExposedToInfectedHazard()
		+"\r\t Birth hazard is: "+generateBirthHazard()
		+"\r\t Death hazard is: "+generateDeathHazard()
		+"\r\t Infection hazard is: "+generateInfectOtherHazard()
		+"\r\t Recovery hazard is: "+generateRecoveryHazard()
		+"\r\t Treatment hazard is: "+generateTreatmentHazard()
		+"\r\t Migration hazard is: "+ generateMigrationHazard()); }  */
		/*
		double[] cumRateEvents = new double[4];
		cumRateEvents[0] = h.getExposedToInfectedHazard();					// this could be 0
		cumRateEvents[1] = h.getInfectOtherHazard() + cumRateEvents[0];			
		cumRateEvents[2] = h.getMigrationHazard() + cumRateEvents[1];		// this could be 0
		cumRateEvents[3] = h.getRecoveryHazard() + cumRateEvents[2];		// this could be 0
		*/
		
	  double totalWeights = h.getTotalHazard();
	  if (totalWeights > 0) {
		
		double[] weights 	= {h.getExposedToInfectedHazard(), h.getInfectOtherHazard(), h.getMigrationHazard(), h.getRecoveryHazard(),
								h.getBirthHazard(), h.getDeathHazard(), h.getTreatmentHazard()};
		
System.out.println(this.uid+ " exposed to infect hazard: "+h.getExposedToInfectedHazard()+"  infect Other hazard: "+h.getInfectOtherHazard()+"   migration hazard: "+h.getMigrationHazard()+"  recovery hazard: "+h.getRecoveryHazard());
		int eventChoice  	= Distributions.chooseWithWeights(weights, totalWeights);
		//int eventChoice  = Distributions.unNormalisedWeightedChoice(cumRateEvents);
//System.out.println("\tDeme.generateEvent - event choice = "+eventChoice);
		
		Event e			 = new Event();
		
		if (eventChoice == 0) {
			// EXPOSED TO INFECTED
			Host aHost = getExposedHost();
			
			if (aHost != null) {
				e.setBecomeInfectiousEvent(aHost, currentTime, actionTime);
			} else {
				e = null;
			}
			
		} else if (eventChoice == 1) {
			// INFECTION
			Host aHost = getInfectedHost();
			
			if (aHost != null) {  
//System.out.println("\tGot aHost: "+aHost);  //uncomment for got aHost
				
//long startTime = System.currentTimeMillis();	//uncomment to time how long finding host takes
				Host bHost = getHost(aHost);
//long endTime = System.currentTimeMillis();
//System.out.println("milis: "+(endTime-startTime));
			
				if (bHost != null) {  
					
//System.out.println("\tGot bHost: "+bHost);  //uncomment for got bHost
					if (modelType.equals(ModelType.SEIR)) {
						e.setExposureEvent(aHost, bHost, currentTime, actionTime);				
					} else {
						e.setInfectionEvent(aHost, bHost, currentTime, actionTime);
					}
					
				} else {
					e = null;
//System.out.println("\tINFECTION - bHost is null");
				}
			} else {
				e = null;
//System.out.println("\tINFECTION - aHost is null");
			}
				
		} else if (eventChoice == 2) {
			// MIGRATION
			Host aHost  = getHost();
			Deme toDeme = getAnotherDeme();
			
			if ((aHost != null) && (toDeme != null) ) {
				e.setMigrationEvent(aHost, this, toDeme, currentTime, actionTime);
			} else {
				e = null;
			}
			
		} else if (eventChoice == 3) {
			// RECOVERY
			if ( modelType.equals(ModelType.SI) ) {
				e = null;
				System.out.println("Deme.generateEvent - event choice = "+eventChoice+" but this is invalid for "+modelType);
					
			} else {
			
				Host aHost = getInfectedHost();
//System.out.println("\tGot Host for recovery: "+aHost);
			
				if (aHost != null) {
					e.setRecoveryEvent(aHost, currentTime, actionTime);
			
				} else {
					e = null;
//System.out.println("\tRECOVERY - aHost is null");
				}
			
			}
				
		} else if (eventChoice == 4) {
			//BIRTH
			Host aHost  = getHost();
//System.out.println("Trying to generate a birth... aHost is null: "+(aHost==null)+" birthDeathGrowth is "+birthDeathGrowth);
			
			if (aHost != null) { //need aHost if in 'growth' mode!
				e.setBirthEvent(aHost, currentTime, actionTime);
			} else if(birthDeathGrowth == false && aHost == null) { // if in 'stable' or exponential mode allow repop of dead demes, so allow parentless birth
				e.setBirthEvent(new Host(this, false), currentTime, actionTime);
			} else {
//System.out.println("\tBIRTH - aHost is null? "+aHost==null+"  birthDeathGrowth false? "+birthDeathGrowth==false);
				e = null;
			}
		
	  	} else if (eventChoice == 5) { 
	  		//DEATH
	  		Host aHost = getHost();
	  		
	  		if (aHost != null) {
				e.setDeathEvent(aHost, currentTime, actionTime);
			} else {
//System.out.println("\tDEATH - aHost is null");
				e = null;
			}
	  		
	  	} else if (eventChoice == 6) {
			//TREATMENT
			Host aHost = getInfectedHost();
			if(aHost != null) { 
				e.setTreatmentEvent(aHost, currentTime, actionTime);
			} else {
//System.out.println("\tTREATMENT - aHost is null");
				e = null;
			}
		
	  	} else {
			System.out.println("\tDeme.generateEvent - event choice = "+eventChoice+" but this is invalild");
			e = null;
		}
		
if(e==null){
	System.out.println("\t This Deme info: "+this.toStringExtensive());
}
		return e;
		
	  } else {
System.out.println("\t This Deme info: "+this.toStringExtensive());
System.out.println("\tTotalWeights is 0! "+totalWeights);
			return null;
	  }
		
	}
	
	
	
	/**
	 * Deme performs event, and also updates the host state count
	 * @param e
	 */
	public void performEvent(Event e) {
		
		e.setSuccess(false);			// event success defaults to false
		//Event newEvent = null;
		
		Host toHost 		 = e.getToHost();		// this could be in this deme, or another deme
		Deme toDeme			 = e.getToDeme();		// may or may not be this deme
		Host fromHost 		 = e.getFromHost();
		InfectionState state = toHost.getState();
		
		if ( e.getFromDeme().equals(this)  ) {	
			
			if ( e.getType() == EventType.EXPOSURE ) {
				
				if ((state == InfectionState.SUSCEPTIBLE ) && (modelType == ModelType.SEIR) ) {
					// event possible
					toHost.setState(InfectionState.EXPOSED);
					e.setSuccess(true);
					
					// S -> E
					decrementS();
					toDeme.incrementE();
					
				} 
				
			} else if ( e.getType() == EventType.INFECTION )  {
				
				if ((state == InfectionState.EXPOSED) && (modelType == ModelType.SEIR)) {
					// event possible
					toHost.setState(InfectionState.INFECTED);
					e.setSuccess(true);
					
					// E -> I
					decrementE();
					toDeme.incrementI();
					
				} else {
					// SIR, SI, SIRT
					if ( state == InfectionState.SUSCEPTIBLE)  {
						if(!heritable)
						{
							toHost.setState(InfectionState.INFECTED);  //original code
							e.setSuccess(true);
							// S -> I
							decrementS();
							toDeme.incrementI();  
							thisPopulation.addInfectedHostToList(toHost);
						} else {
						
							//virus code
							// event possible
							double transmitRisk = fromHost.getVirus().getTransmissionRisk(e.actionTime);  
							if(toDeme.demeGroup.equals("vil")){ //make it so that if transmitting to a Village then transmission always happens!!
								transmitRisk = 100;
								System.out.println("Attempting to transmit to a village");
							}
//							if(fromHost.getDeme().demeGroup.equals("vil"))
//								System.out.println("Attempting to infect population from a village "+fromHost.getDeme().toString());
								
							double fast = Distributions.randomUniform(); //calls MersenneTwisterFast.getDouble(); 
						
//	System.out.println("transmission risk: "+transmitRisk);
						
							if(fast < transmitRisk){   //if chance of transmission...
							
								toHost.setState(InfectionState.INFECTED);
								if(heritable) { toHost.setVirus(new Virus(toHost, e.actionTime, fromHost.getVirus())); }
//	if(heritable) {System.out.println("old VL: "+fromHost.getVirus().getViralLoad()+"  new VL: "+toHost.getVirus().getViralLoad()); }
								e.setSuccess(true);
								if(heritable) { fromHost.getVirus().recordInfection(e.actionTime); }
						
								// S -> I
								decrementS();
								toDeme.incrementI();
								thisPopulation.addInfectedHostToList(toHost);
								
								if(toDeme.demeGroup.equals("vil")){
									System.out.println("!!Succesfully transmitted to a village! "+toDeme.toString());
									toHost.getVirus().isMigrant=true; }
								if(fromHost.getDeme().demeGroup.equals("vil"))
									System.out.println("!!Successfully infected "+toHost.toString()+" by village "+fromHost.getDeme().toString());
								
							}
							else 
								e.setSuccess(false); //no infection  
						}
					}
				}
				
			} else if ( e.getType() == EventType.RECOVERY ) {

				if (state == InfectionState.INFECTED || (state == InfectionState.TREATED && modelType == ModelType.SIRT) ) {
					if(!heritable) {
					
						toHost.setState( InfectionState.RECOVERED );
						if(heritable) { toHost.getVirus().setRecoveryTime(e.actionTime); }
						e.setSuccess(true);
						
						// I -> R
						decrementI();
						toDeme.incrementR();
						thisPopulation.removeInfectedHostFromList(toHost);
					} else {
						double recovAge = 1* toHost.getVirus().getAsymptomaticEndAge();  // when should recover/die
						double curAge = 1*( e.actionTime - toHost.getVirus().getInfectionTime() ); //how long been infected
						NormalDistribution d = new NormalDistribution(recovAge, 1.5);
						double p = d.cumulativeProbability(curAge);
						double fast = Distributions.randomUniform(); //calls MersenneTwisterFast.getDouble();

//System.out.println("curAge: "+curAge+", recovAge: "+recovAge+", prob of recov: "+p+", rand num: "+fast);						
						
						if(fast < p){   //if chance of recovery...
							toHost.setState( InfectionState.RECOVERED );
							toHost.getVirus().setRecoveryTime(e.actionTime);
							e.setSuccess(true);
							
							// I -> R
							if(state == InfectionState.INFECTED)
								decrementI();
							else if(state == InfectionState.TREATED && modelType == ModelType.SIRT)
								decrementT();
							toDeme.incrementR();
							thisPopulation.removeInfectedHostFromList(toHost);
						} else 
							e.setSuccess(false); //no recovery  
					} 
				}
			} else if ( e.getType() == EventType.TREATMENT ) {
				if(heritable){ //shouldn't happen any other way anyway... given error at beginning
					if (state == InfectionState.INFECTED ) { //event possible
						toHost.setState(InfectionState.TREATED);
						toHost.getVirus().setTreatmentTime(e.actionTime);
						e.setSuccess(true);
						
						decrementI();
						toDeme.incrementT();
						//do NOT remove infected host from list because is still infected!!! will be removed when recovered or dead
					}
				}
			} else if ( e.getType() == EventType.MIGRATION ) {
					// event possible
					// change demes
					
					// remove from this
					removeHost(toHost);
			
					toHost.myDeme = toDeme;
					
					// add to toDeme
					toDeme.addHost(toHost);
					
					e.setSuccess(true);
				//} else {
				//	System.out.println("Deme.performEvent: WARNING just tried to action inappropriate MIGRATION event (wrong Deme)");
				//}
				
			} else if (e.getType() == EventType.BIRTH ) { 
				if(gender) {
					setHostGender(toHost); //sets gender according to deme rules
					if(orientation)
						setHostOrientation(toHost); //sets orientation according to deme rules
				}
				
				addHost(toHost);
				toHost.setState(InfectionState.SUSCEPTIBLE);  //change if want to implement MtC transmission
				//incrementS(); shouldn't be needed as addHost calls countHostStates()
				
				e.setSuccess(true);
			
			} else if (e.getType() == EventType.DEATH ) {  
				
				/*
				if(fromHost.getState() == InfectionState.SUSCEPTIBLE){
					decrementS();
				} else if(fromHost.getState() == InfectionState.EXPOSED){
					decrementE();
				} else if(fromHost.getState() == InfectionState.INFECTED){
					decrementI();
				} else if(fromHost.getState() == InfectionState.RECOVERED){
					decrementR();
				} else if(fromHost.getState() == InfectionState.IMMUNE){
					decrementM();
				} else {
					System.out.println("Deme.performEvent: WARNING trying to kill host with no InfectionState");
				}
				*/ // shouldn't be needed as removeHost calls countHostStates()
				
				if(heritable && (fromHost.getState() == InfectionState.INFECTED || (fromHost.getState() == InfectionState.TREATED && modelType == ModelType.SIRT) ) ) {
					fromHost.myVirus.setRecoveryTime(e.actionTime);  //so we know how long infection lasted
				}
				if(fromHost.getState() == InfectionState.INFECTED || (fromHost.getState() == InfectionState.TREATED && modelType == ModelType.SIRT) ) {
					fromHost.setState(InfectionState.RECOVERED);  //do this so that we can identify any and all hosts that were ever 'recovered', even if by death
					thisPopulation.removeInfectedHostFromList(fromHost);
				}
				
				removeHost(fromHost);
				addToDeadHosts(fromHost);
				fromHost.kill(e.actionTime); //sets the 'alive' parameter in the Host to 'false' - also sets death time
				
				
				e.setSuccess(true);
			
			} else {
				System.out.println("Deme.performEvent: WARNING cant do event "+e.getType());
			}
			
			
		} else {
			System.out.println("Deme.performEvent: WARNING just tried to action inappropriate event (wrong Deme)");
			
		}
		
		//return newEvent;
		
	}
	
	
	// end experimental
	
	////////////////////////////////////////////////////////////////////////////////////
	// INFO METHODS
	////////////////////////////////////////////////////////////////////////////////////
	
	
	@Override
	public String toString() {
		if (name == null) {
			name = ""+uid;
		}
		return name;
		//return "Deme [name=" + name + ", uid=" + uid + "]";
	}
	
	public String toStringExtensive() {
		String info = "\t"+uid;
		for (Host h : hosts) {
			if(h!=null)
			{	info = info+"\t"+h.toString()+"\r"; }
			else info = info+"\tThere is a null host here!\r";
		}
		return info;
	}
	
	/**
	 * recounts the number of hosts in each state and returns string
	 * @return
	 */
	public String info() {
		
		countHostStates();
		
		String line = "Deme:"+name;
		
		int S = numberSusceptible();
		int E = numberExposed();
		int I = numberInfected();
		int R = numberRecovered();
		int M = numberImmune();
		int T = numberTreated();
		
		if (modelType == ModelType.SIR) {
			line = line + "\tS="+S+"\tI="+I+"\tR="+R;
		} else if (modelType == ModelType.SIRT) {
			line = line + "\tS="+S+"\tI="+I+"\tR="+R+"\tT="+T;
		} else if (modelType == ModelType.SEIR) {
			line = line + "\tS="+S+"\tE"+E+"\tI="+I+"\tR="+R;	
		} else {
			line = line + "\tS="+S+"\tE"+E+"\tI="+I+"\tR="+R+"\tM="+M;
		}
		
		return line;
	}
	
	////////////////////////////////////////////////////////////////////////////////////
	// implementation of NetworkNode interface
	
	/*
	public void 				setNetworkNeighbours(List<NetworkNode> nn) {
		for (NetworkNode n : nn) {
			if (n instanceof Deme) {
				addNeighbour((Deme)n);
			} else {
				System.out.println("Deme.setNetworkNeighbours - cant add "+n.toString());
			}
		}
	}
	
	
	public List<NetworkNode> 	getNetworkNeighbours() {
		List<NetworkNode> nn = new ArrayList<NetworkNode>();
		for (NetworkNode n : neighbours) {
			nn.add(n);
		}
		return nn;
	}
	
	public void 				addNetworkNode(NetworkNode n) {
		if (n instanceof Deme) {
			addNeighbour((Deme)n);
		} else {
			System.out.println("Deme.addNetworkNode - cant add "+n.toString());
		}
	}
	
	public void 				removeNetworkNode(NetworkNode n) {
		if (n instanceof Deme) {
			Deme d = (Deme)n;
			if (neighbours.contains(d)) {
				neighbours.remove(d);
			} else {
				System.out.println("Deme.removeNetworkNode - cant remove "+d.toString()+" not in neighbours");
			}
		} else {
			System.out.println("Deme.removeNetworkNode - cant remove "+n.toString());
		}
	}
	
	public boolean 				hasNetworkNeighbour(NetworkNode b) {
		if (b instanceof Deme) {
			Deme d = (Deme)b;
			return (neighbours.contains(d));
		} else {
			return false;
		}
		
	}
	
	public int					numberOfNeighbours() {
		return neighbours.size();
	}
	
	*/
	
	///////////////////////////////////////////////////////////////////////////////////
	// hashCode and equals on deme name only
	///////////////////////////////////////////////////////////////////////////////////

	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (obj == null) {
			return false;
		} else if (obj instanceof Deme) {
			Deme other = (Deme) obj;
			if (name == null) {
				if (other.name == null) {
					return true;
				} else {
					return false;
				}
			} else {
				return (name.equals(other.name));
			}
		} else if (obj instanceof String) {
			String other = (String)obj;
			if (name != null) {
				return ( name.equals(other) );
			} else {
				return false;
			}
		} else {
			return false;
		}
			
			
	}
	
}
