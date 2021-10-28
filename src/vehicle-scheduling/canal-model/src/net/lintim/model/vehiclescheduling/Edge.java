package net.lintim.model.vehiclescheduling;

public class Edge {
    private final int leftStopID;
    private final int rightStopID;
    private final int length;

    /*************************************************************************
     * constructor                                                            *
     **************************************************************************/

    public Edge(int leftStopID, int rightStopID, int length) {
        this.leftStopID = leftStopID;
        this.rightStopID = rightStopID;
        this.length = length;
    }


    /************************************************************************
     *  getter/setter                                                        *
     *************************************************************************/

    public int getLeftStopID() {
        return this.leftStopID;
    }

    public int getRightStopID() {
        return this.rightStopID;
    }

    public int getLength() {
        return this.length;
    }

}
