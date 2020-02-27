import logging
from typing import TypeVar, List

from core.model.graph import Node, Edge
from core.model.path import Path

N = TypeVar('N', bound=Node)
E = TypeVar('E', bound=Edge)


class ListPath(Path[N, E]):

    logger = logging.getLogger(__name__)

    def __init__(self, directed: bool):
        self.edge_list = []  # type: [E]
        self.node_list = []  # type: [N]
        self.directed = directed

    def contains(self, sub_path: Path[N, E]) -> bool:
        raise NotImplementedError

    def removeEdge(self, edge: E) -> bool:
        """
        Remove a single edge from the path. Will only work, if the edge is at the start/end of the path or if the edge
        starts and ends at the same node.
        :param edge: the edge to remove
        :return: whether the edge could be removed.
        """
        if not edge:
            return False
        if len(self.edge_list) == 0:
            return False
        if edge == self.edge_list[0]:
            del self.edge_list[0]
            del self.node_list[0]
            self._handle_empty_case()
            return True
        if edge == self.edge_list[-1]:
            del self.edge_list[-1]
            del self.node_list[-1]
            self._handle_empty_case()
            return True
        if edge.getLeftNode() != edge.getRightNode():
            raise ValueError("Edge to be removed is neither in the beginning nor in the end")
        try:
            edge_index = self.edge_list.index(edge)
            del self.edge_list[edge_index]
            del self.node_list[edge_index]
            return True
        except ValueError:
            # The element was not in the list
            return False

    def _handle_empty_case(self):
        if len(self.edge_list) == 0 and len(self.node_list) == 1:
            self.node_list.clear()

    def remove(self, edges: List[E]) -> bool:
        # Figure out, whether we need to delete the edges from the beginning or from the start, i.e., in which way to
        # iterate the list
        list_to_delete = list(edges)
        if list_to_delete[0] == self.edge_list[0] or list_to_delete[0] == self.edge_list[-1]:
            pass
        elif list_to_delete[-1] == self.edge_list[0] or list_to_delete[-1] == self.edge_list[-1]:
            list_to_delete.reverse()
        else:
            raise NotImplementedError("Removing of interior edge sequences from a path is not yet implemented")
        for edges in list_to_delete:
            self.removeEdge(edges)
        return True

    def isDirected(self) -> bool:
        return self.directed

    def addLastEdge(self, edge: E) -> bool:
        """
        Add the given edge to the end of the path.
        :param edge: the edge to add
        :return: whether the edge could be added
        """
        if not edge:
            return False
        # Different cases:
        # Case 1: Nodelist could be empty
        if not self.node_list:
            # We add the nodes in the "directed" direction. If the edge is undirected, we may need to resort the list
            # when we add the second edge
            self.node_list = [edge.getLeftNode(), edge.getRightNode()]
        # Case 2: The list has a size of at least 1 and the order we used in Case 1 was correct
        elif edge.getRightNode() == self.node_list[-1] and not self.directed:
            self.node_list.append(edge.getLeftNode())
        elif edge.getLeftNode() == self.node_list[-1]:
            self.node_list.append(edge.getRightNode())
        # Case 3: The list has exactly size 2 (i.e., only one edge was added yet), is undirected and the order we used
        # in case 1 was wrong (otherwise we will end in case 1 or 2)
        elif len(self.node_list) == 2 and not self.directed:
            self.node_list.reverse()
            # Now we can handle case 2 again
            if edge.getRightNode() == self.node_list[-1] and not self.directed:
                self.node_list.append(edge.getLeftNode())
            elif edge.getLeftNode() == self.node_list[-1]:
                self.node_list.append(edge.getRightNode())
            else:
                self.logger.debug("Edge {} cannot be prepended to path (nodes don't match)".format(edge.getId()))
                return False
        else:
            self.logger.debug("Edge {} cannot be prepended to path (nodes don't match)".format(edge.getId()))
            return False
        self.edge_list.append(edge)
        return True

    def addLast(self, edges: List[E]) -> bool:
        succeeded = True
        failed_element = None
        for edge in edges:
            succeeded = succeeded and self.addLastEdge(edge)
            if not succeeded:
                failed_element = edge
                break
        if not succeeded:
            self.reset_path(edges, failed_element)
        return succeeded

    def getNodes(self) -> List[N]:
        return list(self.node_list)

    def getEdges(self) -> List[E]:
        return list(self.edge_list)

    def addFirstEdge(self, edge: E) -> bool:
        """
        Add the given edge to the start of the path.
        :param edge: the edge to add
        :return: whether the edge could be added
        """
        if not edge:
            return False
        # Different cases:
        # Case 1: Nodelist could be empty
        if not self.node_list:
            # We add the nodes in the "directed" direction. If the edge is undirected, we may need to resort the list
            # when we add the second edge
            self.node_list = [edge.getLeftNode(), edge.getRightNode()]
        # Case 2: The list has a size of at least 1 and the order we used in Case 1 was correct
        elif edge.getLeftNode() == self.node_list[0] and not self.directed:
            self.node_list.insert(0, edge.getRightNode())
        elif edge.getRightNode() == self.node_list[0]:
            self.node_list.insert(0, edge.getLeftNode())
        # Case 3: The list has exactly size 2 (i.e., only one edge was added yet), is undirected and the order we used
        # in case 1 was wrong (otherwise we will end in case 1 or 2)
        elif len(self.node_list) == 2 and not self.directed:
            self.node_list.reverse()
            # Now we can handle case 2 again
            if edge.getLeftNode() == self.node_list[0] and not self.directed:
                self.node_list.insert(0, edge.getRightNode())
            elif edge.getRightNode() == self.node_list[0]:
                self.node_list.insert(0, edge.getLeftNode())
            else:
                self.logger.debug("Edge {} cannot be prepended to path (nodes don't match)".format(edge.getId()))
                return False
        else:
            self.logger.debug("Edge {} cannot be prepended to path (nodes don't match)".format(edge.getId()))
            return False
        self.edge_list.insert(0, edge)
        return True

    def addFirst(self, edges: List[E]) -> bool:
        insert_list = list(edges)
        insert_list.reverse()
        succeeded = True
        failed_element = None
        for edge in insert_list:
            succeeded = succeeded and self.addFirstEdge(edge)
            if not succeeded:
                failed_element = edge
                break
        if not succeeded:
            self.reset_path(insert_list, failed_element)
        return succeeded

    def reset_path(self, insert_list: List[E], failed_element: E) -> None:
        """
        Helper method to reset the path.
        :param insert_list: the list that was tried to add to the path
        :param failed_element: the failed element
        """
        target_index = insert_list.index(failed_element)
        list_to_remove = insert_list[:target_index]
        list_to_remove.reverse()
        self.remove(list_to_remove)


