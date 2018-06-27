
public class Passenger {
	private int weight;
	private DemandPoint origin_demand_point;
	private DemandPoint destination_demand_point;
	private Stop origin_stop;
	private Stop destination_stop;
	private double traveling_time;
	private double prob_lower_bound;
	private double prob_upper_bound;

//Constructor-------------------------------------------------------------------------
	public Passenger(DemandPoint origin, DemandPoint destination){
		origin_demand_point=origin;
		destination_demand_point=destination;
		this.weight=0;
	}
	
//Getter-------------------------------------------------------------------------------
	public double getWeight(){
		return weight;
	}
	
	public DemandPoint getOriginDemandPoint(){
		return origin_demand_point;
	}
	
	public DemandPoint getDestinationDemandPoint(){
		return destination_demand_point;
	}
	
	public Stop getOriginStop(){
		return origin_stop;
	}
	
	public Stop getDestinaitonStop(){
		return destination_stop;
	}
	
	public double getTravelingTime(){
		return traveling_time;
	}
	
	public boolean isEmpty(){
		return weight==0;
	}
//Setter---------------------------------------------------------------------------------
	public void setOriginStop(Stop origin_stop){
		this.origin_stop=origin_stop;
	}
	
	public void setDestinaitonStop(Stop destination_stop){
		this.destination_stop = destination_stop;
	}
	
	public void setTravelingTime(double traveling_time){
		this.traveling_time=traveling_time;
	}
	
	public void setProbLowerBound(double prob_lower_bound){
		this.prob_lower_bound=prob_lower_bound;
	}
	
	public void setProbUpperBound(double prob_upper_bound){
		this.prob_upper_bound=prob_upper_bound;
	}
	
	public void setWeight(double[] probability){
		for(int i=0; i<probability.length; i++){
			if(probability[i] >= prob_lower_bound && probability[i] < prob_upper_bound){
				weight++;
			}
		}
	}
}
