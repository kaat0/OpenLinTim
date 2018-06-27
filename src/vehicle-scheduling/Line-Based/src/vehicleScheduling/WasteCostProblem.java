package vehicleScheduling;

import com.dashoptimization.XPRB;
import com.dashoptimization.XPRBctr;
import com.dashoptimization.XPRBexpr;
import com.dashoptimization.XPRBvar;

import java.io.BufferedWriter;
import java.io.FileWriter;

/**Class implementing the integer program (VS_W^2)
 * Sub class of Model
 */

public class WasteCostProblem extends Model{
    //Variables used for Xpress
    private XPRBvar[][] x;                        //Binaries, x_{ij} in (VS_W^2)
    private XPRBctr objective;                              //objective function
    private XPRBctr[] constraintLinesInOut, constraintLinesFre;	 //constraints
 
    int T=super.getT();
    
    /**empty constructor
     */
    public WasteCostProblem(){
    }
	
    /** Initializing/modeling the IP in the sub class
     * @param linegraph linegraph
     * @param WEIGHTFACTOR variable \alpha in the IPs   
     */
	@Override
	protected void modelProblem(LineGraph linegraph, double WEIGHTFACTOR){
		//Variables describing the line graph
	    int[] start, end;
	    double[] linetime;                 
  		int numberOfLines, numberOfEdges;
    	double[][] dist, time;

		//Transfer data from linegraph to local variables
		numberOfLines = linegraph.numberOfLines;
		numberOfEdges = linegraph.numberOfEdges; 
		
		start = new int[numberOfLines];  
        end = new int[numberOfLines]; 
        linetime = new double[numberOfLines]; 
        dist = new double[numberOfLines][numberOfLines];
        time = new double[numberOfLines][numberOfLines]; 

		for(int i=0; i<numberOfLines; i++){
            start[i]= linegraph.start[i];
            end[i]= linegraph.end[i];
            linetime[i]= linegraph.linetime[i];
            for(int j=0; j<numberOfLines; j++){
                dist[i][j]= linegraph.dist[i][j];
                time[i][j]= linegraph.time[i][j];
            }
        }
        numberOfEdges= numberOfLines*numberOfLines;

		//Initialize contraints 
        constraintLinesInOut = new XPRBctr[numberOfLines];
        constraintLinesFre = new XPRBctr[numberOfLines];
 
	    XPRBexpr le;

	    
	    //Initialize variables
	    x = new XPRBvar[numberOfLines][numberOfLines];    
	    for(int i=0; i<numberOfLines; i++){
	        for(int j=0; j<numberOfLines; j++){
	            x[i][j]= p.newVar("x_"+(i+1)+"_"+(j+1),XPRB.BV); //binary
	        }
	    }	
		
		
		//Set constrains
		for(int i=0; i<numberOfLines; i++){
		    constraintLinesInOut[i] = p.newCtr("in_i"+(i+1));
		    constraintLinesInOut[i].setType(XPRB.E);      //Equality-constraint
		    constraintLinesFre[i] = p.newCtr("out_i"+(i+1));
		    constraintLinesFre[i].setType(XPRB.E);        //Equality-constraint  
		    
		    for(int j=0; j<numberOfLines; j++){
		        constraintLinesInOut[i].addTerm(x[i][j]);
		        constraintLinesInOut[i].addTerm(x[j][i], -1.0);
		            
		        constraintLinesFre[i].addTerm(x[i][j]);
		        
		    }		    
		    constraintLinesInOut[i].setTerm(0);
		    constraintLinesFre[i].setTerm(1);  
		}
		
		//Set objective
		objective = p.newCtr("objective");
        objective.setType(XPRB.N);          
		
		for(int i=0; i<numberOfLines;i++){
		    for(int j=0; j<numberOfLines;j++){
		        objective.addTerm(x[i][j],dist[i][j]); 	 
		    }
		}   
	}
    /** Solving the IP and calculating upper bound z_u^2
     * @param linegraph linegraph
     * @param WEIGHTFACTOR variable \alpha in the IPs   
     */

	@Override
	protected void solveProblem(LineGraph linegraph, double WEIGHTFACTOR)
	                                                      throws Exception{
	    
	    //Transfering relevant data from the line graph                                                   
	    int numberOfLines = linegraph.numberOfLines;
		double totalTime=0;             // the time needed for all lines
		int minimalVehicleNumber=0;     // lower bound to number of vehicles
		
	    p.setObj(objective); 
	    
	    //export problem to file	    
	    /*try {
			p.exportProb(XPRB.LP, "VS.lp");
		} catch (IOException e) {
			System.err.println("Could not export vehicle scheduling problem");
			throw new RuntimeException(e);
		}  */ 
	    
	    p.setSense(XPRB.MINIM);           //Set sense of the obj to minimizing
	    p.mipOptimize();
	    
	    int statmip= p.getMIPStat();      //Get status of the MIP 
	    
	                                            
	    if(( statmip== XPRB.MIP_SOLUTION)       
	    //global search incomplete,although an integer solution has been found        
	            || (statmip == XPRB.MIP_OPTIMAL)
	            //global search complete and integer solution has been found
	            ){    
	        //Integer solution has been found
	        System.out.println("Objective value: " + p.getObjVal());
	         
	        for(int i=0; i<numberOfLines; i++){
	            for(int j=0; j<numberOfLines; j++){
	                if(x[i][j].getSol()!=0){  
	                    System.out.println(x[i][j].getName() + ": " + 
	                                                          x[i][j].getSol());
                    }  
                } 
	        }	   
	    }
	    
	    int numberOfVehicles = calculateNumberOfVehicles(linegraph);
	    
	    // calculate the time needed to conduct all lines (gives the lower bound 
	    // if each line has transfer cost of 0 to at least one other line)
	    for(int i=0;i<numberOfLines;i++){
	        totalTime = totalTime + linegraph.linetime[i];
	    } 
	    // determine the minimal number of vehicles to conduct all lines
	    minimalVehicleNumber = (int) Math.ceil(totalTime / T);
	    
	    // calculate bounds
	    double upperBound =  numberOfVehicles*WEIGHTFACTOR + p.getObjVal();
	    double lowerBound = minimalVehicleNumber*WEIGHTFACTOR + p.getObjVal();
	    
	    // output
	    System.out.println(numberOfVehicles + "vehicles are needed");

	    System.out.println("Objective Value with NumberOfVehicles included = " +
	                        upperBound);
	    System.out.println("at least "+ minimalVehicleNumber + 
	                            " vehicles are needed to operate line graph");

	    System.out.println("Objective Value with minimal VehicleNumber "+
	                                   "included = " + lowerBound);
	                
	    System.out.println("Absolut gap: " + (upperBound - lowerBound));
	}
	
    /** Calculates the number of vehicles needed to operate the vehicle schedule
     *   
     * This provides an upper bound
     * @param linegraph
     * @return number of vehicles needed to operate the vehicle schedule 
     *  obtained by solving the IP
     */	
	private int calculateNumberOfVehicles(LineGraph linegraph) throws Exception{
		int totalNumberOfVehicles = 0;
	    int numberOfLines = linegraph.numberOfLines;
	    int[] numberOfVehiclesInRoute; 
	    double[] timeInRoute;
        int[][] sol;
        int[] solI, solJ;       //initialize mit -1!!
        int routeCounter = 0;
        int lineInRouteCounter = 0;
        int counter = 0;
        
        FileWriter fw = new FileWriter("VehicleSchedule_WasteCost.txt");
        BufferedWriter bw = new BufferedWriter(fw);
        
        numberOfVehiclesInRoute = new int[numberOfLines]; 
        timeInRoute = new double[numberOfLines]; 
        sol = new int[numberOfLines][numberOfLines];
        solI = new int[numberOfLines];
        solJ = new int[numberOfLines];	    
	    
	    for(int i=0; i<numberOfLines; i++){
	        for(int j=0; j<numberOfLines; j++){
	            long solution = Math.round(x[i][j].getSol());
	            sol[i][j] = (int) solution;  
	            if(sol[i][j]==1){
	                solI[counter] = i;
	                solJ[counter] = j;
	                counter++;
	            }
	        } 
	    }
        
        bw.write("#Route, line no; line");
        bw.newLine();
        timeInRoute[routeCounter] = 0;
        int firstLineOfRoute=-1;
        
        
        for(int iter=0; iter<counter; iter++ ){
            if(solI[iter]!=-1){
                int m = iter;
                firstLineOfRoute = solI[m];
                lineInRouteCounter++;
	            bw.write((routeCounter+1) + "; " + lineInRouteCounter + 
	                                        "; " + linegraph.lineIDs[solI[m]]);
	            bw.newLine();                                   	            
	            timeInRoute[routeCounter] = timeInRoute[routeCounter] + 
	                             linegraph.time[solI[m]][solJ[m]];
	            solI[m]=-1;
	            boolean doneWithRoute = false;
	            
	            while(doneWithRoute==false){   

	               for(int n=0; n<counter; n++){
	                    if(solI[n]==solJ[m]){
	                        if(solJ[n]==firstLineOfRoute){  
	                            m=n;
	                            lineInRouteCounter++;
	                            bw.write((routeCounter+1) + "; " + 
	                                          lineInRouteCounter + "; " +
	                                          linegraph.lineIDs[solI[m]]);
	                            bw.newLine();
	                            timeInRoute[routeCounter] = 
	                                        timeInRoute[routeCounter] + 
	                                        linegraph.linetime[solI[m]] + 
	                                        linegraph.time[solI[m]][solJ[m]];                    
	                            solI[m]=-1;
                                
                                doneWithRoute = true;
                                routeCounter++;
                                timeInRoute[routeCounter] = 0;
                                lineInRouteCounter = 0;
                                firstLineOfRoute = -1;
                                break;
	                        }
	                        else{
	                            m=n;
	                            lineInRouteCounter++;
	                            bw.write((routeCounter+1) + "; " + 
	                                             lineInRouteCounter + "; " +
	                                             linegraph.lineIDs[solI[m]]);
	                            bw.newLine();
	                            timeInRoute[routeCounter] = 
	                                        timeInRoute[routeCounter] + 
	                                        linegraph.linetime[solI[m]] + 
	                                        linegraph.time[solI[m]][solJ[m]];
	                            solI[m]=-1;
	                            
	                            
	                            break;
	                        }
	                    }
	                }
	            }
            }
	    }
	    
	    for(int k=0; k<numberOfLines; k++){
	        if(timeInRoute[k]!=0){
	            numberOfVehiclesInRoute[k] = (int)Math.ceil(timeInRoute[k] / T);
	            totalNumberOfVehicles = totalNumberOfVehicles + 
	                                        numberOfVehiclesInRoute[k] ;
	            bw.write("#Route " + k + " needs " +numberOfVehiclesInRoute[k]+ 
	                          " vehicles and its time is "+timeInRoute[k] +".");
	            bw.newLine();                
	        }
	    }
	    
	    bw.write("#In total, "+ totalNumberOfVehicles +" vehicles are needed.");
	    
	    bw.close();
	    
	    return totalNumberOfVehicles;	                  
	}
	
	
}
