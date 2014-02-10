package kc.agents;

import java.util.UUID;

import kc.prediction.Predictor;

public class GathererAgent extends AbstractAgent {

	public GathererAgent(UUID id, String name, Predictor predictor,
			boolean consumer) {
		super(id, name);
		addBehaviour(new GameplayBehaviour(this, predictor));
		addBehaviour(new GathererBehaviour(this));
		if (consumer)
			addBehaviour(new ConsumerBehaviour(this));
	}

}
