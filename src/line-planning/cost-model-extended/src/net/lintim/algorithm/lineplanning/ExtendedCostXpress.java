package net.lintim.algorithm.lineplanning;

import com.dashoptimization.*;
import net.lintim.model.*;
import net.lintim.util.LogLevel;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * A class to solve the cost model of line planning using Xpress.
 */
public class ExtendedCostXpress extends ExtendedCostSolver {

    @Override
    public boolean solveLinePlanningCost(Graph<Stop, Link> ptn, LinePool linePool, int timelimit, int
        commonFrequencyDivisor) {
        Logger logger = Logger.getLogger("net.lintim.algorithm.lineplanning.CostXpress");
        Level logLevel = LogManager.getLogManager().getLogger("").getLevel();

        XPRB bcl = new XPRB();
        XPRBprob costModel = bcl.newProb("cost model line planning");
        costModel.setSense(XPRB.MINIM);
        timelimit = timelimit == -1 ? 0 : -1 * timelimit;
        logger.log(LogLevel.DEBUG, "Set Xpress timelimit to " + timelimit);
        costModel.getXPRSprob().setIntControl(XPRS.MAXTIME, timelimit);

        HashMap<Integer, XPRBvar> frequencies = new HashMap<>();

        if (logLevel.equals(LogLevel.DEBUG)) {
            costModel.setMsgLevel(4);
        } else if (logLevel.equals(LogLevel.INFO)) {
            costModel.setMsgLevel(2);
        } else if (logLevel.equals(LogLevel.WARN)) {
            costModel.setMsgLevel(2);
        } else if (logLevel.equals(LogLevel.ERROR)) {
            costModel.setMsgLevel(0);
        } else if (logLevel.equals(LogLevel.FATAL)) {
            costModel.setMsgLevel(0);
        }

        //Add variables
        logger.log(LogLevel.DEBUG, "Add variables");
        for (Line line : linePool.getLines()) {
            XPRBvar frequency = costModel.newVar("f_" + line.getId(), XPRB.UI, 0, XPRB.INFINITY);
            frequencies.put(line.getId(), frequency);
            XPRBvar systemFrequencyDivisor = costModel.newVar("g_" + line.getId(), XPRB.UI, 0, XPRB.INFINITY);
            costModel.newCtr("systemFrequency_" + line.getId(), frequency.eql(systemFrequencyDivisor.mul
                (commonFrequencyDivisor)));
        }

        //Add constraints
        logger.log(LogLevel.DEBUG, "Add frequency constraints");
        XPRBexpr sumFreqPerLine;
        for (Link link : ptn.getEdges()) {
            sumFreqPerLine = new XPRBexpr();
            for (Line line : linePool.getLines()) {
                if (line.getLinePath().getEdges().contains(link)) {
                    sumFreqPerLine.addTerm(1, frequencies.get(line.getId()));
                }
            }
            costModel.newCtr("lowerBound_" + link.getId(), sumFreqPerLine.gEql(link.getLowerFrequencyBound()));
            costModel.newCtr("upperBound_" + link.getId(), sumFreqPerLine.lEql(link.getUpperFrequencyBound()));
        }

        logger.log(LogLevel.DEBUG, "Construct objective");
        XPRBexpr objective = new XPRBexpr();
        for (Line line : linePool.getLines()) {
            objective.addTerm(line.getCost(), frequencies.get(line.getId()));
        }

        logger.log(LogLevel.DEBUG, "Set objective");
        costModel.setObj(objective);
        logger.log(LogLevel.DEBUG, "Start optimization");
        costModel.mipOptimise();
        logger.log(LogLevel.DEBUG, "End optimization");

        if (costModel.getMIPStat() == XPRB.MIP_OPTIMAL) {
            logger.log(LogLevel.DEBUG, "Optimal solution found");
            for (Line line : linePool.getLines()) {
                line.setFrequency((int) Math.round(frequencies.get(line.getId()).getSol()));
            }
            return true;
        }
        return false;
    }

}
