package kc.agents;

import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

import kc.Measured;
import kc.Strategy;
import kc.prediction.Predictor;
import uk.ac.imperial.einst.Action;
import uk.ac.imperial.einst.Institution;
import uk.ac.imperial.einst.UnavailableModuleException;
import uk.ac.imperial.einst.ipower.IPower;
import uk.ac.imperial.einst.ipower.PowerReactive;
import uk.ac.imperial.einst.resource.Provision;
import uk.ac.imperial.presage2.core.environment.ActionHandlingException;
import uk.ac.imperial.presage2.core.util.random.Random;

public class PlayerAgent extends AbstractAgent {

	public PlayerAgent(String name, Predictor predictor, boolean consumer) {
		super(Random.randomUUID(), name);
		addBehaviour(new GameplayBehaviour(predictor));
		addBehaviour(new ProvisionMeasuredBehaviour());
		if (consumer)
			addBehaviour(new AppropriateMeasuredBehaviour());
		addBehaviour(new InstitutionalBehaviour());
	}

	class GameplayBehaviour implements Behaviour {

		Predictor predictor;
		Queue<Measured> incMeasured;
		boolean measure = true;

		public GameplayBehaviour(Predictor predictor) {
			super();
			this.predictor = predictor;
		}

		@Override
		public void initialise() {
			this.incMeasured = game.measuredQueueSubscribe(getID());
			measured.subscribe(incMeasured);
		}

		@Override
		public void doBehaviour() {
			// s.logger.info("Account is: " + s.game.getScore(s.getID()));

			while (!incMeasured.isEmpty()) {
				this.predictor.addTrainingData(incMeasured.poll());
			}

			Strategy chosen = this.predictor.actionSelection(
					game.getState(getID()), game.getStrategies());
			chosen.setMeasure(measure);

			// play the game
			// s.logger.info(chosen);
			try {
				act(chosen);
			} catch (ActionHandlingException e) {
				logger.warn(e);
			}
		}
	}

	class ProvisionMeasuredBehaviour implements Behaviour, PowerReactive {

		Queue<Measured> incMeasured;
		IPower pow;
		Set<Institution> provisionTargets = new HashSet<Institution>();
		boolean checkTargets = false;

		public ProvisionMeasuredBehaviour() {
			super();
		}

		@Override
		public void initialise() {
			this.incMeasured = game.measuredQueueSubscribe(getID());
			try {
				pow = inst.getSession().getModule(IPower.class);
			} catch (UnavailableModuleException e) {
				throw new RuntimeException(e);
			}
			pow.registerPowerListener(PlayerAgent.this, new Provision(
					PlayerAgent.this, null, new Measured()), this);
		}

		@Override
		public void doBehaviour() {
			if (checkTargets) {
				// check for institutions I can provision to.
				for (Action act : pow.powList(PlayerAgent.this, new Provision(
						PlayerAgent.this, null, new Measured()))) {
					provisionTargets.add(act.getInst());
				}
				checkTargets = false;
			}
			while (!incMeasured.isEmpty()) {
				Measured m = incMeasured.poll();
				// provision to each inst
				for (Institution i : provisionTargets) {
					inst.act(new Provision(PlayerAgent.this, i, m));
				}
				// save this measured
				measured.publish(m);
			}
		}

		@Override
		public void onPower(Action act) {
			provisionTargets.add(act.getInst());
		}

		@Override
		public void onPowerRetraction(Action act) {
			// rebuild list
			provisionTargets.clear();
			checkTargets = true;
		}

	}

}
