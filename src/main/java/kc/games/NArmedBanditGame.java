package kc.games;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.Vector;

import kc.Game;
import kc.GameSimulation;
import kc.State;
import kc.Strategy;

import org.uncommons.maths.number.AdjustableNumberGenerator;
import org.uncommons.maths.number.ConstantGenerator;
import org.uncommons.maths.number.NumberGenerator;
import org.uncommons.maths.random.GaussianGenerator;
import org.uncommons.maths.random.XORShiftRNG;

import uk.ac.imperial.presage2.core.environment.EnvironmentSharedStateAccess;
import uk.ac.imperial.presage2.core.event.EventListener;
import uk.ac.imperial.presage2.core.simulator.EndOfTimeCycle;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class NArmedBanditGame extends Game {

	final List<Strategy> strategies;
	final Vector<NumberGenerator<Double>> bandits;
	final Vector<AdjustableNumberGenerator<Double>> banditMeans;
	final double[] trends;

	double[] currentRound;

	final double maxChangeRate;
	final double maxStd;

	int roundNumber = 0;

	@Inject
	public NArmedBanditGame(EnvironmentSharedStateAccess sharedState) {
		super(sharedState);
		this.strategies = new ArrayList<Strategy>();
		this.bandits = new Vector<NumberGenerator<Double>>();
		this.banditMeans = new Vector<AdjustableNumberGenerator<Double>>();
		this.trends = new double[GameSimulation.numStrategies];
		this.maxChangeRate = GameSimulation.stratVariability;
		this.maxStd = GameSimulation.stratVolatility;

		Random rnd = new XORShiftRNG();
		for (int i = 0; i < GameSimulation.numStrategies; i++) {
			Strategy s = new Strategy(i);
			this.strategies.add(s);
			AdjustableNumberGenerator<Double> mean = new AdjustableNumberGenerator<Double>(
					rnd.nextDouble());
			NumberGenerator<Double> std = new ConstantGenerator<Double>(
					rnd.nextDouble() * maxStd);
			this.bandits.add(s.getId(), new GaussianGenerator(mean, std, rnd));
			this.banditMeans.add(s.getId(), mean);
			this.trends[i] = (rnd.nextDouble() * maxChangeRate * 2)
					- maxChangeRate;
		}

		currentRound = new double[GameSimulation.numStrategies];
		generateRound();
	}

	private void generateRound() {
		// generate values from distribution
		double max = 0;
		for (int i = 0; i < currentRound.length; i++) {
			final double pay = Math.max(0, bandits.get(i).nextValue());
			currentRound[i] = pay;
			if (pay > max)
				max = pay;
		}
		// normalise against maximum
		for (int i = 0; i < currentRound.length; i++) {
			currentRound[i] /= max;
			if (sto != null)
				sto.insertGameRound(roundNumber, i, currentRound[i]);
		}

	}

	private void followTrends() {
		for (int i = 0; i < trends.length; i++) {
			AdjustableNumberGenerator<Double> ng = banditMeans.get(i);
			double mean = ng.nextValue() + trends[i];
			if (mean > 1.0 || mean < 0.0) {
				trends[i] = -1 * trends[i];
			}
			ng.setValue(mean);
		}
	}

	@EventListener
	public void endOfTimeStep(EndOfTimeCycle e) {
		roundNumber++;
		generateRound();
		followTrends();
	}

	@Override
	protected double getReward(UUID actor, Strategy s) {
		return currentRound[s.getId()];
	}

	@Override
	public State getState(UUID actor) {
		return State.NONE;
	}

	@Override
	public List<Strategy> getStrategies() {
		return strategies;
	}

}
