package kc.agents;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

import kc.Game;
import kc.GameSimulation;
import kc.InstitutionService;
import kc.Measured;
import kc.prediction.Predictor;
import kc.util.MultiUserQueue;

import org.apache.log4j.Logger;

import uk.ac.imperial.einst.Action;
import uk.ac.imperial.einst.Actor;
import uk.ac.imperial.einst.Institution;
import uk.ac.imperial.einst.UnavailableModuleException;
import uk.ac.imperial.einst.ipower.IPower;
import uk.ac.imperial.einst.ipower.Obl;
import uk.ac.imperial.einst.ipower.ObligationReactive;
import uk.ac.imperial.einst.ipower.PowerReactive;
import uk.ac.imperial.einst.resource.Appropriate;
import uk.ac.imperial.einst.resource.AppropriationsListener;
import uk.ac.imperial.einst.resource.Provision;
import uk.ac.imperial.einst.resource.ProvisionAppropriationSystem;
import uk.ac.imperial.einst.resource.Request;
import uk.ac.imperial.presage2.core.environment.UnavailableServiceException;
import uk.ac.imperial.presage2.core.messaging.Input;
import uk.ac.imperial.presage2.util.participant.AbstractParticipant;

public class AbstractAgent extends AbstractParticipant implements Actor {

	protected final Logger logger;
	protected Game game;
	InstitutionService inst;

	List<Behaviour> behaviours = new LinkedList<Behaviour>();

	MultiUserQueue<Measured> measured = new MultiUserQueue<Measured>();

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

	}

	class AppropriateMeasuredBehaviour extends PowerReactiveBehaviour implements
			AppropriationsListener {

		ProvisionAppropriationSystem sys;
		int lastRequest = -1;

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
						new MeasuredMatcher().setNewerThan(lastRequest), 10));
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

}
