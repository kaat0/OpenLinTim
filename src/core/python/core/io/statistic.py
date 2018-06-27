import logging
import math
from typing import List

from core.exceptions.input_exceptions import InputFormatException
from core.exceptions.output_exceptions import OutputFileException
from core.io.csv import CsvReader, CsvWriter
from core.util.statistic import Statistic
from core.util.config import Config, default_config


class StatisticReader:
    """
    Class to process csv-lines, formatted in the LinTim statistic format. Use
    CsvReader with the appropriate processing methods as a BiConsumer argument
    to read the files.
    """

    def __init__(self, sourceFileName: str, statistic: Statistic):
        self.sourceFileName = sourceFileName
        self.statistic = statistic

    def process_statistic_line(self, args: List[str], lineNumber: int):
        """
        Process the contents of a statistic line.
        :param args     the content of the line
        :param lineNumber   the line number, used for error handling
        :raise exceptions   if the line does not exactly contain 2 entries
        """
        if len(args) != 2:
            raise InputFormatException(self.sourceFileName, len(args), 2)
        key = args[0]
        value = args[1]
        if len(value) > 2 and value.startswith("\"") and value.endswith("\""):
            value = value.strip("\"")
        self.statistic.setValue(key, value)

    @staticmethod
    def read(file_name: str = "", statistic: Statistic = None, config: Config = Config.getDefaultConfig()) -> Statistic:
        """
        Read the statistic defined by the given file name.
        :param config:
        :param file_name: the filename to read the statistic
        :param statistic    if statistic exists
        :return: the statistic
        """
        if not statistic:
            statistic = Statistic()
        if not file_name:
            file_name = config.getStringValue("default_statistic_file")
        reader = StatisticReader(file_name, statistic)
        CsvReader.readCsv(file_name, reader.process_statistic_line)
        return statistic


class StatisticWriter:
    """
    Class implementing a static method to write a statistic. Call
    writeStatistic.
    """
    logger = logging.getLogger(__name__)

    @staticmethod
    def write(statistic: Statistic = Statistic.getDefaultStatistic(), file_name: str = "",
              config: Config = Config.getDefaultConfig()) -> None:
        if not file_name:
            file_name = config.getStringValue("default_statistic_file")
        CsvWriter.writeListStatic(file_name, list(statistic.getData().items()),
                                  lambda item: [item[0], CsvWriter.shortenDecimalValueIfItsDecimal(item[1])],
                                  lambda item: item[0])
