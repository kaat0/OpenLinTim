package net.lintim.model.vehiclescheduling;

public class Event
{
	static public final String TYPE_START = "START";
	static public final String TYPE_END = "END";

	private int ID; // unique ID, for the given Events the "startID" or the "endID" of the CTrip,
			// for the canal events continous ID's beginning with 2*trips.size()+1
	private int time;
	private String type; // "START" or "END"
	private String moselIndex;
	// the next attribute only is to calculate the output-file in a format
	// like "Trips.giv"
	private int periodicID;

	/*************************************************************************
  	* constructor                                                            *
 	**************************************************************************/

	public Event() { }

	public Event(int ID, int time, String type)
	{
		this.ID = ID;
		this.time = time;
		assert(type == TYPE_START || type == TYPE_END);
		this.type = type;
	}

	public Event(int ID, int time, String type, int periodicID)
	{
		this.ID = ID;
		this.time = time;
		assert(type == TYPE_START || type == TYPE_END);
		this.type = type;
		this.periodicID = periodicID;
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

	public int getTime()
	{
		return this.time;
	}

	public void setTime(int time)
	{
		this.time = time;
	}

	public void setType(String type)
	{
		assert(type == TYPE_START || type == TYPE_END);
		this.type = type;
	}

	public String getType()
	{
		return this.type;
	}

	public final void setMoselIndex(String index) {
		moselIndex = index;
	}

	public final String getMoselIndex() {
		return moselIndex;
	}

	public int getPeriodicID()
	{
		return this.periodicID;
	}

	public void setPeriodicID(int periodicID)
	{
		this.periodicID = periodicID;
	}

	/************************************************************************
  	*  print-methods                                                        *
 	*************************************************************************/

	public void print(Event event)
	{
		System.out.print(ID + "; " + time + "; " + type);
	}

}
