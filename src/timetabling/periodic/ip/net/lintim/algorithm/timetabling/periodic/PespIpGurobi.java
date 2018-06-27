package net.lintim.algorithm.timetabling.periodic;

import gurobi.*;
import net.lintim.exception.SolverGurobiException;
import net.lintim.model.*;
import net.lintim.util.LogLevel;
import net.lintim.util.Statistic;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Pesp solver implementation using Gurobi.
 */
public class PespIpGurobi extends PespSolver {
	@Override
	public boolean solveTimetablingPespModel(Graph<PeriodicEvent, PeriodicActivity> ean,
	                                         PeriodicTimetable<PeriodicEvent> timetable, double changePenalty, int
	                                         timeLimit, double mipGap) {
		Logger logger = Logger.getLogger("net.lintim.algorithm.timetabling.periodic.PespIpGurobi");
		Level logLevel = LogManager.getLogManager().getLogger("").getLevel();
		try {
			logger.log(LogLevel.DEBUG, "Setting up the solver");
			GRBEnv env = new GRBEnv();
			GRBModel model = new GRBModel(env);
			model.set(GRB.IntAttr.ModelSense, GRB.MINIMIZE);
			if (logLevel.equals(LogLevel.DEBUG)) {
				model.set(GRB.IntParam.LogToConsole, 1);
				model.set(GRB.StringParam.LogFile, "PespModelGurobi.log");
			} else {
				model.set(GRB.IntParam.OutputFlag, 0);
			}
			if(timeLimit > 0){
				model.set(GRB.DoubleParam.TimeLimit, timeLimit);
			}
			if(mipGap != 0){
				model.set(GRB.DoubleParam.MIPGap, mipGap);
			}

			logger.log(LogLevel.DEBUG, "Add event variables");
			HashMap<Integer, GRBVar> eventIdToVarMap = new HashMap<>();
			for(PeriodicEvent event : ean.getNodes()){
				GRBVar eventVar = model.addVar(0, timetable.getPeriod()-1, 0, GRB.INTEGER, "pi_" + event.getId());
				eventIdToVarMap.put(event.getId(), eventVar);

			}
			logger.log(LogLevel.DEBUG, "Add variables and constraints for the activities");
			GRBLinExpr objective = new GRBLinExpr();
			GRBLinExpr activityTerm;
			for(PeriodicActivity activity : ean.getEdges()){
				//set objective to 0. The model objective will be overwritten by the build lin expression afterwards
				// anyway
				GRBVar moduloVariable = model.addVar(0, GRB.INFINITY, 0, GRB.INTEGER, "z_" + activity.getId());
				GRBVar sourceEventVariable = eventIdToVarMap.get(activity.getLeftNode().getId());
				GRBVar targetEventVariable = eventIdToVarMap.get(activity.getRightNode().getId());
				activityTerm = new GRBLinExpr();
				activityTerm.addTerm(timetable.getPeriod(), moduloVariable);
				activityTerm.addTerm(1, targetEventVariable);
				activityTerm.addTerm(-1, sourceEventVariable);
				//Add the constraints for this activity
				model.addConstr(activity.getLowerBound(), GRB.LESS_EQUAL, activityTerm, "l_" + activity.getId());
				model.addConstr(activity.getUpperBound(), GRB.GREATER_EQUAL, activityTerm, "u_" + activity.getId());
				if (activity.getType() == ActivityType.CHANGE) {
					activityTerm.addConstant(changePenalty);
				}
				//Add the objective terms
				if(activity.getNumberOfPassengers() != 0){
					objective.multAdd(activity.getNumberOfPassengers(), activityTerm);
				}
			}
			model.setObjective(objective);

			if(logLevel.equals(LogLevel.DEBUG)){
				logger.log(LogLevel.DEBUG, "Writing lp file");
				model.write("PeriodicTimetablingPespIp.lp");
			}
			logger.log(LogLevel.DEBUG, "Start optimization");
			model.optimize();
			logger.log(LogLevel.DEBUG, "End optimization");

			int status = model.get(GRB.IntAttr.Status);
			if(status==GRB.TIME_LIMIT || (status==GRB.OPTIMAL && model.get(GRB.DoubleAttr.MIPGap) > 0.0001)) {
				logger.log(LogLevel.DEBUG, "Feasible solution found");
			}
			else if(status==GRB.OPTIMAL){
				logger.log(LogLevel.DEBUG, "Optimal solution found");
			}
			else {
				Statistic.putStatic("tim_pesp_gap", -1);
				return false;
			}
			for(PeriodicEvent event : ean.getNodes()){
				int time = (int) Math.round(eventIdToVarMap.get(event.getId()).get(GRB.DoubleAttr.X));
				event.setTime(time);
				timetable.put(event, (long) time);
			}
			Statistic.putStatic("tim_pesp_gap", model.get(GRB.DoubleAttr.MIPGap));
			return true;
		} catch (GRBException e) {
			throw new SolverGurobiException(e.toString());
		}
	}
}
