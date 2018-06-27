package vehicleScheduling;

import com.dashoptimization.XPRB;
import com.dashoptimization.XPRBctr;
import com.dashoptimization.XPRBexpr;
import com.dashoptimization.XPRBvar;

import java.util.ArrayList;

/**Class implementing the integer program (VS_T^3)
 * Sub class of Model
 */

public class TotalCostProblemAlt extends Model{
    //Variables
    private XPRBvar[][][] x;           //Binaries, x_{ijk} in (VS_T^3)
    private XPRBvar[] y,z;             //y_k and v_k in (VS_T^3)
    private XPRBvar[][] w;             //w_{kS} in (VS_T^3)  
    private XPRBctr objective;         //objective function
    //constraints
    private XPRBctr[]  ctrCeilY, ctrEdgesIn, ctrEdgesOut, ctrSetZ;	 
    private XPRBctr[][] ctrSetW, ctrAdjacent, ctrFlowInRoute;
    private int bigM;         
    
    int T=super.getT();
    double TOL = 0.001;                 //Tolerance value for output of solution
    final int MAXEDGESINROUTE = Integer.MAX_VALUE;       
    //MAXEDGESINROUTE can be set to a value if the range of l is to be reduced

 
    /**empty constructor
     */
    public TotalCostProblemAlt(){
    }
    
    /** Initializing/modeling the IP in the sub class
     * @param linegraph linegraph
     * @param WEIGHTFACTOR variable \alpha in the IPs   
     */
	@Override
	protected void modelProblem(LineGraph linegraph, double WEIGHTFACTOR){
		//Variables describing the line graph
	    int[] start, end, frequency;
	    double[] linetime;                 
  		int numberOfLines, numberOfEdges;
    	double[][] dist, time;

		//Transfer data from linegraph to local variables
		numberOfLines = linegraph.numberOfLines;
		numberOfEdges = linegraph.numberOfEdges; 
		int numberOfRoutes = linegraph.numberOfLines;
		int edgesInRoute = linegraph.numberOfLines;
		
		if(edgesInRoute > MAXEDGESINROUTE){
		    edgesInRoute = MAXEDGESINROUTE;
		}
		
		start = new int[numberOfLines];  
        end = new int[numberOfLines]; 
        frequency = new int[numberOfLines]; 
        linetime = new double[numberOfLines]; 
        dist = new double[numberOfLines][numberOfLines];
        time = new double[numberOfLines][numberOfLines]; 

		for(int i=0; i<numberOfLines; i++){
            start[i] = linegraph.start[i];
            end[i] = linegraph.end[i];
            frequency[i] = linegraph.frequency[i];
            linetime[i] = linegraph.linetime[i];
            for(int j=0; j<numberOfLines; j++){
                dist[i][j]= linegraph.dist[i][j];
                time[i][j]= linegraph.time[i][j];
            }
        }
        numberOfEdges= numberOfLines*numberOfLines;
        bigM = numberOfEdges*numberOfEdges;
        
        //Calculate PowerSet of {1,...,numberOfLines} without the empty set
        int[] lineSet = new int[numberOfLines];
        for (int i=0; i<numberOfLines; i++ ){
            lineSet[i]=i;
        }
        ArrayList<ArrayList<Integer>> powerSet = 
                                            new ArrayList<ArrayList<Integer>>();
        powerSet = calcPowerSet(lineSet);
        
        //number of sets contained in powerSet
        int powersetSize = powerSet.size();
        

		//Initialize contraints 
        ctrEdgesIn = new XPRBctr[numberOfLines];
        ctrFlowInRoute = new XPRBctr[numberOfLines][numberOfRoutes];
        ctrSetW = new XPRBctr[numberOfRoutes][powersetSize];
        ctrSetZ = new XPRBctr[numberOfRoutes];
        ctrAdjacent = new XPRBctr[numberOfRoutes][powersetSize];
        ctrCeilY = new XPRBctr[numberOfRoutes];
 
	    XPRBexpr le;
	    
	    //Initialize variables
	    x = new XPRBvar[numberOfLines][numberOfLines][numberOfRoutes];    
	    y = new XPRBvar[numberOfRoutes];
	    z = new XPRBvar[numberOfRoutes];  
	    w = new XPRBvar[numberOfRoutes][powersetSize];  
	    
        
	    for(int k=0; k<numberOfRoutes; k++){
	        y[k]= p.newVar("y_"+(k+1),XPRB.UI);               //general integer
	        z[k]= p.newVar("z_"+(k+1),XPRB.UI);  
	        for(int i=0; i<numberOfLines; i++){
	            for(int j=0; j<numberOfLines; j++)
	                x[i][j][k]= p.newVar("x_"+(i+1)+"_"+(j+1)+"_"+
	                                    (k+1),XPRB.BV);       //binary 
	                
	        }
	        
	        for(int S=0; S<powersetSize; S++){
	             w[k][S]= p.newVar("w_"+(k+1)+"_"+S,XPRB.BV);
	        }  
	    }
	    //Set EdgesIn/Out constraints
		for(int i=0; i<numberOfLines; i++){
		    ctrEdgesIn[i] = p.newCtr("EdgesIn_"+(i+1));
		    ctrEdgesIn[i].setType(XPRB.E);                //Equality-constraint
		    for(int k=0; k<numberOfRoutes; k++){
		        ctrFlowInRoute[i][k] = p.newCtr("FlowInRoute_"+(i+1)+"_"+(k+1));
		        ctrFlowInRoute[i][k].setType(XPRB.E);     //Equality-constraint
		        for(int j=0; j<numberOfLines; j++){
		                ctrEdgesIn[i].addTerm(x[i][j][k]);
		                ctrFlowInRoute[i][k].addTerm(x[i][j][k]);
		                ctrFlowInRoute[i][k].addTerm(x[j][i][k], (-1.0));
		        }
		        ctrFlowInRoute[i][k].setTerm(0.0);
		    }
		    ctrEdgesIn[i].setTerm(1.0);
		}
		
		//Set CeilY and SetZ constraint 
		for(int k=0; k<numberOfRoutes; k++){
		    ctrCeilY[k] = p.newCtr("CeilY"+(k+1));
		    ctrCeilY[k].setType(XPRB.L);                  //<=-constraint
		    ctrCeilY[k].addTerm(y[k], (-1)*T);
		    
		    ctrSetZ[k] = p.newCtr("SetZ"+(k+1));
		    ctrSetZ[k].setType(XPRB.E);                   //Equality-constraint
		    ctrSetZ[k].addTerm(z[k], (-1));
		    for(int i=0; i<numberOfLines; i++){
		        for(int j=0; j<numberOfLines; j++){
		            ctrCeilY[k].addTerm(x[i][j][k], time[i][j]);     
		            ctrSetZ[k].addTerm(x[i][j][k]);                 
		        }
		    }
		    ctrCeilY[k].setTerm(0.0);
		}
		
		//Set SetW and Adjacent constraint 
		for(int k=0; k<numberOfRoutes; k++){
		int counter = 0;
            for(ArrayList<Integer> a : powerSet){
            ctrAdjacent[k][counter] = p.newCtr("Adjacent_" + k + "_" + counter);
            ctrAdjacent[k][counter].setType(XPRB.L);      //<=-constraint
            ctrSetW[k][counter] = p.newCtr("SetW_" + k + "_" + counter);
            ctrSetW[k][counter].setType(XPRB.L);          //<=-constraint
                for(int i : a){
                    for(int j : a){
                        ctrAdjacent[k][counter].addTerm(x[i][j][k]);
                    }  
                }
                ctrAdjacent[k][counter].addTerm(w[k][counter],bigM);
                ctrAdjacent[k][counter].setTerm((a.size() + bigM - 1));
                
                ctrSetW[k][counter].addTerm(z[k],
                                            ((double)1/(double)numberOfLines));
                ctrSetW[k][counter].addTerm(w[k][counter],(-1));
                ctrSetW[k][counter].setTerm(
                                     ((double)a.size()/(double)numberOfLines));
            counter++;
            }
		
		}		
		
		//Set objective
		objective = p.newCtr("objective");
        objective.setType(XPRB.N);          
		
		for(int k=0; k<numberOfRoutes;k++){
		    objective.addTerm(y[k],WEIGHTFACTOR); 
		    for(int i=0; i<numberOfLines; i++){
		        for(int j=0; j<numberOfLines; j++){
		            objective.addTerm(x[i][j][k],dist[i][j]); 
		        }
	        } 
	    }
	    System.out.println("Problem modeled.");
	}
	
	
    /** Solving the IP
     * @param linegraph linegraph
     * @param WEIGHTFACTOR variable \alpha in the IPs   
     */
	@Override
	protected void solveProblem(LineGraph linegraph, double WEIGHTFACTOR){
        System.out.println("Solving Problem");
        
        //Transfering relevant data from line graph to local variables
	    int numberOfLines = linegraph.numberOfLines;
	    int numberOfRoutes = linegraph.numberOfLines;
	    int edgesInRoute = linegraph.numberOfLines;
	    
	    if(edgesInRoute > MAXEDGESINROUTE){
		    edgesInRoute = MAXEDGESINROUTE;
		}
	    
		p.setObj(objective); 
	    //export problem to file
	    /*try {
			p.exportProb(XPRB.LP, "VS.lp");
		} catch (IOException e) {
			System.err.println("Could not export vehicle scheduling problem");
			throw new RuntimeException(e);
		}   */
	    
	    //Set sense of the obj function to minimizing
	    p.setSense(XPRB.MINIM);          
	    p.mipOptimize();
	  
    	//Get status of the MIP    	  
	    int statmip= p.getMIPStat();     
	                                         
	    if((statmip == XPRB.MIP_SOLUTION)       
	    //global search incomplete,although an integer solution has been found        
	            || (statmip == XPRB.MIP_OPTIMAL)){ 
	            //global search complete and integer solution has been found
	         /*Integer solution has been found*/
	         System.out.println("Objective value" + p.getObjVal());
	         for(int k=0; k<numberOfRoutes; k++){
	             for(int i=0; i<numberOfLines; i++){
	                 for(int j=0; j<numberOfLines; j++){
	                     if(x[i][j][k].getSol()>TOL){
	                        System.out.println(x[i][j][k].getName() +":"+
	                                                    x[i][j][k].getSol());
	                     }    
                     }  
                 } 
	         }	
	         for(int k=0; k<numberOfRoutes; k++)
                System.out.println(y[k].getName() + ":" + y[k].getSol());
             for(int k=0; k<numberOfRoutes; k++)
                System.out.println(z[k].getName() + ":" + z[k].getSol()); 
             /*for(int k=0; k<numberOfRoutes; k++){
                for(int counter=0; counter<Math.pow(2,numberOfLines);counter++){
                    if(w[k][counter].getSol()>TOL){
                        System.out.println(w[k][counter].getName() + ":" +
                                                     w[k][counter].getSol());
                    }
                }
             }*/
                  
	    }
	}
/** Calculate the power set of a set S without the empty set
     * @param S Set of which the power set is to be calculated
     * @return Power set of S without the empty set
     */
    public static ArrayList<ArrayList<Integer>> calcPowerSet(int[] S) {
	    if (S == null)
	    	return null;
 
	    ArrayList<ArrayList<Integer>> result = 
	                                        new ArrayList<ArrayList<Integer>>();
        //for all items in S
    	for (int i = 0; i < S.length; i++) {
    		ArrayList<ArrayList<Integer>> temp = 
    		                                new ArrayList<ArrayList<Integer>>();
     
		    //get sets that are already in result
		    for (ArrayList<Integer> a : result) {
			    temp.add(new ArrayList<Integer>(a));
		    }
 
		    //add S[i] to existing sets
		    for (ArrayList<Integer> a : temp) {
			    a.add(S[i]);
		    }
 
		    //add S[i] only as a set
		    ArrayList<Integer> single = new ArrayList<Integer>();
		    single.add(S[i]);
		        temp.add(single);
 
		    result.addAll(temp);
	    } 
	    return result;
    }
	
}
