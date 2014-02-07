package kc;

import java.util.HashSet;
import java.util.Set;

import kc.agents.GathererAgent;
import kc.prediction.GreedyPredictor;
import kc.prediction.MeanPredictor;
import kc.prediction.RandomPredictor;
import uk.ac.imperial.einst.EInstSession;
import uk.ac.imperial.presage2.core.TimeDriven;
import uk.ac.imperial.presage2.core.environment.EnvironmentServiceProvider;
import uk.ac.imperial.presage2.core.environment.UnavailableServiceException;
import uk.ac.imperial.presage2.core.simulator.InjectedSimulation;
import uk.ac.imperial.presage2.core.simulator.Parameter;
import uk.ac.imperial.presage2.core.simulator.Scenario;
import uk.ac.imperial.presage2.core.util.random.Random;
import uk.ac.imperial.presage2.util.environment.AbstractEnvironmentModule;
import uk.ac.imperial.presage2.util.network.NetworkModule;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;

public class GameSimulation extends InjectedSimulation implements TimeDriven {

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
		s.addTimeDriven(this);

		s.addParticipant(new GathererAgent(Random.randomUUID(), "a1",
				new RandomPredictor()));
		s.addParticipant(new GathererAgent(Random.randomUUID(), "a2",
				new MeanPredictor()));
		s.addParticipant(new GathererAgent(Random.randomUUID(), "a3",
				new GreedyPredictor(1.0, 0.1, 0.1)));
	}

	@Override
	public void incrementTime() {
		session.incrementTime();
	}
}
