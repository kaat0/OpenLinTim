package net.lintim.algorithm.vehiclescheduling;

import net.lintim.io.ConfigReader;
import net.lintim.io.vehiclescheduling.IO;
import net.lintim.util.Config;

public class FlowsAndTransfers {

	public static void main(String[] args) throws Exception {

		Config config = new ConfigReader.Builder(args[0]).build().read();
		IO.initialize(config);
		IO.calculateMoselInput();

	}
}
