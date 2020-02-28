import edu.asu.emit.algorithm.graph.VariableGraph;
import edu.asu.emit.algorithm.graph.abstraction.BaseVertex;

/**
 */
public class GraphWrapper extends VariableGraph {
	public void addVertex(BaseVertex vertex) {
		vertexNum++;
		vertexList.add(vertex);
		idVertexIndex.put(vertex.getId(), vertex);
	}

	public void addEdgeWrapper(int startId, int targetId, double length) {
		addEdge(startId, targetId, length);
	}
}
