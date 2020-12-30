package net.lintim.util;

/**
 * Parameter class for the direct line planning model
 */
public class DirectParameters {
    private static final Logger logger = new Logger(DirectParameters.class.getCanonicalName());
    private final int commonFrequencyDivisor;
    private final int periodLength;
    private final int capacity;
    private final double budget;
    private final double mipGap;
    private final double timelimit;
    private final double directFactor;
    private final double costFactor;
    private final String weightDrive;


    /**
     * Create a new parameter object for the direct line planning problem. Will read all necessary data from the
     * provided config. For information on the different config parameters, check the LinTim documentation.
     * @param config the config to read
     */
    public DirectParameters(Config config) {
        commonFrequencyDivisor = config.getIntegerValue("lc_common_frequency_divisor");
        periodLength = config.getIntegerValue("period_length");
        capacity = config.getIntegerValue("gen_passengers_per_vehicle");
        budget = config.getDoubleValue("lc_budget");
        mipGap = config.getDoubleValue("lc_mip_gap");
        timelimit = config.getDoubleValue("lc_timelimit");
        boolean respectCosts = config.getBooleanValue("lc_direct_optimize_costs");
        if (respectCosts) {
            double factor = config.getDoubleValue("lc_mult_relation");
            if (factor < 0 || factor > 1) {
                logger.warn("Config key lc_mult_relation should in [0,1] but this is not the case!");
            }
            directFactor = 1 - factor;
        }
        else {
            directFactor = 1;
        }
        costFactor = 1 - directFactor;
        weightDrive = config.getStringValue("ean_model_weight_drive").toUpperCase();
    }

    public int getCommonFrequencyDivisor() {
        return commonFrequencyDivisor;
    }

    public int getPeriodLength() {
        return periodLength;
    }

    public double getMipGap() {
        return mipGap;
    }

    public double getTimelimit() {
        return timelimit;
    }

    public double getDirectFactor() {
        return directFactor;
    }

    public double getCostFactor() {
        return costFactor;
    }

    public String getWeightDrive() {
        return weightDrive;
    }

    public int getCapacity() {
        return capacity;
    }

    public double getBudget() {
        return budget;
    }
}
