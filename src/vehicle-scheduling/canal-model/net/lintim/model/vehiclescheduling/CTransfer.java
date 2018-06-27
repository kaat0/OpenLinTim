package net.lintim.model.vehiclescheduling;

public class CTransfer extends CJourney
{
	private Event startEvent;
	private Event endEvent;
	private int costs;
	private boolean timeCycleJump;
	private String type; // this could be "TRIP", "EMPTY", "PARKING" and "MAINTAINING"

	/*************************************************************************
  	* constructor                                                            *
 	**************************************************************************/

	public CTransfer(int ID, Event startEvent, Event endEvent)
	{
		super.setID(ID);
		this.startEvent = startEvent;
		this.endEvent = endEvent;
		this.costs = 0; // default-value for the variable
		this.type = "TRIP"; // default-value for the variable
		this.timeCycleJump = false; // default-value for the variable
	}

	public CTransfer(int ID, Event startEvent, Event endEvent, int costs, String type, boolean timeCycleJump)
	{
		super.setID(ID);
		this.startEvent = startEvent;
		this.endEvent = endEvent;
		this.costs = costs;
		this.type = type;
		this.timeCycleJump = timeCycleJump;
	}



	/************************************************************************
  	*  getter/setter                                                        *
 	*************************************************************************/

	public int getID()
	{
		return super.getID();
	}

	public void setID(int ID)
	{
		super.setID(ID);
	}

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

	public void setCosts(int costs)
	{
		this.costs = costs;
	}

	public String getType()
	{
		return this.type;
	}

	public void setType(String type)
	{
		this.type = type;
	}

	public boolean getTimeCycleJump()
	{
		return this.timeCycleJump;
	}

	public void setTimeCycleJump(boolean timeCycleJump)
	{
		this.timeCycleJump = timeCycleJump;
	}

	/************************************************************************
  	*  print-methods                                                        *
 	*************************************************************************/

	public void print(CTransfer transfer)
	{
		System.out.println("Transfer " + super.getID() + ": ");
		System.out.print("\t startEvent: ");
		System.out.print("\n \t endEvent: ");
		System.out.println("\n \t costs: " + costs);
		System.out.println("\n \t timeCycleJump: " + timeCycleJump);
		System.out.println("\n \t type: " + type);
	}

}
