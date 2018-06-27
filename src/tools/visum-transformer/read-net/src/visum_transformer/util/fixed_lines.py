from datetime import datetime, timedelta
from typing import Dict, List
import logging

from core.model.graph import Graph
from core.model.lines import LinePool, Line
from core.model.periodic_ean import LineDirection
from core.model.ptn import Link, Stop
from core.util.config import Config
from visum_transformer.model.net import Net
from visum_transformer.util import constants
from visum_transformer.util.constants import VEHICLE_COMBINATION_UNITS_HEADER
from visum_transformer.util.net_helper import transform_time_to_minutes

logger = logging.getLogger(__name__)


def get_fixed_lines_from_visum(net: Net, ptn: Graph[Stop, Link]) -> (LinePool, Dict[Line, int]):
    """
    Get a linepool containing the fixed lines from the net file object
    :param net: the net file object to read the lines from
    :param ptn: the underlying ptn
    :return: a line pool containing only the lines to fix
    """
    store_stops_in_translator(ptn)
    store_links_in_translator(ptn, net)
    name_combination_dict = find_fixed_line_names_and_unit_combinations(net)
    fixed_lines, line_to_name_dict = read_fixed_lines(ptn, net, list(name_combination_dict.keys()))
    compute_frequencies(fixed_lines, line_to_name_dict, net)
    capacities = find_line_capacities(net, line_to_name_dict, name_combination_dict)
    return fixed_lines, capacities


def find_line_capacities(net: Net, line_to_name_dict: Dict[Line, str], name_combination_dict: Dict[str, int]) \
        -> Dict[Line, int]:
    vehicle_unit_lookup_section = net.get_section(constants.VEHICLE_UNITS_COMBINATION_HEADER)
    capacity_lookup_section = net.get_section(constants.VEHICLE_UNITS_SECTION_HEADER)
    result = {}
    for line, line_name in line_to_name_dict.items():
        vehicle_combination_number = name_combination_dict[line_name]
        vehicle_unit_number = int(vehicle_unit_lookup_section.get_entry(int(vehicle_combination_number),
                                                                        VEHICLE_COMBINATION_UNITS_HEADER))
        capacity = capacity_lookup_section.get_entry(vehicle_unit_number, constants.VEHICLE_UNITS_CAPACITY_HEADER)
        result[line] = int(capacity)
    return result


def store_stops_in_translator(ptn: Graph[Stop, Link]) -> None:
    """
    Store all the stops in a translator, i.e., by their short name
    :param ptn: the ptn to store
    """
    for stop in ptn.getNodes():
        StopTranslator.add_stop(stop, stop.getShortName())


def store_links_in_translator(ptn: Graph[Stop, Link], net: Net) -> None:
    """
    Store all links in a translator, i.e., by their name in the net file object
    :param ptn: the ptn to store
    :param net: the net file object to find the names
    """
    edge_section = net.get_section(constants.EDGE_SECTION_HEADER)
    for visum_link_line in edge_section.get_rows():
        name = edge_section.get_entry_from_row(visum_link_line, constants.EDGE_NAME_HEADER)
        from_name = edge_section.get_entry_from_row(visum_link_line, constants.EDGE_FROM_NAME_HEADER)
        to_name = edge_section.get_entry_from_row(visum_link_line, constants.EDGE_TO_NAME_HEADER)
        from_stop = StopTranslator.get_stop(from_name)
        to_stop = StopTranslator.get_stop(to_name)
        corresponding_link = get_link(ptn, from_stop, to_stop)
        if not corresponding_link:
            logger.fatal("Could not find link for visum link {}, going from {} to {}".format(name, from_stop, to_stop))
            raise ValueError("Could not find link for visum link {}".format(name))
        LinkTranslator.add_link(corresponding_link, name)


def find_fixed_line_names_and_unit_combinations(net: Net) -> Dict[str, int]:
    """
    Find all fixed line names and the corresponding unit combinations. The returned dict will contain the line name as
    key and the vehicle combination number as value
    :param net: the net to search in
    :return: a dict containing the line names and their corresponding unit combinations
    """
    line_section = net.get_section(constants.LINE_SECTION_HEADER)
    result = {}
    for row in line_section.get_rows():
        syscode = line_section.get_entry_from_row(row, constants.LINE_VSYSCODE_HEADER)
        if syscode == constants.NON_FIXED_VSYSCODE:
            continue
        name = line_section.get_entry_from_row(row, constants.LINE_NAME_HEADER)
        vehicle_combination_number = line_section.get_entry_from_row(row,
                                                                     constants.LINE_VEHICLE_COMBINATION_NUMBER_HEADER)
        result[name] = int(vehicle_combination_number)
    return result


def read_fixed_lines(ptn: Graph[Stop, Link], net: Net, fixed_line_names: List[str]) -> (LinePool, Dict[Line, str]):
    """
    Read all the fixed lines from the net file object and return them, including a map pointing to their net file names
    :param ptn: the underlying ptn
    :param net: the net file object to read from
    :param fixed_line_names: the names of the fixed lines
    :return: the line pool, containing the fixed lines, and a dict storing the line names by the lines
    """
    fixed_costs = Config.getDoubleValueStatic("lpool_costs_fixed")
    costs_length = Config.getDoubleValueStatic("lpool_costs_length")
    costs_edges = Config.getDoubleValueStatic("lpool_costs_edges")
    line_routes = net.get_section(constants.LINE_ROUTE_SECTION_HEADER)
    current_line_id = 1
    pool = LinePool()
    names: Dict[Line, str] = {}
    for name in fixed_line_names:
        current_line_route = [line for line in line_routes.get_rows()
                              if line_routes.get_entry_from_row(line, constants.LINE_ROUTE_NAME_HEADER) == name]
        node_list: List[Stop] = []
        for line_route_element in current_line_route:
            if not ptn.isDirected() \
                    and line_routes.get_entry_from_row(line_route_element, constants.LINE_ROUTE_DIRECTION_HEADER)\
                    == LineDirection.BACKWARDS.value:
                # We only need to consider one direction for undirected networks
                break
            node_list.append(StopTranslator.get_stop(line_routes.get_entry_from_row(line_route_element,
                                                                                    constants.LINE_ROUTE_NODE_HEADER)))
        link_stop_list = zip(node_list, node_list[1:])
        link_list = [get_link(ptn, source, target) for (source, target) in link_stop_list]
        new_line = Line(current_line_id, ptn.isDirected(), cost=fixed_costs)
        current_line_id += 1
        for link in link_list:
            new_line.addLink(link, True, costs_length, costs_edges)
        pool.addLine(new_line)
        names[new_line] = name
    return pool, names


def get_link(ptn: Graph[Stop, Link], source: Stop, target: Stop) -> Link:
    """
    Get the corresponding link to the two stops. Can return the undirected link, if there is no directed link and the
    ptn is undirected.
    :param ptn: the ptn to search in
    :param source: the source stop
    :param target: the target stop
    :return: the corresponding link to the two stops. May be undirected, if the ptn is undirected
    """
    link = ptn.get_edge_by_function(lambda l: l.getLeftNode() == source and l.getRightNode() == target, True)
    if not link and not ptn.isDirected():
        link = ptn.get_edge_by_function(lambda l: l.getRightNode() == source and l.getLeftNode() == target, True)
    return link


def compute_frequencies(pool: LinePool, line_names: Dict[Line, str], net: Net) -> None:
    """
    Find the frequencies to the given line pool from the net file object
    :param pool: the lines to search for
    :param line_names: the names of the lines in the net file object
    :param net: the net file object to read the frequencies from
    """
    line_traversals = net.get_section(constants.LINE_TRAVERSAL_SECTION_HEADER)
    for line in pool.getLines():
        line_name = line_names[line]
        line_traversals_times = [line_traversals.get_entry_from_row(line_traversal,
                                                                    constants.LINE_TRAVERSAL_TIME_HEADER)
                                 for line_traversal in line_traversals.get_rows()
                                 if line_traversals.get_entry_from_row(line_traversal,
                                                                       constants.LINE_TRAVERSAL_LINE_NAME_HEADER)
                                 == line_name]
        first_departure = transform_time_to_minutes(line_traversals_times[0])
        second_departure = transform_time_to_minutes(line_traversals_times[1])
        headway = abs(second_departure - first_departure)
        if headway == 0:
            # This means the line drives once per hour
            frequency = 1
        else:
            frequency = Config.getIntegerValueStatic("period_length") / headway
        if frequency - int(frequency) != 0:
            logger.warning("Got non-integer frequency for line {}!".format(line_name))
        line.setFrequency(int(frequency))


def merge_pools(read_pool: LinePool, fixed_lines: LinePool, directed: bool, capacities: Dict[Line, int]) -> None:
    """
    Merge the two line pools into one
    :param capacities:
    :param read_pool: the base pool
    :param fixed_lines: the lines to add
    :param directed: whether the lines should be directed
    """
    next_id = max([line.getId() for line in read_pool.getLines()]) + 1
    for line in fixed_lines.getLines():
        new_line = Line(next_id, directed, line.getLength(), line.getCost(), line.getFrequency(), line.getLinePath())
        next_id += 1
        read_pool.addLine(new_line)
        # We can add and remove from fixed_lines since .getLines() gets a copy of the current list, this will not be
        # changed!
        fixed_lines.removeLine(line.getId())
        capacity = capacities[line]
        del capacities[line]
        fixed_lines.addLine(new_line)
        capacities[new_line] = capacity


class StopTranslator:
    """
    Utility class for storing stops by their name
    """
    stops: Dict[str, Stop] = {}

    @staticmethod
    def add_stop(stop: Stop, name: str) -> None:
        """
        Add the given stop to the translator
        :param stop: the stop to add
        :param name: the name of the stop
        """
        StopTranslator.stops[name] = stop

    @staticmethod
    def get_stop(name: str) -> Stop:
        """
        Get the stop for the given name
        :param name: the name to search for
        :return: the corresponding stop
        """
        return StopTranslator.stops[name]


class LinkTranslator:
    """
    Utility class for storing links by their name in the net file object
    """
    links: Dict[str, Link] = {}

    @staticmethod
    def add_link(link: Link, name: str) -> None:
        """
        Add the given link to the translator
        :param link: the link to add
        :param name: the name of the link
        """
        LinkTranslator.links[name] = link

    @staticmethod
    def get_link(name: str):
        """
        Get the link for the given name
        :param name: the name to search for
        :return: the corresponding link
        """
        return LinkTranslator.links[name]
