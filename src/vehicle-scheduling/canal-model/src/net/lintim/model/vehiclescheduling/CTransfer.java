package net.lintim.model.vehiclescheduling;

public class CTransfer extends CJourney
{
	private Event startEvent;
	private Event endEvent;
	private final int costs;
	private final boolean timeCycleJump;
	private final String type; // this could be "TRIP", "EMPTY", "PARKING" and "MAINTAINING"

	/*************************************************************************
  	* constructor                                                            *
 	**************************************************************************/

	public CTransfer(int ID, Event startEvent, Event endEvent, int costs, String type, boolean timeCycleJump)
	{
        this.ID = ID;
		this.startEvent = startEvent;
		this.endEvent = endEvent;
		this.costs = costs;
		this.type = type;
		this.timeCycleJump = timeCycleJump;
	}



	/************************************************************************
  	*  getter/setter                                                        *
 	*************************************************************************/

	public Event getStartEvent()
	{
		return this.startEvent;
	}

	public void setStartEvent(Event startEvent)
	{
		this.startEvent = startEvent;
	}

	public Event getEndEvent()
	{
		return this.endEvent;
	}

	public void setEndEvent(Event endEvent)
	{
		this.endEvent = endEvent;
	}

	public int getCosts()
	{
		return this.costs;
	}

	public String getType()
	{
		return this.type;
	}

	public boolean getTimeCycleJump()
	{
		return this.timeCycleJump;
	}

}
