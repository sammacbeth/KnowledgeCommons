package kc;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;

import kc.util.KCStorage;
import kc.util.MultiUserQueue;

import org.apache.log4j.Logger;

import uk.ac.imperial.einst.micropay.Account;
import uk.ac.imperial.einst.micropay.MicroPayments;
import uk.ac.imperial.presage2.core.Action;
import uk.ac.imperial.presage2.core.environment.ActionHandler;
import uk.ac.imperial.presage2.core.environment.ActionHandlingException;
import uk.ac.imperial.presage2.core.environment.EnvironmentRegistrationRequest;
import uk.ac.imperial.presage2.core.environment.EnvironmentService;
import uk.ac.imperial.presage2.core.environment.EnvironmentServiceProvider;
import uk.ac.imperial.presage2.core.environment.EnvironmentSharedStateAccess;
import uk.ac.imperial.presage2.core.environment.StateTransformer;
import uk.ac.imperial.presage2.core.environment.UnavailableServiceException;
import uk.ac.imperial.presage2.core.event.EventBus;
import uk.ac.imperial.presage2.core.messaging.Input;
import uk.ac.imperial.presage2.core.simulator.SimTime;

import com.google.inject.Inject;

public abstract class Game extends EnvironmentService implements ActionHandler {

	protected final Logger logger = Logger.getLogger(Game.class);
	Map<UUID, String> names = new HashMap<UUID, String>();
	Map<UUID, Account> accounts = new HashMap<UUID, Account>();
	Map<UUID, MultiUserQueue<Measured>> measured = Collections
			.synchronizedMap(new HashMap<UUID, MultiUserQueue<Measured>>());

	final EnvironmentServiceProvider serviceProvider;
	MicroPayments accounting = null;

	protected KCStorage sto = null;

	@Inject
	public Game(EnvironmentSharedStateAccess sharedState,
			EnvironmentServiceProvider serviceProvider) {
		super(sharedState);
		this.serviceProvider = serviceProvider;
	}

	@Inject(optional = true)
	public void setDb(KCStorage sto) {
		this.sto = sto;
	}

	public MicroPayments accounting() {
		if (accounting == null) {
			try {
				this.accounting = this.serviceProvider
						.getEnvironmentService(InstitutionService.class).payments;
			} catch (UnavailableServiceException e) {
				throw new RuntimeException(e);
			}
		}
		return accounting;
	}

	@Override
	public boolean canHandle(Action action) {
		return action instanceof Strategy;
	}

	@Override
	public void registerParticipant(EnvironmentRegistrationRequest req) {
		super.registerParticipant(req);
		Account a = accounting().getAccount(req.getParticipant());
		if (a == null) {
			a = accounting().createAccount(req.getParticipant(), 0, 0);
		}
		sharedState.create("account", req.getParticipantID(), new Double(0));
		sharedState.create("measured", req.getParticipantID(),
				new LinkedList<Measured>());
		names.put(req.getParticipantID(), req.getParticipant().getName());
		accounts.put(req.getParticipantID(), a);
	}

	@Override
	public Input handle(Action action, final UUID actor)
			throws ActionHandlingException {
		if (action instanceof Strategy) {
			final Strategy s = (Strategy) action;
			final double u = getReward(actor, s);
			final int time = SimTime.get().intValue();

			this.sharedState.change("account", actor, new StateTransformer() {
				@Override
				public Serializable transform(Serializable state) {
					// drools way
					Account a = accounts.get(actor);
					a.setBalance(a.getBalance() + u);

					logger.info(s +" got reward: "+ u +"; "+ a);
					// sharedstate way
					double account = (Double) state;
					if (Double.isNaN(account)) {
						logger.warn(account);
					} else {
						account += u;
					}
					if (sto != null) {
						sto.insertPlayerGameRound(time, names.get(actor),
								s.getId(), u, a.getBalance());
					}
					return account;
				}
			});

			if (s.measure) {
				getMeasuredQueue(actor).publish(
						new Measured(names.get(actor), getState(actor), s
								.getId(), u, time));
			}
		}
		return null;
	}

	protected abstract double getReward(UUID actor, Strategy s);

	public abstract State getState(UUID actor);

	public abstract List<Strategy> getStrategies();

	public double getScore(UUID id) {
		double score = (Double) this.sharedState.get("account", id);
		if (Double.isNaN(score)) {
			logger.warn(score);
		}
		return score;
	}

	private MultiUserQueue<Measured> getMeasuredQueue(UUID actor) {
		if (!measured.containsKey(actor))
			measured.put(actor, new MultiUserQueue<Measured>());
		return measured.get(actor);
	}

	public Queue<Measured> measuredQueueSubscribe(UUID actor) {
		return getMeasuredQueue(actor).subscribe();
	}

	@Inject(optional = true)
	public void setEventBus(EventBus eb) {
		eb.subscribe(this);
	}

}
