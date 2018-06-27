package net.lintim.algorithm.tools;

import net.lintim.exception.ConfigTypeMismatchException;
import net.lintim.main.tools.PTNLoadGeneratorMain;
import net.lintim.model.*;
import net.lintim.util.Config;
import net.lintim.util.LogLevel;

import java.util.HashMap;
import java.util.logging.Logger;

/**
 */
public class PTNLoadGenerator {

    private static Logger logger = Logger.getLogger("net.lintim.algorithm.tools.PTNLoadGenerator");
    private static final double EPSILON = 0.01;
    private final Graph<Stop, Link> ptn;
    private final int K;
    private final OD od;
    private final int capacity;
    private final LoadRoutingNetwork loadRoutingNetwork;
    private final int maxIterations;
    private final double lowerFrequencyFactor;
    private final double upperFrequencyFactor;
    private final int fixUpperFrequency;
    private final boolean useFixUpperFrequency;
    private final PTNLoadGeneratorMain.PTNLoadGeneratorType generatorType;

    public static class Builder{
        private Graph<Stop, Link> ptn;
        private OD od;
        private LinePool linePool;
        private Config config;

        public Builder(Graph<Stop, Link> ptn, OD od, LinePool linePool, Config config){
            this.ptn = ptn;
            this.od = od;
            this.linePool = linePool;
            this.config = config;
        }

        private static PTNLoadGeneratorMain.PTNLoadGeneratorType parseLoadGeneratorType(String type) {
            switch (type.toLowerCase()) {
                case "sp":
                    return PTNLoadGeneratorMain.PTNLoadGeneratorType.SHORTEST_PATH;
                case "reward":
                    return PTNLoadGeneratorMain.PTNLoadGeneratorType.REWARD;
                case "reduction":
                    return PTNLoadGeneratorMain.PTNLoadGeneratorType.REDUCTION;
                case "iterative":
                    return PTNLoadGeneratorMain.PTNLoadGeneratorType.ITERATIVE;
                default:
                    throw new ConfigTypeMismatchException("ptn_load_generator_type", "PTNLoadGeneratorType", type);
            }
        }

        public PTNLoadGenerator build(){
            int K = config.getIntegerValue("load_generator_number_of_shortest_paths");
            PTNLoadGeneratorMain.PTNLoadGeneratorType loadGeneratorType = parseLoadGeneratorType(config
                .getStringValue("load_generator_type"));
            int changePenalty = config.getIntegerValue("ean_change_penalty");
            int minChangeTime = config.getIntegerValue("ean_default_minimal_change_time");
            int maxChangeTime = config.getIntegerValue("ean_default_maximal_change_time");
            double minChangeTimeFactor = config.getDoubleValue("load_generator_min_change_time_factor");
            boolean iterateWithCg = config.getBooleanValue("load_generator_use_cg");
            String eanModelWeightDrive = config.getStringValue("ean_model_weight_drive");
            String eanModelWeightWait = config.getStringValue("ean_model_weight_wait");
            double costFactor = config.getDoubleValue("load_generator_scaling_factor");
            int maxIterations = config.getIntegerValue("load_generator_max_iteration");
            double beta = config.getDoubleValue("load_generator_sp_distribution_factor");
            int capacity = config.getIntegerValue("gen_passengers_per_vehicle");
            double lowerFrequencyFactor = config.getDoubleValue("load_generator_lower_frequency_factor");
            double upperFrequencyFactor = config.getDoubleValue("load_generator_upper_frequency_factor");
            boolean useFixUpperFrequency = config.getBooleanValue("load_generator_fix_upper_frequency");
            int fixUpperFrequency = config.getIntegerValue("load_generator_fixed_upper_frequency");
            return new PTNLoadGenerator(ptn, od, linePool, changePenalty, K, iterateWithCg, eanModelWeightDrive,
                eanModelWeightWait, loadGeneratorType, costFactor, beta, capacity, maxIterations,
                lowerFrequencyFactor, useFixUpperFrequency, upperFrequencyFactor, fixUpperFrequency, minChangeTime,
                maxChangeTime, minChangeTimeFactor);
        }
    }

    /**
     * Create a new load generator. The computation can be started afterwards using {@link #computeLoad()}.
     * @param ptn the baseline ptn
     * @param od the od matrix
     * @param linePool the linepool. If no change&go network is used, this may be null
     * @param changePenalty the change penalty, used in the change&go network
     * @param K the number of shortest paths to calculate
     * @param iterateWithCg whether to use a change&go-network
     * @param eanModelWeightDrive the traveling time model to use for the ptn edges
     * @param loadGeneratorType the type of load generator to use
     * @param costFactor the cost factor
     * @param beta the distribution factor, if multiple shortest paths are used
     * @param capacity the capacity of a vehicle
     * @param maxIterations the maximal number of iterations
     * @param lowerFrequencyFactor the factor to multiply the resulting lower frequencies with
     * @param useFixUpperFrequency whether to use fixed upper frequencies
     * @param upperFrequencyFactor the factor to multiply the lower freq. with to get the upper freq.
     * @param fixUpperFrequency the fixed upper frequency to use
     */
    private PTNLoadGenerator(Graph<Stop, Link> ptn, OD od, LinePool linePool, int changePenalty, int K, boolean
        iterateWithCg, String eanModelWeightDrive, String eanModelWeightWait, PTNLoadGeneratorMain
        .PTNLoadGeneratorType loadGeneratorType, double costFactor, double beta, int capacity, int maxIterations,
                             double lowerFrequencyFactor, boolean useFixUpperFrequency, double upperFrequencyFactor,
                             int fixUpperFrequency, int minChangeTime, int maxChangeTime, double minChangeTimeFactor) {
        this.K = K;
        this.ptn = ptn;
        this.od = od;
        this.capacity = capacity;
        this.maxIterations = maxIterations;
        this.lowerFrequencyFactor = lowerFrequencyFactor;
        this.upperFrequencyFactor = upperFrequencyFactor;
        this.useFixUpperFrequency = useFixUpperFrequency;
        this.fixUpperFrequency = fixUpperFrequency;
        this.generatorType = loadGeneratorType;
        this.loadRoutingNetwork = new LoadRoutingNetwork(ptn, od, linePool, eanModelWeightDrive,
            eanModelWeightWait, loadGeneratorType,
            costFactor, iterateWithCg, beta, capacity, changePenalty, minChangeTime, maxChangeTime, minChangeTimeFactor);
    }

    /**
     * Start the computation of the new loads.
     */
    public void computeLoad() {
        //Reset the load on all edges
        for (Link link : ptn.getEdges()) {
            link.setLoad(0);
        }
        switch (generatorType) {
            case REDUCTION:
                computeReductionLoad();
                break;
            case SHORTEST_PATH:
                computeShortestPathLoad();
                break;
            case REWARD:
                computeShortestPathRewardLoad();
                break;
            case ITERATIVE:
                //TODO
                break;
        }
        computeLowerAndUpperBounds();
    }

    private void computeReductionLoad() {
        iterateLoadCalculation();
        long unusedLinks = ptn.getEdges().stream().filter(link -> link.getLoad() == 0).count();
        logger.log(LogLevel.DEBUG, "Reduced ptn network by " + unusedLinks + " links");
        logger.log(LogLevel.DEBUG, "Unused edges are:");
        ptn.getEdges().stream().filter(link -> link.getLoad() == 0).forEach(link -> logger.log(LogLevel.DEBUG, link
            .toString()));
        loadRoutingNetwork.computeNewShortestPathRerouteReduction(K);
    }

    private void computeShortestPathLoad() {
        loadRoutingNetwork.computeNewShortestPaths(K);
    }

    private void computeShortestPathRewardLoad() {
        iterateLoadCalculationPerPassenger();
    }


    private void iterateLoadCalculation() {
        HashMap<Link, Double> lastLoad = getLoadInformation(ptn);
        for (int iteration = 1; iteration <= maxIterations; iteration++) {
            logger.log(LogLevel.DEBUG, "Iteration " + iteration);
            loadRoutingNetwork.computeNewShortestPaths(K);
            HashMap<Link, Double> currentLoad = getLoadInformation(ptn);
            double loadDifference = computeLoadDifference(currentLoad, lastLoad);
            if (loadDifference < EPSILON) {
                logger.log(LogLevel.DEBUG, "End in iteration " + iteration);
                break;
            }
            lastLoad = currentLoad;
        }
    }

    private void iterateLoadCalculationPerPassenger() {
        HashMap<Link, Double> lastLoad = getLoadInformation(ptn);
        for (int iteration = 1; iteration <= maxIterations; iteration++) {
            logger.log(LogLevel.DEBUG, "Iteration " + iteration);
            for(Stop origin: ptn.getNodes()){
                for(Stop destination: ptn.getNodes()){
                    double odValue = od.getValue(origin.getId(), destination.getId());
                    for(int passenger = 1; passenger <= odValue; passenger++){
                        loadRoutingNetwork.computeNewShortestPaths(origin, destination, passenger);
                    }

                }
            }
            loadRoutingNetwork.distributeLoad();
            HashMap<Link, Double> currentLoad = getLoadInformation(ptn);
            double loadDifference = computeLoadDifference(lastLoad, currentLoad);
            if(loadDifference < EPSILON){
                logger.log(LogLevel.DEBUG, "End in iteration " + iteration);
                break;
            }
            lastLoad = currentLoad;
        }
    }

    private void computeLowerAndUpperBounds() {
        for (Link link : ptn.getEdges()) {
            double lowerFrequency = link.getLoad() / capacity;
            link.setLowerFrequencyBound((int) Math.ceil(lowerFrequencyFactor * lowerFrequency));
            if (useFixUpperFrequency) {
                link.setUpperFrequencyBound(fixUpperFrequency);
            } else {
                link.setUpperFrequencyBound((int) (upperFrequencyFactor * lowerFrequency));
            }
        }
    }

    private static HashMap<Link, Double> getLoadInformation(Graph<Stop, Link> ptn) {
        HashMap<Link, Double> loadInformation = new HashMap<>();
        for (Link link : ptn.getEdges()) {
            loadInformation.put(link, link.getLoad());
        }
        return loadInformation;
    }

    private static double computeLoadDifference(HashMap<Link, Double> firstLoad, HashMap<Link, Double> secondLoad) {
        double sum = 0;
        for (Link link : firstLoad.keySet()) {
            sum += Math.pow(firstLoad.get(link) - secondLoad.get(link), 2);
        }
        return sum;
    }
}
