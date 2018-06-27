package net.lintim.main;

import net.lintim.csv.*;
import net.lintim.exception.DataInconsistentException;
import net.lintim.generator.LinePoolCostGenerator;
import net.lintim.generator.LinePoolFromLineConceptGenerator;
import net.lintim.generator.LinePoolFromLineConceptGenerator.LinePoolModel;
import net.lintim.model.Configuration;
import net.lintim.model.LineCollection;
import net.lintim.model.PublicTransportationNetwork;

import java.io.File;
import java.io.IOException;

/**
 * Wraps {@link LinePoolGenerator} to generate a line pool.
 *
 */
public class LinePoolGenerator {

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

            if(config.getStringValue("lpool_model").toLowerCase().equals("void")){
                System.err.println("lpool_model is \"VOID\". Will keep line " +
                        "pool as is.");
                return;
            }

            // -----------------------------------------------------------------
            // --- Load Public Transportation Network --------------------------
            // -----------------------------------------------------------------
            File stationsFile = new File(config.getStringValue("default_stops_file"));
            File linksFile = new File(config.getStringValue("default_edges_file"));
            File turnFile = new File(config.getStringValue("default_turn_file"));

            System.err.print("Loading Public Transportation Network... ");
            Boolean ptnIsUndirected = config.getBooleanValue("ptn_is_undirected");
            PublicTransportationNetwork ptn =
                new PublicTransportationNetwork(config);
            PublicTransportationNetworkCSV.fromFile(ptn, stationsFile,
                    linksFile);
            System.err.println("done!");

            LinePoolFromLineConceptGenerator.LinePoolModel lpoolmodel =
                LinePoolModel.valueOf(config.getStringValue("lpool_model").
                        trim().toUpperCase());

            if(lpoolmodel == LinePoolModel.SEGMENTS_BETWEEN_TURNS){
                System.err.print("Loading Turns... ");
                TurnCSV.fromFile(ptn, turnFile);
                System.err.println("done!");
            }

            // -----------------------------------------------------------------
            // --- Load Line Concept -------------------------------------------
            // -----------------------------------------------------------------
            File linesFile = new File(config.getStringValue(
                    "default_lines_default_file"));
            System.err.print("Loading Line Concept... ");
            LineCollection lc = new LineCollection(ptn);
            LineConceptCSV.fromFile(lc, linesFile);
            System.err.println("done!");

            // -----------------------------------------------------------------
            // --- Compute Line Pool and Costs ---------------------------------
            // -----------------------------------------------------------------
            System.err.print("Generating Line Pool.. ");
            LineCollection pool = new LineCollection(ptn);
            LinePoolFromLineConceptGenerator lpgen =
                new LinePoolFromLineConceptGenerator(lc, config.getIntegerValue(
                        "lpool_minimal_line_length"), lpoolmodel);
            lpgen.linePoolFromLineConcept(pool);
            LinePoolCostGenerator lpcostgen = new LinePoolCostGenerator(pool,
                    config.getDoubleValue("lpool_fixed_cost_per_line"));
            lpcostgen.computeCosts();
            System.err.println("done!");

            // -----------------------------------------------------------------
            // --- Save Pool ---------------------------------------------------
            // -----------------------------------------------------------------
            System.err.print("Saving Line Pool... ");
            File poolFile = new File(config.getStringValue("default_pool_file"));
            File poolCostFile = new File(config.getStringValue("default_pool_cost_file"));
            LinePoolCostCSV.toFile(pool, poolCostFile, ptnIsUndirected);
            LinePoolCSV.toFile(pool, poolFile, ptnIsUndirected);
            System.err.println("done!");

            // -----------------------------------------------------------------
            // --- Embed Line Concept ------------------------------------------
            // -----------------------------------------------------------------
            File linesConceptFile =
                new File(config.getStringValue("default_lines_file"));
            System.err.print("Saving Embedded Line Concept... ");
            if(config.getBooleanValue("lpool_embed_original_line_concept")){
                LineConceptCSV.toFile(pool, linesConceptFile, ptnIsUndirected);
                System.err.println("done!");
            }
            else {
                System.err.println("skipping, set " +
                        "lpool_embed_original_line_concept to true to embed " +
                        "line concept.");
            }

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
