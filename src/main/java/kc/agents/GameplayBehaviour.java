package kc.agents;

import java.util.Queue;

import kc.Measured;
import kc.Strategy;
import kc.prediction.Predictor;

public class GameplayBehaviour implements Behaviour {

	private static final String TYPE = "gameplay";
	final AbstractAgent s;
	Predictor predictor;
	Queue<Measured> incMeasured;
	boolean measure = true;

	public GameplayBehaviour(AbstractAgent self, Predictor predictor) {
		super();
		this.s = self;
		this.predictor = predictor;
	}

	@Override
	public String getType() {
		return TYPE;
	}

	@Override
	public void initialise() {
		this.incMeasured = s.game.getMeasuredQueue(s.getID());
	}

	@Override
	public void doBehaviour() {
		s.logger.info("Account is: " + s.game.getScore(s.getID()));

		while (!incMeasured.isEmpty()) {
			this.predictor.addTrainingData(incMeasured.poll());
		}

		Strategy chosen = this.predictor.actionSelection(
				s.game.getState(s.getID()), s.game.getStrategies());
		chosen.setMeasure(measure);

		// play the game
		s.logger.info("Strategy: " + chosen + "");
		s.act(chosen);
	}

}
