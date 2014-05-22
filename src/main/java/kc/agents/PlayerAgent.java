package kc.agents;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import kc.Measured;
import kc.Review;
import kc.State;
import kc.Strategy;
import kc.prediction.Predictor;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

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
		a.addBehaviour(a.new VoteBehaviour(Profile.SUSTAINABLE));
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
		return a;
	}

	public static PlayerAgent dumbPlayer(String name,
			Predictor defaultPredictor, Profile profile) {
		PlayerAgent a = dumbPlayer(name, defaultPredictor);
		a.addBehaviour(a.new VoteBehaviour(profile));
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

		Map<Predictor, DescriptiveStatistics> history = new HashMap<Predictor, DescriptiveStatistics>();
		int historyLength = 20;
		double q0 = 0.5;

		double prevAccount = 0;

		final int strategyEvalPeriod = 5;
		Predictor last = null;
		int strategyDuration = 0;
		boolean review = false;

		public MultiPredictorGameplayBehaviour(Predictor predictor) {
			this(predictor, 0.5, 20, false);
		}

		public MultiPredictorGameplayBehaviour(Predictor predictor,
				boolean review) {
			this(predictor, 0.5, 20, review);
		}

		public MultiPredictorGameplayBehaviour(Predictor initialPredictor,
				double q0, int length, boolean review) {
			super(initialPredictor);
			this.q0 = q0;
			this.historyLength = length;
			this.review = review;
			initPredictor(initialPredictor);
		}

		private void initPredictor(Predictor p) {
			history.put(p, new DescriptiveStatistics(historyLength));
			history.get(p).addValue(q0);
		}

		private Predictor getBestPredictor() {
			Predictor best = null;
			double bestScore = 0;
			for (Map.Entry<Predictor, DescriptiveStatistics> e : history
					.entrySet()) {
				double score = e.getValue().getMean();
				if (bestScore < score) {
					best = e.getKey();
					bestScore = score;
				}
			}
			return best;
		}

		@Override
		public void initialise() {
			super.initialise();
		}

		@Override
		public void doBehaviour() {
			if (this.predictor == null || --strategyDuration <= 0) {
				// periodically reassess strategy
				if (this.predictor != null
						&& predictor instanceof SlavePredictor) {
					// decrement usage of this role
					SlavePredictor sp = (SlavePredictor) this.predictor;
					decrementRoleUsage(sp.source, "consumer");
				}
				this.predictor = getBestPredictor();
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
				double delta = account - prevAccount;
				double lastPay = game.getLastPayoff(getID());
				if (last instanceof SlavePredictor && review) {
					reviewed.publish(new Review(PlayerAgent.this,
							((SlavePredictor) last).delegate, lastPay, lastPay
									- delta, getTime().intValue()));
				}
				history.get(this.last).addValue(delta);
				prevAccount = account;
			}

			this.last = this.predictor;
			super.doBehaviour();
		}

		@Override
		public void onEvent(String type, Object value) {
			if (type.equals("newPredictor")) {
				initPredictor((Predictor) value);
			} else if (type.equals("removePredictor")) {
				history.remove(value);
			} else if (type.equals("leaveInstitution")) {
				Set<Predictor> toRemove = new HashSet<Predictor>();
				for (Predictor p : history.keySet()) {
					if (p instanceof SlavePredictor) {
						SlavePredictor s = (SlavePredictor) p;
						if (s.source.equals(value)) {
							toRemove.add(p);
						}
					}
				}
				history.keySet().removeAll(toRemove);
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
