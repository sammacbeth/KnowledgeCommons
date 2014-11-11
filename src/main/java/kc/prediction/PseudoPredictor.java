package kc.prediction;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import kc.Measured;
import kc.State;
import kc.Strategy;
import kc.games.PseudoStrategy;
import kc.games.ShortKnowledgeGame;

public class PseudoPredictor implements Predictor {

	String name = "";
	int datapoints = 0;
	double efficiency = 1.0;

	LinkedList<AtomicInteger> history = new LinkedList<AtomicInteger>();
	int sum = 0;
	int offset = 0;

	public PseudoPredictor() {
		super();
	}

	public PseudoPredictor(String name) {
		super();
		this.name = name;
	}

	public PseudoPredictor(String name, int initial, double efficiency) {
		super();
		this.name = name;
		this.efficiency = efficiency;
		setT(ShortKnowledgeGame.dataLimit);
		for(AtomicInteger h : history) {
			h.set(initial);
		}
	}

	@Override
	public synchronized void addTrainingData(Measured data) {
		setT(data.getT());
		int ind = data.getT() - offset;
		history.get(ind).incrementAndGet();
		sum += 1;
		datapoints++;
	}

	public synchronized void setT(int t) {
		while (history.size() + offset <= t) {
			history.add(new AtomicInteger(0));
		}
		while (history.size() > ShortKnowledgeGame.dataLimit) {
			int n = history.remove(0).get();
			offset++;
			sum -= n;
		}
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
