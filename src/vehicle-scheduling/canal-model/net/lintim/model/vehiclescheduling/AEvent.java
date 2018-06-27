package net.lintim.model.vehiclescheduling;

public class AEvent extends Event
{
	private CTrip trip;
	private int station;

	/*************************************************************************
  	* constructor                                                            *
 	**************************************************************************/

	public AEvent()
	{
	}

	public AEvent(CTrip trip, int ID, int time, int station, String type)
	{
		this.trip = trip;
		super.setID(ID);
		super.setTime(time);
		this.station = station;
		super.setType(type);
	}

	public AEvent(CTrip trip, int ID, int time, int station, String type, int periodicID)
	{
		this.trip = trip;
		super.setID(ID);
		super.setTime(time);
		this.station = station;
		super.setType(type);
		super.setPeriodicID(periodicID);
	}



	/************************************************************************
  	*  getter/setter                                                        *
 	*************************************************************************/

	public void setJourney(CTrip trip)
	{
		this.trip = trip;
	}


	public CTrip getJourney()
	{
		return this.trip;
	}

	public int getStation()
	{
		return this.station;
	}

	public void setStation(int station)
	{
		this.station = station;
	}
}
