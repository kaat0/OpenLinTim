package net.lintim.model.vehiclescheduling;

public class CEvent extends Event implements Comparable<CEvent> {
    private final CJourney journey;
    // canal-id

    /*************************************************************************
     * constructor                                                            *
     **************************************************************************/

    public CEvent(CJourney journey, int ID, int time, String type) {
        this.journey = journey;
        super.setID(ID);
        super.setTime(time);
        super.setType(type);
    }


    /************************************************************************
     *  getter/setter                                                        *
     *************************************************************************/


    public CJourney getJourney() {
        return this.journey;
    }

    public int compareTo(CEvent other) {
        // first compare the time values
        if (other.getTime() > this.getTime()) {
            return -1;
        } else if (other.getTime() < this.getTime()) {
            return 1;
        }
        // second compare the ID's of the events with different types
        else if (!other.getType().equals(this.getType())) {
            if (other.getType().equals(Event.TYPE_START) && this.getType().equals(Event.TYPE_END)) {
                return Integer.compare(other.getID(), this.getID());
            } else if (other.getType().equals("END") && this.getType().equals("START")) {
                return Integer.compare(this.getID(), other.getID());
            } else {
                return 0; // FIXME: This branch should not be reached, but it needs a return-statement
            }
        }
        // third compare the ID's of the assigned journeys (in this case, it holds "other.getType().equals(this.getType())"!
        else return Integer.compare(this.journey.getID(), other.getJourney().getID());
    }

}
