import logging

from core.exceptions.data_exceptions import DataIndexNotFoundException, DataLinePoolCostInconsistencyException
from core.exceptions.input_exceptions import InputFormatException, InputTypeInconsistencyException
from core.exceptions.line_exceptions import LineLinkNotAddableException
from core.io.csv import CsvReader, CsvWriter
from core.model.graph import Graph
from core.model.lines import LinePool, Line
from core.model.ptn import Link, Stop
from core.util.config import Config, default_config


class LineReader:
    """
    Class to process csv-lines, formatted in the LinTim Line-Pool.giv, Line-Concept.lin  or Line-Pool-Cost.giv format.
    Use a CsvReader with the appropriate processing methods as an argument to read the files.
    """
    logger = logging.getLogger(__name__)

    def __init__(self, line_collection_file_name: str, line_pool_cost_file_name: str, line_pool: LinePool,
                 ptn: Graph[Stop, Link], directed: bool, read_frequencies: bool):
        """
        Constructor of a LinePoolReader for a line pool or line concept (depending on read_frequencies) and a given
        filename. The given name will not influence the read file but the used name in any error message, so be sure to
        use the same name in here and in the CsvReader!
        :param line_collection_file_name: source file name for exceptions
        :param line_pool_cost_file_name: source file name for exceptions
        :param line_pool: line pool
        :param ptn: the base ptn
        :param directed: whether the links are directed
        :param read_frequencies: whether a line pool or a line concept with frequencies is read
        """
        self.line_collection_file_name = line_collection_file_name
        self.line_pool_cost_file_name = line_pool_cost_file_name
        self.line_pool = line_pool
        self.ptn = ptn
        self.directed = directed
        self.read_frequencies = read_frequencies
        self.read_lines = set()

    def process_line_pool_line(self, args: [str], line_number: int) -> None:
        """
        Process the contents of a linepool or lineconcept line.
        :param args: the content of the line
        :param line_number: the line number, used for error handling
        """
        if not self.read_frequencies and len(args) != 3:
            raise InputFormatException(self.line_collection_file_name, len(args), 3)
        elif self.read_frequencies and len(args) != 4:
            raise InputFormatException(self.line_collection_file_name, len(args), 4)
        try:
            line_id = int(args[0])
        except ValueError:
            raise InputTypeInconsistencyException(self.line_collection_file_name, 1, line_number, "int", args[0])
        try:
            link_number = int(args[1])
        except ValueError:
            raise InputTypeInconsistencyException(self.line_collection_file_name, 2, line_number, "int", args[1])
        try:
            link_id = int(args[2])
        except ValueError:
            raise InputTypeInconsistencyException(self.line_collection_file_name, 3, line_number, "int", args[2])
        if link_number == 1:
            line = Line(line_id, self.directed)
            self.line_pool.addLine(line)
        else:
            try:
                line = self.line_pool.getLine(line_id)
            except ValueError:
                raise DataIndexNotFoundException("Line", line_id)
        link = self.ptn.getEdge(link_id)
        if not link:
            raise DataIndexNotFoundException("Link", link_id)

        if not line.addLink(link):
            raise LineLinkNotAddableException(link_id, line_id)

        if self.read_frequencies and link_number == 1:
            try:
                frequency = int(args[3])
            except ValueError:
                raise InputTypeInconsistencyException(self.line_collection_file_name, 4, line_number, "int", args[3])
            line.setFrequency(frequency)

    def process_line_cost_line(self, args: [str], line_number: int) -> None:
        """
        Process the contents of a line cost line.
        :param args: the content of the line
        :param line_number: the line number, used for error handling
        """
        if len(args) != 3:
            raise InputFormatException(self.line_pool_cost_file_name, len(args), 3)
        try:
            line_id = int(args[0])
        except ValueError:
            raise InputTypeInconsistencyException(self.line_pool_cost_file_name, 1, line_number, "int", args[0])
        try:
            length = float(args[1])
        except ValueError:
            raise InputTypeInconsistencyException(self.line_pool_cost_file_name, 2, line_number, "float", args[1])
        try:
            cost = float(args[2])
        except ValueError:
            raise InputTypeInconsistencyException(self.line_pool_cost_file_name, 3, line_number, "float", args[2])
        try:
            line = self.line_pool.getLine(line_id)
        except ValueError:
            raise DataIndexNotFoundException("Line", line_id)
        line.setLength(length)
        line.setCost(cost)
        self.read_lines.add(line)

    @staticmethod
    def read(ptn: Graph[Stop, Link], read_lines: bool = True, read_costs: bool = True, read_frequencies: bool = True,
             line_file_name: str = "", line_cost_file_name: str = None, line_pool: LinePool = None,
             create_directed_lines: bool = None, config: Config = Config.getDefaultConfig()) -> LinePool:
        """
        Read the line pool or the line concept, depending on read_frequencies. The cost file will be read, if
        line_pool_cost_file_name is not empty or None. Read lines will be added to the given linepool, if there is some.
        :param line_file_name: the line collection file name to read.
        :param read_lines
        :param read_costs
        :param create_directed_lines
        :param config
        :param ptn: the base ptn
        :param line_pool: the line pool to add the lines to. If there is none, a new linepool will be created.
        :param line_cost_file_name: the cost file name to read. Can be None or empty.
        :param read_frequencies: whether to read a line collection or a line pool.
        :return: the line pool with the added lines.
        """
        if not read_lines and read_frequencies:
            LineReader.logger.warning("Can not read frequencies but no lines, will read lines as well!")
            read_lines = True
        if not line_pool:
            line_pool = LinePool()
        if read_lines and not line_file_name:
            if read_frequencies:
                line_file_name = config.getStringValue("default_lines_file")
            else:
                line_file_name = config.getStringValue("default_pool_file")
        if read_costs and not line_cost_file_name:
            line_cost_file_name = config.getStringValue("default_pool_cost_file")
        if create_directed_lines is None:
            create_directed_lines = ptn.isDirected()
        reader = LineReader(line_file_name, line_cost_file_name, line_pool, ptn, create_directed_lines,
                            read_frequencies)
        if read_lines:
            CsvReader.readCsv(line_file_name, reader.process_line_pool_line)
        if read_costs:
            CsvReader.readCsv(line_cost_file_name, reader.process_line_cost_line)
            if len(reader.read_lines) != len(line_pool.getLines()):
                raise DataLinePoolCostInconsistencyException(len(line_pool.getLines()), len(reader.read_lines),
                                                             line_cost_file_name)
        return line_pool


class LineWriter:
    """
    Class implementing writing line pools and concepts as static methods. Use the static methods to write.
    """
    logger = logging.getLogger(__name__)

    @staticmethod
    def write(pool: LinePool, write_pool: bool = True, write_costs: bool = True, write_line_concept: bool = True,
              pool_file_name: str = "", pool_header: str = "", cost_file_name: str = "", cost_header: str = "",
              concept_file_name: str = "", concept_header: str = "", config: Config = Config.getDefaultConfig()):
        """
        Write the given linepool, with or without the cost file. write_cost_file and write_pool_file can be used to
        determine what to write. If no file name or header is given and the respective file should be written, the
        values are read from the given config (or the default config, if there is none).
        :param write_line_concept:
        :param concept_header:
        :param concept_file_name:
        :param pool: the pool to write
        :param config: the config to use to read file names or headers, if necessary. Will use the default config if
        none is given
        :param write_costs: whether to write the cost file
        :param cost_file_name: the name of the file to write the cost file to
        :param cost_header: the header to write in the cost file
        :param write_pool: whether to write the pool file
        :param pool_file_name: the name of the file to write the pool to
        :param pool_header: the header to write in the pool file
        """
        # Sort the lines first, we may need to write them three times
        lines = pool.getLines()
        lines.sort(key=Line.getId)
        if write_pool:
            if not pool_file_name:
                pool_file_name = config.getStringValue("default_pool_file")
            if not pool_header:
                pool_header = config.getStringValue("lpool_header")
            pool_writer = CsvWriter(pool_file_name, pool_header)
            for line in lines:
                edge_index = 1
                for link in line.getLinePath().getEdges():
                    pool_writer.writeLine([str(line.getId()), str(edge_index), str(link.getId())])
                    edge_index += 1
            pool_writer.close()
        if write_costs:
            if not cost_file_name:
                cost_file_name = config.getStringValue("default_pool_cost_file")
            if not cost_header:
                cost_header = config.getStringValue("lpool_cost_header")
            CsvWriter.writeListStatic(cost_file_name, lines, Line.toLineCostCsvStrings, header=cost_header)
        if write_line_concept:
            if not concept_file_name:
                concept_file_name = config.getStringValue("default_lines_file")
            if not concept_header:
                concept_header = config.getStringValue("lines_header")
            line_writer = CsvWriter(concept_file_name, concept_header)
            for line in lines:
                edge_index = 1
                for link in line.getLinePath().getEdges():
                    line_writer.writeLine(
                        [str(line.getId()), str(edge_index), str(link.getId()), str(line.getFrequency())])
                    edge_index += 1
            line_writer.close()
