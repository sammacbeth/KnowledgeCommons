package kc.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;

import uk.ac.imperial.presage2.core.cli.Experiment;
import uk.ac.imperial.presage2.core.cli.InvalidParametersException;
import uk.ac.imperial.presage2.core.cli.Presage2CLI;
import uk.ac.imperial.presage2.core.db.persistent.PersistentSimulation;

public class KCCLI extends Presage2CLI {

	private final Logger logger = Logger.getLogger(KCCLI.class);

	protected KCCLI() {
		super(KCCLI.class);
	}

	public static void main(String[] args) {
		Presage2CLI cli = new KCCLI();
		cli.invokeCommand(args);
	}

	@Command(name = "insert", description = "Insert a batch of simulations to run.")
	public void insert(String[] args) throws InvalidParametersException {
		Options options = new Options();

		Map<String, String> experiments = new HashMap<String, String>();
		experiments.put("bandits", "Get properties of the bandit game.");
		experiments.put("pseudo", "Get properties of the pseudo game.");

		OptionGroup exprOptions = new OptionGroup();
		for (String key : experiments.keySet()) {
			exprOptions.addOption(new Option(key, experiments.get(key)));
		}

		// check for experiment type argument
		if (args.length < 2 || !experiments.containsKey(args[1])) {
			options.addOptionGroup(exprOptions);
			HelpFormatter formatter = new HelpFormatter();
			formatter.setOptPrefix("");
			formatter.printHelp("presage2cli insert <experiment>", options,
					false);
			return;
		}

		// optional random seed arg
		options.addOption(
				"s",
				"seed",
				true,
				"Random seed to start with (subsequent repeats use incrementing seeds from this value)");

		int repeats = 0;
		try {
			repeats = Integer.parseInt(args[2]);
		} catch (ArrayIndexOutOfBoundsException e) {
			logger.warn("REPEATS argument missing");
		} catch (NumberFormatException e) {
			logger.warn("REPEATS argument is not a valid integer");
		}

		if (repeats <= 0) {
			HelpFormatter formatter = new HelpFormatter();
			// formatter.setOptPrefix("");
			formatter.printHelp("presage2cli insert " + args[1] + " REPEATS",
					options, true);
			return;
		}

		CommandLineParser parser = new GnuParser();
		CommandLine cmd;
		int seed = 0;
		try {
			cmd = parser.parse(options, args);
			seed = Integer.parseInt(cmd.getOptionValue("seed"));
		} catch (ParseException e) {
			e.printStackTrace();
			return;
		} catch (NumberFormatException e) {
		} catch (NullPointerException e) {
		}

		if (args[1].equalsIgnoreCase("bandits")) {
			bandits(repeats, seed);
		} else if (args[1].equalsIgnoreCase("pseudo")) {
			pseudo(repeats, seed);
		}

		stopDatabase();
	}

	private void bandits(int repeats, int seed)
			throws InvalidParametersException {
		Experiment bandits = new Experiment(
				"bandits",
				"band:%{p.numStrategies}:%{p.stratVariability}:%{p.stratVolatility}:%{p.gathererLimit}",
				"kc.GameSimulation", 100);
		bandits.addArrayParameter("gameClass",
				new String[] { "kc.games.NArmedBanditGame" });
		bandits.addArrayParameter("numStrategies", new String[] { "2", "4",
				"8", "16", "32", "64", "128", "256", "512", "1024", "2048" });
		bandits.addArrayParameter("stratVariability", new String[] { "0.1",
				"0.01", "0.001" });
		bandits.addArrayParameter("stratVolatility", new String[] { "0.01" });
		bandits.addArrayParameter("gathererLimit", new String[] { "2", "4",
				"8", "16" });

		if (seed == 0)
			seed = 1;
		String[] seeds = new String[repeats];
		for (int i = 0; i < seeds.length; i++) {
			seeds[i] = Integer.toString(seed + i);
		}
		bandits.addArrayParameter("seed", seeds);
		bandits.build();

		while (bandits.hasNext()) {
			PersistentSimulation sim = bandits.next().insert(getDatabase());
			logger.info("Created sim: " + sim.getID() + " - " + sim.getName());
		}
	}

	private void pseudo(int repeats, int seed)
			throws InvalidParametersException {
		Experiment pseudo = new Experiment("pseudo",
				"ps:%{p.gathererLimit}:%{p.qscale}", "kc.GameSimulation", 100);
		pseudo.addFixedParameter("gameClass", "kc.games.KnowledgeGame");
		pseudo.addRangeParameter("gathererLimit", 1, 50, 1);
		pseudo.addArrayParameter("qscale", 0.02, 0.01, 0.005);
		pseudo.addRangeParameter("seed", seed, repeats, 1);
		pseudo.build();

		while (pseudo.hasNext()) {
			PersistentSimulation sim = pseudo.next().insert(getDatabase());
			logger.info("Created sim: " + sim.getID() + " - " + sim.getName());
		}
	}

}
