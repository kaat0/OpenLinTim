package net.lintim.model.vehiclescheduling;

public class Edge
{
	private int ID;
	private int leftStopID;
	private int rightStopID;
	private int length;

	/*************************************************************************
  	* constructor                                                            *
 	**************************************************************************/

	public Edge()
	{
	}

	public Edge(int ID, int leftStopID, int rightStopID, int length)
	{
		this.ID = ID;
		this.leftStopID = leftStopID;
		this.rightStopID = rightStopID;
		this.length = length;
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

	public int getLeftStopID()
	{
		return this.leftStopID;
	}

	public void setLeftStopID(int leftStopID)
	{
		this.leftStopID = leftStopID;
	}

	public int getRightStopID()
	{
		return this.rightStopID;
	}

	public void setRightStopID(int rightStopID)
	{
		this.rightStopID = rightStopID;
	}

	public int getLength()
	{
		return this.length;
	}

	public void setLength(int length)
	{
		this.length = length;
	}

}
