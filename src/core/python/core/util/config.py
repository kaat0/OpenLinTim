import logging

from core.exceptions.config_exceptions import ConfigKeyNotFoundException, ConfigTypeMismatchException
from core.solver.generic_solver_interface import SolverType


class Config:
    """
    Implementation of a config class, handling all the config interaction. Based on the "old" LinTim implementation. Has
    static and non-static methods, where the static methods operate on a "default" config object
    """

    logger_ = logging.getLogger(__name__)

    def __init__(self):
        """
        Initialize an empty config.
        """
        self.data = {}

    def getStringValue(self, key):
        """
        Get the string value of the given config key.
        :param key: the key to look for in the config
        :return: the value to the given key
        """
        if key == "default_activities_periodic_file" and "use_buffered_activities" in self.data and \
                self.getBooleanValue("use_buffered_activities"):
            key = "default_activity_buffer_file"
        elif key == "default_activities_periodic_unbuffered_file":
            if "use_buffered_activities" in self.data and self.getBooleanValue("use_buffered_activities"):
                key = "default_activity_relax_file"
            else:
                key = "default_activities_periodic_file"
        if key not in self.data:
            raise ConfigKeyNotFoundException(key)
        value = self.data[key]
        self.logger_.debug("Read key {0} with value {1} from config".format(key, value))
        return value

    def getBooleanValue(self, key):
        """
        Get the boolean value of the given config key
        :param key: the key to look for
        :return: the boolean value to the given key
        """
        value = self.getStringValue(key).lower()
        if value == "true":
            return True
        elif value == "false":
            return False
        else:
            raise ConfigTypeMismatchException(key, "boolean", value)

    def getDoubleValue(self, key):
        """
        Get the double value of the given config key
        :param key: the key to look for
        :return: the double value to the given key
        """
        value = self.getStringValue(key)
        try:
            return float(value)
        except ValueError:
            raise ConfigTypeMismatchException(key, "double", value)

    def getIntegerValue(self, key):
        """
        Get the integer value of the given config key
        :param key: the key to look for
        :return: the integer value to the given key
        """
        value = self.getStringValue(key)
        try:
            return int(value)
        except ValueError:
            raise ConfigTypeMismatchException(key, "integer", value)

    def getSolverType(self, key):
        """
        Get the solver type value of the given config key
        :param key: the key to look for
        :return: the solver type of the given key
        """
        value = self.getStringValue(key).upper()
        if value == "XPRESS":
            return SolverType.XPRESS
        elif value == "GUROBI":
            return SolverType.GUROBI
        elif value == "CPLEX":
            return SolverType.CPLEX
        elif value == "GLPK":
            return SolverType.GLPK
        else:
            raise ConfigTypeMismatchException(key, "XPRESS/GUROBI/CPLEX/GLPK", value)

    def getLogLevel(self, key):
        """
        Get the log level value of the given config key
        :param key: the key to look for
        :return: the log level of the given key
        """
        value = self.getStringValue(key).upper()
        if value == "FATAL":
            return logging.CRITICAL
        elif value == "ERROR":
            return logging.ERROR
        elif value == "WARN":
            return logging.WARNING
        elif value == "INFO":
            return logging.INFO
        elif value == "DEBUG":
            return logging.DEBUG
        else:
            raise ConfigTypeMismatchException(key, "FATAL/ERROR/WARN/INFO/DEBUG", value)

    def put(self, key: str, value):
        """
        Put the specified data into the config collection. Content with the same key will be overwritten
        :param key: the key to add
        :param value: the value to add
        """
        self.data[key] = str(value)

    def __str__(self):
        return '\n'.join([('{0}; {1}'.format(key, value)) for (key, value) in self.data.items()])

    def __eq__(self, other):
        if isinstance(other, self.__class__):
            return self.data == other.data
        return NotImplemented

    def __ne__(self, other):
        if isinstance(other, self.__class__):
            return not self.__eq__(other)
        return NotImplemented

    def __hash__(self):
        return hash(self.data)

    @staticmethod
    def putStatic(key, value):
        """
        Put the specified data into the config collection of the default config. Content with the same key will be
        overwritten.
        :param key: the key to add
        :param value: the value to add
        """
        default_config.put(key, value)

    @staticmethod
    def getStringValueStatic(key):
        """
        Get the string value of the given config key from the default config.
        :param key: the key to look for in the config
        :return: the value to the given key
        """
        return default_config.getStringValue(key)

    @staticmethod
    def getDoubleValueStatic(key):
        """
        Get the double value of the given config key from the default config
        :param key: the key to look for
        :return: the double value to the given key
        """
        return default_config.getDoubleValue(key)

    @staticmethod
    def getIntegerValueStatic(key):
        """
        Get the integer value of the given config key from the default config.
        :param key: the key to look for
        :return: the integer value to the given key
        """
        return default_config.getIntegerValue(key)

    @staticmethod
    def getBooleanValueStatic(key):
        """
        Get the boolean value of the given config key from the default config
        :param key: the key to look for
        :return: the boolean value to the given key
        """
        return default_config.getBooleanValue(key)

    @staticmethod
    def getSolverTypeStatic(key):
        """
        Get the solver type value of the given config key from the default config.
        :param key: the key to look for
        :return: the solver type of the given key
        """
        return default_config.getSolverType(key)

    @staticmethod
    def getLogLevelStatic(key):
        """
        Get the log level value of the given config key from the default config.
        :param key: the key to look for
        :return: the log level of the given key
        """
        return default_config.getLogLevel(key)

    @staticmethod
    def getDefaultConfig() -> "Config":
        return default_config


# The default config object. Will be used by all non-class methods in this file
default_config = Config()
