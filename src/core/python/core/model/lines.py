from enum import Enum
import logging
import math
from typing import Dict

from core.io.csv import CsvWriter
from core.model.impl.list_path import ListPath
from core.model.path import Path
from core.model.ptn import Stop, Link


class LineDirection(Enum):
    """
    Enum containing forwards and backwards direction for a line. Used in giving
    periodic events a direction.
    """
    FORWARDS = ">"
    BACKWARDS = "<"


class Line:
    """
    A class for representing a line as path of Stop and Link.
    """
    logger = logging.getLogger(__name__)

    def __init__(self, line_id: int, directed: bool, length: float = 0,
                 cost: float = 0, frequency: int = 0,
                 line_path: Path[Stop, Link]=None):
        """
        Constructor for a line with given information.
        :param line_id: the id of the new line
        :param directed: whether the line should be directed
        :param length: length of the line
        :param cost: cost of the line
        :param frequency: frequency of the line
        :param line_path: path of the line
        """
        self.line_id = line_id
        self.directed = directed
        self.length = length
        self.cost = cost
        self.frequency = frequency
        self.line_path = line_path
        if not line_path:
            self.line_path = ListPath(directed)

    def addLink(self, link: Link, compute_cost_and_length: bool = False,
                factor_cost_length: float = 0,
                factor_cost_link: float = 0) -> bool:
        """
        Method to add a new link to the line and factor_cost_link and
        factor_cost_length*length to the line cost, if the
        parameter is set accordingly
        :param link: the link to add
        :param compute_cost_and_length: whether to update the cost and the
                                        length of the line
        :param factor_cost_length: factor of the cost depending on the length
                                   of the line
        :param factor_cost_link: factor of the cost depending on the number of
                                 links in the line
        :return: whether the link could be added
        """
        if self.line_path.containsNode(link.getLeftNode()) and self.line_path.containsNode(link.getRightNode()):
            self.logger.warning("Line {} now contains a loop, closed by link\
                                {}. This may create problems in the LinTim\
                                algorithms!".format(self.getId(),
                                                    link.getId()))
        result = self.line_path.addLastEdge(link)
        if compute_cost_and_length and result:
            self.length += link.getLength()
            self.cost += (link.getLength()
                          * factor_cost_length
                          + factor_cost_link)
        return result

    def toLineCostCsvStrings(self) -> [str]:
        """
        Return a string list, representing the line for a LinTim pool cost csv
        file
        :return: the cost csv representation of this line
        """
        return [str(self.getId()),
                CsvWriter.shortenDecimalValueForOutput(self.getLength()),
                CsvWriter.shortenDecimalValueForOutput(self.getCost())
                ]

    def getId(self) -> int:
        """
        Gets the id of the line.
        :return: line id
        """
        return self.line_id

    def getLength(self) -> float:
        """
        Gets the length of the line.
        :return: length of the line
        """
        return self.length

    def getCost(self) -> float:
        """
        Gets the cost of the line.
        :return: cost of the line
        """
        return self.cost

    def getFrequency(self) -> int:
        """
        Gets the frequency of the line.
        :return: frequency of the line
        """
        return self.frequency

    def getLinePath(self) -> Path[Stop, Link]:
        """
        Gets the path belonging to the line.
        :return: the path of the line
        """
        return self.line_path

    def setFrequency(self, frequency: int) -> None:
        """
        Sets the frequency of the line.
        :param frequency: line frequency
        """
        self.frequency = frequency

    def setLength(self, length: float) -> None:
        """
        Sets the length of the line.
        :param length: length of the line
        """
        self.length = length

    def setCost(self, cost: float) -> None:
        """
        Sets the cost of a line
        :param cost: cost of the line
        """
        self.cost = cost

    def __eq__(self, other):
        if not isinstance(other, Line):
            return False
        return (self.getId() == other.getId()
                and math.isclose(self.getLength(), other.getLength())
                and math.isclose(self.getCost(), other.getCost)
                and self.getLinePath() == other.getLinePath())

    def __ne__(self, other):
        return not self.__eq__(other)

    def __hash__(self) -> int:
        return self.line_id

    def __str__(self):
        return ("Line " + ", ".join(self.toLineCostCsvStrings()) + ", Path "
                + str(self.line_path))


class LinePool:
    """
    A class to represent the line pool.
    """

    def __init__(self):
        """
        Constructor of a new empty line pool.
        """
        self.pool = {}  # type: Dict[int, Line]

    def addLine(self, line: Line) -> bool:
        """
        Method to add a line, if not already a line with the same id is in the
        pool.
        :param line: the line to add
        :return: whether the line could be added.
        """
        if line.getId() in self.pool:
            return False
        self.pool[line.getId()] = line
        return True

    def removeLine(self, line_id: int) -> bool:
        """
        Method to remove line with given id, if it exists in pool.
        :param line_id: id of the line to remove
        :return: whether a line was removed
        """
        if line_id not in self.pool:
            return False
        del self.pool[line_id]
        return True

    def getLines(self) -> [Line]:
        """
        Gets a list of the lines. This is a copy, i.e., removing or adding
        lines to the list will not change the linepool.
        :return: the lines in the pool
        """
        return list(self.pool.values())

    def getLine(self, line_id: int) -> Line:
        """
        Gets the line for a given id or raises KeyError if it is not in the
        pool.
        :param line_id: id of the line to get
        :return: the line with the given id
        """
        return self.pool[line_id]

    def getLineConcept(self) -> [Line]:
        """
        Method to get a list of all lines with frequency > 0.
        :return: a list of all lines with frequency > 0
        """
        return [line for line in self.pool.values() if line.getFrequency() > 0]

    def __eq__(self, other):
        if not isinstance(other, LinePool):
            return False
        return self.pool == other.pool

    def __ne__(self, other):
        return not self.__eq__(other)

    def __str__(self):
        return ("LineConcept:\n"
                + "\n".join(["{}:{}".format(line_id, line)
                             for line_id, line in self.pool.items()]))
