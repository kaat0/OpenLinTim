/** class representing a node for PTN and CAG */
public class Node{
	
	protected String name;
	protected int id;
	/** if the node is a line node in the cag, this parameter is 
	  * initialized with a positive integer, the line id 
	  */
	protected int lineId = -1;
	/** the position of the node in line l_lineId */
	protected int position = -1;
	
//--------------Constructor--------------------------------------------------
	public Node(String name, int id){
		this.name = name;
		this.id = id;
	}
	
	public Node(Node node, int lineId, int pos){
		this.name = node.getName();
		this.id = node.getId();
		this.lineId = lineId;
		this.position = pos;
	}


//---------------Setter/Getter--------------------------------------------------	
	
	public String getName(){
		return this.name;
	}
	
	public int getId(){
		return this.id;
	}
	
	public int getLineId(){
		return this.lineId;
	}
	
	public int getPosition(){
		return this.position;
	}
	
//----------------ToString------------------------------------------------------
	public String toString() {
		if (this.lineId == -1){
			return "" + this.id;
		} 
		else {
			return "(" + this.id + ",l_" + this.lineId + ")";
		}
	}
	
//---------------Equals----------------------------------------------------------

	public boolean equals(Node node) {
		if (this.id == node.getId()|| this.lineId == node.getLineId()) {
			return true;
		}
		else {
			return false;
		}
	}

}
