package kc.agents;

import java.util.Queue;
import java.util.UUID;

import kc.Game;
import kc.GameSimulation;
import kc.InstitutionService;
import kc.Measured;
import kc.prediction.Predictor;
import kc.prediction.RandomPredictor;

import org.apache.log4j.Logger;

import uk.ac.imperial.einst.Actor;
import uk.ac.imperial.presage2.core.environment.UnavailableServiceException;
import uk.ac.imperial.presage2.core.messaging.Input;
import uk.ac.imperial.presage2.util.participant.AbstractParticipant;

public class AbstractAgent extends AbstractParticipant implements Actor {

	protected final Logger logger;
	protected Game game;
	InstitutionService inst;
	Predictor predictor;
	Queue<Measured> incMeasured;

	public AbstractAgent(UUID id, String name) {
		super(id, name);
		logger = Logger.getLogger(name);
		this.predictor = new RandomPredictor();
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
		this.inst.registerAgent(this);
		this.incMeasured = game.getMeasuredQueue(getID());
	}

	@Override
	protected void processInput(Input in) {
		// TODO Auto-generated method stub

	}

	@Override
	public String toString() {
		return this.getName();
	}

}
