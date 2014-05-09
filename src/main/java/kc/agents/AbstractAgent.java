package kc.agents;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import kc.DataInstitution;
import kc.Game;
import kc.GameSimulation;
import kc.InstitutionService;
import kc.Measured;
import kc.choice.SubscriptionFee;
import kc.prediction.Predictor;
import kc.util.MultiUserQueue;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;

import uk.ac.imperial.einst.Action;
import uk.ac.imperial.einst.Actor;
import uk.ac.imperial.einst.Institution;
import uk.ac.imperial.einst.UnavailableModuleException;
import uk.ac.imperial.einst.access.AccessControl;
import uk.ac.imperial.einst.access.Resign;
import uk.ac.imperial.einst.ipower.IPower;
import uk.ac.imperial.einst.ipower.Obl;
import uk.ac.imperial.einst.ipower.ObligationReactive;
import uk.ac.imperial.einst.ipower.PowerReactive;
import uk.ac.imperial.einst.resource.Appropriate;
import uk.ac.imperial.einst.resource.AppropriationsListener;
import uk.ac.imperial.einst.resource.ArtifactMatcher;
import uk.ac.imperial.einst.resource.ArtifactTypeMatcher;
import uk.ac.imperial.einst.resource.Provision;
import uk.ac.imperial.einst.resource.ProvisionAppropriationSystem;
import uk.ac.imperial.einst.resource.Prune;
import uk.ac.imperial.einst.resource.Remove;
import uk.ac.imperial.einst.resource.Request;
import uk.ac.imperial.einst.vote.CloseBallot;
import uk.ac.imperial.einst.vote.Issue;
import uk.ac.imperial.einst.vote.OpenBallot;
import uk.ac.imperial.einst.vote.Preferences;
import uk.ac.imperial.einst.vote.Vote;
import uk.ac.imperial.einst.vote.Voting;
import uk.ac.imperial.presage2.core.environment.UnavailableServiceException;
import uk.ac.imperial.presage2.core.messaging.Input;
import uk.ac.imperial.presage2.util.participant.AbstractParticipant;

public class AbstractAgent extends AbstractParticipant implements Actor {

	protected final Logger logger;
	protected Game game;
	InstitutionService inst;

	List<Behaviour> behaviours = new LinkedList<Behaviour>();

	MultiUserQueue<Measured> measured = new MultiUserQueue<Measured>();

	Map<Pair<Institution, String>, AtomicInteger> roleUsage = new HashMap<Pair<Institution, String>, AtomicInteger>();

	public AbstractAgent(UUID id, String name) {
		super(id, name);
		logger = Logger.getLogger(name);
	}

	@Override
	public void initialise() {
		super.initialise();
		try {
			this.game = getEnvironmentService(GameSimulation.game);
			this.inst = getEnvironmentService(InstitutionService.class);
		} catch (UnavailableServiceException e) {
			throw new RuntimeException("Couldn't get env services", e);
		}
		for (Behaviour b : behaviours) {
			b.initialise();
		}
	}

	@Override
	protected void processInput(Input in) {
	}

	@Override
	public void incrementTime() {
		for (Behaviour b : behaviours) {
			b.doBehaviour();
		}
		super.incrementTime();
	}

	@Override
	public String toString() {
		return this.getName();
	}

	void addBehaviour(Behaviour b) {
		behaviours.add(b);
	}

	void sendEvent(String type, Object value) {
		for (Behaviour b : behaviours) {
			b.onEvent(type, value);
		}
	}

	abstract class PowerReactiveBehaviour implements Behaviour, PowerReactive {
		IPower pow;
		Set<Institution> institutions = new HashSet<Institution>();
		boolean checkInstitutions = false;
		final Action powAction;

		public PowerReactiveBehaviour(Action powAction) {
			super();
			this.powAction = powAction;
		}

		@Override
		public void initialise() {
			try {
				pow = inst.getSession().getModule(IPower.class);
			} catch (UnavailableModuleException e) {
				throw new RuntimeException(e);
			}
			pow.registerPowerListener(AbstractAgent.this, powAction, this);
		}

		@Override
		public void doBehaviour() {
			if (checkInstitutions) {
				// check for institutions I can provision to.
				for (Action act : pow.powList(AbstractAgent.this, powAction)) {
					institutions.add(act.getInst());
				}
				checkInstitutions = false;
			}
		}

		@Override
		public void onPower(Action act) {
			institutions.add(act.getInst());
		}

		@Override
		public void onPowerRetraction(Action pow) {
			// rebuild list
			institutions.clear();
			checkInstitutions = true;
		}

		@Override
		public void onEvent(String type, Object value) {
		}

	}

	class AppropriateMeasuredBehaviour extends PowerReactiveBehaviour implements
			AppropriationsListener {

		ProvisionAppropriationSystem sys;
		int lastRequest = -1;
		int appropriateLim = 100;

		public AppropriateMeasuredBehaviour() {
			super(new Appropriate(AbstractAgent.this, null, new Measured()));
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
			sys.registerForAppropriations(AbstractAgent.this, this);
		}

		@Override
		public void doBehaviour() {
			super.doBehaviour();
			for (Institution i : institutions) {
				inst.act(new Request(AbstractAgent.this, i,
						new MeasuredMatcher().setNewerThan(lastRequest),
						appropriateLim));
			}
		}

		@Override
		public void onAppropriation(Object artifact, Institution from) {
			if (artifact instanceof Measured) {
				Measured m = (Measured) artifact;
				this.lastRequest = Math.max(this.lastRequest, m.getT());
				measured.publish(m);
			}
		}

		@Override
		public void onEvent(String type, Object value) {
		}

	}

	class InstitutionalBehaviour implements Behaviour, ObligationReactive {

		Queue<Obl> obligations = new LinkedList<Obl>();
		boolean meetObligations = true;

		IPower pow;

		public InstitutionalBehaviour() {
			super();
		}

		@Override
		public void initialise() {
			try {
				pow = inst.getSession().getModule(IPower.class);
				pow.registerObligationReactive(this, AbstractAgent.this);
			} catch (UnavailableModuleException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void doBehaviour() {
			if (meetObligations)
				processObligations();
		}

		protected void processObligations() {
			while (!obligations.isEmpty()) {
				inst.act(obligations.poll().getAction());
			}
		}

		@Override
		public void onObligation(Obl obl) {
			obligations.add(obl);
		}

		@Override
		public void onEvent(String type, Object value) {
		}
	}

	class ProvisionPredictorBehaviour extends PowerReactiveBehaviour {

		Predictor predictor;
		Set<Institution> provisionedTo = new HashSet<Institution>();

		public ProvisionPredictorBehaviour(Predictor p) {
			super(new Provision(AbstractAgent.this, null, Predictor.class));
			this.predictor = p;
		}

		@Override
		public void doBehaviour() {
			super.doBehaviour();
			institutions.removeAll(provisionedTo);
			for (Institution i : institutions) {
				inst.act(new Provision(AbstractAgent.this, i, predictor));
				provisionedTo.add(i);
			}
		}

		@Override
		public void onEvent(String type, Object value) {
		}

	}

	class TrainPredictorBehaviour implements Behaviour {

		Predictor predictor;
		Queue<Measured> incMeasured;

		public TrainPredictorBehaviour(Predictor predictor) {
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
			while (!incMeasured.isEmpty()) {
				this.predictor.addTrainingData(incMeasured.poll());
			}
		}

		@Override
		public void onEvent(String type, Object value) {
		}
	}

	class PruneMeasuredBehaviour extends PowerReactiveBehaviour {

		ArtifactMatcher matcher = new ArtifactTypeMatcher(Measured.class);
		int pruneBefore = 5;

		public PruneMeasuredBehaviour() {
			super(new Remove(AbstractAgent.this, null, Measured.class));
		}

		@Override
		public void doBehaviour() {
			super.doBehaviour();
			final int t = getTime().intValue();
			if (t > pruneBefore) {
				for (Institution i : this.institutions) {
					inst.act(new Prune(AbstractAgent.this, i, matcher, t
							- pruneBefore, 0));
				}
			}
		}
	}

	class OpenBallotsBehaviour extends PowerReactiveBehaviour {

		int ballotPeriod = 5;
		int ballotDuration = 2;
		Voting v;
		Set<Issue> ballotWanted = new HashSet<Issue>();
		Queue<Issue> ballotsOpened = new LinkedList<Issue>();

		public OpenBallotsBehaviour() {
			super(new OpenBallot(AbstractAgent.this, null, null));
		}

		@Override
		public void initialise() {
			super.initialise();
			try {
				v = inst.getSession().getModule(Voting.class);
			} catch (UnavailableModuleException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void doBehaviour() {
			super.doBehaviour();
			int t = getTime().intValue();
			if (t > 0 && t % ballotPeriod == 0) {
				for (Institution in : this.institutions) {
					for (Issue i : v.getIssues(in)) {
						if (!ballotWanted.contains(i)) {
							inst.act(new OpenBallot(AbstractAgent.this, in, i));
							ballotsOpened.add(i);
						}
					}
				}
			}
			if (t % ballotPeriod == ballotDuration) {
				while (!ballotsOpened.isEmpty()) {
					Issue i = ballotsOpened.poll();
					inst.act(new CloseBallot(AbstractAgent.this, i.getInst(), v
							.getOpenBallot(i)));
				}
			}
		}

		@Override
		public void onEvent(String type, Object value) {
			super.onEvent(type, value);
			if (type.equals("wantballot") && value instanceof Issue) {
				ballotWanted.add((Issue) value);
			}
		}
	}

	class VoteBehaviour implements Behaviour {

		IPower pow;
		Voting v;
		Map<Issue, Object> preferences = new HashMap<Issue, Object>();

		public VoteBehaviour() {
			super();
		}

		@Override
		public void doBehaviour() {
			for (Action a : pow.powList(AbstractAgent.this, new Vote(
					AbstractAgent.this, null, null, null))) {
				Vote v = (Vote) a;
				inst.act(new Vote(AbstractAgent.this, v.getInst(), v
						.getBallot(), new Preferences(v.getBallot()
						.getOptions()[2])));
			}
		}

		@Override
		public void initialise() {
			try {
				pow = inst.getSession().getModule(IPower.class);
				v = inst.getSession().getModule(Voting.class);
			} catch (UnavailableModuleException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void onEvent(String type, Object value) {
			// TODO Auto-generated method stub

		}

	}

	class RoleManagement implements Behaviour {

		AccessControl ac;

		@Override
		public void initialise() {
			try {
				ac = inst.getSession().getModule(AccessControl.class);
			} catch (UnavailableModuleException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void doBehaviour() {
			// leave institutions we're not using
			if (getTime().intValue() % 3 == 0) {
				for (Map.Entry<Pair<Institution, String>, AtomicInteger> e : roleUsage
						.entrySet()) {
					Institution i = e.getKey().getLeft();
					String role = e.getKey().getRight();
					// resign from instutions we're not using
					if (e.getValue().get() == 0
							&& ac.getRoles(AbstractAgent.this, i)
									.contains(role)) {
						inst.act(new Resign(AbstractAgent.this, i, role));
						sendEvent("leaveInstitution", i);
					}
				}
			}
		}

		@Override
		public void onEvent(String type, Object value) {
			// TODO Auto-generated method stub

		}

	}

	public void incrementRoleUsage(Institution i, String role) {
		final Pair<Institution, String> key = Pair.of(i, role);
		if (!roleUsage.containsKey(key))
			roleUsage.put(key, new AtomicInteger());
		roleUsage.get(key).incrementAndGet();
	}

	public void decrementRoleUsage(Institution i, String role) {
		final Pair<Institution, String> key = Pair.of(i, role);
		if (!roleUsage.containsKey(key))
			roleUsage.put(key, new AtomicInteger(1));
		roleUsage.get(key).decrementAndGet();
	}

	class SubscriptionVote implements Behaviour {

		IPower pow;
		Voting voting;
		AccessControl ac;

		final Profile type;

		public SubscriptionVote(Profile type) {
			super();
			this.type = type;
		}

		@Override
		public void doBehaviour() {
			for (Action a : pow.powList(AbstractAgent.this, new Vote(
					AbstractAgent.this, null, null, null))) {
				Vote v = (Vote) a;
				DataInstitution i = (DataInstitution) v.getInst();
				if (v.getBallot().getIssue() instanceof SubscriptionFee) {
					// determine if agent pays this fee, or if they are
					// responsible for institution costs
					SubscriptionFee issue = (SubscriptionFee) v.getBallot()
							.getIssue();
					Set<String> currentRoles = ac.getRoles(AbstractAgent.this,
							i);
					boolean payer = !Collections.disjoint(currentRoles,
							issue.getRoles());
					boolean initiator = currentRoles
							.contains(((DataInstitution) v.getInst())
									.getPayRole());

					// accumulate vote preferences
					Map<Object, AtomicInteger> preferences = new HashMap<Object, AtomicInteger>();
					final Object[] options = v.getBallot().getOptions();
					for (Object o : v.getBallot().getOptions()) {
						preferences.put(o, new AtomicInteger());
					}

					final Object decrease = options[0];
					final Object nochange = options[1];
					final Object increase = options[2];
					double instBalance = i.getAccount().getBalance();
					double instProfit = i.getProfit();

					if (type == Profile.GREEDY) {
						if (initiator)
							preferences.get(increase).incrementAndGet();
						if (payer)
							preferences.get(decrease).incrementAndGet();
					} else if (type == Profile.SUSTAINABLE) {
						// deficit not recovering (within n timesteps)
						if (instBalance < 0
								&& instProfit * 10 < -1 * instBalance)
							preferences.get(increase).incrementAndGet();
						// credit, increasing
						else if (instBalance > 0 && instProfit > 0)
							preferences.get(decrease).incrementAndGet();
						else
							preferences.get(nochange).incrementAndGet();
					}

					switch (issue.getMethod()) {
					case SINGLE:
						Object chosen = null;
						int max = 0;
						for (Map.Entry<Object, AtomicInteger> pref : preferences
								.entrySet()) {
							if (pref.getValue().get() > max) {
								max = pref.getValue().get();
								chosen = pref.getKey();
							}
						}
						if (chosen != null) {
							inst.act(new Vote(AbstractAgent.this, v.getInst(),
									v.getBallot(), chosen));
						}
						break;
					case PREFERENCE:
					case RANK_ORDER:
						Preferences votePref = new Preferences();
						List<Map.Entry<Object, AtomicInteger>> prefList = new LinkedList<Map.Entry<Object, AtomicInteger>>(
								preferences.entrySet());
						Collections
								.sort(prefList,
										new Comparator<Map.Entry<Object, AtomicInteger>>() {
											@Override
											public int compare(
													Entry<Object, AtomicInteger> o1,
													Entry<Object, AtomicInteger> o2) {
												return o1.getValue().get()
														- o2.getValue().get();
											}
										});
						for (Map.Entry<Object, AtomicInteger> e : prefList) {
							votePref.addPreference(e.getKey());
						}
						inst.act(new Vote(AbstractAgent.this, v.getInst(), v
								.getBallot(), votePref));
						break;
					}
				}
			}
		}

		@Override
		public void initialise() {
			try {
				pow = inst.getSession().getModule(IPower.class);
				voting = inst.getSession().getModule(Voting.class);
				ac = inst.getSession().getModule(AccessControl.class);
			} catch (UnavailableModuleException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void onEvent(String type, Object value) {
		}

	}

}
