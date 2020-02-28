import edu.asu.emit.algorithm.graph.abstraction.BaseVertex;

import java.util.Objects;

/**
 */
public class VertexImplementation implements BaseVertex, Comparable<VertexImplementation> {

	private final int id;
	private double weight;

	public VertexImplementation(int id) {
		this(id, 0);
	}

	public VertexImplementation(int id, double weight) {
		this.id = id;
		this.weight = weight;
	}

	@Override
	public int getId() {
		return id;
	}

	@Override
	public double getWeight() {
		return weight;
	}

	@Override
	public void setWeight(double weight) {
		this.weight = weight;
	}

	@Override
	public int compareTo(VertexImplementation otherVertex) {
		return (int) Math.signum(this.weight - otherVertex.weight);
	}

	@Override
	public String toString() {
		return "VertexImplementation{" +
				"id=" + id +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		VertexImplementation that = (VertexImplementation) o;
		return id == that.id &&
				Double.compare(that.weight, weight) == 0;
	}

	@Override
	public int hashCode() {

		return Objects.hash(id);
	}
}
