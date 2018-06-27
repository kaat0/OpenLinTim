import com.dashoptimization.*;

import java.io.*;
import java.util.*;

//import java.util.TreeMap;

/**
 *
 */
public class CostDirect {

	public static String newline = "\n";

	public static void main(String[] args) throws IOException, InterruptedException {
		// read values from Config file(s)

		Config config = new Config(new File("basis/Config.cnf"));
		String edgeFile = config.getStringValue("default_edges_file"); // "basis/Edge.giv";
		String demandFile = config.getStringValue("default_od_file"); // "basis/OD.giv";
		String lineFile = config.getStringValue("default_pool_file"); // "basis/Pool.giv";
		String loadFile = config.getStringValue("default_loads_file"); // "basis/Load.giv";
		String outputFile = config.getStringValue("default_lines_file"); // "line-planning/Line-Concept.lin";
		String linecostFile = config.getStringValue("default_pool_cost_file"); // "line-planning/Pool-Cost.giv";
		String lcHeader = config.getStringValue("lines_header");
		boolean undirected = config.getBooleanValue("ptn_is_undirected");
		boolean capRestricted = config.getBooleanValue("lc_mult_cap_restrict");
		double multicritRelation = config.getDoubleValue("lc_mult_relation");
		double tolerance = config.getDoubleValue("lc_mult_tolerance");
		int capacity = config.getIntegerValue("gen_passengers_per_vehicle");
		int commonFrequencyDivisor = config.getIntegerValue("lc_common_frequency_divisor");
		String optimizationModel = config.getStringValue("lc_model");
		// parse edges
		// first time: count edges and find highest vertex index
		int n_vertices = findHighestNodeIndex(edgeFile);
		// second time: actually parse edges
		InputData input = new InputData(n_vertices);
		input.readEdges(edgeFile, undirected);
		input.readLoad(loadFile, undirected);
		input.readDemand(demandFile);
		input.readLinePool(lineFile, undirected);

		// find all possible shortest paths in the ptn (not only within line
		// pool)
		String[][][] allPaths = Dijkstra.allShortestPaths(input.cost, input.names);
		// sum up numbers of possible shortest paths (only statistic)
		int x = 0;
		for (int i = 0; i < allPaths.length; i++)
			for (int j = i + 1; j < allPaths[i].length; j++)
				x += allPaths[i][j].length;

		input.readLineCosts(linecostFile);

		for (int l = 0; l < input.linePool.length; l++)
			System.out.println(input.linePool[l]);

		System.out.println(n_vertices + " Knoten eingelesen");
		System.out.println(x + " kuerzeste Wege bestimmt");
		System.out.println(input.linePool.length + " moegliche Linien eingelesen");

		// now set up the problem and solve it
		if(optimizationModel.equals("mult-cost-direct")){
			List<Integer> possibleSystemFrequencies = determinePossibleCommonDivisors(commonFrequencyDivisor);
			boolean optimalSolutionFound = false;
			double bestObjective = Double.NEGATIVE_INFINITY;
			Pair<int[], Double> solution;
			int[] bestSolution = null;
			for(int commonFrequency : possibleSystemFrequencies) {
				try {
					solution = solve(input.linePool, input.lineVertices, allPaths, input.cost, input.names, input.demand,
							input.lowerFrequency, input.upperFrequency, capacity, multicritRelation, input.linecost,
							capRestricted, tolerance, commonFrequency);
					optimalSolutionFound = true;
					if (bestObjective < solution.getSecondElement()) {
						// Found a better solution
						System.out.println("New best objective is " + solution.getSecondElement() + ", before was " +
								bestObjective);
						bestObjective = solution.getSecondElement();
						bestSolution = Arrays.copyOf(solution.getFirstElement(), solution.getFirstElement().length);
					}
				}
				catch (IllegalStateException e) {
					// The mip was infeasible, just skip to next common frequency if the exception text is correct
					if (! e.getMessage().contains("MIP unzulaessig")) {
						// There was some other exception, raise it!
						throw e;
					}
				}
			}
			if (!optimalSolutionFound) {
				System.out.println("Could not find a feasible solution for the problem!");
				System.exit(1);
			}
			System.out.println("Best found solution has objective value " + bestObjective);
			// print the solution as line concept
            PrintStream ps = new PrintStream(outputFile);
            ps.print("# " + lcHeader + newline);
            for (int l = 0; l < input.linePool.length; l++)
                    ps.print(input.outputLines[l].replaceAll("\\$", "" + bestSolution[l]));
            ps.close();
		} else if(optimizationModel.equals("mult-cost-direct-relax")){
			int[] solution = solveRelaxation(input.linePool, input.lineVertices, allPaths, input.cost, input.names,
					input.demand, input.lowerFrequency, input.upperFrequency, capacity, multicritRelation,
					input.linecost, capRestricted, tolerance);
			// print the solution as line concept
			PrintStream ps = new PrintStream(outputFile);
			ps.print("# " + lcHeader + newline);
			for (int l = 0; l < input.linePool.length; l++)
				ps.print(input.outputLines[l].replaceAll("\\$", "" + solution[l]));
			ps.close();
		}
	}

	public static Pair<int[], Double> solve(String[] linePool, TreeSet<Integer>[] lineVertices, String[][][] allPaths,
	                                        double[][] edges, int[][] names, double[][] demand, double[][] lowerFrequency,
	                                        double[][] upperFrequency, double vehicleCapacity, double multicritRelation, double[] linecost, boolean
			                          capRestricted, double tolerance, int commonFrequency) throws
			InterruptedException {

		int n_vertices = edges.length;

		XPRS.init();
		XPRB bcl = new XPRB();

		XPRBprob p = bcl.newProb("Multicriteria Cost Model and Direct Travelers' Approach to Line Planning");

		XPRBvar[][][] d = new XPRBvar[n_vertices][n_vertices][linePool.length];
		XPRBvar[] f = new XPRBvar[linePool.length];
		XPRBctr[][] ctrDemand = new XPRBctr[n_vertices][n_vertices];
		XPRBctr[][][] ctrCapacity = new XPRBctr[n_vertices][n_vertices][linePool.length];
		XPRBctr[][] ctrUpperFrequency = new XPRBctr[n_vertices][n_vertices];
		XPRBctr[][] ctrLowerFrequency = new XPRBctr[n_vertices][n_vertices];
		XPRBctr objective = p.newCtr("objective");
		objective.setType(XPRB.N);

		System.out.println("Weight is set to : " + multicritRelation * 100 + "% cost-model and " + (1 - multicritRelation) * 100 + "% direct-travellers.");

		System.out.println("f-Variablen anlegen");
		for (int l = 0; l < linePool.length; l++){
			f[l] = p.newVar("f_" + l, XPRB.UI, 0.0, Double.POSITIVE_INFINITY);
			XPRBvar systemFrequencyMultiplier = p.newVar("g_" + l, XPRB.UI, 0, XPRB.INFINITY);
			p.newCtr("systemFrequency_" + l, f[l].eql(systemFrequencyMultiplier.mul
					(commonFrequency)));
		}

		System.out.println("d-Variablen anlegen und Bedarfsnebenbedingungen erzeugen");
		for (int i = 0; i < n_vertices; i++) {
			if (Math.floor(100 * i / n_vertices) % 10 == 0)
				System.out.print(".");
			ctrDemand[i] = new XPRBctr[n_vertices];
			d[i] = new XPRBvar[n_vertices][linePool.length];
			for (int j = i + 1; j < n_vertices; j++) { // undirected demand!
				d[i][j] = new XPRBvar[linePool.length];
				ctrDemand[i][j] = p.newCtr("C_" + i + "," + j);
				ctrDemand[i][j].setType(XPRB.L); // Nebenbedingung vom Typ <=
				ctrDemand[i][j].setTerm(demand[i][j]); // right hand side = demand
				for (int l = 0; l < linePool.length; l++)
					if (lineVertices[l].contains(i) && lineVertices[l].contains(j)) {
						for (int k = 0; k < allPaths[i][j].length; k++) { // if there is a shortest path
							if (linePool[l].indexOf(allPaths[i][j][k]) > -1) { // from i to j in line l
								d[i][j][l] = p.newVar("d_" + i + "," + j + "," + l, XPRB.UI, 0.0,demand[i][j]);
								ctrDemand[i][j].setTerm(d[i][j][l], 1.0); // add d_i,j,l to left hand side
								objective.setTerm(d[i][j][l], (((double) Math.round((1 - multicritRelation) * 1000000)) / 1000000));
							}
						}
					}
			}
		}
		System.out.print("\nLinienkosten: ");
		for (int l = 0; l < linePool.length; l++) {
			objective.addTerm(f[l], (((double) Math.round(-multicritRelation * linecost[l] * 1000000)) / 1000000)); // add
		}

		System.out.println("\nweitere Nebenbedingungen sichern");
		for (int i = 0; i < n_vertices; i++)
			for (int j = i + 1; j < n_vertices; j++)
				if (edges[i][j] != Double.POSITIVE_INFINITY) {
					System.out.print(".");
					ctrUpperFrequency[i][j] = p.newCtr("f^max_" + i + "," + j);
					ctrUpperFrequency[i][j].setType(XPRB.L); // <=
					ctrUpperFrequency[i][j].setTerm(upperFrequency[i][j]);
					ctrLowerFrequency[i][j] = p.newCtr("f^min_" + i + "," + j);
					ctrLowerFrequency[i][j].setType(XPRB.G); // >=
					ctrLowerFrequency[i][j].setTerm(lowerFrequency[i][j]);
					for (int l = 0; l < linePool.length; l++) {
						if (linePool[l].indexOf("-" + names[i][j] + "-") > -1) {
							ctrUpperFrequency[i][j].setTerm(f[l], 1.0);
							ctrLowerFrequency[i][j].setTerm(f[l], 1.0);
							if (!capRestricted) {
								vehicleCapacity = 100000000;
							}
							ctrCapacity[i][j][l] = p.newCtr("f^cap_" + i + "," + j + "," + l);
							ctrCapacity[i][j][l].setType(XPRB.L); // <=
							ctrCapacity[i][j][l].setTerm(0.0);
							ctrCapacity[i][j][l].setTerm(f[l], (-1.0) * vehicleCapacity);
							for (int ii : lineVertices[l])
								for (int jj : lineVertices[l])
									if (ii < jj)
										for (int k = 0; k < allPaths[ii][jj].length; k++)
											if ((allPaths[ii][jj][k].indexOf("-" + names[i][j] + "-") > -1) && (linePool[l].indexOf(allPaths[ii][jj][k]) > -1)) {
												ctrCapacity[i][j][l].setTerm(d[ii][jj][l], 1.0);
											}

						}
					}
				}

		p.setObj(objective);
		p.getXPRSprob().setDblControl(XPRS.FEASTOL,tolerance);
		p.getXPRSprob().setDblControl(XPRS.MIPTOL,tolerance);
		p.getXPRSprob().setDblControl(XPRS.OPTIMALITYTOL,tolerance);
		try {
			p.exportProb(XPRB.LP, "direct.lp");
		} catch (Exception e) {
		}
		p.maxim("g");
		Thread.sleep(3000);
		int[] f_sol = new int[linePool.length];
		if (p.getMIPStat() == XPRB.MIP_INFEAS) {
			System.out.println("MIP UNZULAESSIG!");
			throw new IllegalStateException("MIP unzulaessig");
		}
		double travellers = 0;
		double cost = 0;
		double[][] travellersOnEdge = new double[n_vertices][n_vertices];
		double[][][] ctrCapacityOnEdge = new double[n_vertices][n_vertices][linePool.length];
		for (int l = 0; l < linePool.length; l++) {
			System.out.println("Line " + (l+1) + " with frequency " + f[l].getSol());
			f_sol[l] = (int) Math.round(f[l].getSol());
			cost += f_sol[l] * linecost[l];
			for (int i : lineVertices[l])
				for (int j : lineVertices[l]) {
					if ((d[i][j][l] != null) ){
						travellersOnEdge[i][j] += d[i][j][l].getSol();
					}
				}
		}
		travellers = 0;
		for (int i = 0; i < n_vertices; i++)
			for (int j = i+1; j < n_vertices; j++)
				if (travellersOnEdge[i][j] > 0) {
					travellers += travellersOnEdge[i][j];
				}

		System.out.println("Optimalwert: " + p.getObjVal() + ", Travellers: " + travellers + ", Cost: " + cost);

		return new Pair<>(f_sol, p.getObjVal());

	}

	public static int[] solveRelaxation(String[] linePool, TreeSet<Integer>[] lineVertices, String[][][] allPaths, double[][] edges, int[][] names, double[][] demand, double[][] lowerFrequency,
			double[][] upperFrequency, double vehicleCapacity, double multicritRelation, double[] linecost, boolean capRestricted,double tolerance) throws InterruptedException {

		int n_vertices = edges.length;

		XPRS.init();
		XPRB bcl = new XPRB();
		XPRBprob p = bcl.newProb("Multicriteria Cost Model and Direct Travelers' Approach to Line Planning with Relaxation");

		XPRBvar[][] d = new XPRBvar[n_vertices][n_vertices];
		XPRBvar[] f = new XPRBvar[linePool.length];
		// XPRBctr[][] ctrDemand = new XPRBctr[n_vertices][n_vertices];
		XPRBctr[][] ctrCapacity = new XPRBctr[n_vertices][n_vertices];
		XPRBctr[][] ctrUpperFrequency = new XPRBctr[n_vertices][n_vertices];
		XPRBctr[][] ctrLowerFrequency = new XPRBctr[n_vertices][n_vertices];
		XPRBctr objective = p.newCtr("objective");
		objective.setType(XPRB.N);

		System.out.println("f-Variablen anlegen");
		for (int l = 0; l < linePool.length; l++)
			f[l] = p.newVar("f_" + l, XPRB.UI, 0.0, Double.POSITIVE_INFINITY);

		System.out.println("d-Variablen anlegen und Bedarfsnebenbedingungen erzeugen");
		for (int i = 0; i < n_vertices; i++) {
			if (Math.floor(100 * i / n_vertices) % 10 == 0)
				System.out.print(".");
			// ctrDemand[i] = new XPRBctr[n_vertices];
			d[i] = new XPRBvar[n_vertices];
			for (int j = i + 1; j < n_vertices; j++) { // undirected demand!
				d[i][j] = p.newVar("D_" + i + "," + j, XPRB.UI, 0.0, demand[i][j]);
				// ctrDemand[i][j] = p.newCtr("C_" + i + "," + j);
				// ctrDemand[i][j].setType(XPRB.L); // Nebenbedingung vom Typ <=
				// ctrDemand[i][j].setTerm(demand[i][j]); // right hand side =
				// demand
				objective.setTerm(d[i][j], (((double)Math.round((1 - multicritRelation)*1000000)))/1000000);
			}
		}

		for (int l = 0; l < linePool.length; l++) {
			objective.addTerm(f[l], (((double)Math.round((-multicritRelation * linecost[l])*1000000))/1000000)); // add
																		// cost
																		// model
																		// objective
																		// function
		}

		System.out.println("\nweitere Nebenbedingungen sichern");
		for (int i = 0; i < n_vertices; i++)
			for (int j = i + 1; j < n_vertices; j++) {
				if (edges[i][j] != Double.POSITIVE_INFINITY) {
					System.out.print(".");
					ctrUpperFrequency[i][j] = p.newCtr("f^max_" + i + "," + j);
					ctrUpperFrequency[i][j].setType(XPRB.L); // <=
					ctrUpperFrequency[i][j].setTerm(upperFrequency[i][j]);
					ctrLowerFrequency[i][j] = p.newCtr("f^min_" + i + "," + j);
					ctrLowerFrequency[i][j].setType(XPRB.G); // >=
					ctrLowerFrequency[i][j].setTerm(lowerFrequency[i][j]);
					// ctrLowerFrequency[i][j].setTerm(0.0);
					for (int l = 0; l < linePool.length; l++) {
						if (linePool[l].indexOf("-" + names[i][j] + "-") > -1) {
							ctrUpperFrequency[i][j].setTerm(f[l], 1.0);
							ctrLowerFrequency[i][j].setTerm(f[l], 1.0);
						}
					}
				}
				ctrCapacity[i][j] = p.newCtr("f^cap_" + i + "," + j);
				ctrCapacity[i][j].setType(XPRB.L); // <=
				ctrCapacity[i][j].setTerm(0.0);
				ctrCapacity[i][j].setTerm(d[i][j], 1.0);

			}
		if(!capRestricted){
			vehicleCapacity = 100000000;
		}
		for (int l = 0; l < linePool.length; l++)
			for (int i : lineVertices[l])
				for (int j : lineVertices[l])
					if (i < j) {
						// for (int ii = 0; ii < n_vertices; ii++) for (int jj =
						// ii + 1; jj < n_vertices; jj++)
						for (int k = 0; k < allPaths[i][j].length; k++)
							if (linePool[l].indexOf(allPaths[i][j][k]) > -1) {
								ctrCapacity[i][j].setTerm(f[l], -1.0 * vehicleCapacity);
							}
					}

		p.setObj(objective);
        p.getXPRSprob().setDblControl(XPRS.FEASTOL,tolerance);
		p.getXPRSprob().setDblControl(XPRS.MIPTOL,tolerance);
		p.getXPRSprob().setDblControl(XPRS.OPTIMALITYTOL,tolerance);

		//try {
		//	p.exportProb(XPRB.LP, "direct_r.lp");
		//} catch (Exception e) {
		//}
		p.maxim("g");
		Thread.sleep(2000);
		double travellers = 0;
		double cost = 0;
		int[] f_sol = new int[linePool.length];
		if (p.getMIPStat() == XPRB.MIP_INFEAS) {
			System.out.println("MIP UNZULAESSIG!");
			System.exit(1);
		}
		for (int l = 0; l < linePool.length; l++) {
			System.out.println("Linie " + (l + 1) + ": " + f[l].getSol());
			f_sol[l] = (int) Math.round(f[l].getSol());
			cost += f_sol[l] * linecost[l];
		}
		for (int i = 0; i < n_vertices; i++)
			for (int j = i + 1; j < n_vertices; j++)
				if ((d[i][j] != null)) {
					travellers += d[i][j].getSol();
				}

		System.out.println("Optimalwert: " + p.getObjVal() + ", Direct Travelers: " + travellers + " Costs: " + cost);

		return f_sol;

	}

	private static int findHighestNodeIndex(String edgeFile) throws IOException {
		int n_vertices = 0;
		BufferedReader in = new BufferedReader(new FileReader(edgeFile));
		String line;
		while ((line = in.readLine()) != null) {
			if (line.indexOf("#") > -1)
				line = line.substring(0, line.indexOf("#"));
			if (line.indexOf(";") == -1)
				continue;
			line = line.substring(line.indexOf(";") + 1);
			if (line.indexOf(";") == -1)
				continue;
			int v = Integer.parseInt(line.substring(0, line.indexOf(";")).trim());
			if (v > n_vertices)
				n_vertices = v;
			line = line.substring(line.indexOf(";") + 1);
			v = Integer.parseInt(line.substring(0, line.indexOf(";")).trim());
			if (v > n_vertices)
				n_vertices = v;
		}
		in.close();
		return n_vertices;
	}

	private static class InputData {
		int highestNodeIndex;
		double[][] cost;
		int[][] names;
		Vector<Integer> left;
		Vector<Integer> right;
		double[][] upperFrequency;
		double[][] lowerFrequency;
		double[][] demand;
		String[] linePool;
		String[] outputLines;
		TreeSet<Integer>[] lineVertices;
		double[] linecost;

		public InputData(int highestNodeIndex) {
			this.highestNodeIndex = highestNodeIndex;
			cost = new double[highestNodeIndex][highestNodeIndex];
			names = new int[highestNodeIndex][highestNodeIndex];
			upperFrequency = new double[highestNodeIndex][highestNodeIndex];
			lowerFrequency = new double[highestNodeIndex][highestNodeIndex];
			left = new Vector<>();
			right = new Vector<>();
			demand = new double[highestNodeIndex][highestNodeIndex];
		}

		public void readEdges(String edgeFile, boolean undirected) throws IOException {
			String line;
			BufferedReader in = new BufferedReader(new FileReader(edgeFile));
			// initialize cost matrix with infinity (i.e. no connecting edge)
			for (int i = 0; i < highestNodeIndex; i++)
				for (int j = 0; j < highestNodeIndex; j++)
					cost[i][j] = Double.POSITIVE_INFINITY;
			while ((line = in.readLine()) != null) {
				if (line.indexOf("#") > -1)
					line = line.substring(0, line.indexOf("#"));
				if (line.indexOf(";") == -1)
					continue;
				int index = Integer.parseInt(line.substring(0, line.indexOf(";")).trim());
				line = line.substring(line.indexOf(";") + 1);
				if (line.indexOf(";") == -1)
					continue;
				int v1 = Integer.parseInt(line.substring(0, line.indexOf(";")).trim());
				line = line.substring(line.indexOf(";") + 1);
				if (line.indexOf(";") == -1)
					continue;
				int v2 = Integer.parseInt(line.substring(0, line.indexOf(";")).trim());
				line = line.substring(line.indexOf(";") + 1);
				if (line.indexOf(";") == -1)
					continue;
				cost[v1 - 1][v2 - 1] = Double.parseDouble(line.substring(0, line.indexOf(";")).trim());
				if (undirected)
					cost[v2 - 1][v1 - 1] = cost[v1 - 1][v2 - 1];
				line = line.substring(line.indexOf(";") + 1);
				if (line.indexOf(";") == -1)
					continue;
				// lowerFrequency[v1 - 1][v2 - 1] =
				// Double.parseDouble(line.substring(0, line.indexOf(";")).trim());
				// lowerFrequency[v2 - 1][v1 - 1] = lowerFrequency[v1 - 1][v2 - 1];
				if (line.indexOf(";") == -1)
					continue;
				line = line.substring(line.indexOf(";") + 1);
				if (line.indexOf(";") > -1)
					line = line.substring(0, line.indexOf(";"));
				// upperFrequency[v1 - 1][v2 - 1] = Double.parseDouble(line.trim());
				// upperFrequency[v2 - 1][v1 - 1] = upperFrequency[v1 - 1][v2 - 1];
				if (left.size() <= index)
					left.setSize(index + 1);
				if (right.size() <= index)
					right.setSize(index + 1);
				left.set(index, v1 - 1);
				right.set(index, v2 - 1);
				names[v1 - 1][v2 - 1] = index;
				if (undirected)
					names[v2 - 1][v1 - 1] = index;
			}
			in.close();
		}

		public void readLoad(String loadFile, boolean undirected) throws IOException {
			String line;
			BufferedReader in = new BufferedReader(new FileReader(loadFile));
			while ((line = in.readLine()) != null) {
				if (line.indexOf("#") > -1)
					line = line.substring(0, line.indexOf("#"));
				if (line.indexOf(";") == -1)
					continue;
				int index = Integer.parseInt(line.substring(0, line.indexOf(";")).trim());
				int v1 = left.get(index);
				int v2 = right.get(index);
				line = line.substring(line.indexOf(";") + 1);
				if (line.indexOf(";") == -1)
					continue;
				line = line.substring(line.indexOf(";") + 1);
				if (line.indexOf(";") == -1)
					continue;
				lowerFrequency[v1][v2] = Double.parseDouble(line.substring(0, line.indexOf(";")).trim());
				if (undirected)
					lowerFrequency[v2][v1] = lowerFrequency[v1][v2];
				if (line.indexOf(";") == -1)
					continue;
				line = line.substring(line.indexOf(";") + 1);
				if (line.indexOf(";") > -1)
					line = line.substring(0, line.indexOf(";"));
				upperFrequency[v1][v2] = Double.parseDouble(line.trim());
				if (undirected)
					upperFrequency[v2][v1] = upperFrequency[v1][v2];
			}
			in.close();
		}

		public void readDemand(String demandFile) throws IOException {
			String line;
			BufferedReader in = new BufferedReader(new FileReader(demandFile));
			while ((line = in.readLine()) != null) {
				if (line.indexOf("#") > -1)
					line = line.substring(0, line.indexOf("#"));
				if (line.indexOf(";") == -1)
					continue;
				int v1 = Integer.parseInt(line.substring(0, line.indexOf(";")).trim());
				line = line.substring(line.indexOf(";") + 1);
				if (line.indexOf(";") == -1)
					continue;
				int v2 = Integer.parseInt(line.substring(0, line.indexOf(";")).trim());
				line = line.substring(line.indexOf(";") + 1);
				if (line.indexOf(";") > -1)
					line = line.substring(0, line.indexOf(";"));
				demand[v1 - 1][v2 - 1] = Double.parseDouble(line.trim());
			}
			in.close();
		}

		public void readLinePool(String lineFile, boolean undirected) throws IOException {
			// lines are saved in the form "-1-2-3-4-"
			// (format used to determine whether a shortest path is contained within
			// a line
			// by doing string search)
			String line;
			BufferedReader in = new BufferedReader(new FileReader(lineFile));
			Vector<String> linePoolTemp = new Vector<String>();
			Vector<String> output = new Vector<String>();
			int ll = -1;
			int ii = 0;
			Integer v = 0;
			StringBuffer current = new StringBuffer();
			StringBuffer currentoutput = new StringBuffer();
			Vector<TreeSet<Integer>> lineVerticesTemp = new Vector<TreeSet<Integer>>();
			TreeSet<Integer> lvTemp = new TreeSet<Integer>();
			while ((line = in.readLine()) != null) {
				if (line.indexOf("#") > -1)
					line = line.substring(0, line.indexOf("#"));
				if (line.indexOf(";") == -1)
					continue;
				int l = Integer.parseInt(line.substring(0, line.indexOf(";")).trim());
				line = line.substring(line.indexOf(";") + 1);
				if (line.indexOf(";") == -1)
					continue;
				int i = Integer.parseInt(line.substring(0, line.indexOf(";")).trim());
				line = line.substring(line.indexOf(";") + 1);
				if (line.indexOf(";") > -1)
					line = line.substring(0, line.indexOf(";"));
				int k = Integer.parseInt(line.trim());
				if (l < ll)
					System.exit(1);
				if (l == ll) {
					current.append("-" + k);
					if (undirected)
						current.insert(0, k + "-");
				}
				if (l > ll) {
					current.append("-");
					current.insert(0, "-");
					if (ll > 0) {
						linePoolTemp.add(current.toString());
						output.add(currentoutput.toString());
					}
					current = new StringBuffer();
					currentoutput = new StringBuffer();
					current.append(k);
					ll = l;
					ii = 0;
					lvTemp = new TreeSet<Integer>();
					lineVerticesTemp.add(lvTemp);
				}
				lvTemp.add(left.get(k));
				lvTemp.add(right.get(k));
				if (i <= ii)
					System.exit(2);
				ii = i;
				currentoutput.append(l + ";" + i + ";" + k + ";$" + newline);
			}
			current.append("-");
			current.insert(0, "-");
			if (ll > 0) {
				linePoolTemp.add(current.toString());
				output.add(currentoutput.toString());
			}
			in.close();
			linePool = linePoolTemp.toArray(new String[0]);
			outputLines = output.toArray(new String[0]);
			lineVertices = lineVerticesTemp.toArray(new TreeSet[0]);
		}

		public void readLineCosts(String lineCostFile) throws IOException {
			String line;
			BufferedReader in = new BufferedReader(new FileReader(lineCostFile));
			linecost = new double[linePool.length];
			while ((line = in.readLine()) != null) {
				if (line.indexOf("#") > -1)
					line = line.substring(0, line.indexOf("#"));
				if (line.indexOf(";") == -1)
					continue;
				int linenumber = Integer.parseInt(line.substring(0, line.indexOf(";")));
				line = line.substring(line.indexOf(";") + 1);
				if (line.indexOf(";") == -1)
					continue;
				line = line.substring(line.indexOf(";") + 1);
				double lcost = Double.parseDouble(line.trim());
				linecost[linenumber - 1] = lcost;
			}
		}
	}

	private static List<Integer> determinePossibleCommonDivisors(int configCommonDivisor) {
		List<Integer> possibleSystemFrequencies = new ArrayList<>(11);
		if(configCommonDivisor <= 0) {
			for(int value : new int[]{2, 3, 5, 7, 11, 13}) {
				possibleSystemFrequencies.add(value);
			}
		}
		else {
			possibleSystemFrequencies.add(configCommonDivisor);
		}
		return possibleSystemFrequencies;
	}

}
