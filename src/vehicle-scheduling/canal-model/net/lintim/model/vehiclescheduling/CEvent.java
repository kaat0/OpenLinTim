package net.lintim.model.vehiclescheduling;

public class CEvent extends Event implements Comparable<CEvent>
{
	private CJourney journey;
	private CTransfer associatedTransfer;
	private int canal; // canal-id

	/*************************************************************************
  	* constructor                                                            *
 	**************************************************************************/

	public CEvent()
	{
	}

	public CEvent(CJourney journey, CTransfer associatedTransfer, int ID, int time, int canal, String type)
	{
		this.journey = journey;
		this.associatedTransfer = associatedTransfer;
		super.setID(ID);
		super.setTime(time);
		this.canal = canal;
		super.setType(type);
	}

	public CEvent(CJourney journey, CTransfer associatedTransfer, int ID, int time, int canal, String type, int periodicID)
	{
		this.journey = journey;
		this.associatedTransfer = associatedTransfer;
		super.setID(ID);
		super.setTime(time);
		this.canal = canal;
		super.setType(type);
		super.setPeriodicID(periodicID);
	}



	/************************************************************************
  	*  getter/setter                                                        *
 	*************************************************************************/

	public void setJourney(CJourney journey)
	{
		this.journey =journey;
	}


	public CJourney getJourney()
	{
		return this.journey;
	}

	public void setAssociatedTransfer(CTransfer associatedTransfer)
	{
		this.associatedTransfer = associatedTransfer;
	}

	public CTransfer getAssociatedTransfer()
	{
		return this.associatedTransfer;
	}

	public int getCanal()
	{
		return this.canal;
	}

	public void setCanal(int canal)
	{
		this.canal = canal;
	}

	public int compareTo(CEvent other){
	// first compare the time values
		if(other.getTime() > this.getTime()){
			return -1;
		}
		else if(other.getTime() < this.getTime()){
			return 1;
		}
		// second compare the ID's of the events with different types
		else if(other.getType() != this.getType()){
			if(other.getType() == Event.TYPE_START && this.getType() == Event.TYPE_END){
				if(other.getID() > this.getID()){
					return 1;
				}
				else if(other.getID() < this.getID()){
					return -1;
				}
				else{
					return 0; // FIXME: This branch should not be reached, but it needs a return-statement
				}
//				else{
//					throw new Exception("Two Events do have the same ID, but not the same type");
//				}
			}
			else if(other.getType().equals("END") && this.getType().equals("START")){
				if(other.getID() > this.getID()){
					return -1;
				}
				else if(other.getID() < this.getID()){
					return 1;
				}
				else{
					return 0; // FIXME: This branch should not be reached, but it needs a return-statement
				}
//				else{
//					throw new Exception("Two Events do have the same ID, but not the same type");
//				}
			}
			else{
				return 0; // FIXME: This branch should not be reached, but it needs a return-statement
			}
//			else{
//				throw new Exception("Two Events do have different types, but not \"START\" and \"END\".");
//			}
		}
		// third compare the ID's of the assigned journeys (in this case, it holds "other.getType().equals(this.getType())"!
		else if(other.getJourney().getID() > this.journey.getID()){
			return -1;
		}
		else if(other.getJourney().getID() < this.journey.getID()){
			return 1;
		}
		else{
			return 0;
		}
	}

}
