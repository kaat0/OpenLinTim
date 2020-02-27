from core.exceptions.exceptions import LinTimException


class ConfigNoFileNameGivenException(LinTimException):
    """Exception to throw if no config file name to read is given to a program but one is needed"""

    def __init__(self):
        """
        Initialise a new exception
        """
        super().__init__("Error C4: No config file name given.")


class ConfigKeyNotFoundException(LinTimException):
    """Exception to throw if a config key cannot be found"""

    def __init__(self, config_key: str):
        """
        Initialise a new exception
        :param config_key: the key that could not be found
        """
        super().__init__("Error C2: Config parameter {} does not exists".format(config_key))


class ConfigTypeMismatchException(LinTimException):
    """Exception to throw if the type of the config parameter does not match"""

    def __init__(self, config_key: str, expected_type: str, config_parameter: str):
        """
        Initialise a new exception
        :param config_key: the key with the false type
        :param expected_type: name of the expected type
        :param config_parameter: the actual parameter found
        """
        super().__init__(
            "Error C3: Config parameter {} should be of type {} but is {}.".format(config_key, expected_type,
                                                                                   config_parameter))
