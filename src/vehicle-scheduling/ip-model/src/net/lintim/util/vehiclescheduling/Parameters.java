package net.lintim.util.vehiclescheduling;

import net.lintim.util.Config;
import net.lintim.util.SolverType;

import java.util.logging.Level;

import static net.lintim.util.vehiclescheduling.Constants.MINUTES_PER_HOUR;
import static net.lintim.util.vehiclescheduling.Constants.SECONDS_PER_MINUTE;

public class Parameters {

    private final int depotIndex;
    private final double factorLength;
    private final double factorTime;
    private final double vehicleCost;
    private final int timeUnitsPerMinute;
    private final int turnoverTime;
    private final boolean useDepot;
    private final int timeLimit;
    private final Level logLevel;
    private final SolverType solverType;

    /**
     * Create a new parameter class that reads all necessary info from the config
     * @param config the config to read from
     */
    public Parameters(Config config) {
        factorLength = config.getDoubleValue("vs_eval_cost_factor_empty_trips_length");
        factorTime = config.getDoubleValue("vs_eval_cost_factor_empty_trips_duration") / MINUTES_PER_HOUR / SECONDS_PER_MINUTE;
        vehicleCost = config.getDoubleValue("vs_vehicle_costs");
        depotIndex = config.getIntegerValue("vs_depot_index");
        timeUnitsPerMinute = config.getIntegerValue("time_units_per_minute");
        // Convert turnoverTime from time units to minutes
        turnoverTime = config.getIntegerValue("vs_turn_over_time") * SECONDS_PER_MINUTE / timeUnitsPerMinute;
        useDepot = depotIndex != -1;
        timeLimit = config.getIntegerValue("vs_timelimit");
        logLevel = config.getLogLevel("console_log_level");
        solverType = Config.getSolverTypeStatic("vs_solver");
    }

    public boolean useDepot() {
        return useDepot;
    }

    public int getTimeLimit() {
        return timeLimit;
    }

    public Level getLogLevel() {
        return logLevel;
    }

    public int getDepotIndex() {
        return depotIndex;
    }

    public double getFactorLength() {
        return factorLength;
    }

    public double getFactorTime() {
        return factorTime;
    }

    public double getVehicleCost() {
        return vehicleCost;
    }

    public int getTurnoverTime() {
        return turnoverTime;
    }

    public SolverType getSolverType() {
        return solverType;
    }

    public int getTimeUnitsPerMinute() {
        return timeUnitsPerMinute;
    }
}
