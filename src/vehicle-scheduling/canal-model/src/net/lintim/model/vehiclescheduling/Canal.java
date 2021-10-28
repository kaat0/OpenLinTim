package net.lintim.model.vehiclescheduling;

import java.util.*;

public class Canal {
    private final int ID;
    private final int stationID;
    private ArrayList<CEvent> events;
    private final String type; // types: DRIVING, PARKING, MAINTAINING
    private HashMap<CEvent, CEvent> previousEvent;

    /*************************************************************************
     * constructor                                                            *
     **************************************************************************/

    public Canal(int ID, int stationID, String type) {
        this(ID, stationID, type, new ArrayList<>());
    }

    public Canal(int ID, int stationID, String type, ArrayList<CEvent> events) {
        this.ID = ID;
        this.stationID = stationID;
        this.type = type;
        this.events = events;
        previousEvent = null;
    }


    /************************************************************************
     *  getter/setter                                                        *
     *************************************************************************/

    public int getID() {
        return this.ID;
    }

    public int getStationID() {
        return this.stationID;
    }

    public ArrayList<CEvent> getEvents() {
        return this.events;
    }

    public void setEvents(ArrayList<CEvent> events) {
        this.events = events;
        previousEvent = new HashMap<>();

        if (events.isEmpty()) return;

        CEvent oldEvent = null;
        for (CEvent e : events) {
            previousEvent.put(e, oldEvent);
            oldEvent = e;
        }
        previousEvent.put(events.get(0), oldEvent);
    }

    public void addEvent(CEvent event) {
        this.events.add(event);
    }

    public void removeAllEvents(Collection<CEvent> events) {
        this.events.removeAll(events);
    }

    public final CEvent getPreviousEvent(CEvent event) {
        return previousEvent.get(event);
    }


    public String getType() {
        return this.type;
    }

    // This is not an ugly trick but common practice, since the two casts will
    // really work properly
    @SuppressWarnings("unchecked")
    public Canal clone() {
        Canal newCanal = new Canal(ID, stationID, type);

        if (this.events != null) {
            newCanal.events = (ArrayList<CEvent>) (this.events.clone());
        }
        if (this.previousEvent != null) {
            newCanal.previousEvent = (HashMap<CEvent, CEvent>) (this.previousEvent.clone());
        }

        return newCanal;
    }
}
