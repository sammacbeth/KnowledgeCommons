package kc.prediction;

import java.util.List;

import kc.State;
import kc.Strategy;

public class GreedyPredictor extends ReinforcementPredictor {

	final double epsilon;

	public GreedyPredictor() {
		this(0, new WeightedAverage(0), 0.0);
	}

	public GreedyPredictor(double q0, double alpha, double epsilon) {
		this(q0, new WeightedAverage(alpha), epsilon);
	}

	public GreedyPredictor(double q0, Stepsize stepsize) {
		this(q0, stepsize, 1.0);
	}

	public GreedyPredictor(double q0, Stepsize stepsize, double epsilon) {
		super(q0, stepsize);
		this.epsilon = epsilon;
	}

	@Override
	synchronized public Strategy actionSelection(State state,
			List<Strategy> strategies) {
		Strategy chosen = null;
		if (rnd.nextDouble() > epsilon)
			chosen = greedyStrategy(state, strategies);
		if (chosen == null)
			chosen = randomStrategy(strategies);
		return chosen;
	}
}
