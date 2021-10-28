from typing import List

from core.exceptions.input_exceptions import (InputFormatException,
                                              InputTypeInconsistencyException)
from core.model.graph import Graph
from core.model.impl.fullOD import FullOD
from core.model.impl.mapOD import MapOD
from core.model.infrastructure import InfrastructureNode
from core.model.od import OD, ODPair
from core.io.csv import CsvReader, CsvWriter
from core.model.ptn import Stop, Link
from core.util.config import Config, default_config


class ODReader:
    """
    Class to read files of od matrices.
    """

    def __init__(self, source_file_name: str, od: OD):
        """
        Constructor of an ODReader for a demand collection and a given file
        name. The given name will not influence the read file but the used name
        in any error message, so be sure to tuse the same name in here and in
        the CsvReader!
        """
        self.sourceFileName = source_file_name
        self.od = od

    def process_od_line(self, args: [str], lineNumber: int) -> None:
        """
        Process the contents of an od matric line.
        :param args     the content of the line
        :param lineNumber   the numberm used for error handling
        :raise exceptions   if the line does not contain exactly 3 entries
                            if the specific types of the entries do not match
                            the expectations.
        """
        if len(args) != 3:
            raise InputFormatException(self.sourceFileName, len(args), 3)
        try:
            origin = int(args[0])
        except ValueError:
            raise InputTypeInconsistencyException(self.sourceFileName, 1,
                                                  lineNumber, "int", args[0])
        try:
            destination = int(args[1])
        except ValueError:
            raise InputTypeInconsistencyException(self.sourceFileName, 2,
                                                  lineNumber, "int", args[1])
        try:
            passengers = float(args[2])
        except ValueError:
            raise InputTypeInconsistencyException(self.sourceFileName, 3,
                                                  lineNumber, "float", args[2])

        self.od.setValue(origin, destination, passengers)

    @staticmethod
    def read(od: OD, size: int = None, file_name: str = "", config: Config = Config.getDefaultConfig()) -> OD:
        """
        Read the given file into an od object. If parameters are not given but needed,
        the respective values will be read from the given config.
        :param od: the od to fill. If not given, an empty MapOD will be used. If a size is given, a FullOD of the
        corresponding size will be used
        :param size: the size of the FullOD to use (if no od is given directly)
        :param file_name: the file name to read the od matrix from
        :param config: the config to read the parameters from that are not given
        :return the read of matrix
        """
        if not od and size:
            od = FullOD(size)
        if not od:
            od = MapOD()
        if not file_name:
            file_name = config.getStringValue("default_od_file")
        reader = ODReader(file_name, od)
        CsvReader.readCsv(file_name, reader.process_od_line)
        return od

    @staticmethod
    def readNodeOd(od: OD, size: int = None, file_name: str = "", config: Config = Config.getDefaultConfig()) -> OD:
        """
        Read the given file into an od object. If parameters are not given but needed,
        the respective values will be read from the given config.
        :param od: the od to fill. If not given, an empty MapOD will be used. If a size is given, a FullOD of the
        corresponding size will be used
        :param size: the size of the FullOD to use (if no od is given directly)
        :param file_name: the file name to read the od matrix from
        :param config: the config to read the parameters from that are not given
        :return the read of matrix
        """
        if not file_name:
            file_name = config.getStringValue("filename_od_nodes_file")
        return ODReader.read(od, size, file_name, config)


class ODWriter:
    """
    Class implementing the writing of an od matrix as a static method. Just
    call write(Graph, OD, Config) to write the od matrix to the file
    specified in the config.
    """
    @staticmethod
    def write(ptn: Graph[Stop, Link], od: OD, file_name: str= "", header: str= "",
              config: Config = Config.getDefaultConfig()):
        """
        Write the given od matrix to the file specified in the config by
        default_od_file. Will write all od pairs, including those with weight
        0.
        :param ptn     the ptn the od matrix is based on
        :param od   the od matrix to write
        :param config   Used for reading the values of default_od_file and
        od_header
        :param file_name   the file name to write the od matrix to
        :param header     the header to write in the od file
        """

        od_pairs = []
        if not file_name:
            file_name = config.getStringValue("default_od_file")
        if not header:
            header = config.getStringValue("od_header")

        for origin in ptn.getNodes():
            for destination in ptn.getNodes():
                od_pairs.append(ODPair(origin.getId(), destination.getId(), od.getValue(origin.getId(), destination.getId())))
        CsvWriter.writeListStatic(file_name, od_pairs, ODPair.toCsvStrings, header=header)

    @staticmethod
    def writeNodeOd(od: OD, file_name: str="", header: str="",
                    config: Config = Config.getDefaultConfig()):
        """
        Write the given od matrix to the file specified or the corresponding file name from the config. Will write
        only the od pairs with positive demand
        :param od: the od object to write
        :param file_name: the file to write the od data to
        :param header: the header to use
        :param config: the config to read parameters from that are needed but not given
        """
        if not file_name:
            file_name = config.getStringValue("filename_od_nodes_file")
        if not header:
            header = config.getStringValue("od_nodes_header")
        od_pairs = od.getODPairs()
        CsvWriter.writeListStatic(file_name, od_pairs, ODPair.toCsvStrings, header=header)
