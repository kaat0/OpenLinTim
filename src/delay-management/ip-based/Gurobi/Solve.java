import java.util.*;
import gurobi.*;



public class Solve
{
	private Solve() {} // class only contains static methods



	public static void solve(NonPeriodicEANetwork Net, int M, boolean DEBUG,
	                         boolean VERBOSE, boolean timeLimit, double limit,
	                         boolean MAsUpperBound) throws Exception
	{
		LinkedHashSet<NonPeriodicEvent> E = Net.getEvents();
		LinkedHashSet<NonPeriodicActivity> A_nice = Net.getNiceActivities();
		LinkedHashSet<NonPeriodicChangingActivity> A_change = Net.getChangingActivities();
		LinkedHashSet<NonPeriodicHeadwayActivity> A_head = Net.getHeadwayActivities();

		// as the IDs might not be consecutively numbered (due to preprocessing
		// etc.), we use a HashMap to keep track of a new numbering scheme of
		// the events
		HashMap<NonPeriodicEvent, Integer> newEventIDs =
			new HashMap<NonPeriodicEvent, Integer>(E.size());

		if (VERBOSE)
			System.out.println("DM: setting up Gurobi model");

		GRBEnv env = new GRBEnv();

		if (timeLimit)
			env.set(GRB.DoubleParam.TimeLimit, limit);

		if (VERBOSE)
			env.set(GRB.IntParam.OutputFlag, 1);
		else
			env.set(GRB.IntParam.OutputFlag, 0);

		GRBModel model = new GRBModel(env);

		GRBVar[] z = null;
		GRBVar[] g = null;

		// add y variables
		GRBVar[] y = new GRBVar[E.size()];

		int i = 0;
		for (NonPeriodicEvent e: E)
		{
			int lowerBound = e.getSourceDelay();
			// for the upper bound, see Diss. Schachtebeck, Theorem 3.1
			// note that this only holds for an _optimal_ solution,
			// so when using a heuristic, we have to use an upper bound
			// of infinity (otherwise, we might get an infeasible model)
			double upperBound;
			if (MAsUpperBound)
				upperBound = M;
			else
				upperBound = GRB.INFINITY;
			y[i] = model.addVar(lowerBound, upperBound, e.getWeight(), GRB.CONTINUOUS, "y"+i);
			newEventIDs.put(e, i);
			i++;
		}
		model.update();

		// add constraints for nice activities
		for (NonPeriodicActivity a: A_nice)
		{
			int sID = newEventIDs.get(a.getSource());
			int tID = newEventIDs.get(a.getTarget());
			int lowerBound = a.getLowerBound() + a.getSourceDelay() + a.getSource().getTime() - a.getTarget().getTime();
			GRBLinExpr LHS = new GRBLinExpr();
			LHS.addTerm(1, y[tID]);
			LHS.addTerm(-1, y[sID]);
			model.addConstr(LHS, GRB.GREATER_EQUAL, lowerBound, "nice" + a.getID());
		}

		// add constraints for changing activities
		if (A_change.size() > 0)
		{
			HashMap<NonPeriodicActivity, Integer> frequencies = IO.readActivityFrequencies(Net.getActivities());
			int period = Net.getPeriod();
			z = new GRBVar[A_change.size()];
			i = 0;
			for (NonPeriodicChangingActivity a: A_change)
				z[i++] = model.addVar(0, 1, a.getWeight()*(period/frequencies.get(a)), GRB.BINARY, "z" + a.getID());
			model.update();
			i = 0;
			for (NonPeriodicChangingActivity a: A_change)
			{
				int sID = newEventIDs.get(a.getSource());
				int tID = newEventIDs.get(a.getTarget());
				int lowerBound = a.getLowerBound() + a.getSourceDelay() + a.getSource().getTime() - a.getTarget().getTime();
				GRBLinExpr LHS = new GRBLinExpr();
				LHS.addTerm(M, z[i]);
				LHS.addTerm(1, y[tID]);
				LHS.addTerm(-1, y[sID]);
				model.addConstr(LHS, GRB.GREATER_EQUAL, lowerBound, "change" + a.getID());
				i++;
			}
		}

		// add constraints for headway activities
		if (A_head.size() > 0)
		{
			g = new GRBVar[A_head.size()/2];
			for (int k=0; k<g.length; k++)
				g[k] = model.addVar(0, 1, 0, GRB.BINARY, "g" + k);
			model.update();

			i = 0;
			Iterator<NonPeriodicHeadwayActivity> hActivityIterator = A_head.iterator();
			while (hActivityIterator.hasNext())
			{
				NonPeriodicHeadwayActivity a1 = hActivityIterator.next();
				NonPeriodicHeadwayActivity a2 = hActivityIterator.next();
				NonPeriodicEvent source = a1.getSource();
				NonPeriodicEvent target = a1.getTarget();
				int sTime = source.getTime();
				int tTime = target.getTime();
				int sID = newEventIDs.get(source);
				int tID = newEventIDs.get(target);
				int lower1 = a1.getLowerBound() + sTime - tTime;
				int lower2 = a2.getLowerBound() + tTime - sTime;
				GRBLinExpr LHS = new GRBLinExpr();
				LHS.addTerm(M, g[i]);
				LHS.addTerm(1, y[tID]);
				LHS.addTerm(-1, y[sID]);
				model.addConstr(LHS, GRB.GREATER_EQUAL, lower1, "headway_" + sID + "_" + tID);
				LHS = new GRBLinExpr();
				LHS.addConstant(M);
				LHS.addTerm(-M, g[i]);
				LHS.addTerm(1, y[sID]);
				LHS.addTerm(-1, y[tID]);
				model.addConstr(LHS, GRB.GREATER_EQUAL, lower2, "headway_" + tID + "_" + sID);
				i++;
			}
		}

		model.update();

		// solve the problem
		if (VERBOSE)
			System.out.println("DM: solving ILP now...");
		model.optimize();

		int status = model.get(GRB.IntAttr.Status);
		if (status == GRB.TIME_LIMIT)
			System.err.println("\n**********************\nDM: time limit reached\n**********************\n");
		else if (status != GRB.OPTIMAL)
			throw new RuntimeException("DM: model status is " + status);

		// write solutions to the event-activity network
		if (VERBOSE)
			System.out.println("DM: evaluating solution...");

		i=0;
		for (NonPeriodicEvent e: E)
			e.setDispoTime(e.getTime() + (int) Math.round(y[i++].get(GRB.DoubleAttr.X)));

		i=0;
		for (NonPeriodicChangingActivity a: A_change)
			a.setZ((int) Math.round(z[i++].get(GRB.DoubleAttr.X)));

		i=0;
		Iterator<NonPeriodicHeadwayActivity> hActivityIterator = A_head.iterator();
		while (hActivityIterator.hasNext())
		{
			int sol = (int) Math.round(g[i].get(GRB.DoubleAttr.X));
			hActivityIterator.next().setG(sol);
			hActivityIterator.next().setG(1-sol);
			i++;
		}

		// The problem with the obtained solution is that events with
		// weight 0 might have a really large delay. Hence we now fix
		// wait/depart and priority decision and minimize the sum of
		// all delays; as the order of the events is fixed, each event
		// will then take place as early as possible.
		if (VERBOSE)
			System.out.println("DM: setting up ILP with fixed boolean variables again...");
		env.set(GRB.IntParam.OutputFlag, 0);
		model = new GRBModel(env);

		// add x variables
		y = new GRBVar[E.size()];
		i = 0;
		for (NonPeriodicEvent e: E)
		{
			int lowerBound = e.getSourceDelay();
			double upperBound = GRB.INFINITY;
			// as all lower bounds are integral, in each time-minimal solution,
			// y is integral automatically
			y[i++] = model.addVar(lowerBound, upperBound, 1, GRB.CONTINUOUS, "y_" + i);
		}
		model.update();

		// add constraints for nice activities
		for (NonPeriodicActivity a: A_nice)
		{
			int sID = newEventIDs.get(a.getSource());
			int tID = newEventIDs.get(a.getTarget());
			int lowerBound = a.getLowerBound() + a.getSourceDelay() + a.getSource().getTime() - a.getTarget().getTime();
			GRBLinExpr LHS = new GRBLinExpr();
			LHS.addTerm(1, y[tID]);
			LHS.addTerm(-1, y[sID]);
			model.addConstr(LHS, GRB.GREATER_EQUAL, lowerBound, "nice_" + a.getID());
		}

		// add constraints for changing activities
		for (NonPeriodicChangingActivity a: A_change)
		{
			if (a.getZ() == 0)
			{
				int sID = newEventIDs.get(a.getSource());
				int tID = newEventIDs.get(a.getTarget());
				int lowerBound = a.getLowerBound() + a.getSource().getTime() - a.getTarget().getTime();
				GRBLinExpr LHS = new GRBLinExpr();
				LHS.addTerm(1, y[tID]);
				LHS.addTerm(-1, y[sID]);
				model.addConstr(LHS, GRB.GREATER_EQUAL, lowerBound, "nice_" + a.getID());
			}
		}

		for (NonPeriodicHeadwayActivity a: A_head)
		{
			if (a.getG() == 0)
			{
				int sID = newEventIDs.get(a.getSource());
				int tID = newEventIDs.get(a.getTarget());
				int lowerBound = a.getLowerBound() + a.getSource().getTime() - a.getTarget().getTime();
				GRBLinExpr LHS = new GRBLinExpr();
				LHS.addTerm(1, y[tID]);
				LHS.addTerm(-1, y[sID]);
				model.addConstr(LHS, GRB.GREATER_EQUAL, lowerBound, "nice_" + a.getID());
			}
		}
		model.update();

		// solve the problem
		if (VERBOSE)
			System.out.println("DM: solving ILP again...");
		model.optimize();
		status = model.get(GRB.IntAttr.Status);
		if (status != GRB.OPTIMAL)
			throw new RuntimeException("DM: model status is " + status);
		
		// write solutions to the event-activity network
		if (VERBOSE)
			System.out.println("DM: evaluating solution...");
		i=0;
		for (NonPeriodicEvent e: E)
			e.setDispoTime(e.getTime() + (int) Math.round(y[i++].get(GRB.DoubleAttr.X)));
	}

	public static void solveDM1(NonPeriodicEANetwork Net, LinkedList<Path> passenger_paths, boolean DEBUG,
	boolean VERBOSE, boolean timeLimit, int limit, int M) {
		throw new RuntimeException("There is currently no Gurobi implementation for DM1, please choose another model!");
	}

	public static void fixPassengerHeadways(NonPeriodicEANetwork Net, LinkedHashSet<NonPeriodicActivity> A_nice, 
	LinkedHashSet<NonPeriodicHeadwayActivity> A_head, 
	LinkedList<Path> passenger_paths, boolean DEBUG, boolean VERBOSE){
		throw new RuntimeException("There is currently no Gurobi implementation for DM1, please choose another model!");
	}
}
