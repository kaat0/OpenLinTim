from core.exceptions.input_exceptions import (InputFormatException,
                                              InputTypeInconsistencyException)
# TODO: Implement this class
# from core.model.impl.fullOD import FullOD
from core.model.graph import Graph
from core.model.impl.fullOD import FullOD
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
    def read(od: OD, size: int, file_name: str = "", config: Config = Config.getDefaultConfig()) -> OD:
        if not od and not size:
            raise ValueError("For od reading, we need and od matrix or a size!")
        if not od:
            od = FullOD(size)
        if not file_name:
            file_name = config.getStringValue("default_od_file")
        reader = ODReader(file_name, od)
        CsvReader.readCsv(file_name, reader.process_od_line)
        return od


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
