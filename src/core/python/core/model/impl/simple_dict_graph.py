from typing import Callable, TypeVar, Any, Dict, List

from core.model.graph import Graph, Node, Edge

N = TypeVar('N', bound=Node)
E = TypeVar('E', bound=Edge)


class SimpleDictGraph(Graph):
    """
    A base implementation of a graph, using dictionaries. This graph cannot contain multiple nodes or edges with the
    same id.
    """
    def __init__(self):
        self.nodes = {}  # type: Dict[int, N]
        self.edges = {}  # type: Dict[int, E]
        self.incident_edges = {}  # type: Dict[N, List[E]]

    def getNode(self, node_id: int) -> N:
        try:
            return self.nodes[node_id]
        except ValueError:
            return None

    def getEdge(self, edge_id: int) -> E:
        try:
            return self.edges[edge_id]
        except ValueError:
            return None

    T = TypeVar("T")

    def get_node_by_function(self, lookup_map: Callable[[N], T], value: T) -> N:
        return next(iter([x for x in self.nodes.values() if lookup_map(x) == value] or []), None)

    def get_edge_by_function(self, lookup_map: Callable[[E], T], value: T) -> E:
        return next(iter([x for x in self.edges.values() if lookup_map(x) == value] or []), None)

    def addEdge(self, edge: E) -> bool:
        left_node = edge.getLeftNode()
        if left_node.getId() not in self.nodes:
            raise ValueError("The edges left node is not a member of the graph")
        right_node = edge.getRightNode()
        if right_node.getId() not in self.nodes:
            raise ValueError("The edges right node is not a member of the graph")
        if edge.getId() in self.edges:
            return False
        self.edges[edge.getId()] = edge
        self.incident_edges[left_node].append(edge)
        self.incident_edges[right_node].append(edge)
        return True

    def addNode(self, node: N) -> bool:
        if node.getId() in self.nodes:
            return False
        self.nodes[node.getId()] = node
        self.incident_edges[node] = []
        return True

    def removeEdge(self, edge: E) -> bool:
        if not edge.getId() in self.edges:
            return False
        del self.edges[edge.getId()]
        self.incident_edges[edge.getLeftNode()].remove(edge)
        self.incident_edges[edge.getRightNode()].remove(edge)
        return True

    def removeNode(self, node: N) -> bool:
        if not node.getId() in self.nodes:
            return False
        edges_to_remove = self.incident_edges[node].copy()
        for edge in edges_to_remove:
            self.removeEdge(edge)
        del self.nodes[node.getId()]
        del self.incident_edges[node]
        return True

    def orderNodes(self, key_function: Callable[[N], Any]) -> None:
        current_nodes = list(self.nodes.values())
        self.nodes.clear()
        current_nodes.sort(key=key_function)
        index = 1
        for node in current_nodes:
            self.nodes[index] = node
            node.setId(index)
            index += 1

    def orderEdges(self, key_function: Callable[[E], Any]) -> None:
        current_edges = list(self.edges.values())
        self.edges.clear()
        current_edges.sort(key=key_function)
        index = 1
        for edge in current_edges:
            self.edges[index] = edge
            edge.setId(index)
            index += 1

    def getIncidentEdges(self, node: N) -> [E]:
        return [x for x in self.incident_edges[node]]

    def getOutgoingEdges(self, node: N) -> [E]:
        return [x for x in self.incident_edges[node] if not x.isDirected() or x.getLeftNode() == node]

    def getIncomingEdges(self, node: N) -> [E]:
        return [x for x in self.incident_edges[node] if not x.isDirected() or x.getRightNode() == node]

    def getNodes(self) -> [N]:
        return [x for x in self.nodes.values() if x]

    def getEdges(self) -> [E]:
        return [x for x in self.edges.values() if x]

    def isDirected(self) -> bool:
        return any([x.isDirected() for x in self.edges.values()])


