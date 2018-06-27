import logging
from typing import Dict

from core.model.graph import Graph
from core.model.impl.dict_graph import DictGraph
from core.model.lines import Line
from core.model.periodic_ean import PeriodicEvent, PeriodicActivity, LineDirection, EventType
from core.model.ptn import Stop
from visum_transformer.model.net import Net, NetSection
from visum_transformer.util import constants
from visum_transformer.util.net_helper import transform_time_to_minutes


logger = logging.getLogger(__name__)


def get_fixed_times(net: Net, whole_ean: Graph[PeriodicEvent, PeriodicActivity], fixed_lines: Dict[str, Line],
                    directed: bool, period_length: int) -> Graph[PeriodicEvent, PeriodicActivity]:
    """
    Get the fixed times for the given lines.
    :param net: the net file object
    :param whole_ean: the complete ean
    :param fixed_lines: the fixed lines to read the times for
    :param directed: whether the lines are directed
    :param period_length: the period length
    :return: an ean containing the fixed events
    """
    time_traversals = net.get_section(constants.LINE_TIME_TRAVERSAL_SECTION_HEADER)
    fixed_ean: Graph[PeriodicEvent, PeriodicActivity] = DictGraph()
    for line_name, line in fixed_lines.items():
        logger.debug("Searching for events for {}, {}".format(line_name, line.getId()))
        new_events = set_times_for_line(line, line_name, LineDirection.FORWARDS, time_traversals, whole_ean)
        logger.debug("Found events")
        for new_event in new_events:
            logger.debug("Add event {}".format(new_event))
            fixed_ean.addNode(new_event)
        if not directed:
            reverse_events = set_times_for_line(line, line_name, LineDirection.BACKWARDS, time_traversals, whole_ean)
            for new_event in reverse_events:
                logger.debug("Add event {}".format(new_event))
                fixed_ean.addNode(new_event)
            new_events.extend(reverse_events)
        set_time_for_line_repetitions(line, new_events, whole_ean, fixed_ean, period_length)
    return fixed_ean


def set_times_for_line(line: Line, line_name: str, direction: LineDirection, time_section: NetSection,
                       whole_ean: Graph[PeriodicEvent, PeriodicActivity]) -> [PeriodicEvent]:
    """
    Read the times for the given line.
    :param line: the line to read the times for
    :param line_name: the line name
    :param direction: the direction of the line
    :param time_section: the net section containing the times
    :param whole_ean: the complete ean
    :return: a list of the events with fixed times belonging to the given line and direction
    """
    stop_times = get_stop_times(line_name, direction, time_section)
    result = []
    stop_list = line.getLinePath().getNodes()
    if direction == LineDirection.BACKWARDS:
        stop_list.reverse()
    for stop, times in zip(stop_list, stop_times):
        arr_event, dep_event = find_events(line, direction, stop, whole_ean)
        if arr_event:
            arr_event.setTime(times[0])
            result.append(arr_event)
        if dep_event:
            dep_event.setTime(times[1])
            result.append(dep_event)
    return result


def get_stop_times(line_name: str, direction: LineDirection, time_section: NetSection) -> [(int, int)]:
    """
    Get the stop times for the given line in the given direction
    :param line_name: the name of the line to search for
    :param direction: the direction of the line to search for
    :param time_section: the net section containing the times
    :return: a list of the stop times for the given line in the given direction
    """
    time_rows = [row for row in time_section.get_rows() if
                 time_section.get_entry_from_row(row, constants.LINE_TIME_TRAVERSAL_LINE_NAME_HEADER) == line_name
                 and time_section.get_entry_from_row(row,
                                                     constants.LINE_TIME_TRAVERSAL_DIRECTION_HEADER) == direction.value]
    return [(transform_time_to_minutes(time_section.get_entry_from_row(row,
                                                                       constants.LINE_TIME_TRAVERSAL_ARRIVAL_HEADER)),
             transform_time_to_minutes(time_section.get_entry_from_row(row,
                                                                       constants.LINE_TIME_TRAVERSAL_DEPARTURE_HEADER)))
            for row in time_rows]


def find_events(line: Line, direction: LineDirection, stop: Stop, ean: Graph[PeriodicEvent, PeriodicActivity]) -> \
        (PeriodicEvent, PeriodicEvent):
    """
    Find the events belonging to the given line in the given direction.
    :param line: the line to search for
    :param direction: the direction to search for
    :param stop: the stop to search for
    :param ean: the ean to search the events in
    :return: the two corresponding events, first arrival, second departure
    """
    arr_event = ean.get_node_by_function(lambda e: event_corresponds_to(e, line.getId(), direction, stop.getId(),
                                                                        EventType.ARRIVAL, 1), True)
    dep_event = ean.get_node_by_function(lambda e: event_corresponds_to(e, line.getId(), direction, stop.getId(),
                                                                        EventType.DEPARTURE, 1), True)
    return arr_event, dep_event


def event_corresponds_to(event: PeriodicEvent, line_id: int, direction: LineDirection, stop_id: int,
                         event_type: EventType, frequency_repetiton: int) -> bool:
    """
    whether the given events corresponds to the given information
    :param event: the event to check
    :param line_id: the line id
    :param direction: the direction
    :param stop_id: the stop id
    :param event_type: the event type
    :param frequency_repetiton: the frequency repetition
    :return: whether the given event contains the given information
    """
    return event.getLineId() == line_id and event.getStopId() == stop_id and event.getType() == event_type \
           and event.getDirection() == direction and event.getLineFrequencyRepetition() == frequency_repetiton


def set_time_for_line_repetitions(line: Line, line_events: [PeriodicEvent], ean: Graph[PeriodicEvent, PeriodicActivity],
                                  fixed_ean: Graph[PeriodicEvent, PeriodicActivity], period_length: int) -> None:
    """
    Set the event times for the line repetitions.
    :param line: the line to set the times for
    :param line_events: the events for the first repetition of the line, containing the fixed times
    :param ean: the ean to search in
    :param fixed_ean: the fixed ean, the fixed events will be added here
    :param period_length: the period length
    """
    for event in line_events:
        for frequency in range(2, line.getFrequency() + 1):
            corresponding_event = ean.get_node_by_function(
                lambda e: event_corresponds_to(e, event.getLineId(), event.getDirection(), event.getStopId(),
                                               event.getType(), frequency), True
            )
            corresponding_time = (event.getTime() + (frequency-1) * (period_length/line.getFrequency())) % period_length
            corresponding_event.setTime(corresponding_time)
            fixed_ean.addNode(corresponding_event)
