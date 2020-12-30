package net.lintim.model.vehiclescheduling;

import java.util.Objects;

public class CJourney
{
	int ID;
	private int startStation;
	private int endStation;

	/*************************************************************************
  	* constructor                                                            *
 	**************************************************************************/

	public CJourney()
	{
	}

	/************************************************************************
  	*  getter/setter                                                        *
 	*************************************************************************/

	public int getID()
	{
		return this.ID;
	}

	public String getIDString() {
	    return "" + ID;
	}

	public int getStartStation()
	{
		return this.startStation;
	}

	public void setStartStation(int startStation)
	{
		this.startStation = startStation;
	}



	public int getEndStation()
	{
		return this.endStation;
	}

	public void setEndStation(int endStation)
	{
		this.endStation = endStation;
	}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CJourney cJourney = (CJourney) o;
        return ID == cJourney.ID &&
            startStation == cJourney.startStation &&
            endStation == cJourney.endStation;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ID);
    }
}
