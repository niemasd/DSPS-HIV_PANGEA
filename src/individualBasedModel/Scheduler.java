package individualBasedModel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import io.Logger;
import trees.*;
import math.Distributions;

import org.apache.commons.math3.*;
import org.apache.commons.math3.distribution.*;

/**
 * 
 * @author sam
 * @created 15 June 2013
 * @version 25 June 2013 - added transmission tree and sampler
 * @version 4 July 2013  - added method for tau leap
 * @version 24 July 2013 - only keep top event
 * @version 1 Sept 2013  - updated runEvents(Logger,Logger) to match other runEvents (24 July)
 * @version 26 Sept 2013 - population generates events
 * @vesrion 27 Sept 2013 - stopping criterion
 * @author Emma Hodcroft
 * @version 1 Nov 2013	 - added to stop criterion 'if have run out of events, stop, display warning'
 * @author Emma Hodcroft
 * @Version 18 March 2014 - added a parameter so that you can record all events, (successful and unsuccessful) if you want to
 * @Version 3 June 2014 - added an option 'stopWhenNoI' which means stop when there are no individuals left infected - coudlb e the same as 
 * stopWhenAllR, but also means that 5 ppl are infected and all recovered, or 5 ppl were infected but all died.
 * This works by, when no infecteds remain, removing all events from schedule except any remaining sampling events.
 */
public class Scheduler {

	protected double 		time 			= 0;
	protected List<Event> 	events			= new ArrayList<Event>();
	protected Population	thePopulation 	= new Population();
	protected TransmissionTree tt			= new TransmissionTree();
	protected Sampler		theSampler		= new JustBeforeRecoverySampler();
	
	protected String		delim			= ",";
	
	protected int			maxIts			= -1;
	protected double		maxTime			= -1;
	protected boolean		stopWhenAllI	= false;
	protected boolean		stopWhenAllR	= false;
	protected boolean		stopWhenNoI		= true; //this means stop when the infection is cleared (nobody is left infected)
	protected boolean		recordAllEvents = false;
	
//part of terrible hack
boolean hasCheckedNewMaxFile = false;
	
	public Scheduler() {
		
	}
	
	
	////////////////////////////////////////////////////////////
	
	/**
	 * @param maxIts the maxIts to set
	 */
	public void setMaxIts(int maxIts) {
		this.maxIts = maxIts;
	}


	/**
	 * @param maxTime the maxTime to set
	 */
	public void setMaxTime(double maxTime) {
		this.maxTime = maxTime;
	}


	/**
	 * @param stopWhenAllI the stopWhenAllI to set
	 */
	public void setStopWhenAllI(boolean stopWhenAllI) {
		this.stopWhenAllI = stopWhenAllI;
	}


	/**
	 * @param stopWhenAllR the stopWhenAllR to set
	 */
	public void setStopWhenAllR(boolean stopWhenAllR) {
		this.stopWhenAllR = stopWhenAllR;
	}

	/**
	 * @param allEvents the recordAllEvents to set
	 */
	public void setRecordAllEvents(boolean allEvents) {
		this.recordAllEvents = allEvents;
	}

	////////////////////////////////////////////////////////////

	public void addEvent(Event e) {
		events.add(e);
		Collections.sort(events);
	}
	
	public void addEvent(List<Event> es) {
		events.addAll(es);
		Collections.sort(events);
	}
	
	public boolean hasEvents() {
		return (events.size() > 0);
	}
	
	/**
	 * Emma Hodcroft - 12 Nov 2013 - to write out a final file for info on the viruses
	 */
	public List<String> getRecoveredVirusLifeInfo(boolean includeDead){
		return thePopulation.getRecoveredVirusLifeInfo(includeDead);
	}
	
	/**
	 * Emma Hodcroft - 12 Nov 2013 - writes out a final file for info on when viruses infected others
	 */
	public List<String> getVirusInfectionInfo(boolean includeDead){
		return thePopulation.getVirusInfectionInfo(includeDead);
	}
	
	/*
	protected Event doEvent() {
		
		// get first event from list
		Event e = events.remove(0);
		
		// perform the event
		thePopulation.performEvent(e);
		
		// update time to this event
		time	= e.getActionTime();
			
		return e;
		
	}
	*/
	
	////////////////////////////////////////////////////////////
	
	/**
	 * runs events in the list, and generates new events as appropriate
	 * @param eventLogger
	 * @param populationLogger
	 */
	// DEPCRECATED
	/*
	public void runEvents(Logger eventLogger, Logger populationLogger) {
		
		while ( events.size() > 0 ) {
			
			Event eventPerformed = doEvent();
			//System.out.println(eventPerformed);
			
			if (eventPerformed.success) {
				
				if (eventLogger != null) {
					eventLogger.recordEvent(eventPerformed);
				}
				
				if (tt != null) {
					// add sampling event to transmission tree
					tt.processEvent(eventPerformed);
				}
				
				Host h = null;
				
				if ( (eventPerformed.type == EventType.EXPOSURE) ||
					 (eventPerformed.type == EventType.INFECTION) ) {
					// add newly exposed host to active hosts population list
					h = eventPerformed.toHost;
					
					if ((h != null) && (!thePopulation.activeHosts.contains(h))) {
						thePopulation.activeHosts.add(h);
					}
					
				} else if ( eventPerformed.type == EventType.RECOVERY ) {
					// remove newly recovered host from active hosts population list					
					h = eventPerformed.toHost;
					
					if ((h != null) && (thePopulation.activeHosts.contains(h))) {
						thePopulation.activeHosts.remove(h);
					}
					
					// perge list of events from recovered hosts (which will be invalid now)
					List<Event> toRemove = new ArrayList<Event>();
					for (Event tempE : events) {
						if (tempE.fromHost.equals(h)) {
							toRemove.add(tempE);
						} else if (tempE.toHost.equals(h)) {
							toRemove.add(tempE);
						}
					}
					
					if (toRemove.size() > 0) {
						for (Event tempE : toRemove) {
							events.remove(tempE);
							//events.remove(toRemove);
						}
					}
					
				}
			
				// generate sampling events depending on sampler
				// note some samplers dont generate sampling events based on events performed
				List<Event> samplingEvents2 = theSampler.generateSamplingEvents(eventPerformed);
				if (samplingEvents2 != null) {
					events.addAll( samplingEvents2 );
				}
				
			}
			
			
			// generate new events
			// since only those active hosts may generate events, just go from active hosts list
			// rather than entire population
			List<Event> newEvents = new ArrayList<Event>();
			for (Host ah : thePopulation.activeHosts) {
				Event e2 = ah.generateNextEvent(time);
				if (e2 != null) {
					newEvents.add(e2);
				}
			}
			
			// SJL 24 July 2013
			// only keep most recent event
			if (newEvents.size() > 0) {
				Collections.sort(newEvents);
				events.add(newEvents.get(0));
			}
			
			// also generate sampling events depending on sampler
			// note some samplers dont generate population events all the time
			List<Event> samplingEvents = theSampler.generateSamplingEvents(thePopulation, time);
			if (samplingEvents != null) {
				events.addAll( samplingEvents );
			}
						
			//events.addAll(newEvents);
			
			Collections.sort(events);
			

			if (populationLogger == null) {
				System.out.println(time+"\tListI="+thePopulation.activeHosts.size()+"\tEvents="+events.size()+"\t"+thePopulation.info());
			} else {
				populationLogger.write( toOutput() );
			}
			
		}
		
	}
	*/
	
	// TO DO
	/*
	 * Population.generateEvent

	 * 
	 */
	
	
	/**
	 * runs events in the list, and generates new events as appropriate
	 * runs all events in list within leap time
	 * @param eventLogger
	 * @param populationLogger
	 */
	// DEPRECATED
	/*
	public void runEvents(Logger eventLogger, Logger populationLogger, double leap) {
		
		while ( events.size() > 0 ) {
			
		  double stopTime = time + leap;
			
		  List<Event> eventsDone = new ArrayList<Event>();
			
			// do all the events upto the leap time
		  while ( (time < stopTime) && (events.size() > 0) ) {
				eventsDone.add( doEvent() );
		  }
		 
		  if (eventsDone.size() >= 10) {
			  System.out.println("Number of events in leap = "+eventsDone.size()+" at time = "+time);
		  }
		  
		  for (Event eventPerformed : eventsDone ) {
			
			if (eventPerformed.success) {
				
				if (eventLogger != null) {
					eventLogger.recordEvent(eventPerformed);
				}
				
				if (tt != null) {
					// add sampling event to transmission tree
					tt.processEvent(eventPerformed);
				}
				
				Host h = null;
				
				if ( (eventPerformed.type == EventType.EXPOSURE) ||
					 (eventPerformed.type == EventType.INFECTION) ) {
					// add newly exposed host to active hosts population list
					h = eventPerformed.toHost;
					
					if ((h != null) && (!thePopulation.activeHosts.contains(h))) {
						thePopulation.activeHosts.add(h);
					}
					
				} else if ( eventPerformed.type == EventType.RECOVERY ) {
					// remove newly recovered host from active hosts population list					
					h = eventPerformed.toHost;
					
					if ((h != null) && (thePopulation.activeHosts.contains(h))) {
						thePopulation.activeHosts.remove(h);
					}
					
					// perge list of events from recovered hosts (which will be invalid now)
					List<Event> toRemove = new ArrayList<Event>();
					for (Event tempE : events) {
						if (tempE.fromHost.equals(h)) {
							toRemove.add(tempE);
						} else if (tempE.toHost.equals(h)) {
							toRemove.add(tempE);
						}
					}
					
					if (toRemove.size() > 0) {
						for (Event tempE : toRemove) {
							events.remove(tempE);
							//events.remove(toRemove);
						}
					}
					
				}
			
				// generate sampling events depending on sampler
				// note some samplers dont generate sampling events based on events performed
				List<Event> samplingEvents2 = theSampler.generateSamplingEvents(eventPerformed);
				if (samplingEvents2 != null) {
					events.addAll( samplingEvents2 );
				}
				
			}
			
		  }
			
			// generate new events
			// since only those active hosts may generate events, just go from active hosts list
			// rather than entire population
			List<Event> newEvents = new ArrayList<Event>();
			for (Host ah : thePopulation.activeHosts) {
				Event e2 = ah.generateNextEvent(time);
				if (e2 != null) {
					newEvents.add(e2);
				}
			}
			
			// SJL 24 July 2013
			// only keep most recent event
			if (newEvents.size() > 0) {
				Collections.sort(newEvents);
				events.add(newEvents.get(0));
			}
			
		
			events.add(thePopulation.generateEvent( time ));
			
			// also generate sampling events depending on sampler
			// note some samplers dont generate population events all the time
			List<Event> samplingEvents = theSampler.generateSamplingEvents(thePopulation, time);
			if (samplingEvents != null) {
				events.addAll( samplingEvents );
			}
			
			
			
			//events.addAll(newEvents);
			
			Collections.sort(events);
			

			if (populationLogger == null) {
				System.out.println(time+"\tListI="+thePopulation.activeHosts.size()+"\tEvents="+events.size()+"\t"+thePopulation.info());
			} else {
				populationLogger.write( toOutput() );
			}
			
		}
		
	}
	*/
	
	/**
	 * runs events in the list, and generates new events as appropriate
	 * runs all events in list within leap time
	 * 
	 * Emma Hodcroft - 1 Nov 2013 - added a stop condition that if no events remaining, stop - display warning.
	 * This happens if running in Virus mode and initial VL is too low to sustain transmission
	 * 
	 * Emma Hodcroft - 18 Mar 2014 - added a parameter so that if 'recordAllEvents' is true, it will record
	 * the event whether it is successful or not. (If false, it will only record if the event is successful, as before.)
	 * @param eventLogger
	 * @param populationLogger
	 */
	public void runEvents(Logger eventLogger, Logger populationLogger, double leap) {
		
		// temporary fix
		// make sure stopping condition is set for SI
		if ( thePopulation.getDemesModelType().equals(ModelType.SI) ) {
			setStopWhenAllI(true);
		}
		
		boolean goOn = true;
		int numIts   = 0;
		
		// 26 Aug 14 EBH - if time is MAX_VALUE then don't start treatment or check to start treatment (assuming treatment is turned off - was not assigned by user)
		//if timer is 0 , start treatment immediately, don't check to start treatment later
		//if timer is not 0, don't start treatment, check to start later
		boolean checkTreatmentStart = false;
		if(   thePopulation.getTreatmentStartTime() == Double.MAX_VALUE  ){
			//do nothing - there will be no treatment
		} else if(thePopulation.getTreatmentStartTime() == 0.0){
			Deme.setTreatment(true);
			System.out.println("*** Starting Treatment Immediately! ***");
			//checkTreatmentStart remains false
		} else {
			checkTreatmentStart = true;
			//we will start treatment later
		}
		
		//16 Dec 14 - EBH - set so that if exponential growth, check each year and turn on demes/grow demes appropriately
		boolean checkExpGrowth = false;
		double timeTicker = 1;
		if( thePopulation.isExpGrowth() == true) {
			checkExpGrowth = true;
		}
		
		
		while ( goOn ) {
		  numIts++;
			
		  //double stopTime = time + leap;
	
		  // do all the events upto the leap time
		  //List<Event> eventsDone = new ArrayList<Event>();			
		  
		  // temp remove leap
		 // while ( (time <= stopTime) && (events.size() > 0) ) {
			  
			    //Event e = doEvent();
			    //System.out.println("Just done "+e.toString());
			    
			    // get first event from list
				Event e = events.remove(0);
				
				// perform the event
				thePopulation.performEvent(e);
				
				// update time to this event
				time	= e.getActionTime();
				
				// add to done list (whether successful or not)
				//eventsDone.add( e );
				  
				
		  //}
		 
		  //if (eventsDone.size() >= 10) {
		//	  System.out.println("Number of events in leap = "+eventsDone.size()+" at time = "+time);
		  //}
		  
		  // EVENT SAMPLING
		  //for (Event eventPerformed : eventsDone ) {
			Event eventPerformed = e;
				
			if (recordAllEvents || eventPerformed.success) {
			//if (eventPerformed.success) {

				if (eventLogger != null) {
					eventLogger.recordEvent(eventPerformed);
				} //else {
					//System.out.println("Success for: "+eventPerformed.toString());
				//}
				
				if (tt != null && e.success) {
					// add event to transmission tree - if successful!
					tt.processEvent(eventPerformed);
				}
				
				// generate sampling events depending on sampler
				// note some samplers dont generate sampling events based on events performed
				List<Event> samplingEvents2 = theSampler.generateSamplingEvents(eventPerformed);
				if (samplingEvents2 != null) {
					events.addAll( samplingEvents2 );
				}
				
			}
			
//			if (eventPerformed.getType() != EventType.SAMPLING) {
			if (eventPerformed.getType() != EventType.SAMPLING && eventPerformed.planned == false) {//if was planned event (scheduled death), do not generate another event!
				// GENERATE NEW EVENT
			  	Event newEvent = thePopulation.generateEvent( time );
			  	if (newEvent != null) {
			  		events.add( newEvent );
			  	}
else { System.out.println("Generated null event at "+time+"!  (Tried to generate after "+eventPerformed.getType()+" event that was successful? "+eventPerformed.success); }
			}

			
////EBH 25 July 2014
			//if was successful recovery event... then need to generate an event so they die!
			//death is normally distributed mean 2, sd=0.5 - this value will be added to the action time of the recovery
			//This was edited so that it uses the Distributions random number generator instead of NormalDistributions
			// so that it's dependent on the seed and thus is reproducible
			if(eventPerformed.getType() == EventType.RECOVERY && e.isSuccess()){
				NormalDistribution d = new NormalDistribution(2, 0.5);
				double t, fast;
				do{
					fast = Distributions.randomUniform();
					t = d.inverseCumulativeProbability(fast);
				} while(t <= 0);
//System.out.println("time to death: "+t);
				//double t = d.sample();
				double newActionTime = e.getActionTime() + t;
				Event newE = new Event();
				newE.planned = true;
				newE.setDeathEvent(e.getToHost(), e.getActionTime(), newActionTime);
				events.add( newE );
				
			//if was other event or not successful, just generate a random event...
			} /* else if (eventPerformed.getType() != EventType.SAMPLING) {
				// GENERATE NEW EVENT
			  	Event newEvent = thePopulation.generateEvent( time );
			  	if (newEvent != null) {
			  		events.add( newEvent );
			  	}
			}  */
///////////EBH 25 July 2014
			
			
		  //}
			
		  	
			
			// also generate sampling events depending on sampler
			// note some samplers dont generate population events all the time
			List<Event> samplingEvents = theSampler.generateSamplingEvents(thePopulation, time);
			if (samplingEvents != null) {
				events.addAll( samplingEvents );
			}
			
			if (events.size() > 1) {
				Collections.sort(events);
			}
			

			if (populationLogger == null) {
				System.out.println(time+"\tListI="+thePopulation.totalInfected()+"\tEvents="+events.size()+"\t"+thePopulation.info());
			} else {
//				populationLogger.write( toOutput() );
			}
			
			// do next iteration ?
			goOn = (events.size() > 0);
			if (maxTime > 0) {
				
//** terrible hack to enable changing of the stop time while running... EBH 13 Aug 2014 */
	int intTime = (int) time;
	if(intTime%2 == 0 && hasCheckedNewMaxFile == false)
	{
		System.out.println("Checking for new max time!! (at time "+time+")");
		checkNewMaxTime();
		hasCheckedNewMaxFile = true;
	} else  if(intTime%2 == 1){
		hasCheckedNewMaxFile = false;
	}
			
// End terrible hack
	
				//26 Aug 14 - EBH - check whether to start treatment
				if(checkTreatmentStart){
					if(time >= thePopulation.getTreatmentStartTime()) { //if we are past or on the time to start treatment
						Deme.setTreatment(true);  //start treatment
						checkTreatmentStart = false; //stop checking to start treatment (because it's started)
						System.out.println("*** WE ARE STARTING TREATMENT NOW! (time: "+time+") ***");
					}
				}
				
				//16 dec 14 - EBH - if exp growth, turn on demes/grow demes as appropriate!
				if(checkExpGrowth){
					if(time >= timeTicker){ //check at end of every year - or near as we can get!
						timeTicker = timeTicker+1;
						boolean turnOnOk = thePopulation.turnOnDemes();
						if(turnOnOk== false){
							System.out.println("***NO MORE DEMES TO TURN ON! EXPONENTIAL GROWTH FAILS! SIMULATION ENDING!***");
							goOn = false;
						}
						
						thePopulation.growDemes();
					}
				}
	
				
				goOn = (time < maxTime);
			}
			if (maxIts > 0) {
				goOn = goOn && (numIts < maxIts);
			}
			if (stopWhenAllI) {
				goOn = goOn && (thePopulation.totalInfected() < thePopulation.totalHosts());
			}
			if (stopWhenAllR) {
				goOn = goOn && (thePopulation.totalRecovered() < thePopulation.totalHosts());
			}
			//if (stopWhenNoI && thePopulation.getInfectedHosts().size() == 0) { //no infecteds left - clear all events except sampling!
			if (stopWhenNoI && thePopulation.getInfectedListSize() == 0) { //no infecteds left - clear all events except sampling!
				System.out.println("****No infected individuals remain. Clearing all events except sampling.");
				
				List<Event> events2	= new ArrayList<Event>();
				while(!events.isEmpty())
				{
					Event ev = events.remove(0);
					if(ev.getType() == EventType.SAMPLING)
						events2.add(ev);
				}
				events = events2;
				if (events.size() > 1) {
					Collections.sort(events);
				}
				
			}
			
			if(events.isEmpty())
			{	
				goOn=false;
				System.out.println("*****Exited early b/c no events remaining. Time is "+time);
			}
			
		}
		
	}
	
	////////////////////////////////////////////////////////////
	
	String schedulerState() {
		String line = time + delim + thePopulation.totalInfected() + delim + events.size();
		return line;
	}
	
	String schedulerStateHeader() {
		 String line = "Time" + delim + "ActiveHosts" + delim + "Events";
		 return line;
	}
	
	public String toOutput() {
		return ( schedulerState() + delim + thePopulation.populationState());
	}
	
	public String toOutputHeader() {
		return ( schedulerStateHeader() + delim + thePopulation.populationStateHeader() );
	}
	
	public String toTransmissionTreeNexus(){
		return(tt.toNexus());
	}
	
	public String toTransmissionTreeNewick() {
		return (tt.toNewick());
	}

	//////////////////////////////////////////////////////////////////////////////
	// methods to access the population - primarily for testing

	public void setThePopulation(Population thePopulation) {
		this.thePopulation = thePopulation;
	}

	
	public Population getThePopulation() {
		return thePopulation;
	}
	
	public double getTime() {
		return time;
	}
	
	public List<Event> getEvents() {
		return events;
	}
	
	public TransmissionTree getTransmissionTree() {
		return tt;
	}
	
	//to allow that all external villages are infected before simulation proper begins
	public void infectVillages(){
		List<Host> li = thePopulation.getInfectedHosts();
		if(li.size() == 1){
			Host init = (Host)li.get(0);
			List villageDemes = thePopulation.getAllDemesFromGroup("vil");
			if(villageDemes==null){
				System.out.println("No village demes - proceeding without infecting them!");
				return;
			}
			Iterator iter = villageDemes.iterator();
			while(iter.hasNext()) { 
				Deme vil = (Deme)iter.next();
				Host recip = vil.getHost();
				Event newE = new Event();
				newE.planned = true;
				newE.setInfectionEvent(init, recip, 0.0, 0.03835616); //make it 2 weeks - so that phylo generator works!
				addEvent( newE );
			}
			
		} else {
			System.out.println("BIG PROBLEM MORE OR LESS THAN ONE INFECTED! "+li.size());
			throw new NullPointerException();
		}
		
	}
	
	///** TERRIBLE HACK TO ALLOW CHANGING OF MAXTIME WHILE RUNNING */
	public void checkNewMaxTime() {
		FileReader f;
        BufferedReader b = null;
        StringTokenizer s;

        String line, tre;

        try
        {
            f = new FileReader(new File("C:\\Users\\Room65-Admin\\Desktop\\Emma\\newMaxTime.txt"));
            b = new BufferedReader(f);
            // get the name of the system & store in it's head node
            while((line = b.readLine()) != null)
            {
                s = new StringTokenizer(line);
                try 
                {
                        tre = s.nextToken();
                        try {
                        	double newMax = Double.parseDouble(tre); 
                        	if(newMax != maxTime){
                        		maxTime = newMax;
                        		System.out.println("Have updated maxTime to "+newMax); 	}
                        }
                        catch(NumberFormatException ne){
                        	System.out.println("New time '"+tre+"' is not a number! Exiting without changes.");
                        }
                }
                catch(NoSuchElementException pe)
                {
                    System.out.println("Blank line in newMaxTime file.. exiting");
                }
            }
        }
        catch(FileNotFoundException e)
        { // if file not found, ERROR!
            System.out.println("New time File not found - exiting without changes");
            //throw new IllegalArgumentException("File not found!");
        }
        catch(IOException e)
        { // if prob reading file, ERROR!
            System.out.println("Problem Reading New time File - exiting without changes");
            //throw new IllegalArgumentException("Problem Reading File");
        }
        catch(NoSuchElementException pe)
        { // if prob tokenizing file, ERROR!
            System.out.println("New time File may not be a valid format!!- exiting without changes");
            //throw new IllegalArgumentException("File may not be a valid formats!");
        }
        finally
        {
            try
            { b.close();
            }catch(IOException e)
            { // if prob closing file, ERROR!
                System.out.println("Problem closing file");
                //throw new IllegalArgumentException("Problem closing file");
            }
            catch(NullPointerException e)
            {
                System.out.println("New time File not found while closing!");
                //throw new IllegalArgumentException("New time File not found while closing!");
            }
        }
	
	}
	
	///End terrible hack
}
