package net.lintim.model.vehiclescheduling;

public class Transfer
{
	private int ID;
	private int firstTripID;
	private int secondTripID;
	private boolean timeCycleJump;

	/*************************************************************************
  	* constructor                                                            *
 	**************************************************************************/

	public Transfer(){
	}

	public Transfer(int ID, int firstTripID, int secondTripID)
	{
		this.ID = ID;
		this.firstTripID = firstTripID;
		this.secondTripID = secondTripID;
		this.timeCycleJump = false; // default-value for the variable
	}

	public Transfer(int ID, int firstTripID, int secondTripID, boolean timeCycleJump)
	{
		this.ID = ID;
		this.firstTripID = firstTripID;
		this.secondTripID = secondTripID;
		this.timeCycleJump = timeCycleJump;
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

	public int getFirstTripID()
	{
		return this.firstTripID;
	}

	public void setFirstTripID(int firstTripID)
	{
		this.firstTripID = firstTripID;
	}

	public int getSecondTripID()
	{
		return this.secondTripID;
	}

	public void setSecondTripID(int secondTripID)
	{
		this.secondTripID = secondTripID;
	}

	public boolean getValueTimeCycleJump()
	{
		return this.timeCycleJump;
	}

	public void setValueTimeCycleJump(boolean timeCycleJump)
	{
		this.timeCycleJump = timeCycleJump;
	}

}
