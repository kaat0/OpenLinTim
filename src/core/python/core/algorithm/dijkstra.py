import math
from heapq import heappush, heappop, heapify

from core.model.graph import (Edge, Node, Graph)
from core.model.path import Path
from core.model.impl.list_path import ListPath

import core.exceptions.algorithm_dijkstra as exception

from typing import TypeVar, Generic, Callable, Dict, List, Set


N = TypeVar('N', bound=Node)
E = TypeVar('E', bound=Edge[N])
G = TypeVar('G', bound=Graph[N, E])


class SPNode:
    def __init__(self, node: N, distance: float):
        self.node = node
        self.distance = distance

    def __lt__(self, other: "SPNode"):
        try:
            if self.distance < other.distance:
                return True
            elif self.distance > other.distance:
                return False
            else:
                return self.node.getId() < other.node.getId()
        except AttributeError:
            return NotImplemented


class Dijkstra(Generic[N, E, G]):
    """
    A straight forward implementation of the algorithm of Dijkstra, using
    PriorityQueue. The algorithm can be initialized with some graph (directed
    or undirected), a node in the graph and a distance function to compute the
    length of the edges. Shortest paths can be computed using
    computeShortestPath(Node) or computeShortestPaths(). The distance to a node
    and the shortest path can only be queried after they are computed by one og
    the two mentioned methods. If the shortest path for a queried node was
    already calculated in an eralier query (maybe due to a side effect of
    another query) there will be no new computation. This holds for
    computeShortestPaths() as well.
    Note that the used graph may not be altered after initializing an instance
    of this graph, since the results after that are undefined (already computed
    shortest paths may change). Therefore, the algorithm needs to be
    initialized anew after each change!
    """

    def __init__(self, graph: G, start_node: N, distance_function: Callable[[E], float]):
        """
        Initialize a new shortest path algorithm. Computing is done with
        computeShortestPath(Node) or computeShortestPaths() and needs to be
        donedone begore querying a shortest path or a distance with
        getDistance(Node) or getPath(Node).
        :param graph    the graph to compute the sortest paths on. Note that
                        the used graph may not be altered aofter initializing
                        an instance of
                        this graph, since the results after that are undefined
                        (already
                        computed shortest paths may change). Therefore, the
                        algortihm
                        needs to be inizialised anew after each change!
        :param start_node    the start node of the algorithm, i.e., the start of
                            the shortest paths.
        :param distance_function     the distance function to compute the length
                                    of an edge.
        """

        self.graph = graph
        self.start_node = start_node
        self.distance_function = distance_function
        self.predecessors = {}  # type: Dict[N, Set[N]]
        self.distances = {}  # type: Dict[N, float]
        self.finished_nodes = set()  # type: Set[N]
        for node in self.graph.getNodes():
            self.distances[node] = math.inf

        self.distances[self.start_node] = 0
        self.finished_nodes.add(self.start_node)
        self.already_computed_shortest_paths = {}  # type: Dict[N, List[Path[N, E]]]

    def getDistance(self, endNode: N):
        """
        Get the distance form the initialized start node to the given end node.
        The shortest path to the given end node needs to be computed first,
        either by computeShortestPath(Node) or computeShortestPaths().
        :param endNode  the end node of the shortest path.
        :return     the distance between the start and the end node or
        MAX_VALUE if there is no path.
        """
        distance = self.distances.get(endNode)
        if distance is None:
            raise exception.AlgorithmDijkstraUnknownNodeException(endNode)
        if endNode not in self.finished_nodes:
            raise exception.AlgorithmDijkstraQueryDistanceBeforeComputationException(endNode)
        return distance

    def getPath(self, endNode: N) -> Path:
        """
        Get the shortest path from the initialized start node to the given end
        node. The shortest path to the givne end node need sot be computed
        first, either by computeShortestPath(Node) or computeShortestPaths().
        :param endNode  the end node of the shortest path
        :return the shortest path between the start and the end node or null,if
        the start and end node coincide or there is no path between twp nodes.
        """
        if endNode not in self.finished_nodes:
            raise exception.AlgorithmDijkstraQueryPathBeforeComputationException(endNode)
        if self.start_node == endNode or self.distances.get(endNode) == math.inf:
            return None
        if endNode in self.already_computed_shortest_paths:
            return self.already_computed_shortest_paths[endNode][0]

        path = ListPath(self.graph.isDirected())
        current_node = endNode

        while current_node != self.start_node:
            # search for the edge between current and next node
            next_node = next(iter(self.predecessors.get(current_node)))
            found_edge = False
            for edge in self.graph.getIncomingEdges(current_node):
                if((edge.getLeftNode() == current_node and edge.getRightNode() == next_node) or
                   (edge.getRightNode() == current_node and edge.getLeftNode() == next_node)):
                    path.addFirstEdge(edge)
                    current_node = next_node
                    found_edge = True
                    break
            if not found_edge:
                # This should never happen in a valid graph and with a well working algorithm.
                raise exception.AlgorithmDijkstraUnknownNodeException(endNode)
        return path

    def getPaths(self, endNode: N) -> List[Path]:
        """
        Get all shortest paths from the initialized start node to the fiven end
        node. All paths with the length of the shortest path will be retunred.
        The shortest paths to the given end node need to be computed first,
        either by computeShortestPath(Node) or computeShortestPaths().
        :param endNode  the end node of the shortest paths.
        :return     the collection of shortest paths.
        :raise exceptions if the path was queried before it was computed or if
        the nocd is not know, i.e., it was not in the fraph when the Dijkstra
        class was constructed.
        """
        if endNode not in self.finished_nodes:
            raise exception.AlgorithmDijkstraQueryPathBeforeComputationException(endNode)
        if self.start_node == endNode or self.distances.get(endNode) == math.inf:
            return None
        if endNode in self.already_computed_shortest_paths:
            return self.already_computed_shortest_paths[endNode]
        paths = []
        for nextNode in self.predecessors.get(endNode):
            next_edge = None
            for edge in self.graph.getIncomingEdges(endNode):
                if (edge.getLeftNode() == endNode and edge.getRightNode() == nextNode) or \
                        (edge.getRightNode() == endNode and edge.getLeftNode() == nextNode):
                    next_edge = edge
                    break
            if next_edge is None:
                # This should never happen
                raise(exception.AlgorithmDijkstraUnknownNodeException(endNode))

            # Find all shortest paths to nextNode
            if nextNode == self.start_node:
                sp = ListPath(self.graph.isDirected())
                sp.addFirstEdge(next_edge)
                paths.append(sp)
                continue
            shortest_part_paths = self.getPaths(nextNode)
            if shortest_part_paths is None:
                # This is only the case of the distance to nextNode from
                # startNode was infinity. Therefore, the graph is not
                # connected.
                return None
            for shortest_part_path in shortest_part_paths:
                shortest_path = ListPath(self.graph.isDirected())
                shortest_path.addLast(shortest_part_path.getEdges())
                shortest_path.addLastEdge(next_edge)
                paths.append(shortest_path)
        # Copy all paths to store for buffering
        buffer_paths = []
        for sp in paths:
            edges = sp.getEdges()
            buffer_path = ListPath(self.graph.isDirected())
            for edge in edges:
                buffer_path.addLastEdge(edge)
            buffer_paths.append(buffer_path)
        self.already_computed_shortest_paths[endNode] = buffer_paths

        return paths

    def computeShortestPath(self, endNode: N) -> float:
        """
        Compute the shortest path from the initialized start node to the given
        end node.
        :param endNode  the node to compute the shortest path to.
        :return     the distance between start and end node or inf, if there is
        no path.
        """
        if endNode in self.finished_nodes:
            return self.distances[endNode]
        # Initialize queue
        priority_queue = []  # type: List[SPNode]
        shortest_path_nodes = {}  # type: Dict[N, SPNode]

        for key, value in self.distances.items():
            new_node = SPNode(key, value)
            heappush(priority_queue, new_node)
            shortest_path_nodes[key] = new_node

        # while still entries in priorityQueue
        while len(priority_queue) > 0:
            next_node = heappop(priority_queue)

            # The element with the lowest distance has distance infinity.
            # Therefore, we have an unconnected network and abort.
            if next_node.distance == math.inf:
                for node in self.graph.getNodes():
                    if node not in self.finished_nodes:
                        self.finished_nodes.add(node)
                        self.distances[node] = math.inf
                return math.inf
            self.distances[next_node.node] = next_node.distance
            self.finished_nodes.add(next_node.node)
            if next_node.node == endNode:
                # found end node!
                return next_node.distance

            # Check all outgoing edges
            for edge in self.graph.getOutgoingEdges(next_node.node):
                # Check if we found a shortest path
                if edge.getLeftNode() == next_node.node:
                    adjacent_node = edge.getRightNode()
                else:
                    adjacent_node = edge.getLeftNode()
                # the found node may already have a computed shortest path from
                # a previous computation. In this case we may continue.
                if adjacent_node in self.finished_nodes:
                    continue

                adjacent_shortest_path_node = shortest_path_nodes.get(adjacent_node)
                edge_length = self.distance_function(edge)

                if edge_length < 0:
                    raise exception.AlgorithmDijkstraNegativeEdgeLengthException(edge, edge_length)

                new_distance = next_node.distance + edge_length
                if new_distance == adjacent_shortest_path_node.distance and new_distance < math.inf:
                    self.predecessors.get(adjacent_node).add(next_node.node)
                elif new_distance < adjacent_shortest_path_node.distance:
                    # Update the shortest path node and set predecessor
                    predecessors_for_node = set()
                    predecessors_for_node.add(next_node.node)
                    self.predecessors[adjacent_node] = predecessors_for_node
                    priority_queue.remove(adjacent_shortest_path_node)
                    new_pair = SPNode(adjacent_node, new_distance)
                    shortest_path_nodes[adjacent_node] = new_pair
                    priority_queue.append(new_pair)
                    # Restore heap
                    # We could probably speed this up by using the internal methods of heapq, see e.g.
                    # https://stackoverflow.com/a/10163422
                    heapify(priority_queue)

        # If we arrive here, there is no shortest path
        self.finished_nodes.add(endNode)
        return math.inf

    def computeShortestPaths(self) -> None:
        """
        Compute a shortest path between the initialized start node and every
        other node in the graph. Will reuse already computed shortest paths.
        """
        for node in self.graph.getNodes():
            if node not in self.finished_nodes:
                self.computeShortestPath(node)
