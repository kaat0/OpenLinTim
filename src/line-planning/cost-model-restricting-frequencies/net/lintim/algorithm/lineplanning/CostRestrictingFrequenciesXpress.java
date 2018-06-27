package net.lintim.algorithm.lineplanning;

import com.dashoptimization.*;
import net.lintim.model.*;
import net.lintim.util.LogLevel;

import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * A class to solve the cost model with restricted frequencies of line planning using Xpress.
 */
public class CostRestrictingFrequenciesXpress extends CostRestrictingFrequenciesSolver {
    @Override
    public boolean solveLinePlanningCost(Graph<Stop, Link> ptn, LinePool linePool, int timelimit, int
        numberOfPossibleFrequencies, int maximalFrequency) {
        Logger logger = Logger.getLogger("net.lintim.algorithm.lineplanning.CostRestrictingFrequenciesXpress");
        Level logLevel = LogManager.getLogManager().getLogger("").getLevel();

        XPRB bcl = new XPRB();
        XPRBprob costModel = bcl.newProb("cost model line planning restricting frequencies");
        costModel.setSense(XPRB.MINIM);
        // Mult timelimit by -1 to enforce abort after time
        timelimit = timelimit <= 0 ? 0 : -1 * timelimit;
        logger.log(LogLevel.DEBUG, "Set Xpress timelimit to " + timelimit);
        costModel.getXPRSprob().setIntControl(XPRS.MAXTIME, timelimit);

        HashMap<Integer, HashMap<Integer, XPRBvar>> frequencies = new HashMap<>();
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
            frequencies.put(line.getId(), new HashMap<>());
            XPRBexpr lineFreqConstraint = new XPRBexpr();
            for(int freq = 0; freq <= maximalFrequency; freq++){
                XPRBvar lineUsesFreq = costModel.newVar("f_" + line.getId() + "_" + freq, XPRB.BV, 0, 1);
                frequencies.get(line.getId()).put(freq, lineUsesFreq);
                lineFreqConstraint.add(lineUsesFreq);
            }
            costModel.newCtr("line_" + line.getId() + "_only_uses_one_freq", lineFreqConstraint.eql(1));
        }
        if(numberOfPossibleFrequencies != -1) {
            XPRBexpr freqConstraint = new XPRBexpr();
            for (int frequency = 1; frequency <= maximalFrequency; frequency++) {
                XPRBvar freqUsed = costModel.newVar("freq_" + frequency + "_used", XPRB.BV, 0, 1);
                freqConstraint.add(freqUsed);
                for (Line line : linePool.getLines()) {
                    costModel.newCtr("line_" + line.getId() + "_uses_freq_" + frequency, freqUsed.gEql(frequencies.get(line

                        .getId()).get(frequency)));
                }
            }
            costModel.newCtr("freq_bound", freqConstraint.lEql(numberOfPossibleFrequencies));
        }
        //Add constraints
        logger.log(LogLevel.DEBUG, "Add frequency constraints");
        XPRBexpr sumFreqPerLine;
        for (Link link : ptn.getEdges()) {
            sumFreqPerLine = new XPRBexpr();
            for (Line line : linePool.getLines()) {
                if (line.getLinePath().getEdges().contains(link)) {
                    for(int freq = 1; freq <= maximalFrequency; freq++) {
                        sumFreqPerLine.addTerm(freq, frequencies.get(line.getId()).get(freq));
                    }
                }
            }
            costModel.newCtr("lowerBound_" + link.getId(), sumFreqPerLine.gEql(link.getLowerFrequencyBound()));
            costModel.newCtr("upperBound_" + link.getId(), sumFreqPerLine.lEql(link.getUpperFrequencyBound()));
        }

        logger.log(LogLevel.DEBUG, "Construct objective");
        XPRBexpr objective = new XPRBexpr();
        for (Line line : linePool.getLines()) {
            for(int freq = 1; freq <= maximalFrequency; freq++) {
                objective.addTerm(line.getCost() * freq, frequencies.get(line.getId()).get(freq));
            }
        }

        logger.log(LogLevel.DEBUG, "Set objective");
        costModel.setObj(objective);
        if(logLevel == LogLevel.DEBUG){
            logger.log(LogLevel.DEBUG, "Writing model file");
            try {
                costModel.exportProb("xpress_lc.lp");
            } catch (IOException e) {
                logger.log(LogLevel.WARN, "Could not write lp file, exception " + e);
            }
        }
        logger.log(LogLevel.DEBUG, "Start optimization");
        costModel.mipOptimise();
        logger.log(LogLevel.DEBUG, "End optimization");

        int status = costModel.getMIPStat();
        if (status == XPRB.MIP_OPTIMAL) {
            logger.log(LogLevel.DEBUG, "Optimal solution found");
        }
        else if(status == XPRB.MIP_SOLUTION){
            logger.log(LogLevel.DEBUG, "Feasible solution found");
        }
        else {
            logger.log(LogLevel.DEBUG, "No solution found");
            return false;
        }
        for (Line line : linePool.getLines()) {
            for(int freq = 0; freq <= maximalFrequency; freq++){
                if(Math.round(frequencies.get(line.getId()).get(freq).getSol()) > 0){
                    line.setFrequency(freq);
                }
            }
        }
        return true;


    }

}
