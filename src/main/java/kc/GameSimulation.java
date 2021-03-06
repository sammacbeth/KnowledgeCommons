package kc;

import java.util.HashSet;
import java.util.Set;

import kc.agents.AbstractAgent;
import kc.agents.NonPlayerAgent;
import kc.agents.PlayerAgent;
import kc.agents.Profile;
import kc.prediction.GreedyPredictor;
import kc.prediction.Predictor;
import kc.prediction.PseudoPredictor;
import kc.prediction.RandomPredictor;
import uk.ac.imperial.einst.EInstSession;
import uk.ac.imperial.einst.Institution;
import uk.ac.imperial.einst.access.RoleOf;
import uk.ac.imperial.einst.access.Roles;
import uk.ac.imperial.einst.micropay.Account;
import uk.ac.imperial.einst.resource.ArtifactTypeMatcher;
import uk.ac.imperial.einst.resource.Pool;
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

public class GameSimulation extends InjectedSimulation {

	@Parameter(name = "numStrategies", optional = true)
	public static int numStrategies = 2;
	@Parameter(name = "gameClass")
	public String gameClass;
	@Parameter(name = "gathererLimit", optional = true)
	public int gathererLimit = 10;
	@Parameter(name = "stratVolatility", optional = true)
	public static double stratVolatility = 0.01;
	@Parameter(name = "stratVariability", optional = true)
	public static double stratVariability = 0.01;
	@Parameter(name = "seed")
	public static int seed = 1;
	@Parameter(name = "qscale", optional = true)
	public double qscale = 0.02;
	@Parameter(name = "facilitySunk", optional = true)
	public double facilitySunk = 0.0;
	@Parameter(name = "facilityFixed", optional = true)
	public double facilityFixed = 0.0;
	@Parameter(name = "facilityMarginalStorage", optional = true)
	public double facilityMarginalStorage = 0.0;
	@Parameter(name = "facilityMarginalTrans", optional = true)
	public double facilityMarginalTrans = 0.0;
	@Parameter(name = "analystProfile", optional = true)
	public Profile analystProfile = Profile.SUSTAINABLE;
	@Parameter(name = "consumerProfile", optional = true)
	public Profile consumerProfile = Profile.SUSTAINABLE;
	@Parameter(name = "initiatorProfile", optional = true)
	public Profile initiatorProfile = Profile.SUSTAINABLE;
	@Parameter(name = "greedyConsumers", optional = true)
	public int greedyConsumers = 0;
	@Parameter(name = "prune", optional = true)
	public boolean prune = false;

	public int iaCount = 10;
	public int raCount = 5;

	public static Class<? extends Game> game;

	InstitutionService inst;
	EInstSession session;

	public GameSimulation(Set<AbstractModule> modules) {
		super(modules);
		EInstSession.USE_KB_CACHE = true;
	}

	@Inject
	public void setServiceProvider(EnvironmentServiceProvider serviceProvider) {
		try {
			this.inst = serviceProvider
					.getEnvironmentService(InstitutionService.class);
			this.session = this.inst.getSession();
		} catch (UnavailableServiceException e) {
			logger.fatal("No institution service", e);
		}
	}

	@Inject
	public void setEventBus(EventBus eb) {
		eb.subscribe(this);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Set<AbstractModule> getModules() {
		try {
			game = (Class<? extends Game>) Class.forName(gameClass);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
		Set<AbstractModule> modules = new HashSet<AbstractModule>();
		modules.add(new AbstractEnvironmentModule()
				.addActionHandler(game)
				.addParticipantGlobalEnvironmentService(game)
				.addParticipantGlobalEnvironmentService(
						InstitutionService.class));
		modules.add(NetworkModule.noNetworkModule());
		/*
		 * modules.add(new AbstractModule() {
		 * 
		 * @Override protected void configure() {
		 * bind(StorageService.class).to(KCStorage.class); } });
		 */
		return modules;
	}

	@Override
	protected void addToScenario(Scenario s) {
		this.session.LOG_WM = false;
		s.addPlugin(this.inst);

		// banditExprSetup(s);
		// facilityExpr(s);
		// facilitySubExpr(s);
		// subCollectiveExpr(s);
		payForProvisions(s);
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

	void banditExprSetup(Scenario s) {
		Set<String> contribRole = RoleOf.roleSet("gatherer");
		Set<String> extractRole = RoleOf.roleSet("consumer");
		Institution i = new DataInstitution("i1", 0);
		Pool p = new Pool(i, contribRole, extractRole, RoleOf.roleSet(),
				new ArtifactTypeMatcher(Measured.class));
		session.insert(p);

		for (int n = 0; n < gathererLimit; n++) {
			AbstractAgent ag = new PlayerAgent("rand" + n,
					new RandomPredictor(), false);
			s.addParticipant(ag);
			session.insert(new RoleOf(ag, i, "gatherer"));
		}

		// Reinforcement learning agents
		for (double q0 : new Double[] { 0.5 }) {
			for (double alpha : new Double[] { 0.5 }) {
				for (double epsilon : new Double[] { 0.01 }) {
					String name = (q0 == 0.5 ? "pess" : "opt");
					name += "-" + alpha + "-" + epsilon;
					s.addParticipant(new PlayerAgent(name, new GreedyPredictor(
							q0, alpha, epsilon), false));
					AbstractAgent ag = PlayerAgent.knowledgePlayer(name,
							new GreedyPredictor(q0, alpha, epsilon));
					session.insert(new RoleOf(ag, i, "consumer"));
					s.addParticipant(ag);
				}
			}
		}
	}

	void facilityExpr(Scenario s) {
		Institution i = new InstitutionBuilder(session, "i1", 100, "initiator",
				"consumer")
				.addMeasuredPool(0)
				.addPredictorPool(0)
				.addFacility(facilitySunk, facilityFixed,
						facilityMarginalStorage, facilityMarginalTrans).build();
		for (int n = 0; n < gathererLimit; n++) {
			AbstractAgent ag = PlayerAgent.dumbPlayer("p" + n, badPredictor());
			addAgent(s, ag, 10, i, "gatherer", "consumer");
		}

		AbstractAgent ag = NonPlayerAgent.analystAgent("a1",
				goodPredictor("a1"), analystProfile);
		if (prune)
			addAgent(s, ag, 0, i, "analyst", "initiator", "manager");
		else
			addAgent(s, ag, 0, i, "analyst", "initiator");

		addAgent(s, PlayerAgent.knowledgePlayer("ind", goodPredictor("ind")),
				0, null);
		addAgent(s, PlayerAgent.knowledgePlayer("rand", badPredictor()), 0,
				null);
	}

	void facilitySubExpr(Scenario s) {
		Institution i = new InstitutionBuilder(session, "i1", 100)
				.addMeasuredPool(0)
				.addPredictorPool(0)
				.addDynamicSubscription(RoleOf.roleSet("consumer"), 0.0,
						RoleOf.roleSet("initiator"),
						RoleOf.roleSet("initiator"), 0.1)
				.addFacility(facilitySunk, facilityFixed,
						facilityMarginalStorage, facilityMarginalTrans).build();
		for (int n = 0; n < gathererLimit; n++) {
			AbstractAgent ag = PlayerAgent.dumbPlayer("p" + n, badPredictor());
			addAgent(s, ag, 10, i, "gatherer", "consumer");
		}

		AbstractAgent ag = NonPlayerAgent.analystAgent("a1",
				goodPredictor("a1"), analystProfile);
		addAgent(s, ag, 0, i, "analyst", "initiator");

		addAgent(s, PlayerAgent.knowledgePlayer("ind", goodPredictor("ind")),
				0, null);
		addAgent(s, PlayerAgent.knowledgePlayer("rand", badPredictor()), 0,
				null);
	}

	void subCollectiveExpr(Scenario s) {
		Institution i = new InstitutionBuilder(session, "i1", 100)
				.addMeasuredPool(0)
				.addPredictorPool(0)
				.addDynamicSubscription(RoleOf.roleSet("consumer"), 0.0,
						RoleOf.roleSet("initiator"),
						RoleOf.roleSet("initiator", "consumer"), 0.1)
				.addFacility(facilitySunk, facilityFixed,
						facilityMarginalStorage, facilityMarginalTrans).build();
		for (int n = 0; n < gathererLimit; n++) {
			Profile pro = n < greedyConsumers ? Profile.GREEDY
					: consumerProfile;
			AbstractAgent ag = PlayerAgent.dumbPlayer("p" + n, badPredictor(),
					pro);
			addAgent(s, ag, 10, i, "gatherer", "consumer");
		}

		AbstractAgent ag = NonPlayerAgent.analystAgent("a1",
				goodPredictor("a1"), analystProfile);
		addAgent(s, ag, 0, i, "analyst", "initiator", "manager");

		addAgent(s, PlayerAgent.knowledgePlayer("ind", goodPredictor("ind")),
				0, null);
		addAgent(s, PlayerAgent.knowledgePlayer("rand", badPredictor()), 0,
				null);
	}

	void payForProvisions(Scenario s) {
		Institution i = new InstitutionBuilder(session, "i1", 100, "initiator")
				.addMeasuredPool()
				.end()
				.addPredictorPool()
				// .setPayOnAppropriation(0.15)
				// .withFee("consumer", 0.15)
				.dynamicPayOnAppropriation(Roles.set("consumer"), 0.4,
						Roles.set("initiator"),
						Roles.set("analyst", "consumer"), 0.1, true)
				.end()
				.addDynamicSubscription(Roles.set("consumer"), 0.0,
						Roles.set("initiator"), Roles.set("initiator"), 0.1)
				.addFacility(facilitySunk, facilityFixed,
						facilityMarginalStorage, facilityMarginalTrans).build();

		for (int n = 0; n < gathererLimit; n++) {
			AbstractAgent ag = PlayerAgent.dumbPlayer("p" + n, badPredictor(),
					consumerProfile);
			addAgent(s, ag, 10, i, "gatherer", "consumer");
		}
		AbstractAgent ag = NonPlayerAgent.analystAgent("a1",
				goodPredictor("a1"), analystProfile);
		addAgent(s, ag, 0, i, "analyst");
		AbstractAgent initiator = NonPlayerAgent.initiatorAgent("c1",
				initiatorProfile);
		addAgent(s, initiator, 0, i, "initiator", "manager");

		addAgent(s, PlayerAgent.knowledgePlayer("ind", goodPredictor("ind")),
				0, null);
	}

	Predictor badPredictor() {
		if (gameClass.equals("kc.games.KnowledgeGame"))
			return new PseudoPredictor();
		return new RandomPredictor();
	}

	Predictor goodPredictor(String name) {
		if (gameClass.equals("kc.games.KnowledgeGame"))
			return new PseudoPredictor(name);
		return new GreedyPredictor(0.5, 0.1, 0.1);
	}

}
