package kc.games;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import kc.Game;
import kc.State;
import kc.Strategy;
import kc.prediction.PseudoPredictor;
import uk.ac.imperial.presage2.core.environment.EnvironmentServiceProvider;
import uk.ac.imperial.presage2.core.environment.EnvironmentSharedStateAccess;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ShortKnowledgeGame extends Game {

	int maxData = 10;
	int dataLimit = 20;
	double harmonicFactor = 3.6;
	
	@Inject
	public ShortKnowledgeGame(EnvironmentSharedStateAccess sharedState,
			EnvironmentServiceProvider serviceProvider) {
		super(sharedState, serviceProvider);
	}
	
	@Override
	protected double getReward(UUID actor, Strategy s) {
		if (s instanceof PseudoStrategy) {
			PseudoPredictor p = ((PseudoStrategy) s).source;
			double score = 0;
			Iterator<AtomicInteger> it = p.getHistory().iterator();
			int n = Math.min(p.getHistory().size(), dataLimit);
			while(it.hasNext() && n > 0) {
				//score += ((double) Math.min(it.next().get(), maxData)) / (n*harmonicFactor*maxData);
				score += ((double) Math.min(it.next().get(), maxData)) / (maxData*dataLimit);
				n--;
			}
			return score;
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

}
