package kc.prediction;

import java.util.List;

import kc.Measured;
import kc.State;
import kc.Strategy;
import kc.games.PseudoStrategy;

public class PseudoPredictor implements Predictor {

	String name = "";
	int datapoints = 0;
	double efficiency = 1.0;

	public PseudoPredictor() {
		super();
	}

	public PseudoPredictor(String name) {
		super();
		this.name = name;
	}

	public PseudoPredictor(String name, double efficiency) {
		super();
		this.name = name;
		this.efficiency = efficiency;
	}

	@Override
	public void addTrainingData(Measured data) {
		datapoints++;
	}

	@Override
	public Strategy actionSelection(State state, List<Strategy> strategies) {
		return new PseudoStrategy(this);
	}

	public int getDatapoints() {
		return datapoints;
	}

	public double getEfficiency() {
		return efficiency;
	}

	@Override
	public String toString() {
		return "PseudoPredictor [" + name + ", " + datapoints + ", "
				+ efficiency + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PseudoPredictor other = (PseudoPredictor) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

}
