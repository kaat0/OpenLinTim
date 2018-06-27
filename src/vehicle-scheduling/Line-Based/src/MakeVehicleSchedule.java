import vehicleScheduling.*;

/**Main class calculating a vehicle schedule.
 */
public class MakeVehicleSchedule{   

    /** Method to manually set data for a line graph
     *  @return A line graph with the data entered
     */
    public static LineGraph setLineGraphData1(){
        int numberOfLines=4;
        int numberOfEdges=numberOfLines*numberOfLines;
		int[] start = new int[4];
		int[] end = new int[4];    
		double[] linetime = new double[4];
		double[][] dist = new double[4][4];
		double[][] time = new double[4][4];
        
        start[0]= 1;
        end[0]  = 2;
        start[1]= 2;
        end[1]  = 1;
        start[2]= 3;
        end[2]  = 4;
        start[3]= 4;
        end[3]  = 3;
        
        linetime[0]= 30;
        linetime[1]= 30;
        linetime[2]= 30;
        linetime[3]= 30;
        
        /*Kosten der Knoten=Länge der Linien*/
        dist[0][0]= 30;
        dist[0][1]=  100;
        dist[0][2]= 100;
        dist[0][3]= 400;
        dist[1][0]=  100;
        dist[1][1]= 30;
        dist[1][2]= 400;
        dist[1][3]= 100;
        dist[2][0]= 100;
        dist[2][1]= 400;
        dist[2][2]= 30;
        dist[2][3]=  1000;
        dist[3][0]= 400;
        dist[3][1]= 100;
        dist[3][2]=  1000;
        dist[3][3]= 300;
        
        
        for(int i=0; i<numberOfLines; i++){
            for(int j=0; j<numberOfLines; j++){  
                time[i][j]=dist[i][j]+linetime[0];      
            }
        }
        
        LineGraph linegraph1 = new LineGraph(start, end, linetime, 
                            numberOfLines, dist, time);
            
       return linegraph1;     
    
    }

    
   /**Main method calculating a vehicle schedule with the parameters handed over
    * @param args command-line arguments   
    * required parameters
    * 1 Dataset to calculate vehicle schedule on     
    * 2 Model to be used. 2: (VS_W^2), 3: (VS_T^3), 4: (VS_T^4)    
    * 3 Value of alpha 
    */
    public static void main(String[] args) throws Exception{
        String dataset = args[0]; 
        String modelType = args[1];
        double WEIGHTFACTOR = Double.parseDouble(args[2]);
        
        boolean regardFrequencies = true;
        boolean solveLPrelax=false;
        
        Model vschedule;
        if(solveLPrelax==false){
        if((modelType.equals("WasteCost") || modelType.equals("EmptyRides"))
                                                    || modelType.equals("2")){
            vschedule = new WasteCostProblem();
        }
        else{
            if((modelType.equals("alt") || modelType.equals("alternative")) 
                                                    || modelType.equals("3")){
            vschedule = new TotalCostProblemAlt();
            }
            else{
                vschedule = new TotalCostProblem();
            }
        }
        }else{        
        if((modelType.equals("alt") || modelType.equals("alternative")) 
                                                || modelType.equals("3")){
            vschedule = new LPTotalCostProblemAlt();
        }
        else{
            vschedule = new LPTotalCostProblem();
        }
        }
           
        
        LineConceptConverter lineConcept = new LineConceptConverter();
    	LineGraph lineGraph = lineConcept.convertLineConceptToLineGraph(dataset, 
    	                                                    regardFrequencies);
    	
    	int numberOfRoutes = lineGraph.numberOfLines;
        vschedule.makeVehicleSchedule(lineGraph, WEIGHTFACTOR);    
    }
    
    
}
