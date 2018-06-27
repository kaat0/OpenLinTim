import logging.config
from typing import List

import os

from core.exceptions.input_exceptions import (InputFileException,
                                              InputFormatException)
from core.exceptions.config_exceptions import ConfigTypeMismatchException
from core.io.csv import CsvReader
from core.util.config import Config


class ConfigReader:
    """
    A reader for config files. To use, initialize a new ConfigReader and use it
    to call the read_csv method of the
    CsvReader with the config file to read
    """

    def __init__(self, file_name: str, only_if_exists: bool, config: Config):
        """
        Initialize a new ConfigReader with the source file, that should be
        read, the onlyIfExists tag, i.e., whether
        an io error on "include" parameters should abort the reading process,
        and the config to put the read values in.
        :param file_name: the relative path to the config file to read
        :param only_if_exists: whether an io error on "include" parameters
        should abort the reading process. If set to
        true, io errors will be written to output but ignored
        :param config: the config to write the read parameters to
        """
        self._file_name = file_name
        self._only_if_exists = only_if_exists
        self._config = config

    def process_config_line(self, args: List[str], line_number: int):
        """
        Process the contents of a config line.
        :param args: the content of the line
        :param line_number: the line number, used for error handling
        :return:
        """
        # We accept at least two entries (more than two, if the value contains
        # a ";"
        if len(args) < 2:
            raise InputFormatException(self._file_name, len(args), 2)
        key = args[0]
        value = '; '.join(args[1:]).strip('"')
        if key == "include":
            try:
                abspath = os.path.abspath(os.path.join(self._file_name,
                                                       os.pardir))
                new_path = os.path.normpath(os.path.join(abspath, value))
                self.read(new_path, self._only_if_exists, self._config)
            except InputFileException:
                logging.getLogger(__name__)\
                    .warning("Caught InputFileException while reading {}"
                             .format(os.path.join(os.path.abspath(
                                 os.path.join(self._file_name, os.pardir))),
                                 value))
                if not self._only_if_exists:
                    raise
        elif key == "include_if_exists":
            abspath = os.path.abspath(os.path.join(self._file_name, os.pardir))
            new_path = os.path.normpath(os.path.join(abspath, value))
            if os.path.isfile(new_path):
                self.read(new_path, self._only_if_exists, self._config)
        elif key == "console_log_level":
            if value.upper() == "FATAL":
                log_level = logging.FATAL
            elif value.upper() == "ERROR":
                log_level = logging.ERROR
            elif value.upper() == "WARN":
                log_level = logging.WARNING
            elif value.upper() == "INFO":
                log_level = logging.INFO
            elif value.upper() == "DEBUG":
                log_level = logging.DEBUG
            else:
                raise ConfigTypeMismatchException(key, "FATAL/ERROR/WARN/INFO/DEBUG", value)
            logging.getLogger().setLevel(log_level)
            self._config.put(key, value)
        else:
            self._config.put(key, value)

    @staticmethod
    def read(file_name: str, only_if_exists: bool=True, config=Config.getDefaultConfig()):
        """
        Read the config values from the given file and store it into the given config. The finished config is returned.
        If no config is given, a new one will be created
        :param file_name: the relative path to the config file to read
        :param only_if_exists: whether an io error on "include" parameters should abort the reading process. If set to
        true, io errors will be written to output but ignored
        :param config: the config to write the read parameters to
        :return: the finished config file
        """
        reader = ConfigReader(file_name, only_if_exists, config)
        CsvReader.readCsv(file_name, reader.process_config_line)
        return config


# Initialize the logging config, when this file is first imported. Further
# imports will not change anything, the same
# logging config file can be imported multiple times without effect
file_location = os.path.realpath(__file__)
config_file_location = os.path.join(os.path.join(os.path.join(os.path.join(file_location, os.pardir),
                                                                           os.pardir), os.pardir), 'logging.conf')
logging.config.fileConfig(os.path.normpath(config_file_location), disable_existing_loggers=False)
