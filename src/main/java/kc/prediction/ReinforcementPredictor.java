package kc.prediction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kc.Measured;
import kc.State;
import kc.Strategy;

import org.apache.commons.lang3.tuple.Pair;
import org.uncommons.maths.Maths;

public abstract class ReinforcementPredictor extends Predictor {

	final double q0;
	final Stepsize stepsize;

	Map<Pair<State, Integer>, QValue> q = new HashMap<Pair<State, Integer>, QValue>();

	// Set<Measured> alreadyAdded = new HashSet<Measured>();

	public ReinforcementPredictor(double q0, Stepsize stepsize) {
		super();
		this.q0 = q0;
		this.stepsize = stepsize;
	}

	@Override
	public void addTrainingData(Measured m) {
		Pair<State, Integer> key = Pair.of(m.getState(), m.getStrategy());
		QValue qVal = q.get(key);
		if (qVal == null) {
			qVal = new QValue();
			q.put(key, qVal);
		}
		qVal.update(m.getPayoff());
	}

	public double getQ(State state, int strategy) {
		QValue qsa = q.get(Pair.of(state, strategy));
		if (qsa != null)
			return qsa.q;
		else
			return q0;
	}

	protected Strategy greedyStrategy(State state, List<Strategy> strategies) {
		Strategy chosen = null;
		double bestPayoff = 0;
		for (Strategy s : strategies) {
			double qsa = getQ(state, s.getId());
			if (qsa > bestPayoff) {
				chosen = s;
				bestPayoff = qsa;
			}
		}
		return chosen;
	}

	interface Stepsize {
		double alpha(int k);
	}

	static public class Average implements Stepsize {

		@Override
		public double alpha(int k) {
			return 1.0 / k;
		}

	}

	static public class WeightedAverage implements Stepsize {
		final double alpha;

		public WeightedAverage(double alpha) {
			super();
			this.alpha = Maths.restrictRange(alpha, 0.0, 1.0);
		}

		@Override
		public double alpha(int k) {
			return alpha;
		}

	}

	class QValue {
		double q;
		int k = 1;

		QValue() {
			super();
			this.q = q0;
		}

		void update(double r) {
			q += stepsize.alpha(++k) * (r - q);
		}

		@Override
		public String toString() {
			return "" + q + "";
		}

	}

}
