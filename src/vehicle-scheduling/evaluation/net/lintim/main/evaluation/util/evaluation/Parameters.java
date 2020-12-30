package net.lintim.main.evaluation.util.evaluation;

import net.lintim.util.Config;

public class Parameters {

    private final double costFactorFullDuration;
    private final double costFactorEmptyDuration;
    private final double costFactorFullLength;
    private final double costFactorEmptyLength;
    private final double costPerVehicle;
    private final int timeUnitsPerMinute;
    private final int turnoverTime;
    private final int depotIndex;
    private static final int minutesPerHour = 60;
    private static final int secondsPerMinute = 60;

    public Parameters(Config config) {
        //Convert cost factors from cost per hour to cost per second
        costFactorFullDuration = config.getDoubleValue("vs_eval_cost_factor_full_trips_duration") /
            minutesPerHour / secondsPerMinute;
        costFactorEmptyDuration = config.getDoubleValue("vs_eval_cost_factor_empty_trips_duration") / minutesPerHour / secondsPerMinute;
        costFactorFullLength = config.getDoubleValue("vs_eval_cost_factor_full_trips_length");
        costFactorEmptyLength = config.getDoubleValue("vs_eval_cost_factor_empty_trips_length");
        costPerVehicle = config.getDoubleValue("vs_vehicle_costs");
        timeUnitsPerMinute = config.getIntegerValue("time_units_per_minute");
        // convert from time units to seconds
        turnoverTime = config.getIntegerValue("vs_turn_over_time") * secondsPerMinute / timeUnitsPerMinute;
        depotIndex = config.getIntegerValue("vs_depot_index");
    }

    public double getCostFactorFullDuration() {
        return costFactorFullDuration;
    }

    public double getCostFactorEmptyDuration() {
        return costFactorEmptyDuration;
    }

    public double getCostFactorFullLength() {
        return costFactorFullLength;
    }

    public double getCostFactorEmptyLength() {
        return costFactorEmptyLength;
    }

    public double getCostPerVehicle() {
        return costPerVehicle;
    }

    public int getTurnoverTime() {
        return turnoverTime;
    }

    public int getDepotIndex() {
        return depotIndex;
    }

    public int getTimeUnitsPerMinute() {
        return timeUnitsPerMinute;
    }

    public boolean useDepot() {
        return depotIndex != -1;
    }
}
