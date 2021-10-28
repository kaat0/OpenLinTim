from enum import Enum


class ActivityType(Enum):
    """
    Enumeration of all possible activity types.
    """
    DRIVE = "\"drive\""
    WAIT = "\"wait\""
    CHANGE = "\"change\""
    TURNAROUND = "\"turnaround\""
    HEADWAY = "\"headway\""
    SYNC = "\"sync\""
    VIRTUAL = "\"virtual\""
