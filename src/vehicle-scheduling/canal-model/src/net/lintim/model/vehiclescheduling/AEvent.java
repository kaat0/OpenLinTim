package net.lintim.model.vehiclescheduling;

public class AEvent extends Event {

    /*************************************************************************
     * constructor                                                            *
     **************************************************************************/

    public AEvent(int ID, int time, String type) {
        super.setID(ID);
        super.setTime(time);
        super.setType(type);
    }
}
