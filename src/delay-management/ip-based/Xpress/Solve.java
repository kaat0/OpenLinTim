import java.io.IOException;
import java.util.*;
import com.dashoptimization.*;



public class Solve
{
	private Solve() {} // class only contains static methods

	// solve the delay management problem by involving the Xpress solver
	@SuppressWarnings("deprecation")
	public static void solve(NonPeriodicEANetwork Net, int M, boolean DEBUG,
	                         boolean VERBOSE, boolean timeLimit, int limit,
	                         boolean MAsUpperBound) throws IOException
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
			System.out.println("DM: setting up Xpress model");

		XPRS.init();
		XPRB bcl = new XPRB();
		XPRBprob p = bcl.newProb("DM");

		if (timeLimit)
			p.getXPRSprob().setIntControl(XPRS.MAXTIME, limit);
		if (DEBUG)
			p.setMsgLevel(4);
		else
		{
			if (VERBOSE)
				p.setMsgLevel(3);
			else
				p.setMsgLevel(2);
		}

		XPRBvar[] z = null;
		XPRBvar[] g = null;

		// add x variables
		XPRBvar[] x = new XPRBvar[E.size()];

		int i = 0;
		for (NonPeriodicEvent e: E)
		{
			int lowerBound = e.getTime() + e.getSourceDelay();
			// for the upper bound, see Diss. Schachtebeck, Theorem 3.1
			// note that this only holds for an _optimal_ solution,
			// so when using a heuristic, we have to use an upper bound
			// of infinity (otherwise, we might get an infeasible model)
			double upperBound;
			if (MAsUpperBound)
				upperBound = e.getTime() + M;
			else
				upperBound = XPRB.INFINITY;

			// as all lower bounds are integral, in each time-minimal solution,
			// x is integral automatically
			x[i] = p.newVar("x_" + i, XPRB.PL, lowerBound, upperBound);
			// workaround for a bug in Xpress (decision variables that do
			// not appear in the objective or any other constraint might
			// become smaller than the lower bound given when calling
			// newVar)
			p.newCtr(x[i].gEql(lowerBound));

			newEventIDs.put(e, i);
			i++;
		}

		// add constraints for nice activities
		for (NonPeriodicActivity a: A_nice)
		{
			int sID = newEventIDs.get(a.getSource());
			int tID = newEventIDs.get(a.getTarget());
			int lowerBound = a.getLowerBound() + a.getSourceDelay();
			p.newCtr(x[tID].add(x[sID].neg()).gEql(lowerBound));
		}

		// add constraints for changing activities
		if (A_change.size() > 0)
		{
			z = new XPRBvar[A_change.size()];
			i = 0;
			for (NonPeriodicChangingActivity a: A_change)
			{
				int sID = newEventIDs.get(a.getSource());
				int tID = newEventIDs.get(a.getTarget());
				int lowerBound = a.getLowerBound() + a.getSourceDelay();
				z[i] = p.newVar("z_" + i, XPRB.BV);
				p.newCtr((z[i].mul(M)).add(x[tID]).add(x[sID].neg()).gEql(lowerBound));
				i++;
			}
		}

		// add constraints for headway activities
		if (A_head.size() > 0)
		{
			g = new XPRBvar[A_head.size()/2];
			i = 0;
			Iterator<NonPeriodicHeadwayActivity> hActivityIterator = A_head.iterator();
			while (hActivityIterator.hasNext())
			{
				NonPeriodicHeadwayActivity a1 = hActivityIterator.next();
				NonPeriodicHeadwayActivity a2 = hActivityIterator.next();
				int sID = newEventIDs.get(a1.getSource());
				int tID = newEventIDs.get(a1.getTarget());
				int lower1 = a1.getLowerBound();
				int lower2 = a2.getLowerBound();
				g[i] = p.newVar("g_" + sID + "_" + tID, XPRB.BV);
				p.newCtr((g[i].mul(M)).add(x[tID]).add(x[sID].neg()).gEql(lower1));
				p.newCtr((((g[i].neg()).add(1)).mul(M)).add(x[sID]).add(x[tID].neg()).gEql(lower2));
				i++;
			}
		}

		// add objective
		XPRBexpr objective = new XPRBexpr();
		i = 0;
		for (NonPeriodicEvent e: E)
		{
			int Pi = e.getTime();
			double weight = e.getWeight();
			objective.add((x[i].add(-Pi)).mul(weight));
			i++;
		}
		HashMap<NonPeriodicActivity, Integer> frequencies = IO.readActivityFrequencies(Net.getActivities());
		int T = Net.getPeriod();
		System.out.println("Period: " + T);
		
		i = 0;
		for (NonPeriodicChangingActivity a: A_change)
		{
			objective.add(z[i].mul(a.getWeight()*(T/frequencies.get(a))));
			i++;
		}
	
	p.setObj(objective);

		// solve the problem
		try {
            p.exportProb(XPRB.LP, "dm.lp");
        } catch (Exception e) {}
		if (VERBOSE)
			System.out.println("DM: solving ILP now...");
		p.minim("g");
		if (p.getMIPStat() != 6)
		{
			if (timeLimit && p.getMIPStat() < 5)
				System.err.println("\n**********************\nDM: time limit reached\n**********************\n");
			else
				throw new RuntimeException("DM: MIP Status is " + p.getMIPStat());
		}

		// write solutions to the event-activity network
		if (VERBOSE)
			System.out.println("DM: evaluating solution...");

		i=0;
		for (NonPeriodicEvent e: E)
			e.setDispoTime((int) Math.round(x[i++].getSol()));

		i=0;
		for (NonPeriodicChangingActivity a: A_change)
			a.setZ((int) Math.round(z[i++].getSol()));
		

		i=0;
		Iterator<NonPeriodicHeadwayActivity> hActivityIterator = A_head.iterator();
		while (hActivityIterator.hasNext())
		{
			int sol = (int) Math.round(g[i].getSol());
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

		p.finalize();
		p = bcl.newProb("DM");
		p.setMsgLevel(2);
		x = new XPRBvar[E.size()];
		i = 0;
		for (NonPeriodicEvent e: E)
		{
			int lowerBound = e.getTime() + e.getSourceDelay();
			double upperBound = XPRB.INFINITY;
			// as all lower bounds are integral, in each time-minimal solution,
			// x is integral automatically
			x[i] = p.newVar("x_" + i, XPRB.PL, lowerBound, upperBound);
			// workaround for a bug in Xpress (decision variables that do
			// not appear in the objective or any other constraint might
			// become smaller than the lower bound given when calling
			// newVar)
			p.newCtr(x[i].gEql(lowerBound));

			i++;
		}
		// add constraints for nice activities
		for (NonPeriodicActivity a: A_nice)
		{
			int sID = newEventIDs.get(a.getSource());
			int tID = newEventIDs.get(a.getTarget());
			int lowerBound = a.getLowerBound() + a.getSourceDelay();
			p.newCtr(x[tID].add(x[sID].neg()).gEql(lowerBound));
		}

		// add constraints for changing activities
		for (NonPeriodicChangingActivity a: A_change)
		{
			if (a.getZ() == 0)
			{
				int sID = newEventIDs.get(a.getSource());
				int tID = newEventIDs.get(a.getTarget());
				int lowerBound = a.getLowerBound();
				p.newCtr(x[tID].add(x[sID].neg()).gEql(lowerBound));
			}
		}

		for (NonPeriodicHeadwayActivity a: A_head)
		{
			if (a.getG() == 0)
			{
				int sID = newEventIDs.get(a.getSource());
				int tID = newEventIDs.get(a.getTarget());
				int lowerBound = a.getLowerBound();
				p.newCtr(x[tID].add(x[sID].neg()).gEql(lowerBound));
			}
		}

		// add objective
		objective = new XPRBexpr();
		i = 0;
		for (NonPeriodicEvent e: E)
		{
			int Pi = e.getTime();
			objective.add(x[i].add(-Pi));
			i++;
		}
		p.setObj(objective);

		// solve the problem
		if (VERBOSE)
			System.out.println("DM: solving ILP again...");
			
		p.minim("g");
		if (p.getMIPStat() != 6)
		{
			if (timeLimit && p.getMIPStat() < 5)
				System.err.println("\n**********************\nDM: time limit reached\n**********************\n");
			else
				throw new RuntimeException("DM: MIP Status is " + p.getMIPStat());
		}

		// write solutions to the event-activity network
		if (VERBOSE)
			System.out.println("DM: evaluating solution...");
		i=0;
		for (NonPeriodicEvent e: E)
			e.setDispoTime((int) Math.round(x[i++].getSol()));
	}
	
	/**
	 * Solve the delay management problem of Net with the optimization method DM1. 
	 * @param Net the EAN to work with
	 * @param passenger_paths a list of the passenger paths in the EAN
	 * @param DEBUG parameter to control the output
	 * @param VERBOSE parameter to control the output
	 * @param timeLimit whether or not a time limit for the solver should be set
	 * @param limit the time limit for the solver, see timeLimit
	 * @param M an upper bound for the delay of the events
	 * @throws IOException for the in- and output of xpress
	 */
	@SuppressWarnings("deprecation")
	public static void solveDM1(NonPeriodicEANetwork Net, LinkedList<Path> passenger_paths, boolean DEBUG,
            boolean VERBOSE, boolean timeLimit, int limit, int M) throws IOException{
		LinkedHashSet<NonPeriodicEvent> E = Net.getEvents();
		LinkedHashSet<NonPeriodicActivity> A_nice = Net.getNiceActivities();
		LinkedHashSet<NonPeriodicChangingActivity> A_change = Net.getChangingActivities();
		LinkedHashSet<NonPeriodicHeadwayActivity> A_head = Net.getHeadwayActivities();
		
		// as the IDs might not be consecutively numbered (due to preprocessing
		// etc.), we use a HashMap to keep track of a new numbering scheme of
		// the events
		HashMap<NonPeriodicEvent, Integer> newEventIDs =
					new HashMap<NonPeriodicEvent, Integer>(E.size());
		HashMap<NonPeriodicActivity, Integer> newActivityIDs = new HashMap<NonPeriodicActivity, Integer>(A_nice.size()+A_change.size());
		
		if (VERBOSE)
			System.out.println("DM: setting up Xpress model");
		
		XPRS.init();
		XPRB bcl = new XPRB();
		XPRBprob pr = bcl.newProb("DM");
		
		if(timeLimit){
			pr.getXPRSprob().setIntControl(XPRS.MAXTIME,limit);
		}
		System.err.println("Time limit: "+pr.getXPRSprob().getIntControl(XPRS.MAXTIME));
		if(DEBUG){
			pr.setMsgLevel(4);
		}
		else{
			if(VERBOSE){
				pr.setMsgLevel(3);
			}
			else{
				pr.setMsgLevel(2);
			}
		}
		
		//Adding variables. For each event e with index i, y[i] is the delay in this event. For each passenger path p 
		//with index i, z[i] is the boolean variable which indicates whether all changes on p are maintained. 
		//q[p] is a variable for the linearization of the problem, q[p]=y[lp]*(1-z[p]), where lp is the index of the last
		//event on the path with index p. This means q[p]==0 if z[p]=1, i.e. there is a missed connection of the path. 
		//Otherwise it is the delay of the last event in the path.
		//g are the g variable of the headway activities. If g is zero, the first of the two corresponding 
		//headway activities is fulfilled
		XPRBvar[] z = new XPRBvar[passenger_paths.size()];
		XPRBvar[] y = new XPRBvar[E.size()];
		XPRBvar[] q = new XPRBvar[passenger_paths.size()];	
		XPRBvar[] g = new XPRBvar[A_head.size()/2];
		
		if(VERBOSE)
			System.out.println("Setting constraints...");
		
		double upperBound;
		int lowerBound;
		int i=0;
		for(NonPeriodicEvent e:E){
			lowerBound = e.getSourceDelay();
			upperBound = XPRB.INFINITY;
			y[i] = pr.newVar("y_"+i ,XPRB.PL, lowerBound, upperBound);
			pr.newCtr(y[i].gEql(lowerBound));
			newEventIDs.put(e, i);
			i++;
		}
		
		if(VERBOSE){
			System.out.println("\tSetting constraints for nice activities...");
		}
		int slack;
		int sID;
		int tID;
		//add constraints for nice activities
		for(NonPeriodicActivity a:A_nice){
			sID = newEventIDs.get(a.getSource());
			tID = newEventIDs.get(a.getTarget());
			slack = a.getTarget().getTime()-a.getSource().getTime()-a.getLowerBound()-a.getSourceDelay();
			pr.newCtr(y[sID].add(y[tID].neg()).lEql(slack));
			newActivityIDs.put(a, a.getID());
		}
		
		//Iterate over the change activities to store them in the HashMap for the Activity IDs.
		
		for(NonPeriodicActivity a:A_change){
			newActivityIDs.put(a, a.getID());
		}
		
		if(VERBOSE){
			System.out.println("\tSetting constraints for passenger paths...");
		}
		//add constraints for paths of the passengers
		i=0;
		for(Path path:passenger_paths){
			z[i] = pr.newVar("z_"+i ,XPRB.BV);
			q[i] = pr.newVar("q_"+i ,XPRB.UI, 0, XPRB.INFINITY);
			pr.newCtr(q[i].gEql(0));
			tID=newEventIDs.get(path.getTarget());
			pr.newCtr(z[i].neg().mul(M).add(q[i].neg()).add(y[tID]).lEql(0));
			//adding constraint for each change activity on the path
			for(NonPeriodicChangingActivity change:path.getChanges()){
				sID=newEventIDs.get(change.getSource());
				tID = newEventIDs.get(change.getTarget());
				slack = change.getTarget().getTime()-change.getSource().getTime()-change.getLowerBound()-change.getSourceDelay();
				pr.newCtr(z[i].neg().mul(M).add(y[sID]).add(y[tID].neg()).lEql(slack));
			}
			i++;
		}
		
		if(VERBOSE){
			System.out.println("\tSetting constraints for headways...");
		}
		// add constraints for headway activities
		int slack1;
		int slack2;
		if (A_head.size() > 0)
		{
			i = 0;
			Iterator<NonPeriodicHeadwayActivity> hActivityIterator = A_head.iterator();
			while (hActivityIterator.hasNext())
			{
				NonPeriodicHeadwayActivity a1 = hActivityIterator.next();
				NonPeriodicHeadwayActivity a2 = hActivityIterator.next();
				sID = newEventIDs.get(a1.getSource());
				tID = newEventIDs.get(a1.getTarget());
				slack1 = a1.getTarget().getTime()-a1.getSource().getTime()-a1.getLowerBound()-a1.getSourceDelay();
				slack2 = a2.getTarget().getTime()-a2.getSource().getTime()-a2.getLowerBound()-a2.getSourceDelay();
				g[i] = pr.newVar("g_" + sID + "_" + tID, XPRB.BV);
				//g[i]=0 <=> the first headway, which is sID->tID, is fulfilled => For g[i]=0 the first inequality should hold, for g[i]=1 the second
				pr.newCtr((g[i].mul(M).neg()).add(y[sID]).add(y[tID].neg()).lEql(slack1));
				pr.newCtr((((g[i].neg()).add(1)).mul(M).neg()).add(y[tID]).add(y[sID].neg()).lEql(slack2));
				i++;
			}
		}
		
		if(VERBOSE){
			System.out.println("Adding objective...");
		}
		//add objective
		XPRBexpr objective = new XPRBexpr();
		i=0;
		int T=Net.getPeriod();
		for(Path path:passenger_paths){
			int weight = path.getWeight();
			objective.add(q[i].add(z[i].mul(T)).mul(weight));
			i++;
		}
		
		pr.setObj(objective);
		
		//solve the problem
		try{
			pr.exportProb(XPRB.LP, "dm.lp");
		} catch(Exception e){}
		if(VERBOSE){
			System.out.println("DM: solving ILP now...");
		}
		pr.minim("g");
		if(pr.getMIPStat() != 6){
			if(timeLimit && pr.getMIPStat() <5){
				System.err.println("\n**********************\nDM: time limit reached\n**********************\n");
			}
			else{
				throw new RuntimeException("DM: MIP Status is " + pr.getMIPStat());
			}
		}
		
		// write solutions to the event-activity network
		if (VERBOSE){
			System.out.println("DM: evaluating solution...");
		}
		
		for(NonPeriodicEvent e:E){
			e.setDispoTime(e.getTime()+(int)Math.round(y[newEventIDs.get(e)].getSol()));
		}
		
		for(NonPeriodicChangingActivity change:A_change){
			if (change.getTarget().getDispoTime() - change.getSource().getDispoTime() < change.getLowerBound()){
				change.setZ(1);
			}
			else{
				change.setZ(0);
			}
		}
		
		i=0;
		Iterator<NonPeriodicHeadwayActivity> hActivityIterator = A_head.iterator();
		while (hActivityIterator.hasNext())
		{
			int sol = (int) Math.round(g[i].getSol());
			hActivityIterator.next().setG(sol);
			hActivityIterator.next().setG(1-sol);
			i++;
		}
		
		// The problem with the obtained solution is that events with
		// weight 0 might have a really large delay. Hence we now fix
		// wait/depart and priority decision and minimize the sum of
		// all delays; as the order of the events is fixed, each event
		// will then take place as early as possible.
		if (VERBOSE){
			System.out.println("DM: setting up ILP with fixed boolean variables again...");
		}
		
		pr.finalize();
		pr = bcl.newProb("DM");
		pr.setMsgLevel(2);
		//x[e] is the disposed time of event e
		XPRBvar[] x = new XPRBvar[E.size()];
		i=0;
		for(NonPeriodicEvent e:E){
			lowerBound = e.getTime()+e.getSourceDelay();
			upperBound = XPRB.INFINITY;
			// as all lower bounds are integral, in each time-minimal solution,
			// x is integral automatically
			x[i] = pr.newVar("x_" + i, XPRB.PL, lowerBound, upperBound);
			// workaround for a bug in Xpress (decision variables that do
			// not appear in the objective or any other constraint might
			// become smaller than the lower bound given when calling
			// newVar)
			pr.newCtr(x[i].gEql(lowerBound));
			i++;
		}
		// add constraints for nice activities
		for (NonPeriodicActivity a: A_nice)
		{
			sID = newEventIDs.get(a.getSource());
			tID = newEventIDs.get(a.getTarget());
			lowerBound = a.getLowerBound() + a.getSourceDelay();
			pr.newCtr(x[tID].add(x[sID].neg()).gEql(lowerBound));
		}

		// add constraints for changing activities
		for (NonPeriodicChangingActivity a: A_change)
		{
			if (a.getZ() == 0)
			{
				sID = newEventIDs.get(a.getSource());
				tID = newEventIDs.get(a.getTarget());
				lowerBound = a.getLowerBound() + a.getSourceDelay();
				pr.newCtr(x[tID].add(x[sID].neg()).gEql(lowerBound));
			}
		}
		
		//add constraints for headway activities
		for (NonPeriodicHeadwayActivity a: A_head)
		{
			if (a.getG() == 0)
			{
				sID = newEventIDs.get(a.getSource());
				tID = newEventIDs.get(a.getTarget());
				lowerBound = a.getLowerBound() + a.getSourceDelay();
				pr.newCtr(x[tID].add(x[sID].neg()).gEql(lowerBound));
			}
		}
		
		//add objective
		objective = new XPRBexpr();
		i = 0;
		for (NonPeriodicEvent e: E){
			int Pi = e.getTime();
			objective.add(x[i].add(-Pi));
			i++;
		}
		pr.setObj(objective);

		// solve the problem
		if (VERBOSE){
			System.out.println("DM: solving ILP again...");
		}
			
		pr.minim("g");
		if (pr.getMIPStat() != 6)
		{
			if (timeLimit && pr.getMIPStat() < 5)
				System.err.println("\n**********************\nDM: time limit reached\n**********************\n");
			else
				throw new RuntimeException("DM: MIP Status is " + pr.getMIPStat());
		}

		// write solutions to the event-activity network
		if (VERBOSE){
			System.out.println("DM: evaluating solution...");
		}
		i=0;
		for (NonPeriodicEvent e: E){
			e.setDispoTime((int) Math.round(x[i++].getSol()));
		}
	}
	
	/**
	 * Uses an IP to solve the problem of finding the paths with the most summed weight with uncontradicted headways. Changes A_nice and A_head respectively.
	 * @param Net the EAN to work with
	 * @param A_nice the nice activities. Fixed headways will be added
	 * @param A_head the headway activities. Fixed headways and their counterpart will be removed
	 * @param passenger_paths a list of the paths of the passengers in the EAN Net
	 * @param DEBUG a parameter to control the output
	 * @param VERBOSE a paramter to control the output
	 */
	@SuppressWarnings("deprecation")
	public static void fixPassengerHeadways(NonPeriodicEANetwork Net, LinkedHashSet<NonPeriodicActivity> A_nice, 
																					LinkedHashSet<NonPeriodicHeadwayActivity> A_head, 
																					LinkedList<Path> passenger_paths, boolean DEBUG, boolean VERBOSE){
		
		HashMap<NonPeriodicHeadwayActivity, Integer> newActivityIDs = new HashMap<NonPeriodicHeadwayActivity, Integer>(A_head.size());
		HashMap<Path, Integer> pathIDs = new HashMap<Path, Integer>(passenger_paths.size());
		
		if (VERBOSE)
			System.out.println("HWFIX: setting up Xpress model");
		
		XPRS.init();
		XPRB bcl = new XPRB();
		XPRBprob pr = bcl.newProb("HWFIX");

		if(DEBUG){
			pr.setMsgLevel(4);
		}
		else{
			if(VERBOSE){
				pr.setMsgLevel(3);
			}
			else{
				pr.setMsgLevel(2);
			}
		}
			
		XPRBvar[] g = new XPRBvar[A_head.size()];
		XPRBvar[] p = new XPRBvar[passenger_paths.size()];
		
		if(VERBOSE){
			System.err.println("Setting up variables...");
		}
		
		Iterator<NonPeriodicHeadwayActivity> it = A_head.iterator();
		int i=0;
		int sID1,tID1, sID2, tID2;
		NonPeriodicHeadwayActivity a1,a2;
		while(it.hasNext()){
			a1=it.next();
			a2=it.next();
			sID1=a1.getSource().getID();
			tID1=a1.getTarget().getID();
			sID2=a2.getSource().getID();
			tID2=a2.getTarget().getID();
			g[i] = pr.newVar("g_" + sID1 + "_" + tID1, XPRB.BV);
			g[i+A_head.size()/2] = pr.newVar("g_" + sID2 + "_" + tID2, XPRB.BV);
			newActivityIDs.put(a1, i);
			newActivityIDs.put(a2, i+A_head.size()/2);
			i++;
		}
		
		int j=0;
		for(Path path : passenger_paths){
			p[j] = pr.newVar("p_"+j, XPRB.BV);
			pathIDs.put(path, j);
			j++;
		}
		
		if(VERBOSE){
			System.out.println("Setting constraints...");
		}
		
		LinkedList<NonPeriodicHeadwayActivity> headways;
		XPRBexpr new_left_side;
		for(Path path : passenger_paths){
			headways = Net.getHeadwaysOnTrip(path.getSource(), path.getTarget());
			new_left_side = new XPRBexpr();
			for(NonPeriodicHeadwayActivity headway:headways){
				new_left_side = new_left_side.add(g[newActivityIDs.get(headway)]);
			}
			pr.newCtr(new_left_side.lEql(p[pathIDs.get(path)].mul(A_head.size()/2)));
		}
		
		for(j=0;j<A_head.size()/2;j++){
			pr.newCtr(g[j].eql(g[j+A_head.size()/2].neg().add(1)));
		}
		
		if(VERBOSE){
			System.out.println("Adding objective...");
		}
		//add objective
		XPRBexpr objective = new XPRBexpr();
		j=0;
		for(Path path:passenger_paths){
			int weight = path.getWeight();
			objective.add(p[j].neg().add(1).mul(weight));
			j++;
		}
		
		pr.setObj(objective);
		
		//solve the problem
		try{
			pr.exportProb(XPRB.LP, "HWFIX.lp");
		} catch(Exception e){}
		if(VERBOSE){
			System.out.println("HWFIX: solving ILP now...");
		}
		pr.maxim("g");
		if(pr.getMIPStat() != 6){
			throw new RuntimeException("DM: MIP Status is " + pr.getMIPStat());
		}
		
		// write solutions to the event-activity network
		if (VERBOSE){
			System.out.println("HWFIX: evaluating solution...");
		}
		
		for(Path path: passenger_paths){
			if(Math.round(p[pathIDs.get(path)].getSol())==0){
				for(NonPeriodicHeadwayActivity headway:Net.getHeadwaysOnTrip(path.getSource(), path.getTarget())){
					headway.setG(0);
					headway.getCorrespodingHeadway().setG(1);
					A_nice.add(headway);
					A_head.remove(headway);
					A_head.remove(headway.getCorrespodingHeadway());
				}
			}
		}
		
		
	
	}
}
