package net.lintim.algorithm.vehiclescheduling;

import net.lintim.model.*;
import net.lintim.util.GraphHelper;
import net.lintim.util.LogLevel;
import net.lintim.util.PeriodicEanHelper;

import java.util.logging.Logger;

/**
 * Class containing a static method to add vehicle scheduling activities to an existing periodic ean
 */
public class AddToEan {

	private static Logger logger = Logger.getLogger("net.lintim.algorithm.vehiclescheduling.AddToEan");

	/**
	 * Add a simple vehicle schedule to the ean, i.e., connect both directions with the same line repetition number
	 * of a line with circulation activities. The lower bound of these activities will be the given turn over time.
	 * @param ean the ean to add the circulation activities to
	 * @param lineConcept the line concept, including frequencies
	 * @param turnOverTime the minimal turnover time for vehicles at the end of a line
	 * @param eanContainsFrequencies whether the ean was created with frequencies as multiplicity, i.e., whether
	 *                                  there are multiple periodic events with different line frequency repetitions
	 *                                  for every line with frequency > 1
	 */
	public static void addSimpleVehicleSchedulesToEan(Graph<PeriodicEvent, PeriodicActivity> ean, LinePool lineConcept,
	                                                  int turnOverTime, boolean eanContainsFrequencies, int
			                                                  periodLength, int maximumBufferTime){
		for(Line line : lineConcept.getLines()){
			if(line.getFrequency() == 0){
				continue;
			}
			int maxFrequency = eanContainsFrequencies ? line.getFrequency() : 1;
			int maxActivityId = GraphHelper.getMaxEdgeId(ean)+1;
			for(int freq = 1; freq <= maxFrequency; freq++){
				PeriodicEvent startOfForwards = PeriodicEanHelper.getStartOfLine(ean, line.getId(), freq,
						LineDirection.FORWARDS);
				PeriodicEvent endOfForwards = PeriodicEanHelper.getEndOfLine(ean, line.getId(), freq, LineDirection
						.FORWARDS);
				PeriodicEvent startOfBackwards = PeriodicEanHelper.getStartOfLine(ean, line.getId(), freq,
						LineDirection.BACKWARDS);
				PeriodicEvent endOfBackwards = PeriodicEanHelper.getEndOfLine(ean, line.getId(), freq, LineDirection
						.BACKWARDS);
				//Error checking: Are there any circulation activities for current lines?
				if(ean.getOutgoingEdges(endOfForwards).stream().anyMatch(periodicActivity -> periodicActivity.getType()
						== ActivityType.TURNAROUND)){
					logger.log(LogLevel.WARN, "The forwards direction of line " + line.getId() + " in rep " + freq +
							" has already an outgoing circulation activity!");
				}
				if(ean.getOutgoingEdges(endOfBackwards).stream().anyMatch(periodicActivity -> periodicActivity.getType()
						== ActivityType.TURNAROUND)){
					logger.log(LogLevel.WARN, "The backwards direction of line " + line.getId() + " in rep " + freq +
							" has already an outgoing circulation activity!");
				}
				ean.addEdge(new PeriodicActivity(maxActivityId++, ActivityType.TURNAROUND, endOfForwards,
						startOfBackwards, turnOverTime, turnOverTime + 2 * maximumBufferTime, 0));
				ean.addEdge(new PeriodicActivity(maxActivityId++, ActivityType.TURNAROUND, endOfBackwards,
						startOfForwards, turnOverTime, turnOverTime + 2 * maximumBufferTime, 0));
			}
		}
	}
}
