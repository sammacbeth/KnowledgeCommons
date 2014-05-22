package kc.agents;

import kc.prediction.Predictor;
import uk.ac.imperial.presage2.core.util.random.Random;

public class NonPlayerAgent extends AbstractAgent {

	public NonPlayerAgent(String name) {
		super(Random.randomUUID(), name);
	}

	public static NonPlayerAgent analystAgent(String name, Predictor predictor, Profile p) {
		NonPlayerAgent a = new NonPlayerAgent(name);
		a.addBehaviour(a.new AppropriateMeasuredBehaviour());
		a.addBehaviour(a.new ProvisionPredictorBehaviour(predictor));
		a.addBehaviour(a.new TrainPredictorBehaviour(predictor));
		a.addBehaviour(a.new InstitutionalBehaviour());
		a.addBehaviour(a.new PruneMeasuredBehaviour());
		a.addBehaviour(a.new OpenBallotsBehaviour());
		a.addBehaviour(a.new VoteBehaviour(p));
		return a;
	}

}
