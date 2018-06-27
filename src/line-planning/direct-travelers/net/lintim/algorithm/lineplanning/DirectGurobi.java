package net.lintim.algorithm.lineplanning;

import gurobi.*;
import net.lintim.algorithm.Dijkstra;
import net.lintim.exception.ConfigTypeMismatchException;
import net.lintim.exception.SolverGurobiException;
import net.lintim.model.*;
import net.lintim.util.Config;
import net.lintim.util.DirectSolutionDescriptor;
import net.lintim.util.LogLevel;
import net.lintim.util.Pair;

import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 */
public class DirectGurobi {

	private static Logger logger = Logger.getLogger("net.lintim.algorithm.lineplanning.DirectGurobi");

	public static DirectSolutionDescriptor solveLinePlanningDirect(Graph<Stop, Link> ptn, OD od, LinePool linePool, Config config,
	                                                               int commonFrequencyDivisor) {
		return solveLinePlanningDirect(ptn, od, linePool, config, computeShortestPaths(ptn, od, config),
            commonFrequencyDivisor);
	}

	public static DirectSolutionDescriptor solveLinePlanningDirect(Graph<Stop, Link> ptn, OD od, LinePool linePool, Config config,
	                                                               Map<Pair<Integer, Integer>, Collection<Path<Stop, Link>>>
			                                              preferablePaths, int commonFrequencyDivisor) {
		try {
			GRBEnv env = new GRBEnv();
			GRBModel directModel = new GRBModel(env);
			directModel.set(GRB.IntAttr.ModelSense, GRB.MAXIMIZE);
			double mipGap = config.getDoubleValue("lc_mip_gap");
			if (mipGap >= 0) {
				directModel.set(GRB.DoubleParam.MIPGap, mipGap);
			}
			double timeLimit = config.getDoubleValue("lc_timelimit");
			if (timeLimit >= 0) {
				directModel.set(GRB.DoubleParam.TimeLimit, timeLimit);
			}

			Level logLevel = LogManager.getLogManager().getLogger("").getLevel();
			if (logLevel.equals(LogLevel.DEBUG)) {
				directModel.set(GRB.IntParam.LogToConsole, 1);
				directModel.set(GRB.StringParam.LogFile, "DirectModelGurobi.log");
			} else {
				directModel.set(GRB.IntParam.OutputFlag, 0);
			}
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
			//f[l] = frequency of line l
			HashMap<Integer, GRBVar> f = new HashMap<>();
			for (Line line : linePool.getLines()) {
                GRBVar frequency = directModel.addVar(0, GRB.INFINITY, 0, GRB.INTEGER, "f_" + line.getId());
				f.put(line.getId(), frequency);
                GRBVar systemFrequencyDivisor = directModel.addVar(0, GRB.INFINITY, 0, GRB.INTEGER, "g_" +
                    line.getId());
                GRBLinExpr rhs = new GRBLinExpr();
                rhs.addTerm(commonFrequencyDivisor, systemFrequencyDivisor);
                directModel.addConstr(frequency, GRB.EQUAL, rhs, "systemFrequency_" + line.getId());
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
					capacityOfLine.addTerm(capacity, f.get(line.getId()));
					//There may be a line that is not acceptable for anybody
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
						sumOfFrequencies.addTerm(1, f.get(line.getId()));
					}
				}
				directModel.addConstr(sumOfFrequencies, GRB.LESS_EQUAL, link.getUpperFrequencyBound(), "f_" + link
						.getId() + "_max");
				directModel.addConstr(sumOfFrequencies, GRB.GREATER_EQUAL, link.getLowerFrequencyBound(), "f_" +
						link.getId() + "_min");
			}

			//Budget constraints -> Restrict the costs of the line concept
			logger.log(LogLevel.DEBUG, "Add budget constraint");
			int budget = config.getIntegerValue("lc_budget");
			GRBLinExpr costOfLineConcept = new GRBLinExpr();
			for (Line line : linePool.getLines()) {
				costOfLineConcept.addTerm(line.getCost(), f.get(line.getId()));
			}
			directModel.addConstr(costOfLineConcept, GRB.LESS_EQUAL, budget, "budget");
			directModel.set(GRB.IntAttr.ModelSense, GRB.MAXIMIZE);
			if (logLevel.equals(LogLevel.DEBUG)) {
				directModel.write("DirectModelGurobi.lp");
			}
			logger.log(LogLevel.DEBUG, "Start optimizing");
			directModel.optimize();
			if(directModel.get(GRB.IntAttr.Status) != GRB.Status.OPTIMAL){
				logger.log(LogLevel.DEBUG, "Solver could not find an optimal solution, Gurobi status is " +
						directModel.get(GRB.IntAttr.Status));
				if(logLevel == LogLevel.DEBUG){
					directModel.computeIIS();
					directModel.write("DirectModelGurobi.ilp");
				}
				return new DirectSolutionDescriptor(false, Double.NEGATIVE_INFINITY);
			}
			logger.log(LogLevel.DEBUG, "Read back solution");
			//Read the frequencies and set the lines accordingly
			for(Line line : linePool.getLines()){
				line.setFrequency((int) Math.round(f.get(line.getId()).get(GRB.DoubleAttr.X)));
			}
			return new DirectSolutionDescriptor(true, directModel.get(GRB.DoubleAttr.ObjVal));
		} catch (GRBException e) {
			throw new SolverGurobiException(e.toString());
		}
	}

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

	private static boolean lineContainsPath(Line line, Collection<Path<Stop, Link>> paths) {
		for (Path<Stop, Link> path : paths) {
			if (line.getLinePath().contains(path)) {
				return true;
			}
		}
		return false;
	}

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
