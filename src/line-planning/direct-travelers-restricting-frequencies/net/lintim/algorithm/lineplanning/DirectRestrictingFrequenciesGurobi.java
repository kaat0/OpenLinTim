package net.lintim.algorithm.lineplanning;

import gurobi.*;
import net.lintim.algorithm.Dijkstra;
import net.lintim.exception.ConfigTypeMismatchException;
import net.lintim.exception.SolverGurobiException;
import net.lintim.model.*;
import net.lintim.util.Config;
import net.lintim.util.LogLevel;
import net.lintim.util.Pair;

import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 */
public class DirectRestrictingFrequenciesGurobi {

	private static Logger logger = Logger.getLogger("net.lintim.algorithm.lineplanning.DirectRestrictingFrequenciesGurobi");

	/**
	 * Solve the direct line planning problem while restricting the possible frequencies. Will use the shortest paths
	 * as preferable paths (length of the path is determined by config parameter)
	 * @param ptn the ptn
	 * @param od the od matrix
	 * @param linePool the pool of possible lines
	 * @param config the config to read the model parameters from
	 * @return whether a feasible solution could be found
	 */
	public static boolean solveLinePlanningDirect(Graph<Stop, Link> ptn, OD od, LinePool linePool, Config config) {
		return solveLinePlanningDirect(ptn, od, linePool, config, computeShortestPaths(ptn, od, config));
	}

	/**
	 * Solve the direct line planning problem while restricting the possible frequencies.
	 * @param ptn the ptn
	 * @param od the od matrix
	 * @param linePool the pool of possible lines
	 * @param config the config to read the model parameters from
	 * @param preferablePaths the preferable paths of the passengers
	 * @return whether a feasible solution could be found
	 */
	public static boolean solveLinePlanningDirect(Graph<Stop, Link> ptn, OD od, LinePool linePool, Config config,
	                                              Map<Pair<Integer, Integer>, Collection<Path<Stop, Link>>>
			                                              preferablePaths) {
		try {
			GRBEnv env = new GRBEnv();
			GRBModel directModel = new GRBModel(env);
			directModel.set(GRB.IntAttr.ModelSense, GRB.MAXIMIZE);
			Level logLevel = LogManager.getLogManager().getLogger("").getLevel();
			if (logLevel.equals(LogLevel.DEBUG)) {
				directModel.set(GRB.IntParam.LogToConsole, 1);
				directModel.set(GRB.StringParam.LogFile, "DirectModelGurobi.log");
			} else {
				directModel.set(GRB.IntParam.OutputFlag, 0);
			}
			double timeLimit = config.getIntegerValue("lc_timelimit");
			double numberOfPossibleFrequencies = config.getIntegerValue("lc_number_of_possible_frequencies");
			int maximalFrequency = config.getIntegerValue("lc_maximal_frequency");
			timeLimit = timeLimit == -1 ? GRB.INFINITY : timeLimit;
			directModel.set(GRB.DoubleParam.TimeLimit, timeLimit);

			logger.log(LogLevel.DEBUG, "Precomputation, see which lines can be used directly by which passengers");
			HashMap<Pair<Integer, Integer>, HashSet<Integer>> acceptableLineIds = computeAcceptableLineIds(linePool,
					preferablePaths, ptn);
			logger.log(LogLevel.DEBUG, "Add variables");
			//Notation is used from the public transportation script of Prof. Schöbel. When you need explanation on
			// the meaning of the variables/constraints, please see the script. We will try to use the same names as
			// mentioned there.
			//d[i][j][l] = number of passenger directly travelling from i to j using line with id l
			HashMap<Integer, HashMap<Integer, HashMap<Integer, GRBVar>>> d = new HashMap<>();
			for (Stop origin : ptn.getNodes()) {
				d.put(origin.getId(), new HashMap<>());
				for (Stop destination : ptn.getNodes()) {
					d.get(origin.getId()).put(destination.getId(), new HashMap<>());
					GRBLinExpr sumOfAllVariablesPerODPair = new GRBLinExpr();
					if(od.getValue(origin.getId(), destination.getId()) == 0){
						continue;
					}
					for (int lineId : acceptableLineIds.get(new Pair<>(origin.getId(), destination.getId()))) {
						d.get(origin.getId()).get(destination.getId()).put(lineId, directModel.addVar(0, od.getValue
								(origin.getId(), destination.getId()), 1, GRB.INTEGER, "d_" + origin.getId() + "_" +
								destination.getId() + "_" + lineId));
						sumOfAllVariablesPerODPair.addTerm(1, d.get(origin.getId()).get(destination.getId())
								.get(lineId));
					}
					//Constraint 3.6 -> Ensure that only the number of passengers on an od pair can travel directly
					directModel.addConstr(sumOfAllVariablesPerODPair, GRB.LESS_EQUAL, od.getValue(origin.getId(),
							destination.getId()), "od_constraint_" + origin.getId() + "_" + destination.getId());
				}
			}
			//f[l][freq] = line l is used with frequency freq
			HashMap<Integer, HashMap<Integer, GRBVar>> f = new HashMap<>();
			for (Line line : linePool.getLines()) {
				f.put(line.getId(), new HashMap<>());
				GRBLinExpr lineFreqConstraint = new GRBLinExpr();
				for(int freq = 0; freq <= maximalFrequency; freq++){
					GRBVar lineUsesFreq = directModel.addVar(0, 1, 0, GRB.BINARY, "f_" + line.getId() + "_" + freq);
					f.get(line.getId()).put(freq, lineUsesFreq);
					lineFreqConstraint.addTerm(1, lineUsesFreq);
				}
				directModel.addConstr(lineFreqConstraint, GRB.EQUAL, 1, "line_" + line.getId() + "_uses_one_freq");
			}
			logger.log(LogLevel.DEBUG, "Add capacity constraints");
			//Constraint 3.7 -> Ensure that the capacity of each line is not exceeded
			int capacity = config.getIntegerValue("gen_passengers_per_vehicle");
			for (Link link : ptn.getEdges()) {
				HashMap<Integer, GRBLinExpr> directTravellersOnLineAndEdge = new HashMap<>();
				for (Stop origin : ptn.getNodes()) {
					for (Stop destination : ptn.getNodes()) {
						if(od.getValue(origin.getId(), destination.getId()) == 0){
							continue;
						}
						for (int lineId : acceptableLineIds.get(new Pair<>(origin.getId(), destination.getId()))) {
							directTravellersOnLineAndEdge.computeIfAbsent(lineId, k -> new GRBLinExpr());
							directTravellersOnLineAndEdge.get(lineId).addTerm(1, d.get(origin.getId()).get
									(destination.getId()).get(lineId));
						}
					}
				}
				for(Line line : linePool.getLines()){
					GRBLinExpr capacityOfLine = new GRBLinExpr();
					for(int freq = 1; freq <= maximalFrequency; freq++){
						capacityOfLine.addTerm(freq * capacity, f.get(line.getId()).get(freq));
					}
					GRBLinExpr directTravellersOnLine = directTravellersOnLineAndEdge.get(line.getId());
					if(directTravellersOnLine != null){
						directModel.addConstr(directTravellersOnLine, GRB.LESS_EQUAL, capacityOfLine,
								"capacity_constraint_" + link.getId() + "_" + line.getId());
					}
				}
			}
			//Constraint 3.8 -> Ensure the upper and lower frequency bounds on the links
			logger.log(LogLevel.DEBUG, "Add upper and lower frequency bounds");
			for (Link link : ptn.getEdges()) {
				GRBLinExpr sumOfFrequencies = new GRBLinExpr();
				for (Line line : linePool.getLines()) {
					if (line.getLinePath().contains(link)) {
						for(int freq = 1; freq <= maximalFrequency; freq++){
							sumOfFrequencies.addTerm(freq, f.get(line.getId()).get(freq));
						}
					}
				}
				directModel.addConstr(sumOfFrequencies, GRB.LESS_EQUAL, link.getUpperFrequencyBound(), "f_" + link
						.getId() + "_max");
				directModel.addConstr(sumOfFrequencies, GRB.GREATER_EQUAL, link.getLowerFrequencyBound(), "f_" +
						link.getId() + "_min");
			}

			//Frequency constraints -> Restrict the number of frequencies
			if(numberOfPossibleFrequencies != -1) {
				logger.log(LogLevel.DEBUG, "Add frequency constraint");
				GRBLinExpr freqConstraint = new GRBLinExpr();
				for (int freq = 1; freq <= maximalFrequency; freq++) {
					GRBVar freqUsed = directModel.addVar(0, 1, 0, GRB.BINARY, "freq_" + freq + "_used");
					freqConstraint.addTerm(1, freqUsed);
					for (Line line : linePool.getLines()) {
						directModel.addConstr(freqUsed, GRB.GREATER_EQUAL, f.get(line.getId()).get(freq), "line_" + line

								.getId() + "_uses_freq_" + freq);
					}
				}
				directModel.addConstr(freqConstraint, GRB.LESS_EQUAL, numberOfPossibleFrequencies, "freq_bound");
			}

			//Budget constraints -> Restrict the costs of the line concept
			logger.log(LogLevel.DEBUG, "Add budget constraint");
			int budget = config.getIntegerValue("lc_budget");
			GRBLinExpr costOfLineConcept = new GRBLinExpr();
			for (Line line : linePool.getLines()) {
				for(int freq = 1; freq <= maximalFrequency; freq++){
					costOfLineConcept.addTerm(line.getCost() * freq, f.get(line.getId()).get(freq));
				}
			}
			directModel.addConstr(costOfLineConcept, GRB.LESS_EQUAL, budget, "budget");
			directModel.set(GRB.IntAttr.ModelSense, GRB.MAXIMIZE);
			if (logLevel.equals(LogLevel.DEBUG)) {
				directModel.write("DirectModelGurobi.lp");
			}
			logger.log(LogLevel.DEBUG, "Start optimizing");
			directModel.optimize();
			logger.log(LogLevel.DEBUG, "End optimizing");

			int status = directModel.get(GRB.IntAttr.Status);
			if(status == GRB.OPTIMAL){
				logger.log(LogLevel.DEBUG, "Optimal solution found");
			}
			else if(status == GRB.TIME_LIMIT){
				logger.log(LogLevel.DEBUG, "Timelimit hit");
			}
			else {
				logger.log(LogLevel.DEBUG, "No solution found");
				if(logLevel == LogLevel.DEBUG){
					logger.log(LogLevel.DEBUG, "Computing IIS");
					directModel.computeIIS();
					directModel.write("gurobi_lc.ilp");
				}
				return false;
			}
			logger.log(LogLevel.DEBUG, "Read back solution");
			//Read the frequencies and set the lines accordingly
			for(Line line : linePool.getLines()){
				for(int freq = 0; freq <= maximalFrequency; freq++){
					if(Math.round(f.get(line.getId()).get(freq).get(GRB.DoubleAttr.X)) > 0){
						line.setFrequency(freq);
						break;
					}
				}
			}
			return true;
		} catch (GRBException e) {
			throw new SolverGurobiException(e.toString());
		}
	}

	/**
	 * Compute the shortest paths for all the passengers. For each od pair, the returned map will contain a
	 * collection of shortest paths. The length of a path will be determined by the config parameter
	 * "ean_model_weight_drive".
	 * @param ptn the ptn
	 * @param od the od pairs
	 * @param config the config
	 * @return a map of all shortest paths
	 */
	private static HashMap<Pair<Integer, Integer>, Collection<Path<Stop, Link>>> computeShortestPaths(Graph<Stop,
			Link> ptn, OD od, Config config) {
		HashMap<Pair<Integer, Integer>, Collection<Path<Stop, Link>>> paths = new HashMap<>();
		//First determine what the length of an edge in a shortest path should be
		Function<Link, Double> lengthFunction;
		switch (config.getStringValue("ean_model_weight_drive").toUpperCase()) {
			case "AVERAGE_DRIVING_TIME":
				lengthFunction = link -> (link.getLowerBound() + link.getUpperBound()) / 2.0;
				break;
			case "MINIMAL_DRIVING_TIME":
				lengthFunction = link -> (double) link.getLowerBound();
				break;
			case "MAXIMAL_DRIVING_TIME":
				lengthFunction = link -> (double) link.getUpperBound();
				break;
			case "EDGE_LENGTH":
				lengthFunction = Link::getLength;
				break;
			default:
				throw new ConfigTypeMismatchException("ean_model_weight_drive", "String", config.getStringValue
						("ean_model_weight_drive"));
		}
		//Now iterate all od pairs, compute shortest path and add them to the returned map
		for (Stop origin : ptn.getNodes()) {
			Dijkstra<Stop, Link, Graph<Stop, Link>> dijkstra = new Dijkstra<>(ptn, origin, lengthFunction);
			dijkstra.computeShortestPaths();
			for (Stop destination : ptn.getNodes()) {
				if (od.getValue(origin.getId(), destination.getId()) == 0) {
					continue;
				}
				Collection<Path<Stop, Link>> odPath = dijkstra.getPaths(destination);
				if (odPath.size() == 0) {
					logger.log(LogLevel.WARN, "Found no path from " + origin + " to " + destination + "but there are "
							+ od.getValue(origin.getId(), destination.getId()) + " passengers");
				}
				paths.put(new Pair<>(origin.getId(), destination.getId()), odPath);
			}
		}
		return paths;
	}

	/**
	 * Determine whether the given line contain any of the given paths
	 * @param line the line to contain paths
	 * @param paths the paths to check
	 * @return whether a path is contained in the line
	 */
	private static boolean lineContainsPath(Line line, Collection<Path<Stop, Link>> paths) {
		for (Path<Stop, Link> path : paths) {
			if (line.getLinePath().contains(path)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Find all acceptable line ids for the given preferable paths. For each line, there will be a check, whether it
	 * contains any shortest path for a passenger. If this is the case, the line id is added for this passenger
	 * @param linePool the linepool
	 * @param preferablePaths the preferable paths of the passengers
	 * @param ptn the ptn
	 * @return a mapping of an od-pair to the set of acceptable line ids
	 */
	private static HashMap<Pair<Integer, Integer>, HashSet<Integer>> computeAcceptableLineIds(LinePool linePool,
	                                                                                          Map<Pair<Integer,
			                                                                                          Integer>,
			                                                                                          Collection<Path<Stop, Link>>> preferablePaths,
	                                                                                          Graph<Stop, Link> ptn) {
		HashMap<Pair<Integer, Integer>, HashSet<Integer>> acceptableLineIds = new HashMap<>();
		for (Pair<Integer, Integer> odPair : preferablePaths.keySet()) {
			acceptableLineIds.put(odPair, new HashSet<>());
			for (Line line : linePool.getLines()) {
				if(!line.getLinePath().contains(ptn.getNode(odPair.getFirstElement())) || !line.getLinePath()
						.contains(ptn.getNode(odPair.getSecondElement()))){
					continue;
				}
				if (lineContainsPath(line, preferablePaths.get(odPair))) {
					acceptableLineIds.get(odPair).add(line.getId());
				}
			}
		}
		return acceptableLineIds;
	}
}
