package kc;

import java.util.HashSet;
import java.util.Set;

import kc.agents.AbstractAgent;
import kc.agents.NonPlayerAgent;
import kc.agents.PlayerAgent;
import kc.prediction.GreedyPredictor;
import kc.prediction.Predictor;
import kc.prediction.RandomPredictor;
import uk.ac.imperial.einst.EInstSession;
import uk.ac.imperial.einst.Institution;
import uk.ac.imperial.einst.access.RoleOf;
import uk.ac.imperial.einst.micropay.Account;
import uk.ac.imperial.einst.resource.ArtifactTypeMatcher;
import uk.ac.imperial.einst.resource.Pool;
import uk.ac.imperial.einst.resource.facility.Facility;
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

	@Parameter(name = "numStrategies")
	public static int numStrategies;
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

	public int iaCount = 10;
	public int raCount = 5;

	public static Class<? extends Game> game;

	InstitutionService inst;
	EInstSession session;

	public GameSimulation(Set<AbstractModule> modules) {
		super(modules);
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
		this.session.LOG_WM = true;
		s.addPlugin(this.inst);

		// banditExprSetup(s);
		Set<String> gatherer = new HashSet<String>();
		gatherer.add("gatherer");
		Set<String> consumer = new HashSet<String>();
		consumer.add("consumer");
		Set<String> analyst = new HashSet<String>();
		analyst.add("analyst");

		Institution i = new DataInstitution("i1");
		// measured pool
		Set<Pool> pools = new HashSet<Pool>();
		pools.add(new Pool(i, gatherer, analyst, new ArtifactTypeMatcher(
				Measured.class)));
		pools.add(new Pool(i, analyst, consumer, new ArtifactTypeMatcher(
				Predictor.class)));
		for (Pool p : pools) {
			session.insert(p);
		}
		session.insert(new Facility(i, pools, 10, 1, 0.01));
		session.insert(new Account(i, 0));

		for (int n = 0; n < 2; n++) {
			AbstractAgent ag = PlayerAgent.dumbPlayer("p" + n);
			addAgent(s, ag, 0, i, "gatherer", "consumer");
		}

		AbstractAgent ag = NonPlayerAgent.analystAgent("a1",
				new GreedyPredictor(0.5, 0.1, 0.1));
		addAgent(s, ag, 100, i, "analyst");
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
		Set<String> contribRole = new HashSet<String>();
		contribRole.add("gatherer");
		Set<String> extractRole = new HashSet<String>();
		extractRole.add("consumer");
		Institution i = new DataInstitution("i1");
		Pool p = new Pool(i, contribRole, extractRole, new ArtifactTypeMatcher(
				Measured.class));
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

}
