from typing import List

import numpy

from core.model.od import OD, ODPair
from core.exceptions.index_exceptions import IndexOutOfBoundsException


class FullOD(OD):
    """
    Implements the od interface using a two dimensional numpy array. Therefore
    acessing the od pairs by indes is cheap but getting nonempty od-pairs by
    getODPairs may be expensive.
    :param size     the size of the matrix
    """

    def __init__(self, size: int):
        self.matrix = numpy.zeros(shape=(size, size), dtype=float)

    def raiseForInvalidIndexPair(self, origin: int, destination: int):
        if self.matrix.shape[0] < origin or origin <= 0:
            exceptionKey = "Origin"
            raise IndexOutOfBoundsException(exceptionKey, origin,
                                            len(self.matrix))
        if self.matrix.shape[1] < destination or destination <= 0:
            exceptionKey = "Destination"
            raise IndexOutOfBoundsException(exceptionKey, origin,
                                            len(self.matrix))

    def getValue(self, origin: int, destination: int) -> float:
        self.raiseForInvalidIndexPair(origin, destination)
        return self.matrix[origin - 1][destination - 1]

    def setValue(self, origin: int, destination: int, new_value: float) -> None:
        self.raiseForInvalidIndexPair(origin, destination)
        self.matrix[origin - 1][destination - 1] = new_value

    def computeNumberOfPassengers(self) -> float:
        sumValue = 0
        for value in numpy.nditer(self.matrix):
                sumValue += value
        return sumValue

    def getODPairs(self) -> List[ODPair]:
        allODPairs = []
        it = numpy.nditer(self.matrix, flags=["multi_index"])
        while not it.finished:
            if not numpy.isclose(it[0], 0):
                allODPairs.append(ODPair(it.multi_index[0]+1, it.multi_index[1]+1, it[0]))
            it.iternext()
        return allODPairs

    def __str__(self) -> str:
        return "Full OD:\n" + "\n".join([str(pair) for pair in self.getODPairs()])

    def __eq__(self, other) -> bool:
        if not isinstance(other, FullOD):
            return False
        return numpy.allclose(self.matrix, other.matrix)

    def __ne__(self, other):
        return not self.__eq__(other)

    def __hash__(self):
        return hash(self.matrix)
