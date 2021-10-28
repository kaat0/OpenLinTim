from typing import List

from core.exceptions.data_exceptions import DataIndexNotFoundException
from core.exceptions.input_exceptions import InputFormatException, InputTypeInconsistencyException
from core.exceptions.graph_exceptions import GraphEdgeIdMultiplyAssignedException, GraphIncidentNodeNotFoundException, \
    GraphNodeIdMultiplyAssignedException
from core.io.csv import CsvReader, CsvWriter
from core.model.graph import Graph
from core.model.impl.simple_dict_graph import SimpleDictGraph
from core.model.ptn import Stop, Link
from core.util.config import Config


class PTNReader:
    """
    Class containing all methods to read a PTN, i.e., methods for reading stops and links. Use a CsvReader with the
    appropriate processing methods as an argument to read the files.
    """

    def __init__(self, stop_file_name: str, link_file_name: str, ptn: Graph[Stop, Link], directed: bool,
                 load_file_name: str, headway_file_name: str, conversion_factor_length: float,
                 conversion_factor_coordinates: float):
        """
        Initialize a new PTN reader with the source files that should be read. The names of the files given here have no
        influence on the read files, but will be used for error handling, so be sure to give the same names as in the
        processor method.
        :param stop_file_name: the name of the stops file
        :param link_file_name: the name of the link file
        :param ptn: the ptn to add the stops and links to
        :param directed: whether the ptn should be directed
        :param load_file_name: the name of the load file
        :param headway_file_name: the name of the headway file
        :param conversion_factor_length: the factor to convert the input edge length into kilometers
        """
        self.stop_file_name = stop_file_name
        self.link_file_name = link_file_name
        self.ptn = ptn
        self.directed = directed
        self.load_file_name = load_file_name
        self.headway_file_name = headway_file_name
        self.conversion_factor_length = conversion_factor_length
        self.conversion_factor_coordinates = conversion_factor_coordinates

    def process_stop(self, args: List[str], line_number: int):
        """
        Process the contents of a stop line.
        :param args: the content of the line
        :param line_number: the line number, used for error handling
        """
        if len(args) != 5:
            raise InputFormatException(self.stop_file_name, len(args), 5)
        try:
            stop_id = int(args[0])
        except ValueError:
            raise InputTypeInconsistencyException(self.stop_file_name, 1, line_number, "int", args[0])
        short_name = args[1]
        long_name = args[2]
        try:
            x_coordinate = float(args[3]) * self.conversion_factor_coordinates
        except ValueError:
            raise InputTypeInconsistencyException(self.stop_file_name, 4, line_number, "float", args[3])
        try:
            y_coordinate = float(args[4]) * self.conversion_factor_coordinates
        except ValueError:
            raise InputTypeInconsistencyException(self.stop_file_name, 5, line_number, "float", args[4])
        if not self.ptn.addNode(Stop(stop_id, short_name, long_name, x_coordinate, y_coordinate)):
            raise GraphNodeIdMultiplyAssignedException(stop_id)

    def process_link(self, args: List[str], line_number: int) -> None:
        """
        Process the contents of a link line.
        :param args: the content of the line
        :param line_number: the line number, used for error handling
        """
        if len(args) != 6:
            raise InputFormatException(self.link_file_name, len(args), 6)

        try:
            link_id = int(args[0])
        except ValueError:
            raise InputTypeInconsistencyException(self.link_file_name, 1, line_number, "int", args[0])
        try:
            left_stop_id = int(args[1])
        except ValueError:
            raise InputTypeInconsistencyException(self.link_file_name, 2, line_number, "int", args[1])
        try:
            right_stop_id = int(args[2])
        except ValueError:
            raise InputTypeInconsistencyException(self.link_file_name, 3, line_number, "int", args[2])
        try:
            length = float(args[3]) * self.conversion_factor_length
        except ValueError:
            raise InputTypeInconsistencyException(self.link_file_name, 4, line_number, "float", args[3])
        try:
            lower_bound = int(args[4])
        except ValueError:
            raise InputTypeInconsistencyException(self.link_file_name, 5, line_number, "int", args[4])
        try:
            upper_bound = int(args[5])
        except ValueError:
            raise InputTypeInconsistencyException(self.link_file_name, 6, line_number, "int", args[5])

        left_stop = self.ptn.getNode(left_stop_id)
        if not left_stop:
            raise GraphIncidentNodeNotFoundException(link_id, left_stop_id)
        right_stop = self.ptn.getNode(right_stop_id)
        if not right_stop:
            raise GraphIncidentNodeNotFoundException(link_id, right_stop_id)
        link = Link(link_id, left_stop, right_stop, length, lower_bound, upper_bound, self.directed)
        if not self.ptn.addEdge(link):
            raise GraphEdgeIdMultiplyAssignedException(link_id)

    def process_load(self, args: List[str], line_number: int) -> None:
        """
        Process the contents of a load line.
        :param args: the content of the line
        :param line_number: the line number, used for error handling
        """
        if len(args) != 4:
            raise InputFormatException(self.load_file_name, len(args), 4)

        try:
            link_id = int(args[0])
        except ValueError:
            raise InputTypeInconsistencyException(self.load_file_name, 1, line_number, "int", args[0])
        try:
            load = float(args[1])
        except ValueError:
            raise InputTypeInconsistencyException(self.load_file_name, 2, line_number, "int", args[1])
        try:
            lower_frequency_bound = int(args[2])
        except ValueError:
            raise InputTypeInconsistencyException(self.load_file_name, 3, line_number, "int", args[2])
        try:
            upper_frequency_bound = int(args[3])
        except ValueError:
            raise InputTypeInconsistencyException(self.load_file_name, 4, line_number, "int", args[3])

        link = self.ptn.getEdge(link_id)
        if not link:
            raise DataIndexNotFoundException("Link", link_id)

        link.setLoad(load)
        link.setLowerFrequencyBound(lower_frequency_bound)
        link.setUpperFrequencyBound(upper_frequency_bound)

    def process_headway(self, args: List[str], line_number: int) -> None:
        """
        Process the contents of a headway line
        :param args: the content of the line
        :param line_number: the line number, used for error handling
        """
        if len(args) != 2:
            raise InputFormatException(self.load_file_name, len(args), 2)

        try:
            link_id = int(args[0])
        except ValueError:
            raise InputTypeInconsistencyException(self.load_file_name, 1, line_number, "int", args[0])
        try:
            headway = int(args[1])
        except ValueError:
            raise InputTypeInconsistencyException(self.load_file_name, 2, line_number, "int", args[1])

        link = self.ptn.getEdge(link_id)
        if not link:
            raise DataIndexNotFoundException("Link", link_id)

        link.setHeadway(headway)

    @staticmethod
    def read(read_stops: bool = True, read_links: bool = True, read_loads: bool = False, read_headways: bool = False,
             stop_file_name: str = "", link_file_name: str = "", load_file_name: str="", headway_file_name: str="",
             directed: bool = None, ptn: Graph=None, conversion_factor_length: float=None,
             conversion_factor_coordinates: float = None, config: Config = Config.getDefaultConfig()) \
            -> Graph[Stop, Link]:
        """
        Read the given files and add them to the ptn. If no ptn is given, a new one is created.
        :param read_headways:
        :param read_loads:
        :param read_links:
        :param read_stops:
        :param conversion_factor_coordinates:
        :param config:
        :param stop_file_name: the stop file name
        :param link_file_name: the link file name
        :param directed: whether the ptn is directed
        :param load_file_name: the load file name
        :param headway_file_name: the headway file name
        :param ptn: the ptn to add the data to
        :param conversion_factor_length: the factor to convert the length of a link into kilometers
        :return: the ptn with the added data
        """
        if not ptn:
            ptn = SimpleDictGraph()
        if directed is None:
            directed = not config.getBooleanValue("ptn_is_undirected")
        if read_stops:
            if not stop_file_name:
                stop_file_name = config.getStringValue("default_stops_file")
            if not conversion_factor_coordinates:
                conversion_factor_coordinates = config.getDoubleValue("gen_conversion_coordinates")
        if read_links:
            if not link_file_name:
                link_file_name = config.getStringValue("default_edges_file")
            if not conversion_factor_length:
                conversion_factor_length = config.getDoubleValue("gen_conversion_length")
            if directed is None:
                directed = not config.getBooleanValue("ptn_is_undirected")
        if read_loads and not load_file_name:
            load_file_name = config.getStringValue("default_loads_file")
        if read_headways and not headway_file_name:
            headway_file_name = config.getStringValue("default_headways_file")
        reader = PTNReader(stop_file_name, link_file_name, ptn, directed, load_file_name, headway_file_name,
                           conversion_factor_length, conversion_factor_coordinates)
        if read_stops:
            CsvReader.readCsv(stop_file_name, reader.process_stop)
        if read_links:
            CsvReader.readCsv(link_file_name, reader.process_link)
        if read_loads:
            CsvReader.readCsv(load_file_name, reader.process_load)
        if read_headways:
            CsvReader.readCsv(headway_file_name, reader.process_headway)
        return ptn


class PTNWriter:
    """
    Class implementing the writing of the ptn as a static method. Just call writePtn to write the PTN.
    """

    @staticmethod
    def write(ptn: Graph[Stop, Link], config: Config = Config.getDefaultConfig(), write_stops: bool = True,
              stop_file_name: str = "", stop_header: str = "", write_links: bool = True,
              link_file_name: str = "", link_header: str = "", write_loads: bool = False,
              load_file_name: str = "", load_header: str = "", write_headways: bool = False,
              headway_file_name: str = "", headway_header: str = ""):
        """
        Write the given ptn graph to the specified files. The parts to write can be controlled by write_stops,
        write_links, write_load and write_headways. If filename and/or header are not given for data to write, the
        respective values will be read from the given config (or from the default config, if none is given).
        :param ptn: the ptn to write
        :param config: the config to read. Will be used, if some values are not given for data to write.
        :param write_stops: whether to write the stops
        :param stop_file_name: the name of the file to write the stops
        :param stop_header: the header to write in the stop file
        :param write_links: whether to write the links
        :param link_file_name: the name of the file to write the links
        :param link_header: the header to write in the link file
        :param write_loads: whether to write the loads
        :param load_file_name: the name of the file to write the loads
        :param load_header: the header to write in the load file
        :param write_headways: whether to write the headways
        :param headway_file_name: the name of the file to write the headways
        :param headway_header: the header to write in the headway file
        """
        if write_stops:
            if not stop_file_name:
                stop_file_name = config.getStringValue("default_stops_file")
            if not stop_header:
                stop_header = config.getStringValue("stops_header")
            CsvWriter.writeListStatic(stop_file_name, ptn.getNodes(), Stop.toCsvStrings, Stop.getId, stop_header)

        links = ptn.getEdges()
        if write_links or write_loads or write_headways:
            links.sort(key=Link.getId)

        if write_links:
            if not link_file_name:
                link_file_name = config.getStringValue("default_edges_file")
            if not link_header:
                link_header = config.getStringValue("edges_header")
            CsvWriter.writeListStatic(link_file_name, links, Link.toCsvStrings, header=link_header)

        if write_loads:
            if not load_file_name:
                load_file_name = config.getStringValue("default_loads_file")
            if not load_header:
                load_header = config.getStringValue("loads_header")
            CsvWriter.writeListStatic(load_file_name, links, Link.toCsvLoadStrings, header=load_header)

        if write_headways:
            if not headway_file_name:
                headway_file_name = config.getStringValue("default_headways_file")
            if not headway_header:
                headway_header = config.getStringValue("headways_header")
            CsvWriter.writeListStatic(headway_file_name, links, Link.toCsvHeadwayStrings, header=headway_header)
