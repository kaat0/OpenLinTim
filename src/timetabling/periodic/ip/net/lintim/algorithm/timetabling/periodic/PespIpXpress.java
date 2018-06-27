package net.lintim.algorithm.timetabling.periodic;

import com.dashoptimization.*;
import net.lintim.model.*;
import net.lintim.util.LogLevel;
import net.lintim.util.Statistic;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Pesp solver implementation using Xpress.
 */
public class PespIpXpress extends PespSolver{
	@Override
	public boolean solveTimetablingPespModel(Graph<PeriodicEvent, PeriodicActivity> ean,
	                                         PeriodicTimetable<PeriodicEvent> timetable, double changePenalty, int
			                                             timeLimit, double mipGap) {

		Logger logger = Logger.getLogger("net.lintim.algorithm.timetabling.periodic.PespIpXpress");
		Level logLevel = LogManager.getLogManager().getLogger("").getLevel();

		logger.log(LogLevel.DEBUG, "Setting up the solver");
		XPRB bcl = new XPRB();
		XPRBprob model = bcl.newProb("pesp ip periodic timetabling");
		model.setSense(XPRB.MINIM);
		if(timeLimit > 0){
			//Set timetlimit to -timelimit, otherwise Xpress will search until a solution is found, see docs for MAXTIME
			model.getXPRSprob().setIntControl(XPRS.MAXTIME, -1*timeLimit);
		}
		if(mipGap != 0){
			model.getXPRSprob().setDblControl(XPRS.MIPRELSTOP, mipGap);
		}


		if (logLevel.equals(LogLevel.DEBUG)) {
			model.setMsgLevel(4);
		} else if (logLevel.equals(LogLevel.INFO)) {
			model.setMsgLevel(2);
		} else if (logLevel.equals(LogLevel.WARN)) {
			model.setMsgLevel(2);
		} else if (logLevel.equals(LogLevel.ERROR)) {
			model.setMsgLevel(0);
		} else if (logLevel.equals(LogLevel.FATAL)) {
			model.setMsgLevel(0);
		}

		HashMap<Integer, XPRBvar> eventIdToVarMap = new HashMap<>();
		logger.log(LogLevel.DEBUG, "Add event variables");
		for(PeriodicEvent event : ean.getNodes()){
			XPRBvar eventVar = model.newVar("pi_" + event.getId(), XPRB.UI, 0, timetable.getPeriod()-1);
			eventIdToVarMap.put(event.getId(), eventVar);
		}

		logger.log(LogLevel.DEBUG, "Add variables and constraints for the activities");
		XPRBexpr activityTerm;
		XPRBexpr objective = new XPRBexpr();
		for (PeriodicActivity activity : ean.getEdges()) {
			//set objective to 0. The model objective will be overwritten by the build expression afterwards
			// anyway
			XPRBvar moduloVariable = model.newVar("z_" + activity.getId(), XPRB.UI, 0, XPRB.INFINITY);
			XPRBvar sourceEventVariable = eventIdToVarMap.get(activity.getLeftNode().getId());
			XPRBvar targetEventVariable = eventIdToVarMap.get(activity.getRightNode().getId());
			activityTerm = new XPRBexpr();
			activityTerm.addTerm(timetable.getPeriod(), moduloVariable);
			activityTerm.add(targetEventVariable);
			activityTerm.addTerm(-1, sourceEventVariable);
			//Add the constraints for this activity
			model.newCtr("l" + activity.getId(), activityTerm.gEql(activity.getLowerBound()));
			model.newCtr("u" + activity.getId(), activityTerm.lEql(activity.getUpperBound()));
			if (activity.getType() == ActivityType.CHANGE) {
				activityTerm.add(changePenalty);
			}
			//Add the objective terms
			objective.add(activityTerm.mul(activity.getNumberOfPassengers()));
		}

		model.setObj(objective);
		if(logLevel.equals(LogLevel.DEBUG)){
			logger.log(LogLevel.DEBUG, "Writing lp file");
			try {
				model.exportProb("PeriodicTimetablingPespIp.lp");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		logger.log(LogLevel.DEBUG, "Start optimization");
		model.mipOptimise();
		logger.log(LogLevel.DEBUG, "End optimization");

		if(model.getMIPStat()==XPRB.MIP_SOLUTION) {
			logger.log(LogLevel.DEBUG, "Feasible solution found");
		}
		else if(model.getMIPStat()==XPRB.MIP_OPTIMAL){
			logger.log(LogLevel.DEBUG, "Optimal solution found");
		}
		else {
			Statistic.putStatic("tim_pesp_gap", -1);
			return false;
		}
		for(PeriodicEvent event : ean.getNodes()){
			int time = (int) Math.round(eventIdToVarMap.get(event.getId()).getSol());
			event.setTime(time);
			timetable.put(event, (long) time);
		}
		double bestObjective = model.getXPRSprob().getDblAttrib(XPRS.MIPBESTOBJVAL);
		double bestBound = model.getXPRSprob().getDblAttrib(XPRS.BESTBOUND);
		double gap = Math.abs(bestObjective - bestBound)/bestObjective;
		Statistic.putStatic("tim_pesp_gap", new BigDecimal(gap).setScale(2, RoundingMode.HALF_UP).doubleValue());
		return true;
	}
}
