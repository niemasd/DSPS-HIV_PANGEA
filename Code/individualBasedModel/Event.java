package individualBasedModel;

/**
 * 
 * @author sam
 * @created 15 June 2013
 * @version 17 June 2013
 * @version 26 Sept 2013
 * @version 5  Oct  2013
 * @author Emma Hodcroft
 * @Version 18 March 2014 - added a parameter so that you can record all events, (successful and unsuccessful) if you want to
 * @version 3 June 2014 - EBH - Added birth/death event types and setEventType methods 
 */
public class Event implements Comparable<Event> {

	static boolean	recordAllEvents = false;
	
	protected EventType type;
	protected Host		fromHost = null;
	protected Host		toHost   = null;
	protected Deme		fromDeme = null;
	protected Deme		toDeme   = null;
	protected double	creationTime;
	protected double	actionTime;
	protected boolean	success;
	
//EBH 25 July 2014
	protected boolean 	planned;
	
	protected String	delim = ",";
	
	// constructors
	public Event() {
		planned = false;
		
	}
	
	////////////////////////////////////////////////
	// set methods
	
	/**
	 * Infection for SIR
	 * @param fromHost
	 * @param toHost
	 * @param creationTime
	 * @param actionTime
	 */
	public void setInfectionEvent(Host fromHost, Host toHost, double creationTime, double actionTime) {
		this.fromHost 		= fromHost;
		this.toHost   		= toHost;
		this.creationTime 	= creationTime;
		this.actionTime   	= actionTime;
		this.type			= EventType.INFECTION;
		

		this.fromDeme		= fromHost.myDeme;
		this.toDeme			= toHost.myDeme;
	}
	
	/**
	 * Exposure for SEIR
	 * @param fromHost
	 * @param toHost
	 * @param creationTime
	 * @param actionTime
	 */
	public void setExposureEvent(Host fromHost, Host toHost, double creationTime, double actionTime) {
		this.fromHost 		= fromHost;
		this.toHost   		= toHost;
		this.creationTime 	= creationTime;
		this.actionTime   	= actionTime;
		this.type			= EventType.EXPOSURE;
		

		this.fromDeme		= fromHost.myDeme;
		this.toDeme			= toHost.myDeme;
	}
	
	/** 
	 * Becoming Infectious after Exposure for SEIR
	 * @param theHost
	 * @param creationTime
	 * @param actionTime
	 */
	public void setBecomeInfectiousEvent(Host theHost, double creationTime, double actionTime) {
		this.fromHost 		= theHost;
		this.toHost   		= theHost;
		this.creationTime 	= creationTime;
		this.actionTime   	= actionTime;
		this.type			= EventType.INFECTION;
		
		this.fromDeme		= theHost.myDeme;
		this.toDeme			= theHost.myDeme;
	}
	
	/**
	 * Recovered after Infection for SIR or SEIR
	 * @param theHost
	 * @param creationTime
	 * @param actionTime
	 */
	public void setRecoveryEvent(Host theHost, double creationTime, double actionTime) {
		this.fromHost		= theHost;
		this.toHost			= theHost;
		this.creationTime 	= creationTime;
		this.actionTime   	= actionTime;
		this.type			= EventType.RECOVERY;
		
		this.fromDeme		= theHost.myDeme;
		this.toDeme			= theHost.myDeme;
	}
	
	/**
	 * Treatment during infection for SIRT model
	 * @param theHost
	 * @param creationTime
	 * @param actionTime
	 */
	public void setTreatmentEvent(Host theHost, double creationTime, double actionTime) {
		this.fromHost		= theHost;
		this.toHost			= theHost;
		this.creationTime 	= creationTime;
		this.actionTime   	= actionTime;
		this.type			= EventType.TREATMENT;
		
		this.fromDeme		= theHost.myDeme;
		this.toDeme			= theHost.myDeme;
	}
	
	/**
	 * Migration of one host from a deme to another deme
	 * @param theHost
	 * @param fromDeme
	 * @param toDeme
	 * @param creationTime
	 * @param actionTime
	 */
	public void setMigrationEvent(Host theHost, Deme fromDeme, Deme toDeme, double creationTime, double actionTime) {
		this.fromHost		= theHost;
		this.toHost			= theHost;
		this.fromDeme		= fromDeme;
		this.toDeme			= toDeme;
		this.creationTime 	= creationTime;
		this.actionTime   	= actionTime;
		this.type			= EventType.MIGRATION;
	}
	
	/**
	 * Death of a host
	 * EBH - 3 June 14 
	 * @param theHost
	 * @param creationTime
	 * @param actionTime
	 */
	public void setDeathEvent(Host theHost, double creationTime, double actionTime) {
		this.fromHost		= theHost;
		this.toHost			= theHost;
		this.creationTime 	= creationTime;
		this.actionTime   	= actionTime;
		this.type			= EventType.DEATH;
		
		this.fromDeme		= theHost.myDeme;
		this.toDeme			= theHost.myDeme;
	}
	
	/**
	 * Birth of a host
	 * EBH - 3 June 14 - Set up with a parent (theHost) in case in future we want to do Mother-to-child transmission
	 * (would need to set up an event for this, so it is logged appropriately!)
	 * @param theHost
	 * @param creationTime
	 * @param actionTime
	 */
	public void setBirthEvent(Host theHost, double creationTime, double actionTime) {
		this.fromHost		= theHost;
		this.toHost			= new Host(theHost.myDeme);
		this.creationTime 	= creationTime;
		this.actionTime   	= actionTime;
		this.type			= EventType.BIRTH;
		
		this.fromDeme		= theHost.myDeme;
		this.toDeme			= toHost.myDeme;
	}
	
	/**
	 * Sampling a host
	 * @param theHost
	 * @param creationTime
	 * @param actionTime
	 */
	public void setSamplingEvent(Host theHost, double creationTime, double actionTime) {
		this.fromHost		= theHost;
		this.toHost			= theHost;
		this.creationTime 	= creationTime;
		this.actionTime   	= actionTime;
		this.type			= EventType.SAMPLING;
		this.success		= true;
		
		this.fromDeme		= theHost.myDeme;
		this.toDeme			= theHost.myDeme;
	}

	/**
	 * Set success of event in Scheduler, if the event actually happened
	 * @param success
	 */
	public void setSuccess(boolean success) {
		this.success = success;
	}
	
	/**
	 * Set whether to record all events, successful or not
	 * @param recordAll
	 */
	public static void setRecordAllEvents(boolean recordAll) {
		recordAllEvents = recordAll;
	}
	
	// getters
	
	public EventType getType() {
		return type;
	}

	public Host getFromHost() {
		return fromHost;
	}

	public Host getToHost() {
		return toHost;
	}

	public Deme getFromDeme() {
		return fromDeme;
	}

	public Deme getToDeme() {
		return toDeme;
	}
	
	/**
	 * returns the Deme of the fromHost
	 * @return
	 */
	public Deme getResponsibleDeme() {
		return fromHost.myDeme;
	}
	
	/**
	 * returns the Deme of the toHost
	 * @return
	 */
	public Deme getRecipientDeme() {
		return toHost.myDeme;
	}

	public double getCreationTime() {
		return creationTime;
	}

	public double getActionTime() {
		return actionTime;
	}

	public boolean isSuccess() {
		return success;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(actionTime);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(creationTime);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Event other = (Event) obj;
		if (Double.doubleToLongBits(actionTime) != Double
				.doubleToLongBits(other.actionTime))
			return false;
		if (Double.doubleToLongBits(creationTime) != Double
				.doubleToLongBits(other.creationTime))
			return false;
		if (type != other.type)
			return false;
		return true;
	}

	@Override
	/**
	 * compares the action time of this event to another
	 * use EventTest to test whether Collections.sort has the correct effect
	 */
	public int compareTo(Event eventB) {
		
		if (this.actionTime > eventB.actionTime) {
			return 1;
		} else if (this.actionTime < eventB.actionTime) {
			return -1;
		} else {
			// if action times are the same then put sampling below the others
			if ( (type == EventType.SAMPLING) && (eventB.type != EventType.SAMPLING) ) {
				return 1;
			} else if ( ( type != EventType.SAMPLING) && (eventB.type == EventType.SAMPLING) ) {
				return -1;
			}
			
			return 0;
		}
	}

	@Override
	public String toString() {
		return "Event [type=" + type + ", fromHost=" + fromHost + ", toHost="
				+ toHost + ", fromDeme=" + fromDeme + ", toDeme=" + toDeme
				+ ", creationTime=" + creationTime + ", actionTime="
				+ actionTime + ", success=" + success + "]";
	}

	/**
	 * returns details of event in io friendly form if the event was successful
	 * @return
	 * @author Emma Hodcroft 17 March 2014
	 * Modified so that also puts out whether event was success or failure, if the recordAllEvents option is true
	 */
	public String toOutput() {
	
		if (recordAllEvents || success) {
		//if(success){
			/*
			String line = type + delim + creationTime + delim + actionTime + 
					fromDeme.getName() + "-" + fromHost.getName() + delim + 
					toDeme.getName() + "-" + toHost.getName();
			*/
			
			String line = type + delim + creationTime + delim + actionTime;
			
			if (fromDeme != null) {
				line = line + delim + fromDeme.getName() + "-";
			} else {
				line = line + delim;// + "unknown-";
			}
			
			if (fromHost != null) {
				line = line + fromHost.getName();
			} else {
				line = line + "unknown";
			}
			
			if(fromHost.hasGender()) {
				line = line + "-" + fromHost.getGender();
			}
			
//System.out.println("\tLine so far: "+line);
	if(fromHost.getVirus()!=null && fromHost.getVirus().isMigrant){
		line = line + "-MIG";
	}
			
			if (toDeme != null) {
				line = line + delim + toDeme.getName() + "-";
			} else {
				line = line + delim;// + "unknown-";
			}
			
			if (toHost != null) {
				line = line + toHost.getName();
			} else {
				line = line + "unknown";
			}
			
			if(toHost.hasGender()) {
				line = line + "-" + toHost.getGender();
			}
	if(toHost.getVirus()!=null && toHost.getVirus().isMigrant){
		line = line +"-MIG";
	}
//			if(type==EventType.SAMPLING && toHost.getVirus().isMigrant){
//				line = line + "-" + "MIG";
//			}
			
			if(recordAllEvents) {  //if recording all events, add whether event was successful
				line = line + delim + success;
			}

			return line;
		} else {
			return null;
		}
		
	}
	
	/**
	 * @author Emma Hodcroft 17 March 2014
	 * Modified so that also puts out a column for whether event was success or failure, if the recordAllEvents option is true
	 * @return
	 */
	public String toOutputHeader() {
		String line = "EventType" + delim + "CreationTime" + delim + "ActionTime" + delim +
				"FromDeme-FromHost" + delim + "ToDeme-ToHost";
		
		if(recordAllEvents){ //if recording all events, then add extra column to output
			line = line + delim + "SuccessfulEvent";
		}
		return line;
	}
	
	
}
