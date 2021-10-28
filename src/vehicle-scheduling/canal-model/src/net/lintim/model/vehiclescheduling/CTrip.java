package net.lintim.model.vehiclescheduling;

public class CTrip extends CJourney {
    private final int startID;
    private final int endID;
    private final int startTime;
    private final int endTime;
    // the next attributes only is to calculate the output-file in a format
    // like "Trips.giv"
    private final int lineID;
    private final int periodicStartID;
    private final int periodicEndID;

    /*************************************************************************
     * constructor                                                            *
     **************************************************************************/

    public CTrip(int ID, int startID, int endID, int startStation, int endStation, int startTime, int endTime, int lineID, int periodicStartID, int periodicEndID) {
        this.ID = ID;
        this.startID = startID;
        this.endID = endID;
        setStartStation(startStation);
        setEndStation(endStation);
        this.startTime = startTime;
        this.endTime = endTime;
        this.lineID = lineID;
        this.periodicStartID = periodicStartID;
        this.periodicEndID = periodicEndID;
    }


    /************************************************************************
     *  getter/setter                                                        *
     *************************************************************************/

    public int getStartID() {
        return this.startID;
    }

    public int getEndID() {
        return this.endID;
    }

    public int getStartTime() {
        return this.startTime;
    }

    public int getEndTime() {
        return this.endTime;
    }

    public int getLineID() {
        return this.lineID;
    }

    public int getPeriodicStartID() {
        return this.periodicStartID;
    }

    public int getPeriodicEndID() {
        return this.periodicEndID;
    }
}
