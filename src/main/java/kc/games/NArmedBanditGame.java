package kc.games;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.Vector;

import kc.Game;
import kc.State;
import kc.Strategy;

import org.uncommons.maths.number.AdjustableNumberGenerator;
import org.uncommons.maths.number.ConstantGenerator;
import org.uncommons.maths.number.NumberGenerator;
import org.uncommons.maths.random.GaussianGenerator;
import org.uncommons.maths.random.SeedException;
import org.uncommons.maths.random.SeedGenerator;
import org.uncommons.maths.random.XORShiftRNG;

import uk.ac.imperial.presage2.core.environment.EnvironmentServiceProvider;
import uk.ac.imperial.presage2.core.environment.EnvironmentSharedStateAccess;
import uk.ac.imperial.presage2.core.event.EventListener;
import uk.ac.imperial.presage2.core.simulator.EndOfTimeCycle;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

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
	public NArmedBanditGame(EnvironmentSharedStateAccess sharedState,
			EnvironmentServiceProvider serviceProvider,
			@Named("params.numStrategies") int numStrat,
			@Named("params.stratVariability") double var,
			@Named("params.stratVolatility") double vol,
			@Named("params.seed") int seed) throws SeedException {
		super(sharedState, serviceProvider);
		this.strategies = new ArrayList<Strategy>();
		this.bandits = new Vector<NumberGenerator<Double>>();
		this.banditMeans = new Vector<AdjustableNumberGenerator<Double>>();
		this.trends = new double[numStrat];
		this.maxChangeRate = var;
		this.maxStd = vol;

		// seeded random
		Random rnd = new XORShiftRNG(new BadSeedGenerator(seed));
		for (int i = 0; i < numStrat; i++) {
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

		currentRound = new double[numStrat];
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
		double meanSum = 0;
		double[] means = new double[trends.length];
		for (int i = 0; i < trends.length; i++) {
			AdjustableNumberGenerator<Double> ng = banditMeans.get(i);
			means[i] = ng.nextValue() + trends[i];
			meanSum += means[i];
			if ((means[i] > 1.0 && trends[i] > 0)
					|| (means[i] < 0.0 && trends[i] < 0)) {
				trends[i] = -1 * trends[i];
			}
		}
		// normalise means
		double normFactor = 2 * meanSum / means.length;
		meanSum = 0;
		for (int i = 0; i < means.length; i++) {
			AdjustableNumberGenerator<Double> ng = banditMeans.get(i);
			means[i] /= normFactor;
			meanSum += means[i];
			ng.setValue(means[i]);
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

	class BadSeedGenerator implements SeedGenerator {

		final long seed;

		public BadSeedGenerator(long seed) {
			super();
			this.seed = seed;
		}

		@Override
		public byte[] generateSeed(int arg0) throws SeedException {
			return ByteBuffer.allocate(arg0).putLong(this.seed).array();
		}

	}

}
