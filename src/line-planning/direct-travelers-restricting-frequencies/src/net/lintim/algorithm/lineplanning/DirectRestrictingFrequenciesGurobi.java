package net.lintim.algorithm.lineplanning;

import gurobi.*;
import net.lintim.algorithm.Dijkstra;
import net.lintim.exception.ConfigTypeMismatchException;
import net.lintim.exception.SolverGurobiException;
import net.lintim.model.*;
import net.lintim.solver.impl.GurobiHelper;
import net.lintim.util.Logger;
import net.lintim.util.Pair;
import net.lintim.util.lineplanning.Parameters;

import java.util.*;
import java.util.function.Function;

/**
 *
 */
public class DirectRestrictingFrequenciesGurobi {

    private static final Logger logger = new Logger(DirectRestrictingFrequenciesGurobi.class);

    /**
     * Solve the direct line planning problem while restricting the possible frequencies. Will use the shortest paths
     * as preferable paths (length of the path is determined by config parameter)
     *
     * @param ptn        the ptn
     * @param od         the od matrix
     * @param linePool   the pool of possible lines
     * @param parameters the settings of the model
     * @return whether a feasible solution could be found
     */
    public static boolean solveLinePlanningDirect(Graph<Stop, Link> ptn, OD od, LinePool linePool, Parameters parameters) {
        return solveLinePlanningDirect(ptn, od, linePool, parameters, computeShortestPaths(ptn, od, parameters));
    }

    /**
     * Solve the direct line planning problem while restricting the possible frequencies.
     *
     * @param ptn             the ptn
     * @param od              the od matrix
     * @param linePool        the pool of possible lines
     * @param parameters      the settings of the model
     * @param preferablePaths the preferable paths of the passengers
     * @return whether a feasible solution could be found
     */
    public static boolean solveLinePlanningDirect(Graph<Stop, Link> ptn, OD od, LinePool linePool, Parameters parameters,
                                                  Map<Pair<Integer, Integer>, Collection<Path<Stop, Link>>>
                                                      preferablePaths) {
        try {
            GRBEnv env = new GRBEnv();
            GRBModel directModel = new GRBModel(env);
            GurobiHelper.setGurobiSolverParameters(directModel, parameters);

            directModel.set(GRB.IntAttr.ModelSense, GRB.MAXIMIZE);

            logger.debug("Precomputation, see which lines can be used directly by which passengers");
            HashMap<Pair<Integer, Integer>, HashSet<Integer>> acceptableLineIds = computeAcceptableLineIds(linePool,
                preferablePaths, ptn);
            logger.debug("Add variables");
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
                    if (od.getValue(origin.getId(), destination.getId()) == 0) {
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
                for (int freq = 0; freq <= parameters.getMaximalFrequency(); freq++) {
                    GRBVar lineUsesFreq = directModel.addVar(0, 1, 0, GRB.BINARY, "f_" + line.getId() + "_" + freq);
                    f.get(line.getId()).put(freq, lineUsesFreq);
                    lineFreqConstraint.addTerm(1, lineUsesFreq);
                }
                directModel.addConstr(lineFreqConstraint, GRB.EQUAL, 1, "line_" + line.getId() + "_uses_one_freq");
            }
            logger.debug("Add capacity constraints");
            //Constraint 3.7 -> Ensure that the capacity of each line is not exceeded
            for (Link link : ptn.getEdges()) {
                HashMap<Integer, GRBLinExpr> directTravellersOnLineAndEdge = new HashMap<>();
                for (Stop origin : ptn.getNodes()) {
                    for (Stop destination : ptn.getNodes()) {
                        if (od.getValue(origin.getId(), destination.getId()) == 0) {
                            continue;
                        }
                        for (int lineId : acceptableLineIds.get(new Pair<>(origin.getId(), destination.getId()))) {
                            directTravellersOnLineAndEdge.computeIfAbsent(lineId, k -> new GRBLinExpr());
                            directTravellersOnLineAndEdge.get(lineId).addTerm(1, d.get(origin.getId()).get
                                (destination.getId()).get(lineId));
                        }
                    }
                }
                for (Line line : linePool.getLines()) {
                    GRBLinExpr capacityOfLine = new GRBLinExpr();
                    for (int freq = 1; freq <= parameters.getMaximalFrequency(); freq++) {
                        capacityOfLine.addTerm(freq * parameters.getCapacity(), f.get(line.getId()).get(freq));
                    }
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
                        for (int freq = 1; freq <= parameters.getMaximalFrequency(); freq++) {
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
            if (parameters.getNumberOfPossibleFrequencies() != -1) {
                logger.debug("Add frequency constraint");
                GRBLinExpr freqConstraint = new GRBLinExpr();
                for (int freq = 1; freq <= parameters.getMaximalFrequency(); freq++) {
                    GRBVar freqUsed = directModel.addVar(0, 1, 0, GRB.BINARY, "freq_" + freq + "_used");
                    freqConstraint.addTerm(1, freqUsed);
                    for (Line line : linePool.getLines()) {
                        directModel.addConstr(freqUsed, GRB.GREATER_EQUAL, f.get(line.getId()).get(freq), "line_" + line

                            .getId() + "_uses_freq_" + freq);
                    }
                }
                directModel.addConstr(freqConstraint, GRB.LESS_EQUAL, parameters.getNumberOfPossibleFrequencies(), "freq_bound");
            }

            //Budget constraints -> Restrict the costs of the line concept
            logger.debug("Add budget constraint");
            GRBLinExpr costOfLineConcept = new GRBLinExpr();
            for (Line line : linePool.getLines()) {
                for (int freq = 1; freq <= parameters.getMaximalFrequency(); freq++) {
                    costOfLineConcept.addTerm(line.getCost() * freq, f.get(line.getId()).get(freq));
                }
            }
            directModel.addConstr(costOfLineConcept, GRB.LESS_EQUAL, parameters.getBudget(), "budget");
            directModel.set(GRB.IntAttr.ModelSense, GRB.MAXIMIZE);
            if (parameters.writeLpFile()) {
                directModel.write("direct-restricting.lp");
            }
            logger.debug("Start optimizing");
            directModel.optimize();
            logger.debug("End optimizing");

            int status = directModel.get(GRB.IntAttr.Status);
            if (directModel.get(GRB.IntAttr.SolCount) > 0) {
                if (status == GRB.OPTIMAL) {
                    logger.debug("Optimal solution found");
                } else {
                    logger.debug("Feasible solution found");
                }
                logger.debug("Read back solution");
                //Read the frequencies and set the lines accordingly
                for (Line line : linePool.getLines()) {
                    for (int freq = 0; freq <= parameters.getMaximalFrequency(); freq++) {
                        if (Math.round(f.get(line.getId()).get(freq).get(GRB.DoubleAttr.X)) > 0) {
                            line.setFrequency(freq);
                            break;
                        }
                    }
                }
                return true;
            }
            logger.debug("No feasible solution found");
            if (status == GRB.INFEASIBLE) {
                logger.debug("Computing IIS");
                directModel.computeIIS();
                directModel.write("direct-restricting.ilp");
            }
            return false;

        } catch (GRBException e) {
            throw new SolverGurobiException(e.toString());
        }
    }

    /**
     * Compute the shortest paths for all the passengers. For each od pair, the returned map will contain a
     * collection of shortest paths. The length of a path will be determined by the config parameter
     * "ean_model_weight_drive".
     *
     * @param ptn        the ptn
     * @param od         the od pairs
     * @param parameters the parameters
     * @return a map of all shortest paths
     */
    private static HashMap<Pair<Integer, Integer>, Collection<Path<Stop, Link>>> computeShortestPaths(Graph<Stop,
        Link> ptn, OD od, Parameters parameters) {
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

    /**
     * Determine whether the given line contain any of the given paths
     *
     * @param line  the line to contain paths
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
     *
     * @param linePool        the linepool
     * @param preferablePaths the preferable paths of the passengers
     * @param ptn             the ptn
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
                if (!line.getLinePath().contains(ptn.getNode(odPair.getFirstElement())) || !line.getLinePath()
                    .contains(ptn.getNode(odPair.getSecondElement()))) {
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
