from core.exceptions.exceptions import LinTimException
from core.model.graph import Edge, Node


class AlgorithmStoppingCriterionException(LinTimException):

    def __init__(self, algorithm: str):
        """
        Raise if an algorithm terminates before finding a feasible/optimal
        solution.
        :param algorithm    name of the algorithm
        """
        super().__init__("Error A1: Stopping criterion of algorithm {} reached\
                         without finding a feasible/optimal\
                         solution.".format(algorithm))


class AlgorithmInfeasibleParameterSettingException(LinTimException):

    def __init__(self, algorithm: str, configKey: str, configParameter: str):
        """
        Raise if algorithm is started with infeasible parameter settings.
        :param algorithm    name of the algorithm
        :param configKey    config key
        :param configParameter config parameter
        """
        super().__init__("Error A2: Algorithm {} cannot be run with parameter\
                         setting {}; {}.".format(algorithm, configKey,
                                                 configParameter))


class AlgorithmDijkstraQueryDistanceBeforeComputationException(LinTimException):

    def __init__(self, Node: Node):
        """
        Raise if the distance to a node is queried in algorithm.dijkstra befor
        a shortest path to this node was computed.
        """
        super().__init__("Error A3: Distance to {} was queried before\
                         computation.".format(Node))


class AlgorithmDijkstraQueryPathBeforeComputationException(LinTimException):

    def __init__(self, Node: Node):
        """
        Raise if the path to a node is queried in algorithm.dijkstra before a
        shortest path ot this node was computed.
        """
        super().__init__("Error A4: Path to {} was queried before\
                         computation.".format(Node))


class AlgorithmDijkstraUnknownNodeException(LinTimException):

    def __init__(self, unknownNode: Node):
        """
        Raise if any method in algorithm.dijkstra is called with an unknown
        node, i.e., a node that was not in the graph when the used instance of
        the class was initialized.
        :param unknownNode  the unknown node.
        """
        super().__init__("Error A5: Usage of unknown node\
                         {}.".format(unknownNode))


class AlgorithmDijkstraNegativeEdgeLengthException(LinTimException):

    def __init__(self, edge: Edge, length: float):
        """
        Raise if there is an edge with negative length in a graph used for
        alrgorithm and this edge is found during execution of the algortithm.
        """
        # TODO: why not set edge as unsigned?
        super().__init__("Error A6: Edge {} has negative length\
                         {}.".format(edge, length))


class AlgorithmDijkstraNetworkNotConnectedException(LinTimException):

    def __init__(self, sourceNode: Node, notConnectedTargetNode: Node):
        """
        Raise if a shortest path into an unconnected part of the network was
        queried.
        """
        super().__init__("Error A7: Node {} is not connected to node\
                         {}, but a shortest path was\
                         queried.".format(sourceNode, notConnectedTargetNode))
