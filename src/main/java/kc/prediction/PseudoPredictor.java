package kc.prediction;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import kc.Measured;
import kc.State;
import kc.Strategy;
import kc.games.PseudoStrategy;

public class PseudoPredictor implements Predictor {

	String name = "";
	int datapoints = 0;
	double efficiency = 1.0;

	LinkedList<AtomicInteger> history = new LinkedList<AtomicInteger>();
	int sum = 0;
	int offset = 0;
	final int limit = 20;

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
		int ind = data.getT() - offset;
		try {
			history.get(ind).incrementAndGet();
		} catch(IndexOutOfBoundsException e) {
			while(history.size() + offset < data.getT()) {
				history.add(new AtomicInteger(0));
			}
			history.add(new AtomicInteger(1));
			while(history.size() > limit) {
				int n = history.remove(0).get();
				offset++;
				sum -= n;
			}
		}
		sum += 1;
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

	public List<AtomicInteger> getHistory() {
		return Collections.unmodifiableList(history);
	}

}
