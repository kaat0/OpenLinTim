import java.lang.Math;
import java.util.*;

public class EuclideanNorm extends Distance{
	
	public double calcDist(double x1, double x2, double y1, double y2){
		return Math.sqrt(Math.pow(x1-y1,2.)+Math.pow(x2-y2,2.));
	}
	
	/**
	 * Solves quadratic equation to determine the candidates on an edge.
	 */
	public LinkedList<Candidate> candidateOnEdge(DemandPoint demand_point, Edge edge, double radius){
		LinkedList<Candidate> candidate = new LinkedList<Candidate>();
		Candidate point;
		double p1=demand_point.getX_coordinate();
		double p2=demand_point.getY_coordinate();
		double s1=edge.getLeft_stop().getX_coordinate();
		double s2=edge.getLeft_stop().getY_coordinate();
		double t1=edge.getRight_stop().getX_coordinate();
		double t2=edge.getRight_stop().getY_coordinate();
		double denomenator=Math.pow(t1-s1, 2)+Math.pow(t2-s2, 2);
		
		if(denomenator < EPSILON)
			return null;
		double p_half= (t1-s1)*(p1-t1)+(t2-s2)*(p2-t2);
		p_half /=denomenator;
		double q= Math.pow(p1-t1,2)+Math.pow(p2-t2,2)-Math.pow(radius, 2);
		q /= denomenator;
		double discriminate= Math.pow(p_half,2)-q;
		if(discriminate <=0)
			return null;
		if(Math.abs(discriminate)<EPSILON){
			point=convexCombination(-p_half,edge);
			if(point != null)
				candidate.add(point);
		}
		else{
			point=convexCombination(-p_half+Math.sqrt(discriminate),edge);
			if(point != null)
				candidate.add(point);
			point=convexCombination(-p_half-Math.sqrt(discriminate),edge);
			if(point != null)
				candidate.add(point);
		}
		return candidate;
	}
	
}
