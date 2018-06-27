package net.lintim.util.draw;

import net.lintim.exception.ConfigNoFileNameGivenException;
import net.lintim.exception.OutputFileException;
import net.lintim.io.ConfigReader;
import net.lintim.io.PTNReader;
import net.lintim.model.Graph;
import net.lintim.model.Link;
import net.lintim.model.Stop;
import net.lintim.util.Config;
import net.lintim.util.LogLevel;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Utility class for drawing a ptn. Reads the ptn with the core library and write a dot file for further processing with
 * graphviz.
 */
public class TransformPtnToDot {
	private static Logger logger = Logger.getLogger("net.lintim.util.draw.TransformPtnToDot");

	public static void main(String[] args) {
		logger.log(LogLevel.INFO, "Begin reading configuration");
		if (args.length != 1) {
			throw new ConfigNoFileNameGivenException();
		}
		new ConfigReader.Builder(args[0]).build().read();
		boolean readExistingPtn = Config.getBooleanValueStatic("ptn_draw_existing_ptn");
		String stopFileName;
		String linkFileName;
		if (!readExistingPtn) {
			stopFileName = Config.getStringValueStatic("default_stops_file");
			linkFileName = Config.getStringValueStatic("default_edges_file");
		}
		else {
			stopFileName = Config.getStringValueStatic("default_existing_stop_file");
			linkFileName = Config.getStringValueStatic("default_existing_edge_file");
		}
		String ptnDotFileName = Config.getStringValueStatic("default_ptn_graph_file");
		double conversionFactor = Config.getDoubleValueStatic("ptn_draw_conversion_factor");
		logger.log(LogLevel.INFO, "Finished reading configuration");

		logger.log(LogLevel.INFO, "Begin reading input data");
		Graph<Stop, Link> ptn = new PTNReader.Builder().setStopFileName(stopFileName).setLinkFileName(linkFileName)
				.build().read();
		logger.log(LogLevel.INFO, "Finished reading input data");

		logger.log(LogLevel.INFO, "Begin writing output data");
		writeDotFile(ptn, conversionFactor, ptnDotFileName);
		logger.log(LogLevel.INFO, "Finished writing output data");
	}

	/**
	 * Transform the given ptn into the dot format and write it to the given output file
	 *
	 * @param ptn            the ptn to draw
	 * @param outputFileName the name of the output file to write. Will be in dot format for further processing with
	 *                       the graphviz utility
	 */
	private static void writeDotFile(Graph<Stop, Link> ptn, double conversionFactor, String outputFileName) {
		StringBuilder stringBuilder = new StringBuilder();
		if (ptn.isDirected()) {
			stringBuilder.append("di");
		}
		stringBuilder.append("graph G {\n");
		for (Stop stop : ptn.getNodes()) {
			stringBuilder.append(transformStop(stop, conversionFactor));
		}
		for (Link link : ptn.getEdges()) {
			stringBuilder.append(transformLink(link, ptn.isDirected()));
		}
		stringBuilder.append("graph [bgcolor=\"transparent\"] }");
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(outputFileName)));
			writer.write(stringBuilder.toString());
			writer.close();
		} catch (IOException e) {
			throw new OutputFileException(outputFileName);
		}
	}

	private static String transformStop(Stop stop, double conversionFactor) {
		return "\ts" + stop.getId() + " [label=\"" + stop.getShortName() + "\", pos=\"" + stop.getxCoordinate() / conversionFactor +
				"," + stop.getyCoordinate() / conversionFactor + "\"];\n";
	}

	private static String transformLink(Link link, boolean ptnIsDirected) {
		String stopConnector = ptnIsDirected ? "->" : "--";
		return "\ts" + link.getLeftNode().getId() + " " + stopConnector + " s" + link.getRightNode().getId()
				+ "[label=\"" + link.getId() + "\"];\n";
	}
}
