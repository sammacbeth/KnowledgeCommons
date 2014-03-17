package kc.prediction;

import java.util.List;

import kc.Measured;
import kc.State;
import kc.Strategy;

public class RandomPredictor extends AbstractPredictor {

	public RandomPredictor() {
		super();
	}

	@Override
	public void addTrainingData(Measured data) {
	}

	@Override
	public Strategy actionSelection(State state, List<Strategy> strategies) {
		return randomStrategy(strategies);
	}

	@Override
	public double getLastScore() {
		return 0;
	}

}
