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

	class AppropriateMeasuredBehaviour implements Behaviour, PowerReactive,
			AppropriationsListener {

		IPower pow;
		ProvisionAppropriationSystem sys;
		Set<Institution> appTargets = new HashSet<Institution>();
		boolean checkTargets = false;

		int lastRequest = -1;

		public AppropriateMeasuredBehaviour() {
			super();
		}

		@Override
		public void initialise() {
			try {
				pow = inst.getSession().getModule(IPower.class);
				sys = inst.getSession().getModule(
						ProvisionAppropriationSystem.class);
			} catch (UnavailableModuleException e) {
				throw new RuntimeException(e);
			}
			pow.registerPowerListener(AbstractAgent.this, new Appropriate(
					AbstractAgent.this, null, new Measured()), this);
			sys.registerForAppropriations(AbstractAgent.this, this);
		}

		@Override
		public void doBehaviour() {
			if (checkTargets) {
				for (Action act : pow.powList(AbstractAgent.this,
						new Appropriate(AbstractAgent.this, null,
								new Measured()))) {
					appTargets.add(act.getInst());
				}
				checkTargets = false;
			}
			for (Institution i : appTargets) {
				inst.act(new Request(AbstractAgent.this, i,
						new MeasuredMatcher().setNewerThan(lastRequest), 10));
			}
		}

		@Override
		public void onPower(Action act) {
			appTargets.add(act.getInst());
		}

		@Override
		public void onPowerRetraction(Action act) {
			// rebuild list
			appTargets.clear();
			checkTargets = true;
		}

		@Override
		public void onAppropriation(Object artifact) {
			if (artifact instanceof Measured) {
				Measured m = (Measured) artifact;
				this.lastRequest = Math.max(this.lastRequest, m.getT());
				measured.publish(m);
			}
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
	}

}
