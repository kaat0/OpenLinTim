package net.lintim.model.vehiclescheduling;

import java.util.*;

public class Canal
{
	private int ID;
	private int stationID;
	private ArrayList<CEvent> events;
	private String type; // types: DRIVING, PARKING, MAINTAINING
	private HashMap<CEvent, CEvent> previousEvent;

	/*************************************************************************
  	* constructor                                                            *
 	**************************************************************************/

	public Canal(int ID, int stationID, String type)
	{
		this(ID, stationID, type, new ArrayList<CEvent>());
	}

	public Canal(int ID, int stationID, String type, ArrayList<CEvent> events)
	{
		this.ID = ID;
		this.stationID = stationID;
		this.type = type;
		this.events = events;
		previousEvent = null;
	}


	/************************************************************************
  	*  getter/setter                                                        *
 	*************************************************************************/

	public void setID(int ID)
	{
		this.ID = ID;
	}


	public int getID()
	{
		return this.ID;
	}

	public int getStationID()
	{
		return this.stationID;
	}

	public void setStationID(int stationID)
	{
		this.stationID = stationID;
	}

	public ArrayList<CEvent> getEvents()
	{
		return this.events;
	}

	public void setEvents(ArrayList<CEvent> events)
	{
		this.events = events;
		previousEvent = new HashMap<CEvent, CEvent>();

		if(events.isEmpty()) return;

		CEvent oldEvent = null;
		for(CEvent e: events) {
			previousEvent.put(e, oldEvent);
			oldEvent = e;
		}
		previousEvent.put(events.get(0), oldEvent);
	}

	public void addEvent(CEvent event)
	{
		this.events.add(event);
	}

	public void removeEvent(CEvent event)
	{
		this.events.remove(event);
	}

	public void removeAllEvents(Collection<CEvent> events)
	{
		this.events.removeAll(events);
	}

	public final CEvent getPreviousEvent(CEvent event)
	{
		return previousEvent.get(event);
	}

	public void setType(String type)
	{
		this.type = type;
	}


	public String getType()
	{
		return this.type;
	}

	// This is not an ugly trick but common practice, since the two casts will
	// really work properly
	@SuppressWarnings("unchecked")
	public Canal clone(){
		Canal newCanal = new Canal(ID, stationID, type);

		if(this.events != null){
			newCanal.events = (ArrayList<CEvent>)(this.events.clone());
		}
		if(this.previousEvent != null){
			newCanal.previousEvent = (HashMap<CEvent, CEvent>)(this.previousEvent.clone());
		}

		return newCanal;
	}
}
