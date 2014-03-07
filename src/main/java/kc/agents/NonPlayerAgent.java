package kc.agents;

import uk.ac.imperial.presage2.core.util.random.Random;

public class NonPlayerAgent extends AbstractAgent {

	public NonPlayerAgent(String name) {
		super(Random.randomUUID(), name);
		addBehaviour(new InstitutionalBehaviour());
	}

}
