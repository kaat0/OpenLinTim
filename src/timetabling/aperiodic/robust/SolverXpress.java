// Robust timetabling for uncertainty sets containing
// increases of minimal durations up to some multiplicative factor

import com.dashoptimization.*;
import java.util.LinkedHashMap;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.util.Date;
import java.util.PriorityQueue;

public class SolverXpress {

	public static boolean verbose = true;
	public static int offset = 0;

	public static double solveNominal(NonPeriodicEANetwork ean)
			throws SolverException {
		// nominal problem is a special case of the
		// strict robust problem: maximum delay of 0 %
		return solveStrictU1(ean, 0.0);
	}

	public static double solveStrictU1(NonPeriodicEANetwork ean, double s)
			throws SolverException {
		// strict robust problem implemented inside light robust problem
		// (due to many common parts, avoid duplicate source code)
		// as special case with delta < 0
		return solveLightU1(ean, s, 0.0, -1.0);

	}

	/**
	* Setting up the problem and solving it.
	*
	* @param ean instance of NonPeriodicEANetwork containing precedence decisions
	* @param s maximum multiplicative factor of delays
	* @param z nominal objective value
	* @param delta light robustness parameter, 0 means strict robustness
	*/
	public static double solveLightU1(NonPeriodicEANetwork ean, double s, double z,
			double delta) throws SolverException {

		boolean light = (delta >= 0);
		String name = light ? "light problem (U1, s=" + s + ", delta=" + delta + ")"
				: "strict problem (U1, s=" + s + ")";
		if (verbose) System.out.println("SolverXpress: Setting up " + name);
		XPRS.init();
		XPRB bcl = new XPRB();
		XPRBprob prob = bcl.newProb("NonPeriodic Timetabling: " + name);
		XPRBctr lightObj = prob.newCtr("light_objective");
		lightObj.setType(light ? XPRB.N : XPRB.G);
		if (!light) lightObj.setTerm(0.0);
		XPRBctr nomObj = prob.newCtr("nominal_objective");
		nomObj.setType(light ? XPRB.L : XPRB.N);
		if (light) nomObj.setTerm((1 + delta) * z);
		XPRBvar[] pi = new XPRBvar[ean.getEvents().size()+1];
		for (int i = 1; i < pi.length; i++)
			pi[i] = prob.newVar("pi_" + i, XPRB.PL, 0.0, 86400.0);
		double[] nomObjCoeff = new double[pi.length];
		for (NonPeriodicActivity a : ean.getActivities()) {
			if ((a instanceof NonPeriodicHeadwayActivity) &&
					(((NonPeriodicHeadwayActivity) a).getG() == 1))
				continue; // ignore inactive headways
			XPRBctr ctr = prob.newCtr("activity_" + a.getID());
			ctr.setType(XPRB.G);
			ctr.setTerm(a.getLowerBound() * (light || !a.getType().equals("drive") ? 1 : (1 + s)));
			ctr.setTerm(pi[a.getSource().getID()], -1.0);
			ctr.setTerm(pi[a.getTarget().getID()], 1.0);
			if (light && a.getType().equals("drive")) {
				XPRBvar gamma = prob.newVar("gamma_" + a.getID(), XPRB.PL, 0.0,
						XPRB.INFINITY);
				lightObj.setTerm(gamma, a.getWeight());
				ctr = prob.newCtr("activity_lightbuffer_" + a.getID());
				ctr.setType(XPRB.G);
				ctr.setTerm(a.getLowerBound() * (1 + s));
				ctr.setTerm(pi[a.getSource().getID()], -1.0);
				ctr.setTerm(pi[a.getTarget().getID()], 1.0);
				ctr.setTerm(gamma, 1.0);
			}
			nomObjCoeff[a.getSource().getID()] -= a.getWeight();
			nomObjCoeff[a.getTarget().getID()] += a.getWeight();
		}
		for (int i = 1; i < pi.length; i++)
			nomObj.setTerm(pi[i], nomObjCoeff[i]);
		prob.setObj(light ? lightObj : nomObj);

		if (verbose) System.out.println("SolverXpress: Solving " + name);
		prob.minim("");

		if (verbose) System.out.println("SolverXpress: Obtaining solution to " + name);
		for (NonPeriodicEvent e : ean.getEvents())
			e.setDispoTime((int) Math.round(pi[e.getID()].getSol()) + offset);
		if (prob.getLPStat() != XPRB.LP_OPTIMAL)
			throw new SolverException(name + " returned LP status " + prob.getLPStat());
		return prob.getObjVal();

	}

	public static double solveAdjustableU2(NonPeriodicEANetwork ean, double s, int k, boolean bufferOnLastArc)
			throws SolverException {
		String name = "adjustable problem (U2, s=" + s + ", k=" + k + ") with buffer on "
				+ (bufferOnLastArc ? "last" : "cheapest") + " arc per path";
		if (verbose) System.out.println("SolverXpress: Setting up " + name);
		XPRS.init();
		XPRB bcl = new XPRB();
		XPRBprob prob = bcl.newProb("NonPeriodic Timetabling: " + name);
		XPRBctr nomObj = prob.newCtr("nominal_objective");
		nomObj.setType(XPRB.N);
		if (verbose) System.out.println("SolverXpress: Constructing adjustably reduced counterpart");
		XPRBvar[] pi = new XPRBvar[ean.getEvents().size()+1];
		for (int i = 1; i < pi.length; i++)
			pi[i] = prob.newVar("pi_" + i, XPRB.PL, 0.0, 86400.0);
		double[] nomObjCoeff = new double[pi.length];
		for (NonPeriodicEvent e : ean.getEvents())
			if (!isTrivial(e))
				for (NonPeriodicActivity a : e.getOutgoingActivities()) {
					PriorityQueue<Integer> driveLowerBounds = new PriorityQueue<Integer>();
					int sumLowerBounds = 0;
					NonPeriodicEvent x = e;
					NonPeriodicActivity y = a;
					do {
						nomObjCoeff[y.getSource().getID()] -= y.getWeight();
						nomObjCoeff[y.getTarget().getID()] += y.getWeight();
						XPRBctr ctr = prob.newCtr("activity_" + y.getID());
						ctr.setType(XPRB.G);
						ctr.setTerm(y.getLowerBound());
						ctr.setTerm(pi[y.getSource().getID()], -1.0);
						ctr.setTerm(pi[y.getTarget().getID()], 1.0);
						if (y.getType().equals("drive"))
							driveLowerBounds.add(0 - y.getLowerBound());
						sumLowerBounds += y.getLowerBound();
						x = y.getTarget();
						if (isTrivial(x))
							y = x.getOutgoingActivities().getFirst();
					} while (isTrivial(x));
					// x is now the last node of the trivial path
					double sharedBuffer = 0.0;
					for (int i = 0; i < k; i++)
						if (!driveLowerBounds.isEmpty())
							sharedBuffer -= s * driveLowerBounds.poll();
					XPRBctr ctr = prob.newCtr("necessarybuffer_activity_" + a.getID());
					ctr.setType(XPRB.G);
					if (bufferOnLastArc) {
						sumLowerBounds = y.getLowerBound();
						if (sharedBuffer > 0)
							while (!y.getType().equals("drive")) {
								y = y.getSource().getIncomingActivities().getFirst();
								sumLowerBounds += y.getLowerBound();
							}
						ctr.setTerm(pi[y.getSource().getID()], -1.0);
					} else ctr.setTerm(pi[e.getID()], -1.0);
					ctr.setTerm(sumLowerBounds + Math.ceil(sharedBuffer));
					ctr.setTerm(pi[x.getID()], 1.0);
				}
		for (int i = 1; i < pi.length; i++)
			nomObj.setTerm(pi[i], nomObjCoeff[i]);
		prob.setObj(nomObj);

		if (verbose) System.out.println("SolverXpress: Solving " + name);
		prob.minim("");

		if (verbose) System.out.println("SolverXpress: Obtaining solution to " + name);
		for (NonPeriodicEvent e : ean.getEvents())
			e.setDispoTime((int) Math.round(pi[e.getID()].getSol()) + offset);
		if (prob.getLPStat() != XPRB.LP_OPTIMAL)
			throw new SolverException(name + " returned LP status " + prob.getLPStat());
		return prob.getObjVal();
	}

	private static boolean isTrivial(NonPeriodicEvent e) {
		return (e.getIncomingActivities().size() == 1 && e.getOutgoingActivities().size() == 1);
	}

	public static void main(String[] args) throws Exception {
		PrintWriter log = new PrintWriter(new FileWriter("Robust_Timetabling.log", true), true);
		Config config = new Config(new File("basis/Config.cnf"));
		String filename = config.getStringValue("default_timetable_file");
		String header = config.getStringValue("timetable_header_disposition");
		offset = config.getIntegerValue("DM_earliest_time");
		NonPeriodicEANetwork ean = IO.readNonPeriodicEANetwork(false, false);
		ean.resetDispositionDecisions();
		ean.setG();
		solveNominal(ean);
		IO.outputDispoTimetable(ean, filename, header);
		/*double z = solveAdjustableU2(ean, 0.5, 1, true);
		IO.outputDispoTimetable(ean, filename + ".adjustable_50_1_last");
		z = solveAdjustableU2(ean, 0.5, 1, false);
		IO.outputDispoTimetable(ean, filename + ".adjustable_50_1_cheapest");*/
		/*double z = solveNominal(ean);
		IO.outputDispoTimetable(ean, filename + ".nominal");
		log.println(new Date() + " - nominal: " + z);
		for (int s = 50; s <= 50; s += 10) {
			double z1 = solveStrictU1(ean, 0.01*s);
			log.println(new Date() + " - strict(s=" + s + "): " + z1);
			IO.outputDispoTimetable(ean, filename + ".strict_" + s);
			for (int delta = 0; delta < s; delta += s/10) {
				double z2 = solveLightU1(ean, 0.01*s, z, 0.01*delta);
				log.println(new Date() + " - light(s=" + 0.01*s + ",delta=" + 0.01*delta + "): " + z2);
				IO.outputDispoTimetable(ean, filename + ".light_" + s + "_" + delta);
			}
		}*/
	}

}
