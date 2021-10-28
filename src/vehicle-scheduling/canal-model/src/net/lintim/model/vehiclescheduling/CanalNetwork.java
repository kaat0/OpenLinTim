package net.lintim.model.vehiclescheduling;

import java.util.*;

public class CanalNetwork {
    private ArrayList<CTrip> trips;
    private ArrayList<CTransfer> transfers;
    private Canal[] canals;

    /*************************************************************************
     * constructor                                                            *
     **************************************************************************/

    public CanalNetwork() {
    }

    /************************************************************************
     *  getter/setter                                                        *
     *************************************************************************/

    public ArrayList<CTrip> getTrips() {
        return this.trips;
    }

    public void setTrips(ArrayList<CTrip> trips) {
        this.trips = trips;
    }

    public ArrayList<CTransfer> getTransfers() {
        return this.transfers;
    }

    public void setTransfers(ArrayList<CTransfer> transfers) {
        this.transfers = transfers;
    }

    public Canal[] getCanals() {
        return this.canals;
    }

    public void setCanals(Canal[] canals) {
        this.canals = canals;
    }

    public int getTotalNumberOfCanalEvents() {
        int sum = 0;

        for (Canal canal : canals) {
            if (canal == null) continue;
            sum += canal.getEvents().size();
        }

        return sum;
    }

}
