import logging
import unittest


from core.algorithm.dijkstra import Dijkstra
from core.exceptions.algorithm_dijkstra import AlgorithmDijkstraQueryDistanceBeforeComputationException, \
    AlgorithmDijkstraQueryPathBeforeComputationException, AlgorithmDijkstraNegativeEdgeLengthException
from core.model.impl.dict_graph import DictGraph
from core.model.impl.simple_dict_graph import SimpleDictGraph
from core.model.ptn import Stop, Link

logging.disable(logging.CRITICAL)

class TestDijkstra(unittest.TestCase):

    def test_can_find_shortest_path(self):
        graph = DictGraph()
        stop1 = Stop(1, "1", "1", 1, 1)
        stop2 = Stop(2, "2", "2", 2, 2)
        stop3 = Stop(3, "3", "3", 3, 3)
        graph.addNode(stop1)
        graph.addNode(stop2)
        graph.addNode(stop3)
        link1 = Link(1, stop1, stop2, 1, 1, 1, True)
        link2 = Link(2, stop1, stop3, 1, 1, 1, True)
        link3 = Link(3, stop3, stop2, 1, 1, 1, True)
        graph.addEdge(link1)
        graph.addEdge(link2)
        graph.addEdge(link3)
        dijkstra = Dijkstra(graph, stop1, Link.getLength)
        self.assertEqual(1, dijkstra.computeShortestPath(stop2))
        self.assertEqual(1, dijkstra.getDistance(stop2))
        path = dijkstra.getPath(stop2)
        self.assertEqual(1, len(path.getEdges()))
        self.assertEqual(2, len(path.getNodes()))
        self.assertTrue(link1 in path.getEdges())
        self.assertTrue(stop1 in path.getNodes())
        self.assertTrue(stop2 in path.getNodes())
        with self.assertRaises(AlgorithmDijkstraQueryDistanceBeforeComputationException):
            dijkstra.getDistance(stop3)
        with self.assertRaises(AlgorithmDijkstraQueryPathBeforeComputationException):
            dijkstra.getPath(stop3)

    def test_can_find_shortest_path_with_more_links(self):
        graph = DictGraph()
        stop1 = Stop(1, "1", "1", 1, 1)
        stop2 = Stop(2, "2", "2", 2, 2)
        stop3 = Stop(3, "3", "3", 3, 3)
        graph.addNode(stop1)
        graph.addNode(stop2)
        graph.addNode(stop3)
        link1 = Link(1, stop1, stop2, 3, 1, 1, True)
        link2 = Link(2, stop1, stop3, 1, 1, 1, True)
        link3 = Link(3, stop3, stop2, 1, 1, 1, True)
        graph.addEdge(link1)
        graph.addEdge(link2)
        graph.addEdge(link3)
        dijkstra = Dijkstra(graph, stop1, Link.getLength)
        self.assertEqual(2, dijkstra.computeShortestPath(stop2))
        self.assertEqual(2, dijkstra.getDistance(stop2))
        self.assertEqual(1, dijkstra.getDistance(stop3))
        path = dijkstra.getPath(stop2)
        self.assertEqual(2, len(path.getEdges()))
        self.assertEqual(3, len(path.getNodes()))
        self.assertTrue(link2 in path.getEdges())
        self.assertTrue(link3 in path.getEdges())
        self.assertTrue(stop1 in path.getNodes())
        self.assertTrue(stop3 in path.getNodes())
        self.assertTrue(stop2 in path.getNodes())

    def test_fails_with_negative_edge_length(self):
        graph = DictGraph()
        stop1 = Stop(1, "1", "1", 1, 1)
        stop2 = Stop(2, "2", "2", 2, 2)
        stop3 = Stop(3, "3", "3", 3, 3)
        graph.addNode(stop1)
        graph.addNode(stop2)
        graph.addNode(stop3)
        link1 = Link(1, stop1, stop2, 3, 1, 1, True)
        link2 = Link(2, stop1, stop3, 1, 1, 1, True)
        link3 = Link(3, stop3, stop2, -1, 1, 1, True)
        graph.addEdge(link1)
        graph.addEdge(link2)
        graph.addEdge(link3)
        dijkstra = Dijkstra(graph, stop1, Link.getLength)
        with self.assertRaises(AlgorithmDijkstraNegativeEdgeLengthException):
            dijkstra.computeShortestPath(stop2)

    def test_can_compute_all_shortest_paths(self):
        graph = DictGraph()
        stop1 = Stop(1, "1", "1", 1, 1)
        stop2 = Stop(2, "2", "2", 2, 2)
        stop3 = Stop(3, "3", "3", 3, 3)
        graph.addNode(stop1)
        graph.addNode(stop2)
        graph.addNode(stop3)
        link1 = Link(1, stop1, stop2, 2, 1, 1, True)
        link2 = Link(2, stop1, stop3, 1, 1, 1, True)
        graph.addEdge(link1)
        graph.addEdge(link2)
        dijkstra = Dijkstra(graph, stop1, Link.getLength)
        dijkstra.computeShortestPaths()
        path = dijkstra.getPath(stop2)
        self.assertEqual(1, len(path.getEdges()))
        path = dijkstra.getPath(stop3)
        self.assertEqual(1, len(path.getEdges()))

    def test_can_compute_shortest_path_with_multiple_shortest_subpaths(self):
        graph = SimpleDictGraph()
        stop1 = Stop(1, "1", "1", 1, 1)
        stop2 = Stop(2, "2", "2", 2, 2)
        stop3 = Stop(3, "3", "3", 3, 3)
        stop4 = Stop(4, "4", "4", 4, 4)
        graph.addNode(stop1)
        graph.addNode(stop2)
        graph.addNode(stop3)
        graph.addNode(stop4)
        link1 = Link(1, stop1, stop2, 10, 10, 10, True)
        link2 = Link(2, stop1, stop3, 10, 10, 10, True)
        link3 = Link(3, stop2, stop4, 10, 10, 10, True)
        link4 = Link(4, stop3, stop4, 10, 10, 10, True)
        graph.addEdge(link1)
        graph.addEdge(link2)
        graph.addEdge(link3)
        graph.addEdge(link4)
        dijkstra = Dijkstra(graph, stop1, Link.getLength)
        dijkstra.computeShortestPaths()
        path = dijkstra.getPath(stop4)
        self.assertEqual(2, len(path.getEdges()))


    def test_can_compute_multiple_shortest_paths(self):
        graph = SimpleDictGraph()
        stop1 = Stop(1, "1", "1", 1, 1)
        stop2 = Stop(2, "2", "2", 2, 2)
        stop3 = Stop(3, "3", "3", 3, 3)
        stop4 = Stop(4, "4", "4", 4, 4)
        graph.addNode(stop1)
        graph.addNode(stop2)
        graph.addNode(stop3)
        graph.addNode(stop4)
        link1 = Link(1, stop1, stop2, 10, 10, 10, True)
        link2 = Link(2, stop1, stop3, 10, 10, 10, True)
        link3 = Link(3, stop2, stop4, 10, 10, 10, True)
        link4 = Link(4, stop3, stop4, 10, 10, 10, True)
        graph.addEdge(link1)
        graph.addEdge(link2)
        graph.addEdge(link3)
        graph.addEdge(link4)
        dijkstra = Dijkstra(graph, stop1, Link.getLength)
        dijkstra.computeShortestPaths()
        paths = dijkstra.getPaths(stop4)
        self.assertEqual(2, len(paths))
        self.assertEqual(2, len(paths[0].getEdges()))
        self.assertEqual(20, dijkstra.getDistance(stop4))




