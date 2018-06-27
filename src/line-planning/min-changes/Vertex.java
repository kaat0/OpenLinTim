/**
 */


import java.util.ArrayList;

public class Vertex{
	
	int index;
	int stopIndex;
	Line line; // Possible Values: OD, line
	public ArrayList<Arc> outgoingArcs;
	
	public Vertex(int index, int stopIndex, Line line){
		this.index = index;
		this.stopIndex = stopIndex;
		this.line = line;
	}
	
	public int getStopIndex(){
		return this.stopIndex;
	}
	
	public int getIndex(){
		return this.index;
	}
	
	public Line getLine(){
		return this.line;
	}
	
	public void setOutgoingArcs(ArrayList<Arc> outgoingArcs){
		this.outgoingArcs = outgoingArcs;
	}
	
	public void addOutgoingArc(Arc arc){
		if(this.outgoingArcs == null){
			this.outgoingArcs = new ArrayList<Arc>();
		}
		this.outgoingArcs.add(arc);
	}
	
	public ArrayList<Arc> getOutgoingArcs(){
		return this.outgoingArcs;
	}
	
	@Override
	public String toString(){
		return "Vertex " + this.index + ", stopIndex: " + this.stopIndex + ", lineIndex: " + ((line==null)?"":line.getIndex());
	}
	
	public boolean equalsStopLine(Vertex vertex){
		return (this.getStopIndex()==vertex.getStopIndex() && this.getLine()==vertex.getLine());
	}
}
