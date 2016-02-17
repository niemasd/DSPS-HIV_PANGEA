package individualBasedModel;

import java.util.*;

import math.Distributions;
import io.*;

/**
 * Population class - this contains the demes
 * @author Samantha Lycett
 * @version 1 July 2013
 * @version 24 July 2013
 * @version 27 Sept 2013
 * @version 3 June 2014 - EBH - Added birth/death events/calculations
 * @version 5 Aug 14 - EBH started adding a way to group demes that determines migration params
 */
public class Population {

	private	  int totalHosts 		 = -1;
	private   List<Deme> demes		 = new ArrayList<Deme>();
	//protected List<Host> activeHosts = new ArrayList<Host>();
	private String	 delim		 	 = ",";
	
	private PopulationType popType 	 = PopulationType.FULL;
	private double	 pedge		 	 = 1;						// probability of edge between demes
																// only applicable for popType=RANDOM
	private boolean	 directed	 	 = false;					// only applicable for popType=NETWORK at moment
	
	private List<Parameter> all_deme_params 	= new ArrayList<Parameter>();
																// parameters to apply to all demes
	
	private EventGenerator eventGenerator 	= new EventGenerator();
	
	// 5 Aug 14 - EBH modifcations so that can specify demeType to determine migration params
	private HashMap<String, List<Deme>> demeGroupHash = new HashMap<String, List<Deme>>();
	private boolean usingDemeGroups 	= false;
	
	//25 Aug 14 - EBH - add list of infected (active) hosts so that we can track more efficiently when to end programme...
	private 	List<Host> infectedHosts 	= new ArrayList<Host>();
	
	//26 Aug 14 - EBH - adding timer so can turn on treatment at some specified time
	protected double			treatmentStartTime 	= Double.MAX_VALUE;
	
	//16 Dec 14 - EBH - added some thing so can try exponential growth: including boolean flag, exp rate, list of 'off' demes, and list of demes to grow
	protected boolean			expGrowth = false;
	protected double			expGrowthRate;
	private List<Deme> 			offDemes = new ArrayList<Deme>();
	private List<Deme>			autoGrowDemes = new ArrayList<Deme>();
	
	
	public Population() {
		
	}
	
	public Population(String demeGroups)	{
		usingDemeGroups = true;
		String[] dt = demeGroups.split(",");
		for(int i=0; i<dt.length; i++){
			dt[i] = dt[i].trim();
		}
		
		//put a list of demes into the hash so hash of each demeGroup will lead to that list
		for(int i=0; i<dt.length; i++){
			demeGroupHash.put(dt[i], new ArrayList<Deme>() );
		}
	}
	
	public String info() {
		String line = "Population:";
		for (Deme d : demes ) {
			line = line + "-" + d.info();
		}
		return line;
	}
	
	///////////////////////////////////////////////////////////////////////////
	// methods for io
	
	/**
	 * returns the host states of each deme, in a format suitable for io
	 * @return
	 */
	public String populationState() {
		
		String line = "";
		
		for (int i = 0; i < demes.size(); i++) {
			Deme d		  = demes.get(i);
			int[] hstates = d.hostStates();
			
			String tempLine = "" + hstates[0];
			for (int j = 1; j < hstates.length; j++) {
				tempLine = tempLine + delim + hstates[j];
			}
			
			if (i==0) {
				line = tempLine;
			} else {
				line = line + delim + tempLine;
			}
		}
		
		return line;
	}
	
	/**
	 * returns the column headers for the host states in each deme, in a format suitable for io
	 * @return
	 */
	public String populationStateHeader() {
		
		String line = "";
		
		for (int i = 0; i < demes.size(); i++) {
			Deme d			  	= demes.get(i);
			String[] hstates 	= d.hostStatesHeader();
			
			String tempLine = "" + hstates[0];
			for (int j = 1; j < hstates.length; j++) {
				tempLine = tempLine + delim + hstates[j];
			}
			
			if (i==0) {
				line = tempLine;
			} else {
				line = line + delim + tempLine;
			}
		}
		
		return line;
		
	}
	
	/**
	 * Emma Hodcroft - 12 Nov 2013 - to write out a final file for info on the viruses
	 */
	public List<String> getRecoveredVirusLifeInfo(boolean includeDead){
		List<Host> recovered = getRecoveredHosts(includeDead);
		List<String> result = new ArrayList<String>();
		result.add("Host,VL,Infected,Recovered,Infect.Duration,Num.Infected");
		
		for (Host o: recovered) {
		    result.add(""+o.getName()+","+o.getVirus().getVirusLifeInfo());
		}
		
		return result;
		
	}
	
	/**
	 * Emma Hodcroft - 12 Nov 2013 - writes out a final file for info on when viruses infected others
	 */
	public List<String> getVirusInfectionInfo(boolean includeDead){
		List<Host> recovered = getRecoveredHosts(includeDead);
		List<String> result = new ArrayList<String>();
		result.add("Host,VL,Infect.Duration,Transmit.Time");
		
		for(Host o: recovered){
			result.addAll(o.getVirus().getVirusTransmissionInfo());
		}
		
		return result;
	}

	//////////////////////////////////////////////////////////////////////////////////
	// total population state information methods
	
	/**
	 * returns sum of all infected in each deme
	 */
	public int totalInfected() {
		int I = 0;
		for (Deme d : demes) {
			I += d.numberInfected();
		}
		return I;
	}
	
	/**
	 * returns sum of all exposed in each deme
	 * @return
	 */
	public int totalExposed() {
		int E = 0;
		for (Deme d : demes) {
			E += d.numberExposed();
		}
		return E;
	}
	
	/**
	 * returns sum of all exposed in each deme
	 * @return
	 */
	public int totalRecovered() {
		int R = 0;
		for (Deme d : demes) {
			R += d.numberRecovered();
		}
		return R;
	}
	
	/**
	 * returns the total number of hosts in all demes
	 * this is not expected to change, so is calculated once then re-used
	 * 
	 * For birth/death models this will not be accurate!!
	 * @return
	 */
	public int totalHosts() {
		if ( totalHosts <= 0) {
			totalHosts = 0;
			for (Deme d : demes) {
				totalHosts += d.numberOfHosts;
			}
		}
		return totalHosts;
	}
	
	/**
	 * returns sum of all in each class
	 * @return
	 */
	public int[] totalStates() {
		int[] hs = demes.get(0).getSEIRM();
		
		for (int i = 1; i < demes.size(); i++) {
			int[] hs2 = demes.get(i).getSEIRM();
			
			for (int j = 0; j < hs.length; j++) {
				hs[j] = hs[j] + hs2[j];
			}
			
		}
		
		return hs;
	}
	
	/////////////////////////////////////////////////////////////////////
	// access methods for demes
	
	public void setDemes(List<Deme> demes) {
		this.demes = demes;
	}

	public void addDeme(Deme deme) {
		if (this.demes == null) {
			this.demes = new ArrayList<Deme>();
		}
		
		//if have exponential growth, look out for 'off' demes and 'auto growth' demes
		if(expGrowth==true) {
			if(deme.demeOn==false){
				offDemes.add(deme);
			}
			if(deme.maxNumberOfHosts==Integer.MAX_VALUE) { //is an auto grow deme
				autoGrowDemes.add(deme);
				deme.maxNumberOfHosts = deme.initialNumberOfHosts;
			}
		}
		
		//if we're using demeGroups, add each deme to the appropriate group
		if (this.usingDemeGroups && deme.demeOn==true){
			if(demeGroupHash.get(deme.demeGroup)== null ) {
				demeGroupHash.put(deme.demeGroup, new ArrayList<Deme>());
System.out.println("deme group hash List is null for "+deme.demeGroup);
			}
			demeGroupHash.get(deme.demeGroup).add(deme);
		}
		
		if (!this.demes.contains(deme)) {
			this.demes.add(deme);
		} else {
			System.out.println("Population.addDeme - sorry cant add Deme "+deme.getName()+" because already in list");
		}
	}
	
	/**
	 * 16 Dec 14 - EBH - 
	 * calculates how many demes should be turned on this year according to the growth rate, and tries to turn on that many (randomly chosen)
	 * if there are no demes left to turn off, it returns false which will stop simulation because exponential growth has failed
	 */
	public boolean turnOnDemes()
	{
System.out.println("There are "+demes.size()+" demes, and "+(demes.size()-offDemes.size())+" are on.");

		int numOnDemes = demes.size() - offDemes.size();
		int toTurnOn = (int) Math.round((double)numOnDemes * expGrowthRate); 
System.out.print("I am going to turn on "+toTurnOn+" demes.\n");
		while(toTurnOn > 0){
			if(offDemes.size()==0)
				return false; //there are no more demes to turn on! something wrong! stop simulation!
			Deme d = offDemes.remove(Distributions.randomInt(offDemes.size()));
			d.demeOn = true;
			//now add to deme group if using groups
			if (this.usingDemeGroups){
				if(demeGroupHash.get(d.demeGroup)== null ) {
					demeGroupHash.put(d.demeGroup, new ArrayList<Deme>());
				}
				demeGroupHash.get(d.demeGroup).add(d);
			}
			toTurnOn = toTurnOn-1;
		}
		
		return true;
	}
	
	/**
	 * 16 Dec 14 - EBH - 
	 * finds all demes that are being grown automatically in size, and calcualtes by how much they should increase, and then increases the size by that much
	 */
	public void growDemes()
	{
		for (Deme d : autoGrowDemes ) {
			if(d.demeOn == true){
				int toAdd = (int) Math.round((double)d.maxNumberOfHosts * expGrowthRate); 
				d.maxNumberOfHosts = d.maxNumberOfHosts+toAdd;
System.out.println("I am growing deme "+d.name+" by "+toAdd+" hosts, to become "+d.maxNumberOfHosts);
			}
		}
	}
	
	/**
	 * For debug - just reports on how many deme groups there are, their keys, and how many demes in each
	 * 6 Aug 2014 - EBH
	 */
	public void reportDemeGroups() {
		System.out.println("****Deme Group Report****");
		System.out.println("\tWe are using deme groups? " + this.usingDemeGroups);
		int siz = demeGroupHash.size();
		System.out.println("\tWe have " + siz + " deme groups");
		
		String p = "";
		String nums = "";
		Iterator iter = demeGroupHash.keySet().iterator();
		while(iter.hasNext()) { 
			String key = (String) iter.next();
			p = p+key+"|";
			nums = nums + demeGroupHash.get(key).size()+ " ";

		}
		System.out.println("\tThey are: |" + p);
		System.out.println("\tThey each contain: "+nums);
		System.out.println("****End Deme Group Report****");
		
	}
	
	/**
	 * adds one infected host into the first deme
	 */
	public void setIndexCaseFirstDeme() {
		Deme d = demes.get(0);
		d.setIndexCase();
	}
	
	/**
	 * same as above, but allows initial viral load to be set by user
	 * @param initVL
	 * @author Emma Hodcroft - 28 Mar 2014
	 */
	public void setIndexCaseFirstDeme(double initVL){
		Deme d = demes.get(0);
		d.setIndexCase(initVL);
	}
	
	public void setIndexCaseAnyDeme() {
		if (demes.size() > 1) {
		int choice = Distributions.randomInt(demes.size());
				
		Deme d = demes.get(choice);
		d.setIndexCase();
		} else {
			setIndexCaseFirstDeme();
		}
	}
	
	/**
	 * same as above but allows initial viral load to be set by user
	 * @param initVL
	 * @author Emma Hodcroft - 28 Mar 2014
	 */
	public void setIndexCaseAnyDeme(double initVL) {
		if (demes.size() > 1) {
		int choice = Distributions.randomInt(demes.size());
				
		Deme d = demes.get(choice);
		d.setIndexCase(initVL);
		} else {
			setIndexCaseFirstDeme(initVL);
		}
	}
	
	/** start infection in SW deme... for testing only **/
/////	
	public void setIndexCaseSWDeme(double initVL) {
		int choice = Distributions.randomInt(demes.size());
		Deme d = demes.get(choice);
		while( !d.demeGroup.equals("sw")) {
			choice = Distributions.randomInt(demes.size());
			d = demes.get(choice);
		}
		d.setIndexCase(initVL);
	}
	
	public void setExpGrowthRate(double expG)
	{
		expGrowth = true;
		expGrowthRate = expG;
	}
	
	public boolean isExpGrowth() {
		return expGrowth;
	}
	
	public double getExpGrowthRate() {
		if(expGrowth==true)
			return expGrowthRate;
		else
			return 0.0;
	}
	
	public List<Deme> getDemes() {
		return demes;
	}
	
	public double getTreatmentStartTime() {
		return treatmentStartTime;
	}
	
	/**
	 * 6 Aug 2014 - EBH
	 * Returns a deme selected from the specified group of demes, which is not this deme
	 * called from Deme
	 * @param groupKey
	 * @return
	 */
	public Deme getDemeFromGroup (String groupKey, Deme notThisDeme){
		if(!demeGroupHash.containsKey(groupKey)){
			System.out.println("ERROR: Deme Group "+groupKey+" not found!!");
			return null;
		}
		
		List<Deme> temp = demeGroupHash.get(groupKey);

		Deme d = temp.get(Distributions.randomInt(temp.size()));
		while( d.equals(notThisDeme) ){
			d = temp.get(Distributions.randomInt(temp.size()));
		}

		return d;
	}
	
	/**
	 * 2 Sept 2014 - EBH
	 * Returns all demes in a specified group
	 * @param groupKey
	 */
	public List getAllDemesFromGroup (String groupKey){
		if(!demeGroupHash.containsKey(groupKey)){
			System.out.println("ERROR: Deme Group "+groupKey+" not found!!");
			return null;
		}
		
		return demeGroupHash.get(groupKey);
	}
	
	/*
	 * adds infected Host to the pop list to keep it updated
	 */
	public void addInfectedHostToList(Host h) {
		if(!infectedHosts.contains(h)){
			infectedHosts.add(h);
		} else {
			System.out.println("ERROR: Host is already infected and trying to be re-added to list!!");
			throw new NullPointerException();
		}
	}
	
	/*
	 * removes infected Host from the pop list to keep it updated
	 */
	public void removeInfectedHostFromList(Host h){
		if(infectedHosts.contains(h)){
			infectedHosts.remove(h);
		} else {
			System.out.println("ERROR: Host is NOT infected and trying to be removed from list!!");
			throw new NullPointerException();
		}
	}
	
	public int getInfectedListSize(){
		return infectedHosts.size();
		
	}
	
	
	public List<Host> getInfectedHosts() {
		
		List<Host> activeHosts = new ArrayList<Host>();
		for (Deme d : demes) {
			activeHosts.addAll(d.getInfectedHosts());
		}
		
		return activeHosts;
	}
	
	/**
	 * Emma Hodcroft 12 Nov 13 - gets all recovered hosts (used for getting info about viruses at the end)
	 * this proably does not do what you think it does anymore.... (Aug 2014)
	 * @return
	 */
	public List<Host> getRecoveredHosts(boolean includeDead) {
		
		List<Host> recoveredHosts = new ArrayList<Host>();
		for (Deme d : demes) {
			recoveredHosts.addAll(d.getRecoveredHosts(includeDead));
		}
		
		return recoveredHosts;
	}
	
	/**
	 * returns the model type of the first deme (and assumes that all the demes have the same model type)
	 * @return
	 */
	public ModelType getDemesModelType() {
		return ( demes.get(0).modelType );
	}
	
	/**
	 * returns the deme type of the first deme (and assumes that all the demes have the same deme type)
	 * @return
	 */
	public DemeType getDemeType() {
		return ( demes.get(0).demeType );
	}
	
	////////////////////////////////////////////////////////////////////
	// POPULATION STRUCTURE
	// methods for linking demes together
	
	/**
	 * set one way link between deme1 and deme2 ( deme1 -> deme2 )
	 * @param demeName1
	 * @param demeName2
	 */
	private void setDirectedLink(String demeName1, String demeName2) {
		int i = demes.indexOf(demeName1);
		int j = demes.indexOf(demeName2);
		Deme deme1 = demes.get(i);
		Deme deme2 = demes.get(j);
		deme1.addNeighbour(deme2);
	}
	
	/**
	 * set two way link between deme1 and deme2 ( deme1 <-> deme2 )
	 * @param demeName1
	 * @param demeName2
	 */
	private void setLink(String demeName1, String demeName2) {
		int i = demes.indexOf(demeName1);
		int j = demes.indexOf(demeName2);
		Deme deme1 = demes.get(i);
		Deme deme2 = demes.get(j);
		deme1.addNeighbour(deme2);
		deme2.addNeighbour(deme1);
	}
	
	/**
	 * all demes connected to all demes (two connectivity)
	 */
	private void setFullConnectivity() {
		pedge = 1;
		for (int i = 0; i < (demes.size()-1); i++) {
			for (int j = (i+1); j < demes.size(); j++) {
				Deme deme1 = demes.get(i);
				Deme deme2 = demes.get(j);
				deme1.addNeighbour(deme2);
				deme2.addNeighbour(deme1);
			}
		}
	}
	
	/**
	 * two way connections, 0 - 1 - 2 - 3 - 4 etc
	 */
	private void setLineConnectivity() {
		pedge = 1;
		for (int i = 0; i < (demes.size()-1); i++) {
			int j 	   = i+1;
			Deme deme1 = demes.get(i);
			Deme deme2 = demes.get(j);
			deme1.addNeighbour(deme2);
			deme2.addNeighbour(deme1);
		}
	}
	
	/**
	 * add demes connected to the first one (two way connectivity), 0 - 1, 0 - 2, 0 - 3 etc
	 */
	private void setStarConnectivity() {
		pedge = 1;
		Deme deme0 = demes.get(0);
		for (int i = 1; i < demes.size(); i++) {
			Deme demei = demes.get(i);
			deme0.addNeighbour(demei);
			demei.addNeighbour(deme0);
		}
	}
	
	/**
	 * connect pairs of demes with probability pedge (two way connectivity)
	 */
	private void setRandomConnectivity() {		
		for (int i = 0; i < (demes.size()-1); i++) {
			for (int j = (i+1); j < demes.size(); j++) {
				double x = Distributions.randomUniform();
				if (x <= pedge) {
					Deme deme1 = demes.get(i);
					Deme deme2 = demes.get(j);
					deme1.addNeighbour(deme2);
					deme2.addNeighbour(deme1);
				}
			}
		}
	}
	
	/**
	 * sets network structure according to popType and pedge
	 * called from setPopulationStructure(List<Parameter> ps)
	 */
	private void setNetworkStructure(List<String[]> demePairs) {
		if (popType.equals(PopulationType.FULL)) {
			System.out.println("***Connecting all demes fully.***");
			setFullConnectivity();
		} else if (popType.equals(PopulationType.LINE)) {
			setLineConnectivity();
		} else if (popType.equals(PopulationType.STAR)) {
			setStarConnectivity();
		} else if (popType.equals(PopulationType.RANDOM)) {
			setRandomConnectivity();
		} else if (popType.equals(PopulationType.NETWORK)) {
			for (String[] demeNames : demePairs) {
				if (directed) {
					setDirectedLink(demeNames[0], demeNames[1]);
				} else {
					setLink(demeNames[0], demeNames[1]);
				}
			}
		} else {
			System.out.println("Population.setNetworkStructure - sorry dont understand type "+popType);
		}
	}
	
	public void setPopulationStructure(List<Parameter> ps) {
		//System.out.println("** Population.setPopulationStructure - NOT IMPLEMENTED YET **");
		List<String[]> demePairs = null;
		
		for (Parameter p : ps) {
			if (p.getId().equals("NetworkType")) {
				popType = PopulationType.valueOf( p.getValue() );
				
			} else if (p.getId().equals("ProbabilityConnect")) {
				pedge   = Double.parseDouble( p.getValue() );
				
			} else if (p.getId().equals("Link")) {
				String[] demeNames = p.getValue().split(",");
				if (demePairs == null) {
					demePairs = new ArrayList<String[]>();
				}
				demePairs.add(demeNames);
				
			} else if (p.getId().equals("TreatmentTimer")) {
				treatmentStartTime = Double.parseDouble(p.getValue().trim() );
				
			} else if (p.getId().equals("Directed")) {
				directed = Boolean.parseBoolean(p.getValue().toLowerCase());
				
			} else if (p.getId().equals("DemeType")) {
				all_deme_params.add(p);
				
			} else if (p.getId().equals("NumberOfHostsPerDeme")) {
				all_deme_params.add(p);
				
			} else if (p.getId().equals("MaxHostsPerDeme")) {
				all_deme_params.add(p);
				
			} else if (p.getId().equals("ModelType")) {
				all_deme_params.add(p);
				
			} else if (p.getId().equals("InfectionParameters")) {
				all_deme_params.add(p);
				
			} else if (p.getId().equals("BirthDeathParameters")) { 
				all_deme_params.add(p);
				
			} else if (p.getId().equals("BirthDeathType")) {
				all_deme_params.add(p);
				
			} else if (p.getId().equals("NumberOfMaleFemaleHosts")) {
				all_deme_params.add(p);
				
			} else if (p.getId().equals("OrientationChoice")) {
				all_deme_params.add(p);
				
			} else if (p.getId().equals("TreatmentParameter")) {
				all_deme_params.add(p);
				
			} else if (p.getId().equals("ProbabilityInfectionAnyOtherDeme")) {
				all_deme_params.add(p);
				
			} else if (p.getId().equals("ProbabilityMigrationAnyOtherDeme")) {
				all_deme_params.add(p);
				
			} else if (p.getId().equals("ProbabilityInfectionScaledByDemeSize")) {
				System.out.println("Population.setPopulationStructure - Not implemented yet "+p.getId()+" "+p.getValue());
				
			} else {
				System.out.println("Population.setPopulationStructure - sorry dont understand "+p);
			}
		}
		
		setNetworkStructure(demePairs);
	}
	
	/**
	 * sets population structure from deme neighbours if applicable and 
	 * also sets the deme parameters which are the same for all demes
	 */
	public void setPopulationStructure() {
		for (Deme deme : demes) {
			deme.setNeighbours(this);
			
			if (all_deme_params.size() > 0) {
				deme.setDemeParameters(all_deme_params);
			}
		}
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////
	// EXPERIMENTAL 24 Sept 2013
	// Demes are generating own hazards
	// Population gets list of all hazards
	
	private List<Hazard> allHazards() {
		List<Hazard> h = new ArrayList<Hazard>();
		
		for (Deme d : demes) {
			Hazard demeHazard = d.generateHazards();
			//System.out.println("Hazard: "+demeHazard.getMyDeme()+" "+demeHazard.getTotalHazard());
			h.add(demeHazard);
		}
		
		return h;
	}
	
	/**
	 * Generate an event from the Population
	 * @param currentTime
	 * @return
	 */
	public Event generateEvent(double currentTime) {
		Event e					= eventGenerator.generateEvent(allHazards(), currentTime);
		
		/*
		if (e != null) {
			System.out.println("Population.generateEvent: "+e.toString());
		} else {
			System.out.println("Population.generateEvent: no event");
		}
		*/
		
		return e;
	}
	
	
	/**
	 * perform an Event - will farm out to Demes
	 * @param e
	 */
	protected void performEvent(Event e) {
		
		// try to do this event
		if (e == null) {
			System.out.println("NULL EVENT");
			
		} else if (e.getType() == EventType.SAMPLING) {
			// set success = true to allow the event to be processed in runEvents
			e.success = true;
			
		} else if (e.getType() == EventType.MIGRATION) {
			System.out.println("MIGRATION not implemented yet");
				
		} else {
				
			//Host actor 		= e.getToHost();
			//actor.performEvent(e);
			
			Deme responsibleDeme = e.getResponsibleDeme();
			//responsibleDeme.performEvent(e);
			

//if all is well, perform the event
if(responsibleDeme != null && e.getFromHost().isAlive()){
	responsibleDeme.performEvent(e);
	
	//if the person doing the event has died before this new event happens...
	//do nothing, because this is ok.
} else if(responsibleDeme == null && !e.getFromHost().isAlive()){
	System.out.println("***ZOMBIE ALERT*** at time "+e.getActionTime()+" trying to do event "+e.getType()+". Host recovered at "+e.getFromHost().getVirus().getRecoveryTime()+", Host died at "+e.getFromHost().getDeathTime()+", event was created at "+e.creationTime);
	
	//if deme is null BUT person is still alive - this is bad, so try to execute so that Exception is thrown.
} else {
	System.out.println("RESPONSIBLE DEME IS NULL AND HOST IS NOT DEAD!");
	responsibleDeme.performEvent(e);  
}

			
		}
		
		//return e;
		
	}
	
}
