package vehicleScheduling;

import com.dashoptimization.*;

/**Class for setting up the integer program in Xpress and solving it
 * Super class for the different variants of the vehicle scheduling IPs
 */
public abstract class Model{   
    private int T = 60;         //duration of one period
    protected XPRB bcl;
    protected XPRBprob p;           
    
    /** empty constructor*/
    public Model(){
    }
    
    
    /** Initializing/modeling the IP in the sub class
     * @param linegraph linegraph
     * @param WEIGHTFACTOR variable \alpha in the IPs   
     */
    protected abstract void modelProblem(LineGraph linegraph,
                                                    double WEIGHTFACTOR);
    
     /** Solving the IP
     * @param linegraph linegraph
     * @param WEIGHTFACTOR variable \alpha in the IPs   
     */
    protected abstract void solveProblem(LineGraph linegraph,  
                                        double WEIGHTFACTOR) throws Exception;
                                                        
    /** Read out T for the subclasses 
     * @return value of the period T
     */
    protected int getT(){
        return T;
    }
    
    /** Set up IP and solve it
     * @param linegraph linegraph
     * @param WEIGHTFACTOR variable \alpha in the IPs  
     */
    public void makeVehicleSchedule(LineGraph linegraph, double WEIGHTFACTOR)
                                                        throws Exception {
		XPRS.init();
		bcl = new XPRB();                           //Initialize BCL
	    p = bcl.newProb("VS_MinNumberOfVeh");       //Create a problem
        p.getXPRSprob().setDblControl(XPRS.FEASTOL, 0.005);
        p.getXPRSprob().setIntControl(XPRS.CPUTIME, 1);     
        //1: CPU-time, 2: process time, 0: wall on the clock-time

    	modelProblem(linegraph, WEIGHTFACTOR);      //Initialize IP
    	solveProblem(linegraph, WEIGHTFACTOR);      //Solve IP, print solution 
	
	    p.finalize();                               //Delete the problem
	    p = null;        

	}
}
