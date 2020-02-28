import logging
import sys

from core.exceptions.algorithm_dijkstra import AlgorithmStoppingCriterionException
from core.exceptions.config_exceptions import ConfigNoFileNameGivenException
from core.io.config import ConfigReader
from core.io.lines import LineReader, LineWriter
from core.io.ptn import PTNReader
from core.solver.generic_solver_interface import Solver, VariableType, ConstraintSense, IntParam, DoubleParam, \
    OptimizationSense, Status
from core.util.config import Config

logger = logging.getLogger(__name__)


if __name__ == '__main__':
    if len(sys.argv) < 2:
        raise ConfigNoFileNameGivenException()
    logger.info("Start reading configuration")
    ConfigReader.read(sys.argv[1])
    time_limit = Config.getIntegerValueStatic("lc_timelimit")
    mip_gap = Config.getIntegerValueStatic("lc_mip_gap")
    solver_type = Config.getSolverTypeStatic("lc_solver")
    logger.info("Finished reading configuration")

    logger.info("Begin reading input data")
    ptn = PTNReader.read(read_loads=True)
    line_pool = LineReader.read(ptn, read_frequencies=False)
    logger.info("Finished reading input data")

    logger.info("Begin execution of line planning cost model")
    solver = Solver.createSolver(solver_type)
    model = solver.createModel()
    logger.debug("Add variables")
    frequencies = {}
    for line in line_pool.getLines():
        frequency = model.addVariable(0, float('inf'), VariableType.INTEGER, line.getCost(), f"f_{line.getId()}")
        frequencies[line] = frequency

    logger.debug("Adding constraints")
    for link in ptn.getEdges():
        sum_freq_per_line = model.createExpression()
        for line in line_pool.getLines():
            if link in line.getLinePath().getEdges():
                sum_freq_per_line.addTerm(1, frequencies[line])
        model.addConstraint(sum_freq_per_line, ConstraintSense.LESS_EQUAL, link.getUpperFrequencyBound(),
                            f"u_{link.getId()}")
        model.addConstraint(sum_freq_per_line, ConstraintSense.GREATER_EQUAL, link.getLowerFrequencyBound(),
                            f"l_{link.getId()}")

    logger.debug("Add parameters")
    model.setIntParam(IntParam.TIMELIMIT, time_limit)
    model.setIntParam(IntParam.OUTPUT_LEVEL, logger.getEffectiveLevel())
    model.setDoubleParam(DoubleParam.MIP_GAP, mip_gap)
    model.setSense(OptimizationSense.MINIMIZE)
    if logger.level == logging.DEBUG:
        logger.debug("Writing lp file")
        model.write("costModel.lp")
        logger.debug("Finished writing lp file")

    logger.debug("Start optimization")
    model.solve()
    logger.debug("Finished optimization")

    status = model.getStatus()
    if status == Status.OPTIMAL or status == Status.FEASIBLE:
        if status == Status.OPTIMAL:
            logger.debug("Optimal solution found")
        else:
            logger.debug("Feasible solution found")
        for line in line_pool.getLines():
            line.setFrequency(int(round(model.getValue(frequencies[line]))))
    else:
        logger.debug("Problem is infeasible")
        if logger.level == logging.DEBUG:
            model.computeIIS("costModel.ilp")
        raise AlgorithmStoppingCriterionException("cost model line planning")
    logger.info("Finished execution of line planning cost model")

    logger.info("Begin writing output data")
    LineWriter.write(line_pool, write_pool=False, write_costs=False)
    logger.info("Finished writing output data")
