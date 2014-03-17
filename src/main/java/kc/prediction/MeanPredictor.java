package kc.prediction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import kc.Measured;
import kc.State;
import kc.Strategy;

import org.apache.commons.math.stat.descriptive.SummaryStatistics;

public class MeanPredictor extends AbstractPredictor {

	Set<Measured> data = new HashSet<Measured>();

	Map<Integer, SummaryStatistics> cache = new HashMap<Integer, SummaryStatistics>();

	public MeanPredictor() {
		super();
	}

	@Override
	public void addTrainingData(Measured m) {
		int s = m.getStrategy();
		SummaryStatistics stat = cache.get(s);
		if (stat == null) {
			stat = new SummaryStatistics();
			cache.put(s, stat);
		}
		stat.addValue(m.getPayoff());
	}

	public double predict(State input, int strategyId) {
		if (cache.containsKey(strategyId)) {
			return cache.get(strategyId).getMean();
		}
		return -1;
	}

	@Override
	public Strategy actionSelection(State state, List<Strategy> strategies) {
		Strategy chosen = null;
		double bestCost = 0;
		for (Strategy s : strategies) {
			double pred = this.predict(state, s.getId());
			if (chosen == null || pred > bestCost) {
				chosen = s;
				bestCost = pred;
			}
		}
		return chosen;
	}

}
