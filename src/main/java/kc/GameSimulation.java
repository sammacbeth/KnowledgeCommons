package kc;

import java.util.HashSet;
import java.util.Set;

import kc.agents.AbstractAgent;
import kc.agents.GathererAgent;
import kc.prediction.GreedyPredictor;
import kc.prediction.MeanPredictor;
import kc.prediction.RandomPredictor;
import uk.ac.imperial.einst.EInstSession;
import uk.ac.imperial.einst.Institution;
import uk.ac.imperial.einst.access.RoleOf;
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
import uk.ac.imperial.presage2.core.util.random.Random;
import uk.ac.imperial.presage2.util.environment.AbstractEnvironmentModule;
import uk.ac.imperial.presage2.util.network.NetworkModule;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;

public class GameSimulation extends InjectedSimulation {

	@Parameter(name = "numStrategies")
	public static int numStrategies;
	@Parameter(name = "gameClass")
	public String gameClass;

	public int iaCount = 10;
	public int raCount = 5;

	public static Class<? extends Game> game;

	EInstSession session;

	public GameSimulation(Set<AbstractModule> modules) {
		super(modules);
	}

	@Inject
	public void setServiceProvider(EnvironmentServiceProvider serviceProvider) {
		try {
			this.session = serviceProvider.getEnvironmentService(
					InstitutionService.class).getSession();
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
		return modules;
	}

	@Override
	protected void addToScenario(Scenario s) {
		this.session.LOG_WM = false;

		AbstractAgent a1 = new GathererAgent(Random.randomUUID(), "a1",
				new RandomPredictor(), false);
		AbstractAgent a2 = new GathererAgent(Random.randomUUID(), "a2",
				new MeanPredictor(), true);
		AbstractAgent a3 = new GathererAgent(Random.randomUUID(), "a3",
				new GreedyPredictor(1.0, 0.1, 0.1), true);
		AbstractAgent a4 = new GathererAgent(Random.randomUUID(), "a4",
				new MeanPredictor(), true);
		AbstractAgent a5 = new GathererAgent(Random.randomUUID(), "a5",
				new GreedyPredictor(1.0, 0.1, 0.1), true);
		s.addParticipant(a1);
		s.addParticipant(a2);
		s.addParticipant(a3);
		s.addParticipant(a4);
		s.addParticipant(a5);

		Set<String> roles = new HashSet<String>();
		roles.add("gatherer");
		Institution i = new DataInstitution("i1");
		Pool p = new Pool(i, roles, roles, new ArtifactTypeMatcher(
				Measured.class));
		session.insert(p);

		session.insert(new RoleOf(a1, i, "gatherer"));
		session.insert(new RoleOf(a2, i, "gatherer"));
		session.insert(new RoleOf(a3, i, "gatherer"));
	}

	@EventListener
	public void incrementTime(EndOfTimeCycle e) {
		session.incrementTime();
	}
}
