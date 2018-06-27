package net.lintim.model.vehiclescheduling;

public class Trip
{
	private int ID;
	private int startID;
	private int endID;
	private int startStation;
	private int endStation;
	private int startTime;
	private int endTime;
	// the next attributes only is to calculate the output-file in a format
	// like "Trips.giv"
	private int lineID;
	private int periodicStartID;
	private int periodicEndID;

	/*************************************************************************
  	* constructor                                                            *
 	**************************************************************************/

	public Trip(){

	}

	public Trip(int ID, int startID, int endID, int startStation, int endStation, int startTime, int endTime)
	{
		this(ID, startID, endID, startStation, endStation, startTime, endTime, 0, 0, 0);
	}

	public Trip(int ID, int startID, int endID, int startStation, int endStation, int startTime, int endTime, int lineID, int periodicStartID, int periodicEndID)
	{
		setID(ID);
		this.startID = startID;
		this.endID = endID;
		setStartStation(startStation);
		setEndStation(endStation);
		this.startTime = startTime;
		this.endTime = endTime;
		this.lineID = lineID;
		this.periodicStartID = periodicStartID;
		this.periodicEndID = periodicEndID;
	}



	/************************************************************************
  	*  getter/setter                                                        *
 	*************************************************************************/

	public int getID()
	{
		return this.ID;
	}

	public void setID(int ID)
	{
		this.ID = ID;
	}

	public int getStartID()
	{
		return this.startID;
	}

	public void setStartID(int startID)
	{
		this.startID = startID;
	}

	public int getEndID()
	{
		return this.endID;
	}

	public void setEndID(int endID)
	{
		this.endID = endID;
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

	public int getStartTime()
	{
		return this.startTime;
	}

	public void setStartTime(int startTime)
	{
		this.startTime = startTime;
	}

	public int getEndTime()
	{
		return this.endTime;
	}

	public void setEndTime(int endTime)
	{
		this.endTime = endTime;
	}

	public int getLineID()
	{
		return this.lineID;
	}

	public void setLineID(int lineID)
	{
		this.lineID = lineID;
	}

	public int getPeriodicStartID()
	{
		return this.periodicStartID;
	}

	public void setPeriodicStartID(int periodicStartID)
	{
		this.periodicStartID = periodicStartID;
	}

	public int getPeriodicEndID()
	{
		return this.periodicEndID;
	}

	public void setPeriodicEndID(int periodicEndID)
	{
		this.periodicEndID = periodicEndID;
	}
}
