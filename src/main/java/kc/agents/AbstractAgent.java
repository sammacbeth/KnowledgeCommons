package kc.agents;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;

import kc.Game;
import kc.GameSimulation;
import kc.InstitutionService;

import org.apache.log4j.Logger;

import uk.ac.imperial.einst.Actor;
import uk.ac.imperial.einst.ipower.Obl;
import uk.ac.imperial.einst.ipower.ObligationReactive;
import uk.ac.imperial.presage2.core.Action;
import uk.ac.imperial.presage2.core.environment.ActionHandlingException;
import uk.ac.imperial.presage2.core.environment.UnavailableServiceException;
import uk.ac.imperial.presage2.core.messaging.Input;
import uk.ac.imperial.presage2.util.participant.AbstractParticipant;

public class AbstractAgent extends AbstractParticipant implements Actor,
		ObligationReactive {

	protected final Logger logger;
	protected Game game;
	InstitutionService inst;

	Map<String, Behaviour> behaviours = new HashMap<String, Behaviour>();
	GathererBehaviour gathering;
	ConsumerBehaviour consuming;
	GameplayBehaviour gameplay;

	Queue<Obl> obligations = new LinkedList<Obl>();

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
		for (Behaviour b : behaviours.values()) {
			b.initialise();
		}
	}

	@Override
	protected void processInput(Input in) {
	}

	@Override
	public void incrementTime() {
		for (Behaviour b : behaviours.values()) {
			b.doBehaviour();
		}
		super.incrementTime();
	}

	protected void processObligations() {
		while (!obligations.isEmpty()) {
			inst.act(obligations.poll().getAction());
		}
	}

	@Override
	public String toString() {
		return this.getName();
	}

	@Override
	public void onObligation(Obl obl) {
		obligations.add(obl);
	}

	void act(Action act) {
		try {
			environment.act(act, getID(), authkey);
		} catch (ActionHandlingException e) {
			logger.warn(e);
		}
	}

	void addAdditionalBehaviour(Behaviour b) {
		behaviours.put(b.getType(), b);
	}

	void addBehaviour(ConsumerBehaviour b) {
		this.consuming = b;
		addAdditionalBehaviour(b);
	}

	void addBehaviour(GameplayBehaviour b) {
		this.gameplay = b;
		addAdditionalBehaviour(b);
	}

	void addBehaviour(GathererBehaviour b) {
		this.gathering = b;
		addAdditionalBehaviour(b);
	}

}
