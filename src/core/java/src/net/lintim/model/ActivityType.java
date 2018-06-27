package net.lintim.model;

/**
 * Enumeration of all possible activity types.
 */
public enum ActivityType {
    DRIVE("\"drive\""),
    WAIT("\"wait\""),
    CHANGE("\"change\""),
    TURNAROUND("\"turnaround\""),
    HEADWAY("\"headway\""),
    SYNC("\"sync\"");

    private String representation;

    ActivityType(String representation){
        this.representation = representation;
    }

    public String toString(){
        return representation;
    }
}
