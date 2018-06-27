import logging
import sys
from typing import Dict, List

from core.io.config import ConfigReader
from core.io.lines import LineReader
from core.io.periodic_ean import PeriodicEANReader, PeriodicEANWriter
from core.io.ptn import PTNReader
from core.model.graph import Graph
from core.model.lines import Line, LinePool
from core.model.ptn import Stop, Link
from core.util.config import Config
from visum_transformer.io.fixed_timetable import write_fixed_timetable
from visum_transformer.io.net import NetReader
from visum_transformer.model.net import Net
from visum_transformer.util.fixed_lines import store_stops_in_translator, store_links_in_translator, read_fixed_lines, \
    find_fixed_line_names_and_unit_combinations
from visum_transformer.util.fixed_times import get_fixed_times

logger = logging.getLogger(__name__)


def find_fixed_lines_in_pool(ptn: Graph[Stop, Link], whole_pool: LinePool, net: Net) -> Dict[str, Line]:
    """
    Find the fixed line in the line pool and store them by their name from the config
    :param ptn: the ptn
    :param whole_pool: the pool with all lines
    :return: a dict containing the fixed lines, stored by their name
    """
    store_stops_in_translator(ptn)
    store_links_in_translator(ptn, net)
    fixed_lines, line_names = read_fixed_lines(ptn, net, list(find_fixed_line_names_and_unit_combinations(net).keys()))
    # Now find the created lines in the whole pool
    result: Dict[str, Line] = {}
    for line in fixed_lines.getLines():
        for compare_line in whole_pool.getLines():
            if line.getLinePath() == compare_line.getLinePath():
                result[line_names[line]] = compare_line
    return result


if __name__ == '__main__':
    if len(sys.argv) != 2:
        logger.fatal("Program takes exactly one argument, the name of the config to read")
        exit(1)
    ConfigReader.read(sys.argv[1])
    net_file_name = Config.getStringValueStatic("filename_net_file")
    fixed_timetable_file_name = Config.getStringValueStatic("filename_tim_fixed_times")
    period_length = Config.getIntegerValueStatic("period_length")
    logging.info("Begin reading input files")
    ptn = PTNReader.read()
    pool = LineReader.read(ptn, read_frequencies=True, read_costs=False)
    ean, _ = PeriodicEANReader.read()
    net = NetReader.parse_file(net_file_name)
    logging.info("Finished reading input files")
    logging.info("Begin transforming visum files")
    line_dict: Dict[str, Line] = find_fixed_lines_in_pool(ptn, pool, net)
    fixed_ean = get_fixed_times(net, ean, line_dict, ptn.isDirected(), period_length)
    logger.debug("Found fixed ean:")
    logger.debug(fixed_ean)
    logging.info("Finished transforming visum files")
    logging.info("Begin writing output files")
    write_fixed_timetable(fixed_ean)
    logging.info("Finished writing output files")
    exit(0)
