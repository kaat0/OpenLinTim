import logging
import sys

from core.io.config import ConfigReader
from core.io.lines import LineReader, LineWriter
from core.io.ptn import PTNReader
from core.util.config import Config
from visum_transformer.io.line_capacities import write_line_capacities
from visum_transformer.io.net import NetReader
from visum_transformer.util import fixed_lines

logger = logging.getLogger(__name__)

if __name__ == '__main__':
    if len(sys.argv) != 2:
        logger.fatal("Program takes exactly one argument, the name of the config to read")
        exit(1)
    ConfigReader.read(sys.argv[1])
    net_file_name = Config.getStringValueStatic("filename_net_file")
    fixed_lines_file_name = Config.getStringValueStatic("filename_lc_fixed_lines")
    line_capacity_file_name = Config.getStringValueStatic("filename_lc_fixed_line_capacities")
    line_capacity_header = Config.getStringValueStatic("line_capacities_header")
    logging.info("Begin reading input files")
    ptn = PTNReader.read()
    net = NetReader.parse_file(net_file_name)
    logging.info("Finished reading input files")
    logging.info("Begin reading fixed lines from visum file")
    current_pool = LineReader.read(ptn, read_frequencies=False)
    lines, capacities = fixed_lines.get_fixed_lines_from_visum(net, ptn)
    fixed_lines.merge_pools(current_pool, lines, ptn.isDirected(), capacities)
    logging.info("Finished reading fixed lines from visum file")
    logging.info("Begin writing output files")
    LineWriter.write(current_pool, write_line_concept=False)
    LineWriter.write(lines, write_pool=False, write_costs=False, concept_file_name=fixed_lines_file_name)
    write_line_capacities(capacities, line_capacity_file_name, line_capacity_header)
    logging.info("Finished writing output files")
    exit(0)

