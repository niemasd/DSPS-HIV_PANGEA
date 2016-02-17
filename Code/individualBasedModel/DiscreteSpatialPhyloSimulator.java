package individualBasedModel;

import io.*;
import math.Distributions;
import java.util.*;

/**
 * Main class
 * @author sam
 * @created 19 June 2013
 * @version 25 June 2013
 * @version 1 July 2013
 * @version 24 July 2013 - connections between demes in progress
 * @version 6  Sept 2013
 * @version 27 Sept 2013
 * @version 3  Oct  2013 - SI and SIR are working
 * @author Emma Hodcroft
 * @version 1 Nov 2013 - added options to set Max run time, and set tree output (newick of nexus)
 * @version 12 Nov 2013 - added option to set heritable/virus run from XML as well as type of run (acute or chronic)
 * @version 18 Mar 2014 - added option to set whether to record ALL events from XML (both unsuccessful and successful)
 * @version 3 June 2014 - EBH - Added birth/death events/calculations
 * @version 9 June 2014 - EBH - add a way to specify whether gender is turned on in XML
 * @version 5 Aug 14 - EBH started adding a way to group demes that determines migration params
 */

public class DiscreteSpatialPhyloSimulator {
	
	//////////////////////////////////////////////////////////////////////////////////
	// class variables
	
	public final static String 				  version 		= "DiscreteSpatialPhyloSimulator - 3 Oct 2013";
	protected 	 static List<List<Parameter>> params;		// from configuration XML
	
	protected static String		path 	 					= "test//";
	protected static String 	rootname 					= "test";
	protected static int		nreps	 					= 1;
	protected static int		repCounter		 			= 0;
	protected static long		seed;
	protected static double		tauleap						= 0;
	protected static double		maxTime						= -1;
	protected static boolean 	newick						= true; //default output trees are newick format
	protected static boolean	heritable					= false; // default do not use Virus/heritability stuff
	protected static double		initialViralLoad			= 0; //default VL to 0 - allows to set initial VL - only works if heritability is true!
	protected static boolean	recordAllEvents				= false; //default to only recording successful events - set from XML
	protected static boolean 	gender						= false; //default to not using gender
	protected static boolean	orientation					= false; //default to not using orientation
	
	protected static boolean	usingDemeGroups				= false; //default to not using deme groups - if set uses deme group to set up neighbors & migration
	protected static String		demeGroups					= null; //holds the demeGroups to pass to Population
	
	protected static boolean	expGrowth					= true; //true if using exponential growth
	protected static double		expGrowthRate;				//holds value for at what rate population will grow

	/////////////////////////////////////////////////////////////////////////////////
	// instance variables
	
	protected Scheduler theScheduler;
	
	protected Logger	populationLogger;
	protected Logger	eventLogger;
	protected Logger	logFile;
	protected int		rep;
	
	
	////////////////////////////////////////////////////////////////////////
	// constructors
	
	public DiscreteSpatialPhyloSimulator() {
		//seed = Distributions.initialise();
		rep	   		 = getNextRep();
		theScheduler = new Scheduler();
		if(recordAllEvents){
			theScheduler.setRecordAllEvents(recordAllEvents);
		}
	}

	/*
	public DiscreteSpatialPhyloSimulator(int seed) {
		Distributions.initialiseWithSeed(seed);
		this.seed = (long)seed;
		
		theScheduler = new Scheduler();
	}
	*/
		
	///////////////////////////////////////////////////////////////////
	// logging methods
	
	/**
	 * initialise loggers - must be done after the rest of scheduler set up
	 */
	private void initialiseLoggers() {
		
		System.out.println("* Initialising loggers *");
		
		/*
		 * Population log
		 */
		populationLogger = new Logger(path, rootname + "_" +rep + "_popLog", ".csv");
		populationLogger.write(theScheduler.toOutputHeader());
		populationLogger.setEchoEvery(100);
		
		/*
		 * event log
		 */
List<EventType> et = new ArrayList<EventType>(); 
et.add(EventType.INFECTION); et.add(EventType.RECOVERY);
et.add(EventType.DEATH); et.add(EventType.BIRTH); et.add(EventType.TREATMENT); et.add(EventType.SAMPLING);

		if (theScheduler.thePopulation.getDemesModelType() == ModelType.SIR || theScheduler.thePopulation.getDemesModelType() == ModelType.SIRT) {
			eventLogger		 = new Logger(path, rootname + "_" +rep + "_infectionEventLog", ".csv", et);//EventType.INFECTION);
		} else if (theScheduler.thePopulation.getDemesModelType() == ModelType.SEIR) {
			eventLogger		 = new Logger(path, rootname + "_" +rep + "_exposureEventLog", ".csv", EventType.EXPOSURE);	
		} else {
			eventLogger		 = new Logger(path, rootname + "_" +rep + "_eventLog", ".csv");
		}
		eventLogger.setEchoEvery(0);
		
		System.out.println("** WARNING NOT IMPLEMENTED MIGRATION LOGGING YET **");
		
	}
	
	/**
	 * add maxtime and newickoutput
	 * @author Emma Hodcroft 
	 * 1 Nov 2013
	 * @author Emma Hodcroft
	 * 19 Mar 2014 - added recordAllEvents parameter log
	 * 9 June 2014 - added Gender parameter log
	 */
	private void writeParametersLog() {
		/*
		 * Parameters log 
		 */
		logFile			 = new Logger(path, rootname + "_" +rep+ "_params_log", ".xml");
		logFile.setEchoEvery(0);
		
		logFile.write("<General>");
		List<String[]> simParams = new ArrayList<String[]>();
		simParams.add(new String[]{"Seed",""+seed});
		simParams.add(new String[]{"Path",path});
		simParams.add(new String[]{"Rootname",rootname});
		simParams.add(new String[]{"Nreps",""+nreps});
		simParams.add(new String[]{"Rep",""+rep});
		simParams.add(new String[]{"Tauleap",""+tauleap});
		simParams.add(new String[]{"MaxTime",""+maxTime});
		simParams.add(new String[]{"NewickOutput",""+newick});
		if(heritable==true) {
			simParams.add(new String[]{"Heritability",""+Virus.heritability});
			simParams.add(new String[]{"ViralLoad",""+initialViralLoad});
		}
		simParams.add(new String[]{"RecordAllEvents", ""+recordAllEvents});
		simParams.add(new String[]{"GenderUsed", ""+gender});
		simParams.add(new String[]{"OrientationUsed", ""+orientation});
		simParams.add(new String[]{"ExponentialGrowth",""+expGrowth});
		if(expGrowth==true) {
			simParams.add(new String[]{"ExponentialGrowthRate",""+expGrowthRate});
		}
		logFile.writeParametersXML(simParams,1);
		logFile.write("</General>");
		
		logFile.write("<Demes>");
//if(theScheduler.thePopulation.getDemes().size() > 1000){
//	logFile.write("\tOver 1000 demes - will not write them to save time.");
//} else {
		//for (Deme d : theScheduler.thePopulation.getDemes() ) {
Deme d = theScheduler.thePopulation.getDemes().get(1);
			logFile.write("\t<Deme>");

			List<String[]> params = d.getDemeParameterList();
			logFile.writeParametersXML(params,2);
			logFile.write("\t</Deme>");
		//}  
//}
		logFile.write("</Demes>");
		
		logFile.write("<Sampler>");
		List<String[]> samplingParams = theScheduler.theSampler.getSamplerParameterList();
		logFile.writeParametersXML(samplingParams,1);
		logFile.write("</Sampler>");
		//logFile.closeFile();
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////
	// set up and run methods
	
	/**
	 * set up an example deme for a simple test
	 */
	void setUpExampleDeme() {
		
		
		System.out.println("* Set up example deme *");
		System.out.println("- make one deme of 100 SIR -");
		Deme deme1 = new Deme();
		deme1.setDemeType(DemeType.MIGRATION_OF_INFECTEDS);
		deme1.setModelType(ModelType.SIR);
		deme1.setHosts(100);
		
		double[] infectionParameters = { 0.1, 0.05 };
		deme1.setInfectionParameters(infectionParameters);
		
		theScheduler.setThePopulation(new Population());
		theScheduler.thePopulation.addDeme(deme1);
		
		System.out.println("- infect the first host -");
		if(heritable && initialViralLoad != 0)
			theScheduler.thePopulation.setIndexCaseFirstDeme(initialViralLoad);  // @author Emma Hodcroft - 28 Mar 2014
		else
			theScheduler.thePopulation.setIndexCaseFirstDeme();
		
		System.out.println("- generate the first event and add to Scheduler -");
		
		// make sure it is an infection event for this test
		Event e = theScheduler.thePopulation.generateEvent(theScheduler.time);
		while (e.type != EventType.INFECTION) {
			e = theScheduler.thePopulation.generateEvent(theScheduler.time);
		}
		
		
		theScheduler.addEvent(e);
		//theScheduler.thePopulation.activeHosts.add(h);
		
		System.out.println("First event in Scheduler:");
		System.out.println(theScheduler.events.get(0).toString());
		
		rootname = "oneExampleDeme";

		initialiseLoggers();
		writeParametersLog();
		
		populationLogger.write(theScheduler.toOutput());
		
	}
	
	
	/**
	 * method to initialise simulation from parameters read from xml file
	 * note that this should be called for each separate simulation
	 * Emma Hodcroft 12 nov 13 - changes so that Demes had heritability/virus turned on if applicable
	 */
	public void initialise() {
		Population thePopulation;
		if(usingDemeGroups == true){
			thePopulation 	 = new Population(demeGroups); 
		} else {
			thePopulation	 = new Population();
		}
		
		if(expGrowth == true){
			thePopulation.setExpGrowthRate(expGrowthRate);
		}
		
		// create set of demes and sampler
		for (List<Parameter> ps : params) {
			Parameter firstP = ps.get(0);
			
			if (firstP.getParentTag().equals("Deme")) {
				if(usingDemeGroups){   //give link to population so that can get random demes from deme groups!
					thePopulation.addDeme( DemeFactory.getDeme(ps, thePopulation) );
				} else {
					thePopulation.addDeme( DemeFactory.getDeme(ps) );
				}
				
			} else if (firstP.getParentTag().equals("Sampler")) {
				theScheduler.theSampler = SamplerFactory.getSampler(ps);
				
			}
		}
		
		if(heritable){
			List<Deme> demes = thePopulation.getDemes();
			for(Deme d : demes){
				d.setHeritable(heritable);
			}
		}
		
thePopulation.reportDemeGroups();
		
		System.out.println("- connecting demes together in a network -");
		
		
		// link demes together to fully specify population
		// get parameters from xml file
		for (List<Parameter> ps : params) {
			Parameter firstP = ps.get(0);
			
			if (firstP.getParentTag().equals("PopulationStructure")) {
				thePopulation.setPopulationStructure(ps);
			}
		}
		
		// finish off setting the population
		// this might include setting neighbours from demes
		thePopulation.setPopulationStructure();
		
		// add population to scheduler
		theScheduler.setThePopulation(thePopulation);
		
		if(thePopulation.getTreatmentStartTime() != Double.MAX_VALUE && heritable==false){ //if have tried to turn treatment on but heritable is false, warn!
			System.out.println("*** ERROR!!!: Using TREATMENT without heritability/viral load being on will result in TREATMENT having no effect! ***");
			throw new NullPointerException();
		}
		
		System.out.println("** Replicate "+rep+" of "+nreps+" **");
		//System.out.println("- infect the first host -");
		//theScheduler.thePopulation.setIndexCaseAnyDeme();
		
		System.out.println("- infect the first host -");
		if(heritable && initialViralLoad != 0)
			theScheduler.thePopulation.setIndexCaseSWDeme(initialViralLoad);  //EBH 13 Aug 14
			//theScheduler.thePopulation.setIndexCaseAnyDeme(initialViralLoad);  // @author Emma Hodcroft - 28 Mar 2014
		else
			theScheduler.thePopulation.setIndexCaseAnyDeme();
		
		System.out.println("- generate the first infection event and add to Scheduler -");
		
		// make sure it is an infection event for this test
		Event e = theScheduler.thePopulation.generateEvent(theScheduler.time);
		while (e.type != EventType.INFECTION) {
			e = theScheduler.thePopulation.generateEvent(theScheduler.time);
		}
		theScheduler.infectVillages();
		
		theScheduler.addEvent(e);

		//theScheduler.thePopulation.activeHosts.add(h);
		
		System.out.println("First event in Scheduler:");
		System.out.println(theScheduler.events.get(0).toString());
		
		initialiseLoggers();
System.out.println("Finished initializing loggers");
		writeParametersLog();
		
System.out.println("Finished writing params log");
		populationLogger.write(theScheduler.toOutput());
System.out.println("Finished writing schedular output");
		
	}
	
	public void run() {
		System.out.println("- run events -");
		if (tauleap <= 0) {
			theScheduler.runEvents(eventLogger, populationLogger, 0);
		} else {
			theScheduler.runEvents(eventLogger, populationLogger, tauleap);
		}
		System.out.println("- end state -");
		System.out.println(theScheduler.toOutput());
	}
	
	/**
	 * Emma Hodcroft - 1 Nov 2013 - does option for tree output in newick or nexus
	 * Emma Hodcroft - 12 Nov 2013 - now outputs life and transmission information for each recovered(sampled) virus
	 */
	public void finish() {
		populationLogger.closeFile();
		eventLogger.closeFile();
		logFile.closeFileWithStamp();
		
		System.out.println("* Write transmission trees to file *");
		PrunedTreeWriter treesOut = new PrunedTreeWriter(path + rootname + "_" +rep, theScheduler.tt);
		if(newick == true)
			treesOut.writeNewickTrees();
		else
			treesOut.writeNexusTrees();
		
		if(heritable){
			writeVirusLifeInfo();
			writeVirusInfectInfo();
		}
	}
	
	/**
	 * Emma Hodcroft - 12 Nov 2013 - writes out a final file for info on the life of viruses
	 * @return
	 */
	public void writeVirusLifeInfo(){
		Logger vir = new Logger(path, "virusLifeLog", ".csv");
		List<String>info = theScheduler.getRecoveredVirusLifeInfo(true); //change to FALSE if you don't want info on DEAD hosts
		
		vir.write(info);
		
		vir.closeFile();
		
	}
	
	/**
	 * Emma Hodcroft - 12 Nov 2013 - writes out a final file for info on when viruses infected others
	 * @return
	 */
	public void writeVirusInfectInfo(){
		Logger vir = new Logger(path, "virusInfectionLog", ".csv");
		List<String>info = theScheduler.getVirusInfectionInfo(true); //change to FALSE if you don't want info on DEAD hosts
		
		vir.write(info);
		
		vir.closeFile();
	}
	
	////////////////////////////////////////////////////////////////
	// class methods
	
	private int getNextRep() {
		repCounter++;
		return repCounter;
	}
	
	/**
	 * Emma Hodcroft - 1 Nov 2013 - added options to read in 'MaxTime' and 'OutputTrees' to specify limit of run by 
	 * max time, and also specify what kind of Output tree to output (nexus or Newick)
	 * 
	 * Emma Hodcroft - 18 Mar 2014 - added options to read in whether to record ALL events. If set to true, it will record
	 * ALL events in the event long - whether unsuccessful or successful
	 * @param xmlName
	 */
	public static void readParametersFromXML(String xmlName) {
		ParameterReader pr 			 = new ParameterReader(xmlName);
		params 						 = pr.getParameters();
		
		// set global parameters
		for (List<Parameter> ps : params) {
			if (ps.get(0).getParentTag().equals("General")) {
				for (Parameter p : ps) {
					if (p.getId().equals("Seed")) {				
						seed = Long.parseLong(p.getValue());
						Distributions.initialiseWithSeed((int)seed);
					} else if (p.getId().equals("Rootname")) {
						rootname = p.getValue();
					} else if (p.getId().equals("Path")) {
						path = p.getValue();
					} else if (p.getId().equals("Nreps")) {
						nreps = Integer.parseInt(p.getValue());
					} else if (p.getId().equals("Tauleap")) {
						tauleap = Double.parseDouble(p.getValue());
					} else if (p.getId().equals("MaxTime")) { 
						maxTime = Double.parseDouble(p.getValue());
					} else if (p.getId().equals("OutputTrees")) { 
						newick = (p.getValue().equals("Newick")|p.getValue().equals("newick"));
					} else if (p.getId().equals("Heritability")) {
						Virus.heritability = Double.parseDouble(p.getValue());
						heritable = true;
					} else if (p.getId().equals("ViralLoad")) {
						if(heritable == true)
							initialViralLoad = Double.parseDouble(p.getValue());
					} else if (p.getId().equals("RecordAllEvents")){
						recordAllEvents = (p.getValue().equals("TRUE")|p.getValue().equals("True")|p.getValue().equals("true"));
						Event.setRecordAllEvents(recordAllEvents);
					} else if (p.getId().equals("Gender")){
						gender = (p.getValue().equals("TRUE")|p.getValue().equals("True")|p.getValue().equals("true"));
						Deme.setGender(gender);
					} else if (p.getId().equals("Orientation")) {
						orientation = (p.getValue().equals("TRUE")|p.getValue().equals("True")|p.getValue().equals("true"));
						if(gender == true) //can only be used if gender is true!
							Deme.setOrientation(orientation);
						else
							System.out.println("WARNING! Can only use Orientation if Gender is set to true!");
					} else if (p.getId().equals("DemeGroups")) {
						usingDemeGroups = true;
						demeGroups = p.getValue().toLowerCase();
						Deme.setUsingDemeTypes(true);
					} else if (p.getId().equals("ExpGrowthRate")) {
						expGrowth = true;
						expGrowthRate = Double.parseDouble(p.getValue()); 
					} else {
						System.out.println("DiscreteSpatialPhyoSimulator.readParametersFromXML - sorry couldnt understand "+p.getId()+" "+p.getValue());
					}
				}
			}
		}
	}
	
	/**
	 * example for testing
	 */
	static void doExample() {
		
		long t1 = System.currentTimeMillis();
		
		seed = 283397407;
		Distributions.initialiseWithSeed((int)seed);
		
		DiscreteSpatialPhyloSimulator dsps = new DiscreteSpatialPhyloSimulator();
		dsps.setUpExampleDeme();
		dsps.run();
		dsps.finish();
		
		long t2 = System.currentTimeMillis();
		System.out.println("* This example took = "+(t2-t1)+" milli seconds *");
		
		System.out.println("* End Example *");
	}

	
	static void exampleFromXML() {
		//String xmlName = "test//deme1000_tau0_params.xml";
		String xmlName = "test//example_structure_params.xml";
		runFromXML(xmlName);
	}
	
	static void validation() {
		repCounter = 0;
		runFromXML("test//simpleSI_params.xml");
		repCounter = 0;
		runFromXML("test//simpleSIR_params.xml");
	}
	
	static void runFromXML(String xmlName) {
		readParametersFromXML(xmlName);
		
		for (int i = 0; i < nreps; i++) {

			long t1 = System.currentTimeMillis();
			
			DiscreteSpatialPhyloSimulator dsps = new DiscreteSpatialPhyloSimulator();
			if(maxTime != -1)
				dsps.theScheduler.setMaxTime(maxTime);
			dsps.initialise();
			dsps.run();
			dsps.finish();
			
			Host.resetHostCounter();
			Deme.resetDemeCounter();
			
			long t2 = System.currentTimeMillis();
			System.out.println("* This replicate took = "+(t2-t1)+" milli seconds *");
			
		}
		
	}
	
	/**
	 * method to run DSPS from xml file (enter filename)
	 * @param args
	 */
	public static void run(String[] args) {
		
		if ( (args == null) || (args.length == 0) ) {
			Scanner keyboard = new Scanner(System.in);
			boolean again 	 = true;
			while (again) {
				System.out.println("Please enter configuration parameter file xml, include // path separators and .xml extension:");
				System.out.println("e.g. test//simpleSIR_params.xml:");
				String xmlName = keyboard.nextLine().trim();
				if (xmlName.equals("validation")) {
					validation();
					again = false;
				} else if (!xmlName.equals("x")) {
					runFromXML(xmlName);
				
					System.out.println("Again ? (y/n)");
					String ans = keyboard.nextLine().trim().toLowerCase();
					if (ans.startsWith("y")) {
						again = true;
					} else {
						again = false;
					}
				} else {
					again = false;
				}
			}
			keyboard.close();
			
		} else {
			if (args[0].equals("validation")) {
				validation();
			} else {
				for (String xmlName : args) {
					runFromXML(xmlName);
				}
			}
		}
		
	}
	
	public static void main(String[] args) {
		System.out.println("** DiscreteSpatialPhyloSimulator **");
		
		//doExample();
		//exampleFromXML();
		run(args);
		
		System.out.println("** END **");
	}
	
	
}
