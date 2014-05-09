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
		a.addBehaviour(a.new RoleManagement());
		a.addBehaviour(a.new SubscriptionVote(Profile.SUSTAINABLE));
		return a;
	}

	/**
	 * Create a player which will attempt to appropriate predictors and will
	 * then periodically chose the best ones to decide its strategy.
	 * 
	 * @param name
	 * @return
	 */
	public static PlayerAgent dumbPlayer(String name, Predictor defaultPredictor) {
		PlayerAgent a = new PlayerAgent(name);
		a.addBehaviour(a.new MultiPredictorGameplayBehaviour(defaultPredictor));
		a.addBehaviour(a.new AppropriatePredictorBehaviour());
		a.addBehaviour(a.new ProvisionMeasuredBehaviour());
		a.addBehaviour(a.new AppropriateMeasuredBehaviour());
		a.addBehaviour(a.new InstitutionalBehaviour());
		a.addBehaviour(a.new RoleManagement());
		a.addBehaviour(a.new SubscriptionVote(Profile.GREEDY));
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

		Map<Integer, Predictor> options = new HashMap<Integer, Predictor>();
		private int ind = 0;
		Predictor strategy;

		double prevAccount = 0;

		final int strategyEvalPeriod = 5;
		Strategy current = null;
		Strategy last = null;
		int strategyDuration = 0;

		public MultiPredictorGameplayBehaviour(Predictor predictor) {
			this(predictor, new GreedyPredictor(1.0, 0.5, 0.0));
		}

		public MultiPredictorGameplayBehaviour(Predictor predictor,
				Predictor strategy) {
			super(predictor);
			this.options.put(ind++, predictor);
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
				if (this.predictor != null
						&& predictor instanceof SlavePredictor) {
					// decrement usage of this role
					SlavePredictor sp = (SlavePredictor) this.predictor;
					decrementRoleUsage(sp.source, "consumer");
				}
				current = strategy.actionSelection(State.NONE, getStrategies());
				this.predictor = options.get(current.getId());
				strategyDuration = strategyEvalPeriod;
				logger.info("Chosen Predictor is: " + this.predictor);

				if (predictor instanceof SlavePredictor) {
					// increment usage of this role
					SlavePredictor sp = (SlavePredictor) this.predictor;
					incrementRoleUsage(sp.source, "consumer");
				}
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
			for (Integer i : options.keySet()) {
				s.add(new Strategy(i, measure));
			}
			return s;
		}

		@Override
		public void onEvent(String type, Object value) {
			if (type.equals("newPredictor") && !options.containsValue(value)) {
				options.put(ind++, (Predictor) value);
			} else if (type.equals("removePredictor")) {
				Set<Integer> toRemove = new HashSet<Integer>();
				for (Map.Entry<Integer, Predictor> e : options.entrySet()) {
					if (e.getValue().equals(value)) {
						toRemove.add(e.getKey());
					}
				}
				for (int id : toRemove) {
					options.remove(id);
					if (current != null && current.getId() == id) {
						current = null;
					}
				}
			} else if (type.equals("leaveInstitution")) {
				Set<Integer> toRemove = new HashSet<Integer>();
				for (Map.Entry<Integer, Predictor> e : options.entrySet()) {
					if (e.getValue() instanceof SlavePredictor) {
						SlavePredictor p = (SlavePredictor) e.getValue();
						if (p.source.equals(value)) {
							toRemove.add(e.getKey());
						}
					}
				}
				for (int id : toRemove) {
					options.remove(id);
					if (current != null && current.getId() == id) {
						current = null;
					}
				}
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
			for (Institution i : institutions) {
				if (!sources.containsKey(i))
					sources.put(i, new HashSet<Predictor>());
			}
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

	class SlavePredictor implements Predictor {

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
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result
					+ ((delegate == null) ? 0 : delegate.hashCode());
			result = prime * result
					+ ((source == null) ? 0 : source.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			SlavePredictor other = (SlavePredictor) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (delegate == null) {
				if (other.delegate != null)
					return false;
			} else if (!delegate.equals(other.delegate))
				return false;
			if (source == null) {
				if (other.source != null)
					return false;
			} else if (!source.equals(other.source))
				return false;
			return true;
		}

		private PlayerAgent getOuterType() {
			return PlayerAgent.this;
		}

	}

}
