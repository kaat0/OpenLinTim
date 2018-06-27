package net.lintim.model.vehiclescheduling;

public class CJourney
{
	private int ID;
	private String IDString;
	private int startStation;
	private int endStation;

	/*************************************************************************
  	* constructor                                                            *
 	**************************************************************************/

	public CJourney()
	{
	}

	public CJourney(int ID, int startStation, int endStation)
	{
		this.ID = ID;
		this.IDString = "" + ID;
		this.startStation = startStation;
		this.endStation = endStation;
	}

	/************************************************************************
  	*  getter/setter                                                        *
 	*************************************************************************/

	public int getID()
	{
		return this.ID;
	}

	public String getIDString() {
		return this.IDString;
	}

	public void setID(int ID)
	{
		this.ID = ID;
		this.IDString = "" + ID;
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
}
