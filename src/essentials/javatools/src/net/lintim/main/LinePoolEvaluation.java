package net.lintim.main;

import net.lintim.csv.*;
import net.lintim.evaluator.LineCollectionEvaluator;
import net.lintim.exception.DataInconsistentException;
import net.lintim.model.*;

import java.io.File;
import java.io.IOException;

/**
 * Runs the line concept evaluation.
 */
public class LinePoolEvaluation {

    public static void main(String[] args){

        if(args.length != 1){
            System.err.println("Error: number of arguments invalid; first " +
                    "argument must be the path to the configuration file.");
            System.exit(1);
        }

        try {
            // -----------------------------------------------------------------
            // --- Load Configuration ------------------------------------------
            // -----------------------------------------------------------------
            File configFile = new File(args[0]);

            System.err.print("Loading Configuration... ");
            Configuration config = new Configuration();
            ConfigurationCSV.fromFile(config, configFile);
            System.err.println("done!");

            // -----------------------------------------------------------------
            // --- Initialize Statistic ----------------------------------------
            // -----------------------------------------------------------------
            File statisticFile = new File(config.getStringValue(
                    "default_statistic_file"));
            System.err.print("Initializing Statistic... ");
            Statistic statistic = new Statistic();
            if(statisticFile.exists()){
                StatisticCSV.fromFile(statistic, statisticFile);
            }
            System.err.println("done!");

            // -----------------------------------------------------------------
            // --- Load Public Transportation Network --------------------------
            // -----------------------------------------------------------------
            File stationsFile =
                new File(config.getStringValue("default_stops_file"));
            File linksFile =
                new File(config.getStringValue("default_edges_file"));
            File headwayFile =
                new File(config.getStringValue("default_headways_file"));
            File loadsFile =
                new File(config.getStringValue("default_loads_file"));

            System.err.print("Loading Public Transportation Network... ");
            PublicTransportationNetwork ptn =
                new PublicTransportationNetwork(config);
            PublicTransportationNetworkCSV.fromFile(ptn, stationsFile,
                    linksFile);
            System.err.println("done!");

            /*System.err.print("Loading Initial Headway Data... ");
            HeadwayCSV.fromFile(ptn, headwayFile);
            System.err.println("done!");*/

            System.err.print("Loading Load Data... ");
            LoadCSV.fromFile(ptn, loadsFile);
            System.err.println("done!");

            // -----------------------------------------------------------------
            // --- Load Origin Destination Matrix ------------------------------
            // -----------------------------------------------------------------
            File odFile = new File(config.getStringValue("default_od_file"));

            System.err.print("Loading Origin Destination Matrix... ");
            OriginDestinationMatrix od = new OriginDestinationMatrix(ptn);
            OriginDestinationMatrixCSV.fromFile(od, odFile);
            System.err.println("done!");

            // -----------------------------------------------------------------
            // --- Load Line Pool ----------------------------------------------
            // -----------------------------------------------------------------
            File lpFile = new File(config.getStringValue("default_pool_file"));
            File poolCostFile = new File(config.getStringValue(
                    "default_pool_cost_file"));

            System.err.print("Loading Line Pool... ");
            LineCollection lp = new LineCollection(ptn);
            LinePoolCSV.fromFile(lp, lpFile);
            LinePoolCostCSV.fromFile(lp, poolCostFile);
            System.err.println("done!");

            // -----------------------------------------------------------------
            // --- Evaluate ----------------------------------------------------
            // -----------------------------------------------------------------
            System.err.println("Evaluating Line Pool...");
            System.err.println("  Computing Statistic...");
            // - - Line Pool - - - - - - - - - - - - - - - - - - - - - - - - - -
            System.err.print("    Cost: ");
            Double lpCost = LineCollectionEvaluator.cost(lp);
            statistic.setDoubleValue("lpool_cost", lpCost);
            System.err.println(lpCost);
            
            System.err.print("    Feasible OD: ");
            Boolean lpFeasibleOd = LineCollectionEvaluator.lcFeasibleOd(lp, od, 
            		config.getDoubleValue("ean_default_minimal_waiting_time"));
            statistic.setBooleanValue("lpool_feasible_od", lpFeasibleOd);
            System.err.println(lpFeasibleOd);
            
            System.err.print("    Feasible Circles: ");
            Boolean lpFeasibleCircles = LineCollectionEvaluator.lcFeasibleCircles(lp);
            statistic.setBooleanValue("lpool_feasible_circles", lpFeasibleCircles);
            System.err.println(lpFeasibleCircles);

            System.err.print("    Directed Lines: ");
            Integer lpDirectedLines =
                LineCollectionEvaluator.directedLinesUsedCount(lp);
            statistic.setIntegerValue("lpool_prop_directed_lines", 
            		lpDirectedLines);
            System.err.println(lpDirectedLines);

            System.err.print("    Line-Pool Average Time: ");
            Double lpTimeAverage =
                    LineCollectionEvaluator.lineCollectionTimeAverage(lp,
                            od, config.getDoubleValue(
                            "ptn_stop_waiting_time"));
                statistic.setDoubleValue("lpool_time_average",
                        lpTimeAverage);
                System.err.println(lpTimeAverage);
                
            

            System.err.println("  ... done!");

            // -----------------------------------------------------------------
            // --- Save Statistic ----------------------------------------------
            // -----------------------------------------------------------------
            System.err.print("Saving Statistic... ");
            StatisticCSV.toFile(statistic, statisticFile);
            System.err.println("done!");

        } catch (IOException e) {
            System.err.println();
            System.err.println("An error occured. See stacktrace below.");
            e.printStackTrace();
            System.exit(1);
        } catch (DataInconsistentException e) {
            System.err.println();
            System.err.println("An error occured. See stacktrace below.");
            e.printStackTrace();
            System.exit(1);
        }

    }

}

