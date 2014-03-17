package kc.prediction;

import java.util.List;

import kc.Measured;
import kc.State;
import kc.Strategy;

public interface Predictor {

	public void addTrainingData(Measured data);

	public Strategy actionSelection(State state, List<Strategy> strategies);

}