from core.exceptions.exceptions import LinTimException


class DataIndexNotFoundException(LinTimException):
    """
    Exception to throw if an element with a specific index is not found.
    """

    def __init__(self, element: str, index: int):
        """
        Exception to throw if an element with a specific index is not found.
        :param element: type of element which is searched
        :param index: index of the element
        """
        super().__init__("Error D3: {} with index {} not found.".format(element, index))


class DataIllegalEventTypeException(LinTimException):
    """
    Exception to throw if the type of an event is undefined.
    """

    def __init__(self, event_id: int, event_type: str):
        """
        Exception to throw if the type of an event is undefined.
        :param event_id: event id
        :param event_type: event type
        """
        super().__init__("Error D4: {} of event {} is no legal event type.".format(event_id, event_type))


class DataIllegalActivityTypeException(LinTimException):
    """
    Exception to throw if the type of an activity is undefined.
    """

    def __init__(self, activity_id: int, activity_type: str):
        """
        Exception to throw if the type of an activity is undefined.
        :param activity_id: activity id
        :param activity_type: activity type
        """
        super().__init__("Error D5: {} of activity {} is no legal activity type.".format(activity_type, activity_id))


class DataIllegalLineDirectionException(LinTimException):
    """
    Exception to throw if the direction of an event is undefined.
    """

    def __init__(self, event_id: int, value: str):
        """
        Exception to throw if the direction of an event is undefined.
        :param event_id: event id
        :param value: the false direction value
        """
        super().__init__("Error D6: {} of event {} is no legal line direction".format(value, event_id))


class DataLinePoolCostInconsistencyException(LinTimException):
    """
    Exception to throw when the number of read line costs does not match the number of lines in the corresponding pool.
    """

    def __init__(self, lines: int, read_line_costs: int, cost_file_name: str):
        """
        Exception to throw when the number of read line costs does not match the number of lines in the corresponding
        pool.
        :param readLines: the number of lines in the line pool
        :param readLineCosts: the number of read line costs
        :param costFileName: the read cost file
        """
        super().__init__("Error D7: Read {} entries in the line cost file {}, but {} lines are in the line pool!"
                         .format(read_line_costs, cost_file_name, lines))
