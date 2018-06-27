package net.lintim.model;

/**
 * Enumeration of all possible event types.
 */
public enum EventType {
    ARRIVAL("\"arrival\""),
    DEPARTURE("\"departure\"");

    private String representation;

    EventType(String representation){
        this.representation = representation;
    }

    public String toString(){
        return representation;
    }
}
