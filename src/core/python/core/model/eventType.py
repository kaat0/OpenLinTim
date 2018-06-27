from enum import Enum


class EventType(Enum):
    """
    Enumeration of all possible event types.
    """
    ARRIVAL = "\"arrival\""
    DEPARTURE = "\"departure\""

    def __init__(self, representation: str):
        self.representation = representation

    def __str__(self) -> str:
        return self.representation
