package kc;

import java.util.HashSet;
import java.util.Set;

import kc.agents.AbstractAgent;
import kc.agents.NonPlayerAgent;
import kc.agents.PlayerAgent;
import kc.agents.Profile;
import kc.games.KnowledgeGame;
import kc.prediction.Predictor;
import kc.prediction.PseudoPredictor;
import uk.ac.imperial.einst.EInstSession;
import uk.ac.imperial.einst.Institution;
import uk.ac.imperial.einst.access.RoleOf;
import uk.ac.imperial.einst.access.Roles;
import uk.ac.imperial.einst.micropay.Account;
import uk.ac.imperial.presage2.core.environment.EnvironmentServiceProvider;
import uk.ac.imperial.presage2.core.environment.UnavailableServiceException;
import uk.ac.imperial.presage2.core.event.EventBus;
import uk.ac.imperial.presage2.core.event.EventListener;
import uk.ac.imperial.presage2.core.simulator.EndOfTimeCycle;
import uk.ac.imperial.presage2.core.simulator.InjectedSimulation;
import uk.ac.imperial.presage2.core.simulator.Parameter;
import uk.ac.imperial.presage2.core.simulator.Scenario;
import uk.ac.imperial.presage2.util.environment.AbstractEnvironmentModule;
import uk.ac.imperial.presage2.util.network.NetworkModule;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;

public class FullSimulation extends InjectedSimulation {

	@Parameter(name = "seed")
	public static int seed = 1;
	@Parameter(name = "facilityCostProfile")
	public int facilityCostProfile;
	@Parameter(name = "analystProfile", optional = true)
	public Profile analystProfile = Profile.SUSTAINABLE;
	@Parameter(name = "consumerProfile", optional = true)
	public Profile consumerProfile = Profile.SUSTAINABLE;
	@Parameter(name = "initiatorProfile", optional = true)
	public Profile initiatorProfile = Profile.SUSTAINABLE;
	@Parameter(name = "nProsumers")
	public int nProsumers;
	@Parameter(name = "measuringCost")
	public double measuringCost;
	@Parameter(name = "initiatorCredit")
	public int initiatorCredit;
	@Parameter(name = "qscale", optional = true)
	public double qscale = 0.05;
	@Parameter(name = "subscription", optional = true)
	public boolean subscription = false;

	InstitutionService inst;
	EInstSession session;
	Game game;

	public FullSimulation(Set<AbstractModule> modules) {
		super(modules);
		EInstSession.USE_KB_CACHE = true;
		GameSimulation.game = KnowledgeGame.class;
	}

	@Inject
	public void setServiceProvider(EnvironmentServiceProvider serviceProvider) {
		try {
			this.inst = serviceProvider
					.getEnvironmentService(InstitutionService.class);
			this.game = serviceProvider.getEnvironmentService(KnowledgeGame.class);
			this.session = this.inst.getSession();
		} catch (UnavailableServiceException e) {
			logger.fatal("No institution service", e);
		}
		this.game.measuringCost = measuringCost;
	}

	@Inject
	public void setEventBus(EventBus eb) {
		eb.subscribe(this);
	}

	@Override
	protected Set<AbstractModule> getModules() {
		Set<AbstractModule> modules = new HashSet<AbstractModule>();
		modules.add(new AbstractEnvironmentModule()
				.addActionHandler(KnowledgeGame.class)
				.addParticipantGlobalEnvironmentService(KnowledgeGame.class)
				.addParticipantGlobalEnvironmentService(
						InstitutionService.class));
		modules.add(NetworkModule.noNetworkModule());

		return modules;
	}

	@Override
	protected void addToScenario(Scenario s) {
		this.session.LOG_WM = false;
		s.addPlugin(this.inst);

		double facilitySunk = 0;
		double facilityFixed = 0;
		double facilityMarginalStorage = 0;
		double facilityMarginalTrans = 0;
		switch (facilityCostProfile) {
		case 0:
			facilitySunk = 50;
			facilityFixed = 1;
			break;
		case 1:
			facilitySunk = 10;
			facilityFixed = 2;
		}
		final Set<String> consumers = Roles.set("consumer");
		final Set<String> initiators = Roles.set("initiator");

		InstitutionBuilder ib = new InstitutionBuilder(session, "i1", 100, "initiator")
				.addMeasuredPool()
				.end()
				.addPredictorPool()
				.end()
				.addFacility(facilitySunk, facilityFixed,
						facilityMarginalStorage, facilityMarginalTrans);
		if(subscription) {
			ib.addDynamicSubscription(consumers, 0.1, initiators, initiators, 0.1);
		}
		Institution i = ib.build();
		// prosumers
		for (int n = 0; n < nProsumers; n++) {
			AbstractAgent ag = PlayerAgent.dumbPlayer("p" + n, badPredictor(),
					consumerProfile);
			addAgent(s, ag, 10, i, "gatherer", "consumer");
		}
		// analyst
		AbstractAgent ag = NonPlayerAgent.analystAgent("a1",
				goodPredictor("a1"), analystProfile);
		addAgent(s, ag, 0, i, "analyst");
		// initiator
		AbstractAgent initiator = NonPlayerAgent.initiatorAgent("c1",
				initiatorProfile);
		addAgent(s, initiator, initiatorCredit, i, "initiator", "manager");
		// benchmark
		addAgent(s, PlayerAgent.knowledgePlayer("ind", goodPredictor("ind")),
				0, null);
	}

	@EventListener
	public void incrementTime(EndOfTimeCycle e) {
		session.incrementTime();
	}

	void addAgent(Scenario s, AbstractAgent ag, double borrowLimit,
			Institution initialInst, String... roles) {
		s.addParticipant(ag);
		session.insert(new Account(ag, 0, borrowLimit));
		for (String role : roles) {
			session.insert(new RoleOf(ag, initialInst, role));
		}
	}

	Predictor badPredictor() {
		return new PseudoPredictor();
	}

	Predictor goodPredictor(String name) {
		return new PseudoPredictor(name);
	}

}
