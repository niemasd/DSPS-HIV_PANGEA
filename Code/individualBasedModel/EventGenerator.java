package individualBasedModel;

import java.util.*;

import math.Distributions;

/**
 * class to generate events from list of hazards
 * @author Samantha Lycett
 * @created 24 Sept 2013
 * 
 */
public class EventGenerator {

	public EventGenerator() {
		
	}
	
	public Event generateEvent(List<Hazard> hazards, double currentTime) {
		
		if (hazards.size()  > 1) {
		
			// find time to any event
			double rateAny	 		= 0;
			
			double[] demeRates		= new double[hazards.size()];
			for (int i = 0; i < hazards.size(); i++) {
				Hazard h			=  hazards.get(i);
				double demeHazard 	=  h.getTotalHazard();
				rateAny 			+= demeHazard;
				demeRates[i]		= demeHazard;
//System.out.println(""+h.myDeme+"  "+demeRates[i]);
			}
			
		
			if (rateAny > 0) {
			
				double dt		  = -Math.log( Distributions.randomUniform() )/rateAny;
				double actionTime = currentTime + dt;
		
				// who is doing the event ?
				int demeIndex	  = Distributions.chooseWithWeights(demeRates, rateAny);
				
			/*
			if ((demeIndex < 0) || ( demeIndex >= hazards.size() )) {
				System.out.println("EventGenerator.generateEvent - why have you choosen an out of bounds hazard ?");
				System.out.println("RateAny = "+rateAny);
				System.out.println("DemeIndex = "+demeIndex);
				System.out.println("dt = "+dt);
				for (Hazard h : hazards) {
					System.out.println(h.getMyDeme().info());
				}
			}
			*/
			
				// generate the Event for this Deme
				Hazard selectedHazard 	= hazards.get(demeIndex);
				Deme selectedDeme 		= selectedHazard.getMyDeme();
				
/*if(selectedDeme.uid == 3187) {				
System.out.println("The selected deme had hazard: "+demeRates[demeIndex]+", total hazard is: "+selectedHazard.getTotalHazard()
		+"\r\t Birth hazard is: "+selectedHazard.getBirthHazard()
		+"\r\t Death hazard is: "+selectedHazard.getDeathHazard()
		+"\r\t Infection hazard is: "+selectedHazard.getInfectOtherHazard()
		+"\r\t Recovery hazard is: "+selectedHazard.getRecoveryHazard()
		+"\r\t Treatment hazard is: "+selectedHazard.getTreatmentHazard()
		+"\r\t Migration hazard is: "+selectedHazard.getMigrationHazard()
		+"\r\t Exposed hazard is: "+selectedHazard.getExposedToInfectedHazard());
System.out.println("Hazards for 20 random other demes: "+demeRates[1141]+" "+demeRates[1472]+" "+demeRates[738]+" "+demeRates[597]+" "
		+demeRates[430]+" "+demeRates[2732]+" "+demeRates[1402]+" "+demeRates[1928]+" "+demeRates[795]+" "+demeRates[3943]+" "
		+demeRates[2543]+" "+demeRates[825]+" "+demeRates[3208]+" "+demeRates[355]+" "+demeRates[2053]+" "+demeRates[2147]+" "
		+demeRates[3446]+" "+demeRates[2505]+" "+demeRates[87]+" "+demeRates[1903]+" "); 
}	*/			
//System.out.println("Selected deme is: "+selectedDeme+" with index: "+demeIndex);
				// generate event
				Event e			  		= selectedDeme.generateEvent(selectedHazard, currentTime, actionTime);
			
if(e == null) { System.out.println("Event is still null at generation in EventGenerator!"); }
				return e;
			} else {
				return null;
			}
		
		} else if (hazards.size() == 1) {
			
			Hazard h = hazards.get(0);
			return generateEvent(h, currentTime);
			
		} else {
			
			System.out.println("EventGenerator.generateEvent - WARNING NO EVENT GENERATED");
			return null;
		}
		
	}
	
	public Event generateEvent(Hazard h, double currentTime) {
		
		// find time to any event
		double rateAny			= h.getTotalHazard();
		
		double dt		  		= -Math.log( Distributions.randomUniform() )/rateAny;
		double actionTime 		= currentTime + dt;
	
		// get Deme for this hazard
		Deme selectedDeme 		= h.getMyDeme();
		
		// generate event
		Event e			  		= selectedDeme.generateEvent(h, currentTime, actionTime);
		
		return e;
		
	}
	
}
