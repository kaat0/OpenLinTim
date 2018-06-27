from datetime import datetime


def transform_time_to_minutes(time_str: str) -> int:
    """
    Read the visum formatted time and return the number of minutes
    :param time_str: the visum formatted time string
    :return: the minutes
    """
    return datetime.strptime(time_str, "%H:%M:%S").minute