package kc;

import java.util.HashSet;
import java.util.Set;

import kc.agents.AbstractAgent;
import kc.agents.NonPlayerAgent;
import kc.agents.PlayerAgent;
import kc.agents.Profile;
import kc.games.ShortKnowledgeGame;
import kc.prediction.Predictor;
import kc.prediction.PseudoPredictor;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;

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

public class MarketSimulation extends InjectedSimulation {

	@Parameter(name = "seed")
	public static int seed = 1;
	@Parameter(name = "facilityCostProfile")
	public int facilityCostProfile;
	@Parameter(name = "nProsumers")
	public int nProsumers;
	@Parameter(name = "nNcProsumers", optional = true)
	public int nNcProsumers = 0;
	@Parameter(name = "measuringCost")
	public double measuringCost;
	@Parameter(name = "initiatorCredit")
	public int initiatorCredit;

	@Parameter(name = "analystProfile", optional = true)
	public Profile analystProfile = Profile.SUSTAINABLE;
	@Parameter(name = "consumerProfile", optional = true)
	public Profile consumerProfile = Profile.SUSTAINABLE;
	@Parameter(name = "ncconsumerProfile", optional = true)
	public Profile ncconsumerProfile = Profile.PROFITABLE;
	@Parameter(name = "initiatorProfile", optional = true)
	public Profile initiatorProfile = Profile.SUSTAINABLE;

	InstitutionService inst;
	EInstSession session;
	Game game;

	public MarketSimulation(Set<AbstractModule> modules) {
		super(modules);
		EInstSession.USE_KB_CACHE = true;
		GameSimulation.game = ShortKnowledgeGame.class;
	}

	@Inject
	public void setServiceProvider(EnvironmentServiceProvider serviceProvider) {
		try {
			this.inst = serviceProvider
					.getEnvironmentService(InstitutionService.class);
			this.game = serviceProvider
					.getEnvironmentService(GameSimulation.game);
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
				.addActionHandler(GameSimulation.game)
				.addParticipantGlobalEnvironmentService(GameSimulation.game)
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
			facilitySunk = 0;
			facilityFixed = 2;
			break;
		case 1:
			facilitySunk = 0;
			facilityFixed = 0;
			facilityMarginalTrans = 0.1;
			break;
		case 2:
			facilitySunk = 0;
			facilityFixed = 0;
			break;
		}

		final Set<String> consumers = Roles.set("consumer");
		final Set<String> initiators = Roles.set("initiator");
		final Set<String> analysts = Roles.set("analyst");
		final Set<String> gatherers = Roles.set("gatherer");

		Institution ki = new InstitutionBuilder(session, "ki", 10, "analyst")
				.addFacility(facilitySunk, facilityFixed,
						facilityMarginalStorage, facilityMarginalTrans)
				.addPredictorPool()
				.dynamicPayOnAppropriation(consumers, 0.0, initiators,
						analysts, 0.05, true).end().build();
		((DataInstitution)ki).maxPayRate = 1.5;

		// initiator to coordinate cfvs
		AbstractAgent initiator = NonPlayerAgent.initiatorAgent("c1",
				initiatorProfile);
		addAgent(s, initiator, initiatorCredit, ki, "initiator", "manager");

		// analyst
		AbstractAgent analyst = NonPlayerAgent.analystAgent("a1",
				goodPredictor("a1"), analystProfile);
		addAgent(s, analyst, 10, ki, "analyst");

		for (int n = 0; n < nProsumers; n++) {
			Profile p = consumerProfile;
			String name = "p" + (n - nNcProsumers);
			if (n < nNcProsumers) {
				p = ncconsumerProfile;
				name = "nc" + n;
			}
			InstitutionBuilder ib = new InstitutionBuilder(session,
					"i_" + name, 10, "gatherer")
					.addFacility(facilitySunk, facilityFixed,
							facilityMarginalStorage, facilityMarginalTrans)
					.addMeasuredPool()
					.dynamicPayOnAppropriation(analysts, 0.0, initiators,
							gatherers, 0.02, true).end();
			Institution i = ib.build();

			AbstractAgent ag = PlayerAgent.dumbPlayer(name, badPredictor(), p);
			addAgent(s, ag, 10, i, "gatherer");
			session.insert(new RoleOf(ag, i, "owner"));
			session.insert(new RoleOf(ag, ki, "consumer"));

			session.insert(new RoleOf(initiator, i, "initiator"));
			session.insert(new RoleOf(analyst, i, "analyst"));
		}
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
