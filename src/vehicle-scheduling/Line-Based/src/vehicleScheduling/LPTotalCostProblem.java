package vehicleScheduling;

import vehicleScheduling.*;
import java.io.*;
import java.util.*;
import com.dashoptimization.*;

/**Class implementing the linear program (LP^4)
 * Sub class of Model
 */
public class LPTotalCostProblem extends Model{
    //Variables
    private XPRBvar[][][][] x;                  //Binaries, x_{ijkl} in (LP^4) 
    private XPRBvar[] y;                        //y_k in (LP^4)
    private XPRBctr objective;                  //objective function
    //constraints
    private XPRBctr[]  ctrLineCovered, ctrCeiling;	
    private XPRBctr[][] ctrInEqualsOut, ctrAtMostOneEdge;
    private XPRBctr[][][][] ctrAdjacent;
    
    int T=super.getT();
    double TOL = 0.001;                 //Tolerance value for output of solution
    final int MAXEDGESINROUTE = Integer.MAX_VALUE;      
    //MAXEDGESINROUTE can be set to a value if the range of l is to be reduced

 
    /**empty constructor
     */
    public LPTotalCostProblem(){
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

		//Initialize contraints 
        ctrLineCovered = new XPRBctr[numberOfLines];
        ctrInEqualsOut = new XPRBctr[numberOfLines][numberOfRoutes];
        ctrAtMostOneEdge = new XPRBctr[numberOfRoutes][edgesInRoute];
        ctrAdjacent = new 
            XPRBctr[numberOfLines][numberOfLines][numberOfRoutes][edgesInRoute];
        ctrCeiling = new XPRBctr[numberOfRoutes];
 
	    XPRBexpr le;
	    
	    //Initialize variables
	    x = new XPRBvar[numberOfLines][numberOfLines][numberOfRoutes]
	                                                            [edgesInRoute];    
	    y = new XPRBvar[numberOfRoutes];           	    

	    for(int k=0; k<numberOfRoutes; k++){
	        y[k]= p.newVar("y_"+(k+1),XPRB.PL,0.0,Double.MAX_VALUE);//continuous
	        for(int i=0; i<numberOfLines; i++){
	            for(int j=0; j<numberOfLines; j++)
	                for(int l=0; l<edgesInRoute; l++)
	                x[i][j][k][l]= p.newVar("x_"+(i+1)+"_"+(j+1)+"_"+(k+1)+
	                            "_"+(l+1),XPRB.PL,0.0,1.0);         //continuous
	        }
	    }

        //Set constraints OnePerLine and InEqualsOut
		for(int i=0; i<numberOfLines; i++){
		    ctrLineCovered[i] = p.newCtr("LineCovered_"+(i+1));
		    ctrLineCovered[i].setType(XPRB.E);            //Equality-constraint    
		    for(int k=0; k<numberOfRoutes; k++){
		        ctrInEqualsOut[i][k] = p.newCtr("InEqualsOut"+(i+1)+"_"+(k+1));
		        ctrInEqualsOut[i][k].setType(XPRB.E);     //Equality-constraint
		        for(int j=0; j<numberOfLines; j++){
		            for(int l=0; l<edgesInRoute; l++){
		                ctrLineCovered[i].addTerm(x[i][j][k][l]);
		                ctrInEqualsOut[i][k].addTerm(x[i][j][k][l]);
		                ctrInEqualsOut[i][k].addTerm(x[j][i][k][l],-1.0);
		            }
		        }
		        ctrInEqualsOut[i][k].setTerm(0.0);
		    }
		    ctrLineCovered[i].setTerm(1.0);
		}
		
		//Set constraints AtMostOneEdge and Adjacent and CeilY
		for(int k=0; k<numberOfRoutes; k++){
		    ctrCeiling[k] = p.newCtr("Ceiling"+(k+1));
		    ctrCeiling[k].setType(XPRB.L);                      //<=-constraint
		    ctrCeiling[k].addTerm(y[k], (-1)*T);
		    for(int l=0; l<edgesInRoute; l++){
		        ctrAtMostOneEdge[k][l] = p.newCtr("AtMostOneEdge"+(k+1)+"_"
		                                                                +(l+1));
		        ctrAtMostOneEdge[k][l].setType(XPRB.L);         //<=-constraint
		        for(int i=0; i<numberOfLines; i++){
		            for(int j=0; j<numberOfLines; j++){
		                ctrCeiling[k].addTerm(x[i][j][k][l], time[i][j]);
		                ctrAtMostOneEdge[k][l].addTerm(x[i][j][k][l]);
		                ctrAdjacent[i][j][k][l] = p.newCtr("Adjacent"+(i+1)+"_"
		                                            +(j+1)+"_"+(k+1)+"_"+(l+1));
		                ctrAdjacent[i][j][k][l].setType(XPRB.L);//<=-constraint
		                ctrAdjacent[i][j][k][l].addTerm(x[i][j][k][l]);
		                for(int i3=0; i3<numberOfLines; i3++){
		                    if(l<edgesInRoute-1){
		                        ctrAdjacent[i][j][k][l].addTerm(
		                                                x[j][i3][k][l+1],-1.0);
		                    }		                
		                    ctrAdjacent[i][j][k][l].addTerm(
		                                                x[j][i3][k][0],-1.0);
		                
		                }
		                ctrAdjacent[i][j][k][l].setTerm(0.0);
		                
		            }
		        }
		        ctrAtMostOneEdge[k][l].setTerm(1.0);
		    }
		    ctrCeiling[k].setTerm(0.0);
		}
		
		//Set objective
		objective = p.newCtr("objective");
        objective.setType(XPRB.N);          
		
		for(int k=0; k<numberOfRoutes;k++){
		    objective.addTerm(y[k],WEIGHTFACTOR); 
		    for(int i=0; i<numberOfLines; i++){
		        for(int j=0; j<numberOfLines; j++){
		            for(int l=0; l<edgesInRoute; l++){
		                 objective.addTerm(x[i][j][k][l],dist[i][j]); 
		            }    
		        }
	        } 
	    }
	    System.out.println("Problem modeled.");
	}
	

    /** Solving the LP
     * @param linegraph linegraph
     * @param WEIGHTFACTOR variable \alpha in the LPs   
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
		}  */ 
	    
	    //Set sense of the obj function to minimizing
	    p.setSense(XPRB.MINIM);    
	    p.lpOptimize();
    	
    	//Get status of the MIP    
	    int statmip= p.getMIPStat();     
	    
	                                           
	    if((statmip == XPRB.MIP_SOLUTION)       
	    //global search incomplete,although an integer solution has been found        
	            || (statmip == XPRB.MIP_OPTIMAL)){ 
	            //global search complete and integer solution has been found
	         //Integer solution has been found
	         System.out.println("Objective value" + p.getObjVal());
	         for(int k=0; k<numberOfRoutes; k++){
	             for(int i=0; i<numberOfLines; i++){
	                 for(int j=0; j<numberOfLines; j++){
	                     for(int l=0; l<edgesInRoute; l++){
	                        if(x[i][j][k][l].getSol()>TOL){
	                            System.out.println(x[i][j][k][l].getName() +":"
	                                                   +x[i][j][k][l].getSol());
	                        }
	                     } 
                     }  
                 } 
	         }	
	         for(int k=0; k<numberOfRoutes; k++)
                    System.out.println(y[k].getName() + ":" + y[k].getSol()); 
	    }
	}
	
	
	
}
