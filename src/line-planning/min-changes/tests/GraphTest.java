import edu.asu.emit.algorithm.graph.abstraction.BaseVertex;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

/**
 */
public class GraphTest {

	@Test
	public void canAddVertices() {
		GraphWrapper graph = new GraphWrapper();
		BaseVertex vertex = new VertexImplementation(1);
		graph.addVertex(vertex);
		assertEquals(1, graph.getVertexList().size());
	}

	@Test
	public void canAddEdges() {
		GraphWrapper graph = new GraphWrapper();
		BaseVertex vertex = new VertexImplementation(1);
		BaseVertex vertex2 = new VertexImplementation(2);
		BaseVertex vertex3 = new VertexImplementation(3);
		graph.addVertex(vertex);
		graph.addVertex(vertex2);
		graph.addVertex(vertex3);
		assertEquals(3, graph.getVertexList().size());
		graph.addEdgeWrapper(1, 2, 1);
		graph.addEdgeWrapper(2, 3, 1);
		graph.addEdgeWrapper(1, 3, 1);
		assertEquals(2, graph.getPrecedentVertices(vertex3).size());
		assertEquals(0, graph.getAdjacentVertices(vertex3).size());
		assertEquals(2, graph.getAdjacentVertices(vertex).size());

	}
}
