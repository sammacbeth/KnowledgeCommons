package kc.agents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import kc.Measured;
import kc.State;
import kc.Strategy;
import kc.prediction.GreedyPredictor;
import kc.prediction.Predictor;
import kc.prediction.RandomPredictor;
import uk.ac.imperial.einst.Institution;
import uk.ac.imperial.einst.UnavailableModuleException;
import uk.ac.imperial.einst.ipower.IPower;
import uk.ac.imperial.einst.resource.Appropriate;
import uk.ac.imperial.einst.resource.AppropriationsListener;
import uk.ac.imperial.einst.resource.ArtifactTypeMatcher;
import uk.ac.imperial.einst.resource.Provision;
import uk.ac.imperial.einst.resource.ProvisionAppropriationSystem;
import uk.ac.imperial.einst.resource.Request;
import uk.ac.imperial.presage2.core.environment.ActionHandlingException;
import uk.ac.imperial.presage2.core.util.random.Random;

public class PlayerAgent extends AbstractAgent {

	public PlayerAgent(String name) {
		super(Random.randomUUID(), name);
	}

	public PlayerAgent(String name, Predictor predictor, boolean consumer) {
		super(Random.randomUUID(), name);
		addBehaviour(new GameplayBehaviour(predictor));
		addBehaviour(new TrainPredictorBehaviour(predictor));
		addBehaviour(new ProvisionMeasuredBehaviour());
		if (consumer)
			addBehaviour(new AppropriateMeasuredBehaviour());
		addBehaviour(new InstitutionalBehaviour());
	}

	/**
	 * Create a player which uses the provided predictor, training it with
	 * information appropriated from available institutions.
	 * 
	 * @param name
	 * @param predictor
	 * @return
	 */
	public static PlayerAgent knowledgePlayer(String name, Predictor predictor) {
		PlayerAgent a = new PlayerAgent(name);
		a.addBehaviour(a.new GameplayBehaviour(predictor));
		a.addBehaviour(a.new TrainPredictorBehaviour(predictor));
		a.addBehaviour(a.new ProvisionMeasuredBehaviour());
		a.addBehaviour(a.new AppropriateMeasuredBehaviour());
		a.addBehaviour(a.new InstitutionalBehaviour());
		return a;
	}

	/**
	 * Create a player which will attempt to appropriate predictors and will
	 * then periodically chose the best ones to decide its strategy.
	 * 
	 * @param name
	 * @return
	 */
	public static PlayerAgent dumbPlayer(String name) {
		PlayerAgent a = new PlayerAgent(name);
		a.addBehaviour(a.new MultiPredictorGameplayBehaviour(
				new RandomPredictor()));
		a.addBehaviour(a.new AppropriatePredictorBehaviour());
		a.addBehaviour(a.new ProvisionMeasuredBehaviour());
		a.addBehaviour(a.new AppropriateMeasuredBehaviour());
		a.addBehaviour(a.new InstitutionalBehaviour());
		return a;
	}

	class GameplayBehaviour implements Behaviour {
		Predictor predictor;
		boolean measure = true;

		public GameplayBehaviour(Predictor predictor) {
			super();
			this.predictor = predictor;
		}

		@Override
		public void initialise() {
		}

		@Override
		public void doBehaviour() {
			Strategy chosen = this.predictor.actionSelection(
					game.getState(getID()), game.getStrategies());
			chosen.setMeasure(measure);

			// play the game
			try {
				act(chosen);
			} catch (ActionHandlingException e) {
				logger.warn(e);
			}
		}

		@Override
		public void onEvent(String type, Object value) {
		}

	}

	class MultiPredictorGameplayBehaviour extends GameplayBehaviour {

		List<Predictor> options = new ArrayList<Predictor>();
		Predictor strategy;

		double prevAccount = 0;

		final int strategyEvalPeriod = 2;
		Strategy current = null;
		Strategy last = null;
		int strategyDuration = 0;

		public MultiPredictorGameplayBehaviour(Predictor predictor) {
			this(predictor, new GreedyPredictor(0.5, 0.5, 0.1));
		}

		public MultiPredictorGameplayBehaviour(Predictor predictor,
				Predictor strategy) {
			super(predictor);
			this.options.add(predictor);
			this.strategy = strategy;
		}

		@Override
		public void initialise() {
			super.initialise();
		}

		@Override
		public void doBehaviour() {
			if (current == null || --strategyDuration <= 0) {
				// periodically reassess strategy
				current = strategy.actionSelection(State.NONE, getStrategies());
				this.predictor = options.get(current.getId());
				strategyDuration = strategyEvalPeriod;
				logger.info("Chosen Predictor is: " + this.predictor
						+ " with score: " + strategy.getLastScore());
			}

			if (last != null) {
				double account = game.getScore(getID());
				strategy.addTrainingData(new Measured(null, State.NONE, current
						.getId(), account - prevAccount, 0));
				prevAccount = account;
			}

			last = current;
			super.doBehaviour();
		}

		private List<Strategy> getStrategies() {
			List<Strategy> s = new ArrayList<Strategy>();
			for (int i = 0; i < options.size(); i++) {
				s.add(new Strategy(i, measure));
			}
			return s;
		}

		@Override
		public void onEvent(String type, Object value) {
			if (type.equals("newPredictor")) {
				options.add((Predictor) value);
			}
		}

	}

	class ProvisionMeasuredBehaviour extends PowerReactiveBehaviour {

		Queue<Measured> incMeasured;
		IPower pow;

		public ProvisionMeasuredBehaviour() {
			super(new Provision(PlayerAgent.this, null, new Measured()));
		}

		@Override
		public void initialise() {
			this.incMeasured = game.measuredQueueSubscribe(getID());
			super.initialise();
		}

		@Override
		public void doBehaviour() {
			super.doBehaviour();
			while (!incMeasured.isEmpty()) {
				Measured m = incMeasured.poll();
				// provision to each inst
				for (Institution i : institutions) {
					inst.act(new Provision(PlayerAgent.this, i, m));
				}
				// save this measured
				measured.publish(m);
			}
		}

		@Override
		public void onEvent(String type, Object value) {
		}

	}

	class AppropriatePredictorBehaviour extends PowerReactiveBehaviour
			implements AppropriationsListener {

		ProvisionAppropriationSystem sys;
		Map<Institution, Set<Predictor>> sources = new HashMap<Institution, Set<Predictor>>();

		public AppropriatePredictorBehaviour() {
			super(new Appropriate(PlayerAgent.this, null, Predictor.class));
		}

		@Override
		public void initialise() {
			super.initialise();
			try {
				sys = inst.getSession().getModule(
						ProvisionAppropriationSystem.class);
			} catch (UnavailableModuleException e) {
				throw new RuntimeException(e);
			}
			sys.registerForAppropriations(PlayerAgent.this, this);
		}

		@Override
		public void doBehaviour() {
			super.doBehaviour();
			for (Institution i : sources.keySet()) {
				if (!institutions.contains(i)) {
					// no longer access to this inst, we cannot use these
					// predictors anymore
					for (Predictor p : sources.get(i)) {
						sendEvent("removePredictor", p);
					}
					sources.remove(i);
				} else {
					inst.act(new Request(PlayerAgent.this, i,
							new ArtifactTypeMatcher(Predictor.class), 5));
				}
			}
		}

		@Override
		public void onAppropriation(Object artifact, Institution from) {
			if (artifact instanceof Predictor
					&& (!sources.containsKey(from) || !sources.get(from)
							.contains(artifact))) {
				Predictor p = new SlavePredictor((Predictor) artifact, from);
				sendEvent("newPredictor", p);
				if (!sources.containsKey(from)) {
					sources.put(from, new HashSet<Predictor>());
				}
				sources.get(from).add(p);
			}
		}

		@Override
		public void onEvent(String type, Object value) {
		}

	}

	class SlavePredictor extends Predictor {

		final Predictor delegate;
		final Institution source;

		SlavePredictor(Predictor delegate, Institution source) {
			super();
			this.delegate = delegate;
			this.source = source;
		}

		@Override
		public void addTrainingData(Measured data) {
		}

		@Override
		public Strategy actionSelection(State state, List<Strategy> strategies) {
			inst.act(new Appropriate(PlayerAgent.this, this.source,
					this.delegate));
			return delegate.actionSelection(state, strategies);
		}

		@Override
		public String toString() {
			return "" + delegate + " from " + source + "";
		}

		@Override
		public double getLastScore() {
			return delegate.getLastScore();
		}

	}

}
