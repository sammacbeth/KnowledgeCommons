package kc.prediction;

import java.util.List;
import java.util.Random;

import kc.Measured;
import kc.State;
import kc.Strategy;

public abstract class Predictor {

	Random rnd = new Random();

	protected Predictor() {
		super();
	}

	public abstract void addTrainingData(Measured data);

	// reinforcement learning method
	public abstract Strategy actionSelection(State state,
			List<Strategy> strategies);

	protected Strategy randomStrategy(List<Strategy> strategies) {
		return strategies.get(rnd.nextInt(strategies.size()));
	}

}
