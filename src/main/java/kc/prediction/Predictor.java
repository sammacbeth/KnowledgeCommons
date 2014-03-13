package kc.prediction;

import java.util.List;
import java.util.Random;

import kc.Measured;
import kc.State;
import kc.Strategy;

public abstract class Predictor {

	Random rnd = new Random();
	double lastScore = 0;

	protected Predictor() {
		super();
	}

	public abstract void addTrainingData(Measured data);

	// reinforcement learning method
	public abstract Strategy actionSelection(State state,
			List<Strategy> strategies);

	public double getLastScore() {
		return lastScore;
	}

	protected Strategy randomStrategy(List<Strategy> strategies) {
		this.lastScore = 0;
		return strategies.get(rnd.nextInt(strategies.size()));
	}

}
