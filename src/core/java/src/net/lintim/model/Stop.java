package net.lintim.model;

import java.util.Arrays;

/**
 * Template implementation of a stop in a PTN.
 */
public class Stop implements Node {
    protected int stopId;
    protected String shortName;
    protected String longName;
    protected double xCoordinate;
    protected double yCoordinate;
    protected boolean station;

	/**
	 * Create a new Stop given the information of a LinTim stop.
	 * @param stopId the id of the stop. Needs to be unique for any graph, this stop may be part of.
	 * @param shortName the short name of the stop. This is a short representation of the stop. Need not be unique.
	 * @param longName the long name of the stop. This is a longer representation of the stop.
	 * @param xCoordinate the x-coordinate of the stop. This should be the longitude coordinate of the stop. Using
     *                       the euclidean distance on the coordinates of the stop should result in a length given in
     *                       kilometers.
	 * @param yCoordinate the y-coordinate of the stop. This should be the latitude coordinate of the stop. Using
     *                       the euclidean distance on the coordinates of the stop should result in a length given in
     *                       kilometers.
	 */
	public Stop(int stopId, String shortName, String longName, double xCoordinate, double yCoordinate) {
		this.stopId = stopId;
		this.shortName = shortName;
		this.longName = longName;
		this.xCoordinate = xCoordinate;
		this.yCoordinate = yCoordinate;
		this.station = true;
	}

	public int getId() {
		return stopId;
	}

	public void setId(int id) {
		this.stopId = id;
	}

	/**
	 * Get the short name of the stop. This is a short representation of the stop. Need not be unique.
	 * @return the short name
	 */
	public String getShortName() {
		return shortName;
	}

	/**
	 * Get the long name of the stop. This is a longer representation of the stop.
	 * @return the long name
	 */
	public String getLongName() {
		return longName;
	}

	/**
	 * Get the x-coordinate of the stop. This should be the longitude coordinate of the stop.  Using
     * the euclidean distance on the coordinates of the stop should result in a length given in kilometers.
	 * @return the x-coordinate
	 */
	public double getxCoordinate() {
		return xCoordinate;
	}

	/**
	 * Get the y-coordinate of the stop. This should be the latitude coordinate of the stop. Using the euclidean
     * distance on the coordinates of the stop should result in a length given in kilometers.
	 * @return the y-coordinate
	 */
	public double getyCoordinate() {
		return yCoordinate;
	}

	/**
	 * Get whether this stop is actually a station. E.g. it may be the case that the stop is just a candidate in a
	 * stop location problem and not a built station (at least at the time of creation). Is initially set to true.
	 * @return whether the stop is a station
	 */
	public boolean isStation() {
		return station;
	}

	/**
	 * Set whether this stop is actually a station. E.g. it may be the case that the stop is just a candidate in a
	 * stop location problem and not a built station (at least at the time of creation).
	 * @param isStation the new value
	 */
	public void setStation(boolean isStation) {
		this.station = isStation;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Stop stop = (Stop) o;

		if (stopId != stop.stopId) return false;
		if (Double.compare(stop.getxCoordinate(), getxCoordinate()) != 0) return false;
		if (Double.compare(stop.getyCoordinate(), getyCoordinate()) != 0) return false;
		if (getShortName() != null ? !getShortName().equals(stop.getShortName()) : stop.getShortName() != null)
			return false;
		return getLongName() != null ? getLongName().equals(stop.getLongName()) : stop.getLongName() == null;
	}

	@Override
	public int hashCode() {
		int result;
		long temp;
		result = stopId;
		result = 31 * result + (getShortName() != null ? getShortName().hashCode() : 0);
		result = 31 * result + (getLongName() != null ? getLongName().hashCode() : 0);
		temp = Double.doubleToLongBits(getxCoordinate());
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(getyCoordinate());
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public String toString() {
		return "Stop " + Arrays.toString(toCsvStrings());
	}

    /**
     * Return a string array, representing the stop for a LinTim csv file.
     * @return the csv representation of this stop
     */
	public String[] toCsvStrings(){
	    return new String[] {
	        String.valueOf(getId()),
            getShortName(),
            getLongName(),
            String.valueOf(getxCoordinate()),
            String.valueOf(getyCoordinate())
        };
    }
}
