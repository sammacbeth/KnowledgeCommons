package kc.agents;

import java.util.UUID;

import kc.Strategy;
import uk.ac.imperial.presage2.core.environment.ActionHandlingException;

public class GathererAgent extends AbstractAgent {

	public GathererAgent(UUID id, String name) {
		super(id, name);
	}

	@Override
	public void incrementTime() {

		while (!incMeasured.isEmpty()) {
			this.predictor.addTrainingData(incMeasured.poll());
		}

		Strategy chosen = this.predictor.actionSelection(
				game.getState(getID()), game.getStrategies());

		// play the game
		logger.info("Strategy: " + chosen + "");
		try {
			environment.act(chosen, getID(), authkey);
		} catch (ActionHandlingException e) {
			logger.warn(e);
		}

		super.incrementTime();
	}

}
