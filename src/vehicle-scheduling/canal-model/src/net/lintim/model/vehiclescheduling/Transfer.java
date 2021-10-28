package net.lintim.model.vehiclescheduling;

public class Transfer {
    private final int firstTripID;
    private final int secondTripID;
    private final boolean timeCycleJump;

    /*************************************************************************
     * constructor                                                            *
     **************************************************************************/

    public Transfer(int firstTripID, int secondTripID) {
        this(firstTripID, secondTripID, false);
    }

    public Transfer(int firstTripID, int secondTripID, boolean timeCycleJump) {
        this.firstTripID = firstTripID;
        this.secondTripID = secondTripID;
        this.timeCycleJump = timeCycleJump;
    }


    /************************************************************************
     *  getter/setter                                                        *
     *************************************************************************/

    public int getFirstTripID() {
        return this.firstTripID;
    }

    public int getSecondTripID() {
        return this.secondTripID;
    }

    public boolean getValueTimeCycleJump() {
        return this.timeCycleJump;
    }

}
