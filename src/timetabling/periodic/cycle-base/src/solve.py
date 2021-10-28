import logging

from event_activity_network import PeriodicEventActivityNetwork

import solver
import preprocessing as pp
from helper import Parameters

logger_ = logging.getLogger(__name__)


def solve(ean: PeriodicEventActivityNetwork, parameters: Parameters):
    logger_.debug("Run: solve")
    pp.deleting_light_edges(ean, parameters)

    ean.calculate_span()
    ean.calculate_nx_graph()
    ean.calculate_nx_spanning_tree()
    ean.calculate_cycle_base()

    solver.naive_solve(ean, parameters)
