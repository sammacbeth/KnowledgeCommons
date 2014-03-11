package kc.agents;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import kc.Game;
import kc.GameSimulation;
import kc.InstitutionService;
import kc.Strategy;
import kc.games.NArmedBanditGame;
import kc.prediction.RandomPredictor;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.Test;
import org.uncommons.maths.random.SeedException;

import uk.ac.imperial.einst.UnavailableModuleException;
import uk.ac.imperial.presage2.core.Action;
import uk.ac.imperial.presage2.core.environment.ActionHandler;
import uk.ac.imperial.presage2.core.environment.ActionHandlingException;
import uk.ac.imperial.presage2.core.environment.EnvironmentConnector;
import uk.ac.imperial.presage2.core.environment.EnvironmentRegistrationRequest;
import uk.ac.imperial.presage2.core.environment.EnvironmentRegistrationResponse;
import uk.ac.imperial.presage2.core.environment.EnvironmentService;
import uk.ac.imperial.presage2.core.environment.EnvironmentSharedStateAccess;
import uk.ac.imperial.presage2.core.messaging.Input;
import uk.ac.imperial.presage2.core.network.Message;
import uk.ac.imperial.presage2.core.network.NetworkAddress;
import uk.ac.imperial.presage2.core.network.NetworkConnector;
import uk.ac.imperial.presage2.core.network.NetworkConnectorFactory;
import uk.ac.imperial.presage2.core.util.random.Random;
import uk.ac.imperial.presage2.util.environment.MappedSharedState;
import uk.ac.imperial.presage2.util.participant.AbstractParticipant;

public class TestBehaviours {

	Mockery context;
	EnvironmentSharedStateAccess sharedState;
	EnvironmentConnector env;

	@Before
	public void setUp() throws Exception {
		context = new Mockery();
		sharedState = new MappedSharedState();
		env = context.mock(EnvironmentConnector.class);
	}


	@Test
	public void testGamePlayBehaviour() throws NoSuchMethodException,
			SecurityException, InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException,
			UnavailableModuleException, SeedException, ActionHandlingException {

		final PlayerAgent a = new PlayerAgent("test");
		a.addBehaviour(a.new GameplayBehaviour(new RandomPredictor()));

		// mock components
		final EnvironmentConnector env = mockEnvironment();
		InstitutionService inst = new InstitutionService(sharedState);

		final UUID authkey = initialiseParticipant(a, inst, getGame());

		// does a strategy each time increment.
		context.checking(new Expectations() {
			{
				oneOf(env).act(with(any(Strategy.class)),
						with(equal(a.getID())), with(equal(authkey)));
				will(returnValue(null));
			}
		});
		a.incrementTime();
		context.checking(new Expectations() {
			{
				oneOf(env).act(with(any(Strategy.class)),
						with(equal(a.getID())), with(equal(authkey)));
				will(returnValue(null));
			}
		});
		a.incrementTime();
	}


	Game getGame() throws SeedException {
		Game game = new NArmedBanditGame(sharedState, 1, 0, 0,
				Random.randomInt());
		GameSimulation.game = NArmedBanditGame.class;
		return game;
	}

	EnvironmentConnector mockEnvironment() {
		return this.env;
	}

	UUID initialiseParticipant(final AbstractParticipant p,
			EnvironmentService... services) {
		final UUID authkey = Random.randomUUID();
		final Set<EnvironmentService> envServices = new HashSet<EnvironmentService>();
		for (EnvironmentService s : services) {
			envServices.add(s);
		}
		final Matcher<EnvironmentRegistrationRequest> requestMatcher = new RegRequestMatcher(
				p.getID());
		context.checking(new Expectations() {
			{
				oneOf(env).register(with(requestMatcher));
				will(returnValue(new EnvironmentRegistrationResponse(authkey,
						envServices)));
			}
		});
		p.initialiseEnvironment(env);
		p.initialiseNetwork(new StubNetworkConnectorFactory());
		p.initialise();

		return authkey;
	}

	class RegRequestMatcher extends
			TypeSafeMatcher<EnvironmentRegistrationRequest> {

		final UUID aid;

		RegRequestMatcher(UUID aid) {
			super();
			this.aid = aid;
		}

		@Override
		public void describeTo(Description description) {
			description.appendText("from agent id: " + aid);
		}

		@Override
		public boolean matchesSafely(EnvironmentRegistrationRequest item) {
			return item.getParticipantID().equals(aid);
		}

	}

	class MockGame extends NArmedBanditGame {

		ActionHandler delegate;

		public MockGame(EnvironmentSharedStateAccess sharedState,
				ActionHandler delegate, int numStrat, int seed)
				throws SeedException {
			super(sharedState, numStrat, 0, 0, seed);
			this.delegate = delegate;
			GameSimulation.game = this.getClass();
		}

		@Override
		public Input handle(Action action, UUID actor)
				throws ActionHandlingException {
			delegate.handle(action, actor);
			return null;
		}

	}

	class StubNetworkConnectorFactory implements NetworkConnectorFactory {

		@Override
		public NetworkConnector create(UUID id) {
			return new StubNetworkConnector(id);
		}

	}

	class StubNetworkConnector extends NetworkConnector {

		public StubNetworkConnector(UUID id) {
			super(null, new NetworkAddress(id));
		}

		@Override
		public List<Message> getMessages() {
			return Collections.emptyList();
		}

		@Override
		public Set<NetworkAddress> getConnectedNodes()
				throws UnsupportedOperationException {
			return Collections.emptySet();
		}

		@Override
		public void sendMessage(Message m) {
		}

		@Override
		public void deliverMessage(Message m) {
		}
	}
}
