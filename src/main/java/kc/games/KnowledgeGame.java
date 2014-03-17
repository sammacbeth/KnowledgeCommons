package kc.games;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import uk.ac.imperial.presage2.core.environment.EnvironmentServiceProvider;
import uk.ac.imperial.presage2.core.environment.EnvironmentSharedStateAccess;
import kc.Game;
import kc.State;
import kc.Strategy;
import kc.prediction.PseudoPredictor;

@Singleton
public class KnowledgeGame extends Game {

	@Inject
	public KnowledgeGame(EnvironmentSharedStateAccess sharedState,
			EnvironmentServiceProvider serviceProvider) {
		super(sharedState, serviceProvider);
	}

	@Override
	protected double getReward(UUID actor, Strategy s) {
		if (s instanceof PseudoStrategy) {
			PseudoPredictor p = ((PseudoStrategy) s).source;
			return fn(p.getDatapoints(), p.getEfficiency());
		}
		throw new RuntimeException("Invalid strategy type: " + s);
	}

	@Override
	public State getState(UUID actor) {
		return State.NONE;
	}

	@Override
	public List<Strategy> getStrategies() {
		return Collections.emptyList();
	}

	double fn(int n, double e) {
		return (1.0 - (1.0 / (n/50.0 + 1.0))) * e;
	}

}
