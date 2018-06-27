package net.lintim.algorithm;

import net.lintim.exception.AlgorithmDijkstraNegativeEdgeLengthException;
import net.lintim.exception.AlgorithmDijkstraQueryDistanceBeforeComputationException;
import net.lintim.exception.AlgorithmDijkstraQueryPathBeforeComputationException;
import net.lintim.model.Graph;
import net.lintim.model.Link;
import net.lintim.model.Path;
import net.lintim.model.Stop;
import net.lintim.model.impl.ArrayListGraph;
import net.lintim.util.TestHelper;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 */
public class DijkstraTest {

    private static final double DELTA = 1e-15;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @BeforeClass
    public static void setupClass() {
        TestHelper.disableLogging();
    }

    @Test
    public void canFindShortestPath() {
        Graph<Stop, Link> graph = new ArrayListGraph<>();
        Stop stop1 = new Stop(1, "1", "1", 1, 1);
        Stop stop2 = new Stop(2, "2", "2", 2, 2);
        Stop stop3 = new Stop(3, "3", "3", 3, 3);
        graph.addNode(stop1);
        graph.addNode(stop2);
        graph.addNode(stop3);
        Link link1 = new Link(1, stop1, stop2, 1, 1, 1, true);
        Link link2 = new Link(1, stop1, stop3, 1, 1, 1, true);
        Link link3 = new Link(1, stop3, stop2, 1, 1, 1, true);
        graph.addEdge(link1);
        graph.addEdge(link2);
        graph.addEdge(link3);
        Dijkstra<Stop, Link, Graph<Stop, Link>> dijkstra = new Dijkstra<>(graph, stop1, Link::getLength);
        Assert.assertEquals(1, dijkstra.computeShortestPath(stop2), DELTA);
        Assert.assertEquals(1, dijkstra.getDistance(stop2), DELTA);
        Path<Stop, Link> path = dijkstra.getPath(stop2);
        Assert.assertEquals(1, path.getEdges().size());
        Assert.assertEquals(2, path.getNodes().size());
        Assert.assertTrue(path.contains(link1));
        Assert.assertTrue(path.contains(stop1));
        Assert.assertTrue(path.contains(stop2));
        exception.expect(AlgorithmDijkstraQueryDistanceBeforeComputationException.class);
        dijkstra.getDistance(stop3);
        exception.expect(AlgorithmDijkstraQueryPathBeforeComputationException.class);
        dijkstra.getPath(stop3);
    }

    @Test
    public void canFindShortestPathWithMoreLinks() {
        Graph<Stop, Link> graph = new ArrayListGraph<>();
        Stop stop1 = new Stop(1, "1", "1", 1, 1);
        Stop stop2 = new Stop(2, "2", "2", 2, 2);
        Stop stop3 = new Stop(3, "3", "3", 3, 3);
        graph.addNode(stop1);
        graph.addNode(stop2);
        graph.addNode(stop3);
        Link link1 = new Link(1, stop1, stop2, 3, 1, 1, true);
        Link link2 = new Link(1, stop1, stop3, 1, 1, 1, true);
        Link link3 = new Link(1, stop3, stop2, 1, 1, 1, true);
        graph.addEdge(link1);
        graph.addEdge(link2);
        graph.addEdge(link3);
        Dijkstra<Stop, Link, Graph<Stop, Link>> dijkstra = new Dijkstra<>(graph, stop1, Link::getLength);
        Assert.assertEquals(2, dijkstra.computeShortestPath(stop2), DELTA);
        Assert.assertEquals(2, dijkstra.getDistance(stop2), DELTA);
        Assert.assertEquals(1, dijkstra.getDistance(stop3), DELTA);
        Path<Stop, Link> path = dijkstra.getPath(stop2);
        Assert.assertEquals(2, path.getEdges().size());
        Assert.assertEquals(3, path.getNodes().size());
        Assert.assertTrue(path.contains(link2));
        Assert.assertTrue(path.contains(link3));
        Assert.assertTrue(path.contains(stop1));
        Assert.assertTrue(path.contains(stop3));
        Assert.assertTrue(path.contains(stop2));
    }

    @Test
    public void failsWithNegativeEdgeLength() {
        Graph<Stop, Link> graph = new ArrayListGraph<>();
        Stop stop1 = new Stop(1, "1", "1", 1, 1);
        Stop stop2 = new Stop(2, "2", "2", 2, 2);
        Stop stop3 = new Stop(3, "3", "3", 3, 3);
        graph.addNode(stop1);
        graph.addNode(stop2);
        graph.addNode(stop3);
        Link link1 = new Link(1, stop1, stop2, 3, 1, 1, true);
        Link link2 = new Link(1, stop1, stop3, 1, 1, 1, true);
        Link link3 = new Link(1, stop3, stop2, -1, 1, 1, true);
        graph.addEdge(link1);
        graph.addEdge(link2);
        graph.addEdge(link3);
        Dijkstra<Stop, Link, Graph<Stop, Link>> dijkstra = new Dijkstra<>(graph, stop1, Link::getLength);
        exception.expect(AlgorithmDijkstraNegativeEdgeLengthException.class);
        dijkstra.computeShortestPath(stop2);
    }

    @Test
    public void canComputeAllShortestPaths() {
        Graph<Stop, Link> graph = new ArrayListGraph<>();
        Stop stop1 = new Stop(1, "1", "1", 1, 1);
        Stop stop2 = new Stop(2, "2", "2", 2, 2);
        Stop stop3 = new Stop(3, "3", "3", 3, 3);
        graph.addNode(stop1);
        graph.addNode(stop2);
        graph.addNode(stop3);
        Link link1 = new Link(1, stop1, stop2, 2, 1, 1, true);
        Link link2 = new Link(1, stop1, stop3, 1, 1, 1, true);
        graph.addEdge(link1);
        graph.addEdge(link2);
        Dijkstra<Stop, Link, Graph<Stop, Link>> dijkstra = new Dijkstra<>(graph, stop1, Link::getLength);
        dijkstra.computeShortestPaths();
        Path<Stop, Link> path = dijkstra.getPath(stop2);
        Assert.assertEquals(1, path.getEdges().size());
        path = dijkstra.getPath(stop3);
        Assert.assertEquals(1, path.getEdges().size());
    }
}
