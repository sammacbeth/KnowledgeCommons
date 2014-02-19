package kc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import kc.agents.AbstractAgent;
import kc.agents.GathererAgent;
import kc.prediction.GreedyPredictor;
import kc.prediction.MeanPredictor;
import kc.prediction.RandomPredictor;
import kc.util.KCStorage;
import uk.ac.imperial.einst.EInstSession;
import uk.ac.imperial.einst.Institution;
import uk.ac.imperial.einst.access.RoleOf;
import uk.ac.imperial.einst.resource.ArtifactTypeMatcher;
import uk.ac.imperial.einst.resource.Pool;
import uk.ac.imperial.presage2.core.db.StorageService;
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
		modules.add(new AbstractModule() {
			@Override
			protected void configure() {
				bind(StorageService.class).to(KCStorage.class);
			}
		});
		return modules;
	}

	@Override
	protected void addToScenario(Scenario s) {
		this.session.LOG_WM = false;

		List<AbstractAgent> individuals = new ArrayList<AbstractAgent>(10);
		List<AbstractAgent> gatherers = new ArrayList<AbstractAgent>(10);
		gatherers.add(new GathererAgent("rand", new RandomPredictor(), false));

		individuals.add(new GathererAgent("mean", new MeanPredictor(), false));
		gatherers.add(new GathererAgent("mean-con", new MeanPredictor(), true));

		// Reinforcement learning agents
		for (double q0 : new Double[] { 0.5, 1.0 }) {
			for (double alpha : new Double[] { 0.5 }) {
				for (double epsilon : new Double[] { 0.005, 0.01, 0.00 }) {
					String name = (q0 == 0.5 ? "pess" : "opt");
					name += "-" + alpha + "-" + epsilon;
					individuals.add(new GathererAgent(name,
							new GreedyPredictor(q0, alpha, epsilon), false));
					gatherers.add(new GathererAgent(name + "-con",
							new GreedyPredictor(q0, alpha, epsilon), true));
				}
			}
		}

		Set<String> contribRole = new HashSet<String>();
		contribRole.add("gatherer");
		Set<String> extractRole = new HashSet<String>();
		extractRole.add("consumer");
		Institution i = new DataInstitution("i1");
		Pool p = new Pool(i, contribRole, extractRole, new ArtifactTypeMatcher(
				Measured.class));
		session.insert(p);

		for (AbstractAgent ag : individuals) {
			s.addParticipant(ag);
			if (gathererLimit-- > 0)
				session.insert(new RoleOf(ag, i, "gatherer"));
		}
		for (AbstractAgent ag : gatherers) {
			s.addParticipant(ag);
			if (gathererLimit-- > 0)
				session.insert(new RoleOf(ag, i, "gatherer"));
			session.insert(new RoleOf(ag, i, "consumer"));
		}

		s.addPlugin(this.inst);
	}

	@EventListener
	public void incrementTime(EndOfTimeCycle e) {
		session.incrementTime();
	}
}
