package net.lintim.algorithm.lineplanning;

import gurobi.*;
import net.lintim.algorithm.Dijkstra;
import net.lintim.exception.ConfigTypeMismatchException;
import net.lintim.exception.SolverGurobiException;
import net.lintim.model.*;
import net.lintim.util.*;

import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.LogManager;

/**
 */
public class DirectGurobi {

	private static Logger logger = new Logger(DirectGurobi.class.getCanonicalName());

    /**
     * Solve the direct line planning problem without a given set of preferable paths, i.e., the shortest paths for each
     * passenger will be computed beforehand and set as preferable.
     * @param ptn the infrastructure network
     * @param od the demand
     * @param linePool the line pool
     * @param commonFrequency the common frequency, i.e., an integer divisor to all allowed frequencies
     * @param parameters the parameters of the model
     * @return a descriptor of the found solution
     */
	public static DirectSolutionDescriptor solveLinePlanningDirect(Graph<Stop, Link> ptn, OD od, LinePool linePool, int commonFrequency,
                                                                   DirectParameters parameters) {
		return solveLinePlanningDirect(ptn, od, linePool, computeShortestPaths(ptn, od, parameters), commonFrequency,
            parameters);
	}

    /**
     * Solve the direct line planning problem for a given set of preferable paths.
     * @param ptn the infrastructure network
     * @param od the demand
     * @param linePool the line pool
     * @param preferablePaths the preferable paths of the passengers. A passenger is counted as a direct traveller if
     *                        it can travel directly on a preferable path.
     * @param commonFrequency the common frequency, i.e., an integer divisor to all allowed frequencies
     * @param parameters the parameters of the model
     * @return a descriptor of the found solution
     */
	public static DirectSolutionDescriptor solveLinePlanningDirect(Graph<Stop, Link> ptn, OD od, LinePool linePool,
	                                                               Map<Pair<Integer, Integer>, Collection<Path<Stop, Link>>>
			                                              preferablePaths, int commonFrequency, DirectParameters parameters) {
		try {
			GRBEnv env = new GRBEnv();
			GRBModel directModel = new GRBModel(env);
			directModel.set(GRB.IntAttr.ModelSense, GRB.MAXIMIZE);
			double mipGap = parameters.getMipGap();
			if (mipGap >= 0) {
				directModel.set(GRB.DoubleParam.MIPGap, mipGap);
			}
			double timeLimit = parameters.getTimelimit();
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
			logger.debug("Precomputation, see which lines can be used directly by which passengers");
			Map<Pair<Integer, Integer>, Map<Integer, Path<Stop, Link>>> acceptableLineIds = computeAcceptableLineIds(linePool,
					preferablePaths, ptn);
			logger.debug("Add variables");
			//Notation is used from the public transportation script of Prof. Schöbel. When you need explanation on
			// the meaning of the variables/constraints, please see the script. We will try to use the same names as
			// mentioned there.
			//d[i][j][l] = number of passenger directly travelling from i to j using line with id l
			HashMap<Integer, HashMap<Integer, HashMap<Integer, GRBVar>>> d = new HashMap<>();
            double directFactor = parameters.getDirectFactor();
            double costFactor = parameters.getCostFactor();
            logger.debug("Using directFactor " + directFactor + " and costFactor " + costFactor);
			for (Stop origin : ptn.getNodes()) {
				d.put(origin.getId(), new HashMap<>());
				for (Stop destination : ptn.getNodes()) {
					d.get(origin.getId()).put(destination.getId(), new HashMap<>());
					GRBLinExpr sumOfAllVariablesPerODPair = new GRBLinExpr();
					if(od.getValue(origin.getId(), destination.getId()) == 0){
						continue;
					}
					for (int lineId : acceptableLineIds.get(new Pair<>(origin.getId(), destination.getId())).keySet()) {
						d.get(origin.getId()).get(destination.getId()).put(lineId, directModel.addVar(0, od.getValue
								(origin.getId(), destination.getId()), directFactor, GRB.INTEGER, "d_" + origin.getId() + "_" +
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
                GRBVar frequency = directModel.addVar(0, GRB.INFINITY, -1 * costFactor * line.getCost(), GRB.INTEGER, "f_" + line.getId());
				f.put(line.getId(), frequency);
                GRBVar systemFrequencyDivisor = directModel.addVar(0, GRB.INFINITY, 0, GRB.INTEGER, "g_" +
                    line.getId());
                GRBLinExpr rhs = new GRBLinExpr();
                rhs.addTerm(commonFrequency, systemFrequencyDivisor);
                directModel.addConstr(frequency, GRB.EQUAL, rhs, "systemFrequency_" + line.getId());
			}
			logger.debug("Add capacity constraints");
			//Constraint 3.7 -> Ensure that the capacity of each line is not exceeded
			int capacity = parameters.getCapacity();
			for (Link link : ptn.getEdges()) {
				HashMap<Integer, GRBLinExpr> directTravellersOnLineAndEdge = new HashMap<>();
				for (Line line : linePool.getLines()) {
					if (!line.getLinePath().contains(link)) {
						continue;
					}
					for (Stop origin : ptn.getNodes()) {
						for (Stop destination : ptn.getNodes()) {
							if (od.getValue(origin.getId(), destination.getId()) == 0) {
								continue;
							}
							Pair<Integer, Integer> odPair = new Pair<>(origin.getId(), destination.getId());
							if (!acceptableLineIds.get(odPair).containsKey(line.getId())) {
								continue;
							}
							if (!acceptableLineIds.get(odPair).get(line.getId()).contains(link)) {
								continue;
							}
							directTravellersOnLineAndEdge.computeIfAbsent(line.getId(), k -> new GRBLinExpr());
							directTravellersOnLineAndEdge.get(line.getId()).addTerm(1, d.get(origin.getId()).get
									(destination.getId()).get(line.getId()));
						}
					}
					GRBLinExpr capacityOfLine = new GRBLinExpr();
					capacityOfLine.addTerm(capacity, f.get(line.getId()));
					//There may be a line that is not acceptable for anybody
					GRBLinExpr directTravellersOnLine = directTravellersOnLineAndEdge.get(line.getId());
					if (directTravellersOnLine != null) {
						directModel.addConstr(directTravellersOnLine, GRB.LESS_EQUAL, capacityOfLine,
								"capacity_constraint_" + link.getId() + "_" + line.getId());
					}
				}
			}
			//Constraint 3.8 -> Ensure the upper and lower frequency bounds on the links
			logger.debug("Add upper and lower frequency bounds");
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
			logger.debug("Add budget constraint");
			double budget = parameters.getBudget();
			GRBLinExpr costOfLineConcept = new GRBLinExpr();
			for (Line line : linePool.getLines()) {
				costOfLineConcept.addTerm(line.getCost(), f.get(line.getId()));
			}
			directModel.addConstr(costOfLineConcept, GRB.LESS_EQUAL, budget, "budget");
			directModel.set(GRB.IntAttr.ModelSense, GRB.MAXIMIZE);
			if (logLevel.equals(LogLevel.DEBUG)) {
				directModel.write("DirectModelGurobi.lp");
			}
			logger.debug("Start optimizing");
			directModel.optimize();
			if(directModel.get(GRB.IntAttr.Status) != GRB.Status.OPTIMAL){
				logger.debug("Solver could not find an optimal solution, Gurobi status is " +
						directModel.get(GRB.IntAttr.Status));
				if(logLevel == LogLevel.DEBUG && commonFrequency == 1){
					directModel.computeIIS();
					directModel.write("DirectModelGurobi.ilp");
				}
				return new DirectSolutionDescriptor(false, Double.NEGATIVE_INFINITY);
			}
			logger.debug("Read back solution");
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
			Link> ptn, OD od, DirectParameters parameters) {
		HashMap<Pair<Integer, Integer>, Collection<Path<Stop, Link>>> paths = new HashMap<>();
		//First determine what the length of an edge in a shortest path should be
		Function<Link, Double> lengthFunction;
		switch (parameters.getWeightDrive()) {
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
				throw new ConfigTypeMismatchException("ean_model_weight_drive", "String", parameters.getWeightDrive());
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
					logger.warn("Found no path from " + origin + " to " + destination + "but there are "
							+ od.getValue(origin.getId(), destination.getId()) + " passengers");
				}
				paths.put(new Pair<>(origin.getId(), destination.getId()), odPath);
			}
		}
		return paths;
	}

	private static Pair<Boolean, Path<Stop, Link>> lineContainsPath(Line line, Collection<Path<Stop, Link>> paths) {
		for (Path<Stop, Link> path : paths) {
			if (line.getLinePath().contains(path)) {
				return new Pair<>(true, path);
			}
		}
		return new Pair<>(false, null);
	}

	private static Map<Pair<Integer, Integer>, Map<Integer, Path<Stop, Link>>> computeAcceptableLineIds(LinePool linePool,
	                                                                                          Map<Pair<Integer,
			                                                                                          Integer>,
			                                                                                          Collection<Path<Stop, Link>>> preferablePaths,
	                                                                                          Graph<Stop, Link> ptn) {
		Map<Pair<Integer, Integer>, Map<Integer, Path<Stop, Link>>> acceptableLineIds = new HashMap<>();
		for (Pair<Integer, Integer> odPair : preferablePaths.keySet()) {
			acceptableLineIds.put(odPair, new HashMap<>());
			for (Line line : linePool.getLines()) {
				if(!line.getLinePath().contains(ptn.getNode(odPair.getFirstElement())) || !line.getLinePath()
						.contains(ptn.getNode(odPair.getSecondElement()))){
					continue;
				}
				Pair<Boolean, Path<Stop, Link>> isContained = lineContainsPath(line, preferablePaths.get(odPair));
				if (isContained.getFirstElement()) {
					acceptableLineIds.get(odPair).put(line.getId(), isContained.getSecondElement());
				}
			}
		}
		return acceptableLineIds;
	}
}
