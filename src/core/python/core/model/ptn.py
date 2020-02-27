import math
from typing import List

from core.io.csv import CsvWriter
from core.model.graph import Edge, Node


class Stop(Node):
    """
    Template implementation of a stop in a PTN.
    """

    def __init__(self, stopId: int, shortName: str, longName: str, xCoordinate: float, yCoordinate: float) -> None:
        """
        Create a new Stop given the information of a LinTim stop.
        :param stopId: the id of the stop. Needs to be unique for any graph,
        this stop may be part of.
        :param shortName: the short name of the stop. This is a short
        representation of the stop. Need not be unique.
        :param longName: the long name of the stop. This is a longer
        representation of the stop.
        :param xCoordinate: the x-coordinate of the stop. This should be the
        longitude coordinate of the stop.
        :param yCoordinate: the y-coordinate of the stop. This should be the
        latitude coordinate of the stop.
        """
        self.stopId = stopId
        self.shortName = shortName
        self.longName = longName
        self.xCoordinate = xCoordinate
        self.yCoordinate = yCoordinate
        self.station = True

    def getId(self) -> int:
        return self.stopId

    def setId(self, newId: int) -> None:
        self.stopId = newId

    def getShortName(self) -> str:
        """
        Get the short name of the stop. This is a short representation of the
        stop. Need not be unique.
        :return: the short name
        """
        return self.shortName

    def getLongName(self) -> str:
        """
        Get the long name of the stop. This is a longer representation of the
        stop.
        :return: the long name
        """
        return self.longName

    def getXCoordinate(self) -> float:
        """
        Get the x-coordinate of the stop. This should be the longitude
        coordinate of the stop.
        :return: the x-coordinate
        """
        return self.xCoordinate

    def getYCoordinate(self) -> float:
        """
        Get the y-coordinate of the stop. This should be the latitude
        coordinate of the stop.
        :return: the y-coordinate
        """
        return self.yCoordinate

    def isStation(self) -> bool:
        """
        Get whether this stop is actually a station. E.g. it may be the case
        that the stop is just a candidate in a
        stop location problem and not a built station (at least at the time of
        creation). Is initially set to true.
        :return: whether the stop is a station
        """
        return self.station

    def setStation(self, isStation: bool) -> None:
        """
        Set whether this stop is actually a station. E.g. it may be the case
        that the stop is just a candidate in a
        stop location problem and not a built station (at least at the time of
        creation).
        :param isStation: the new value
        """

    def toCsvStrings(self) -> [str]:
        """
        Return a string list, representing the stop for a LinTim csv file.
        :return: the csv representation of this stop
        """
        x_coordinate = self.getXCoordinate()
        if math.isclose(x_coordinate, int(x_coordinate)):
            x_coordinate = int(x_coordinate)
        y_coordinate = self.getYCoordinate()
        if math.isclose(y_coordinate, int(y_coordinate)):
            y_coordinate = int(y_coordinate)
        return [str(self.getId()), self.getShortName(), self.getLongName(),
                str(x_coordinate), str(y_coordinate)]

    def __hash__(self) -> int:
        result = self.stopId
        result = 31 * result + self.shortName.__hash__()
        result = 31 * result + self.longName.__hash__()
        result = 31 * result + self.xCoordinate.__hash__()
        result = 31 * result + self.yCoordinate.__hash__()
        return result

    def __str__(self) -> str:
        return "Stop " + ", ".join(self.toCsvStrings())

    def __eq__(self, o: object) -> bool:
        if not isinstance(o, Stop):
            return False
        return (o.getId() == self.getId() and
                self.getShortName() == o.getShortName() and
                self.getLongName() == o.getLongName() and
                self.getXCoordinate() == o.getXCoordinate() and
                self.getYCoordinate() == o.getYCoordinate() and
                self.isStation() == o.isStation())

    def __ne__(self, other):
        return not self.__eq__(other)


class Link(Edge[Stop]):
    """
    A class representing an edge in a public transportation network (PTN).
    This class will contain all information that normally associated with a PTN
    edge in the LinTim context, i.e.,
    structural information as well as some passenger data.
    """

    def __init__(self, link_id: int, left_stop: Stop, right_stop: Stop,
                 length: float, lower_bound: int,
                 upper_bound: int, directed: bool) -> None:
        """
        Create a new Link, i.e., an edge in a Public Transportation Network.
        :param link_id: the id of the link, i.e., the id to reference the link.
                        Needs to be unique for a given graph
        :param left_stop: the left stop of the edge. This is the source of the
                          link, if the edge is directed
        :param right_stop: the right stop of the edge. This is the target of
                           the link if the edge is directed
        :param length: the length of the link, given in kilometers
        :param lower_bound: the lowerBound of the link, i.e., the minimal time
                            in minutes, a vehicle needs to traverse the edge.
        :param upper_bound: the upperBound of the link, i.e., the maximal time
                            in minutes, a vehicle needs to traverse the edge.
        :param directed: whether the link is directed
        """
        self.link_id = link_id
        self.left_stop = left_stop
        self.right_stop = right_stop
        self.length = length
        self.lower_bound = lower_bound
        self.upper_bound = upper_bound
        self.headway = 0
        self.directed = directed
        self.load = 0
        self.lower_frequency_bound = 0
        self.upper_frequency_bound = 0

    def getLeftNode(self) -> Stop:
        return self.left_stop

    def setId(self, new_id: int) -> None:
        self.link_id = id

    def getRightNode(self) -> Stop:
        return self.right_stop

    def getId(self) -> int:
        return self.link_id

    def isDirected(self) -> bool:
        return self.directed

    def getLength(self) -> float:
        """
        Get the length of the link, given in kilometers.
        :return: the length
        """
        return self.length

    def getLowerBound(self) -> int:
        """
        Get the lowerBound of the link, i.e., the minimal time in minutes, a
        vehicle needs to  traverse the edge.
        :return: the lower bound
        """
        return self.lower_bound

    def getUpperBound(self) -> int:
        """
        Get the upperBound of the link, i.e., the maximal time in minutes, a
        vehicle needs to traverse the edge.
        :return: the upper bound
        """
        return self.upper_bound

    def getLoad(self) -> float:
        """
        Get the load of the link, i.e., how many passengers traverse this link
        in the given period.
        :return: the load
        """
        return self.load

    def setLoad(self, new_load: float) -> None:
        """
        Set the load of the link, i.e., how many passengers traverse this link
        in the given period.
        :param new_load: the new load
        """
        self.load = new_load

    def getLowerFrequencyBound(self) -> int:
        """
        Get the lower frequency bound on the link, i.e., the minimal number of
        times a vehicle needs to traverse this
        link in a given period to serve the load.
        :return: the lower frequency bound
        """
        return self.lower_frequency_bound

    def setLowerFrequencyBound(self, new_bound: int) -> None:
        """
        Set the lower frequency bound on the link, i.e., the minimal number of
        times a vehicle needs to traverse this
        link in a given period to serve the load.
        :param new_bound: the lower frequency bound
        """
        self.lower_frequency_bound = new_bound

    def getUpperFrequencyBound(self) -> int:
        """
        Get the upper frequency bound on the link, i.e., the maximal number of
        times a vehicle may traverse this link
        in a given period.
        :return: the upper frequency bound
        """
        return self.upper_frequency_bound

    def setUpperFrequencyBound(self, new_bound: int) -> None:
        """
        Set the upper frequency bound on the link, i.e., the maximal number of
        times a vehicle may traverse this link
        in a given period.
        :param new_bound: the upper frequency bound
        """
        self.upper_frequency_bound = new_bound

    def setLoadInformation(self, load: float, lower_frequency_bound: int,
                           upper_frequency_bound: int) -> None:
        """
        Set all information regarding the passenger load for the link.
        :param load: the new load of the link, i.e., how many passengers
                     traverse this link in the given period
        :param lower_frequency_bound: the lower frequency bound on the link,
                                      i.e., the minimal number of times a
                                      vehicle
        needs to traverse this link in a given period to serve the load
        :param upper_frequency_bound: the upper frequency bound on the link,
                                      i.e., the maximal number of times a
                                      vehicle
        may traverse this link in a given period
        """
        self.setLoad(load)
        self.setLowerFrequencyBound(lower_frequency_bound)
        self.setUpperFrequencyBound(upper_frequency_bound)

    def getHeadway(self):
        """
        Get the headway of the stop. This is the minimal time needed between
        two vehicle that serve this stop. Given
        in minutes. Is initially set to 0.
        :return: the headway of the stop
        """
        return self.headway

    def setHeadway(self, new_headway: int):
        """
        Set the headway of the stop. This is the minimal time needed between
        two vehicle that serve this stop. Should
        be given in minutes.
        :param new_headway: the new headway
        """
        self.headway = new_headway

    def toCsvStrings(self) -> List[str]:
        """
        Return a string list, representing the link for a LinTim csv file.
        :return: the csv representation of this link
        """
        return [str(self.getId()),
                str(self.getLeftNode().getId()),
                str(self.getRightNode().getId()),
                CsvWriter.shortenDecimalValueForOutput(self.getLength()),
                str(self.getLowerBound()),
                str(self.getUpperBound())]

    def toCsvLoadStrings(self) -> [str]:
        """
        Return a string list, representing the link for a LinTim csv load file.
        :return: the csv load representation of this link
        """
        return [str(self.getId()),
                CsvWriter.shortenDecimalValueForOutput(self.getLoad()),
                str(self.getLowerFrequencyBound()),
                str(self.getUpperFrequencyBound())]

    def toCsvHeadwayStrings(self) -> [str]:
        """
        Return a string list, representing the link for a LinTim csv headway
        file.
        :return: the csv headway representation of this link
        """
        return [str(self.getId()), str(self.getHeadway())]

    def __str__(self):
        return "Link " + ", ".join(self.toCsvStrings())

    def __eq__(self, o: object) -> bool:
        if not isinstance(o, Link):
            return False
        if not self.getId() == o.getId():
            return False
        if not math.isclose(self.getLength(), o.getLength()):
            return False
        if not self.getLowerBound() == o.getLowerBound():
            return False
        if not self.getUpperBound() == o.getUpperBound():
            return False
        if not math.isclose(self.getLoad(), o.getLoad()):
            return False
        if not self.getLowerFrequencyBound() == o.getLowerFrequencyBound():
            return False
        if not self.getUpperFrequencyBound() == o.getUpperFrequencyBound():
            return False
        if not self.getHeadway() == o.getHeadway():
            return False
        if not self.isDirected() == o.isDirected():
            return False
        # Check for the directed attribute and compare the edges
        if self.isDirected():
            return (self.getLeftNode() == o.getLeftNode()
                    and self.getRightNode() == o.getRightNode())
        else:
            return ((self.getLeftNode() == o.getLeftNode()
                     and self.getRightNode() == o.getRightNode())
                    or (self.getLeftNode() == o.getRightNode()
                        and self.getRightNode() == o.getLeftNode()))

    def __ne__(self, other):
        return not self.__eq__(other)

    def __hash__(self):
        return hash((self.link_id, self.left_stop, self.right_stop, self.length, self.lower_bound, self.upper_bound, self.directed))
