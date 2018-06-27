from core.exceptions.exceptions import LinTimException


class InputFileException(LinTimException):
    """Exception to throw if an input file cannot be found."""

    def __init__(self, file_name: str):
        """
        Initialise a new exception
        :param file_name: name of the file that could not be found
        """
        super().__init__("Error I1: File {} cannot be found.".format(file_name))


class InputFormatException(LinTimException):
    """
    Exception to throw if the input file is not formatted correctly, i.e., if the wrong number of columns is given.
    """

    def __init__(self, file_name: str, columns_given: int, columns_required: int):
        """
        Initialise a new exception
        :param file_name: the read file
        :param columns_given: the number of columns given
        :param columns_required: the required number of columns
        """
        super().__init__("Error I2: File {} is not formatted correctly: {} columns given, {} needed."
                         .format(file_name, columns_given, columns_required))


class InputTypeInconsistencyException(LinTimException):
    """
    Exception to throw if the input file has a type inconsistent.
    """

    def __init__(self, file_name: str, column_index: int, line_number: int, expected_type: str, found: str):
        """
        Exception to throw if the input file has a type inconsistent.
        :param file_name: input file name
        :param column_index: column in which exception occurs
        :param line_number: number of line in which the exception occurs
        :param expected_type: expected type
        :param found: entry of wrong type
        """
        super().__init__("Error I3: Column {} of file {} should be of type {} but entry in line {} is {}."
                         .format(column_index, file_name, expected_type, line_number, found))
