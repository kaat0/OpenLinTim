/**
 */


import java.util.ArrayList;

public class ChangeGo{
	
	public ArrayList<Vertex> vertices;
	public ArrayList<Arc> arcs;
	public Vertex rootVertex;
	public Vertex destinationVertex;
	
	public ChangeGo(){
		this.vertices = new ArrayList<Vertex>();
		this.arcs = new ArrayList<Arc>();
	}
	
	public ChangeGo(ArrayList<Vertex> vertices, ArrayList<Arc> arcs){
		this.vertices = vertices;
		this.arcs = arcs;	
	}
	
	public void addVertex(Vertex vertex){
		vertices.add(vertex);
	}
	
	public void addArc(Arc arc){
		arcs.add(arc);
	}
	
	public ArrayList<Vertex> getVertices(){
		return vertices;
	}
	
	public ArrayList<Arc> getArcs(){
		return arcs;
	}
	
	public void setRootVertex(Vertex vertex){
		this.rootVertex = vertex;
	}
	
	public void setDestinationVertex(Vertex vertex){
		this.destinationVertex = vertex;
	}

	public Vertex getRootVertex(){
		return this.rootVertex;
	}
	
	public Vertex getDestinationVertex(){
		return this.destinationVertex;
	}
	
	public Vertex getVertex(int index){
		for(Vertex vertex:vertices)
			if(index==vertex.getIndex())
				return vertex;
				
		return null;
	}
	
	public Arc getArc(int index){
		for(Arc arc:arcs)
			if(index==arc.getIndex())
				return arc;
		return null;	
	}
	
	public void removeArc(Arc arc){
		for(Arc nextArc : this.getOutgoingArcs(arc.getRightVertex())){
			if(nextArc != arc){
				if(arc.getRightVertex() == nextArc.getLeftVertex()){
					nextArc.setLeftVertex(arc.getLeftVertex());
				} else if(arc.getRightVertex() == nextArc.getRightVertex()){
					nextArc.setRightVertex(arc.getLeftVertex());
				}
			}
		}
		this.arcs.remove(arc);
		this.vertices.remove(arc.getRightVertex());
	}
	
	public ArrayList<Arc> getOutgoingArcs(Vertex vertex){
		ArrayList<Arc> outgoingArcs = new ArrayList<Arc>();
		for(Arc arc:this.arcs){
			if(arc.getLeftVertex() == vertex)
				outgoingArcs.add(arc);
			else if(arc.getRightVertex() == vertex && !arc.isDirected())
				outgoingArcs.add(arc);
		}
		return outgoingArcs;
	}
	
	public String toString(){
		String s = "";
		for(Vertex vertex:this.vertices){
			s+=vertex.toString()+"\n";
		}
		for(Arc arc:this.arcs){
			s+=arc.toString()+"\n";
		}
		return s;
	}
}	
