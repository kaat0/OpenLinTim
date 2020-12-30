package net.lintim.algorithm.timetabling.periodic;

import net.lintim.exception.SolverNotSupportedException;
import net.lintim.model.Graph;
import net.lintim.model.PeriodicActivity;
import net.lintim.model.PeriodicEvent;
import net.lintim.model.PeriodicTimetable;
import net.lintim.util.Logger;
import net.lintim.util.SolverType;

/**
 * Generic solver for a pesp ip.
 */
public abstract class PespSolver {
	private static Logger logger = new Logger(PespSolver.class.getCanonicalName());

	/**
	 * Solve the periodic timetabling problem for the given data.
	 * @param ean the ean
	 * @param timetable the timetable to store the results in. Note that the times of the ean will be set as well.
	 * @param timeLimit the timelimit for the computation. Set to negative values to disable.
	 * @param mipGap the desired mip gap
	 * @param solutionLimit limit for found solutions
	 * @param bestBoundStop value to terminate when bound reached this value
	 * @return whether a feasible solution was found
	 */
	public abstract boolean solveTimetablingPespModel(Graph<PeriodicEvent, PeriodicActivity> ean, PeriodicTimetable<PeriodicEvent> timetable,
																					 boolean solverOutput, int threadLimit, boolean useOldSolution,
	                                         double changePenalty, int timeLimit, double mipGap,
																					 int solutionLimit, double bestBoundStop, int mipFocus);

	/**
	 * Get a pesp solver of the given type
	 * @param solverType the type of the desired solver
	 * @return the solver
	 */
	public static PespSolver getSolver(SolverType solverType) throws ClassNotFoundException, IllegalAccessException,
			InstantiationException {
		String solverClassName = "";
		switch (solverType) {
			case GUROBI:
				logger.debug("Will use Gurobi for optimization");
				solverClassName = "net.lintim.algorithm.timetabling.periodic.PespIpGurobi";
				break;
			case XPRESS:
				logger.debug("Will use Xpress for optimization");
				solverClassName = "net.lintim.algorithm.timetabling.periodic.PespIpXpress";
				break;
			default:
				throw new SolverNotSupportedException(solverType.toString(), "line planning cost model");
		}
		Class<?> solverClass = Class.forName(solverClassName);
		return (PespSolver) solverClass.newInstance();
	}
}
