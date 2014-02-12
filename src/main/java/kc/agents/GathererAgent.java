package kc.agents;

import kc.prediction.Predictor;
import uk.ac.imperial.presage2.core.util.random.Random;

public class GathererAgent extends AbstractAgent {

	public GathererAgent(String name, Predictor predictor, boolean consumer) {
		super(Random.randomUUID(), name);
		addBehaviour(new GameplayBehaviour(this, predictor));
		addBehaviour(new GathererBehaviour(this));
		if (consumer)
			addBehaviour(new ConsumerBehaviour(this));
		addBehaviour(new InstitutionalBehaviour(this));
	}

}
