from core.exceptions.exceptions import LinTimException


class StatisticKeyNotFoundException(LinTimException):

    def __init__(self, key: str):
        super().__init__("Error ST2: Statistic parameter {} does not exist.".format(key))


class StatisticTypeMismatchException(LinTimException):

    def __init__(self, key: str, expected_type: str, value: str):
        super().__init__("Error ST1: Statistic key {} should have type {} but has value {}.".format(key, expected_type, value))