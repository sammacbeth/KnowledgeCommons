package kc.util;

import java.util.HashMap;
import java.util.Map;

import kc.agents.Profile;

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
import uk.ac.imperial.presage2.core.cli.MultiExperiment;
import uk.ac.imperial.presage2.core.cli.ParameterSweep;
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

		Map<String, Experiment> experiments = new HashMap<String, Experiment>();
		addExperiment(experiments, bandits());
		addExperiment(experiments, pseudo());
		addExperiment(experiments, facilityCosts());
		addExperiment(experiments, facilityCostsSub());
		addExperiment(experiments, subCollective());
		addExperiment(experiments, building());
		addExperiment(experiments, supply());
		// Map<String, String> experiments = new HashMap<String, String>();
		// experiments.put("bandits", "Get properties of the bandit game.");
		// experiments.put("pseudo", "Get properties of the pseudo game.");

		OptionGroup exprOptions = new OptionGroup();
		for (Experiment expr : experiments.values()) {
			exprOptions.addOption(new Option(expr.getName(), expr
					.getDescription()));
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

		Experiment expr = experiments.get(args[1]);

		if (seed == 0)
			seed = 1;
		String[] seeds = new String[repeats];
		for (int i = 0; i < seeds.length; i++) {
			seeds[i] = Integer.toString(seed + i);
		}
		expr.addRangeParameter("seed", seed, repeats, 1);
		expr.build();

		while (expr.hasNext()) {
			PersistentSimulation sim = expr.next().insert(getDatabase());
			logger.info("Created sim: " + sim.getID() + " - " + sim.getName());
		}

		stopDatabase();
	}

	static void addExperiment(Map<String, Experiment> experiments,
			Experiment expr) {
		experiments.put(expr.getName(), expr);
	}

	private Experiment bandits() throws InvalidParametersException {
		Experiment bandits = new ParameterSweep(
				"bandits",
				"Get properties of the bandit game.",
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

		return bandits;
	}

	private Experiment pseudo() throws InvalidParametersException {
		Experiment pseudo = new ParameterSweep("pseudo",
				"Get properties of the pseudo game.",
				"ps:%{p.gathererLimit}:%{p.qscale}", "kc.GameSimulation", 100);
		pseudo.addFixedParameter("gameClass", "kc.games.KnowledgeGame");
		pseudo.addRangeParameter("gathererLimit", 1, 50, 1);
		pseudo.addArrayParameter("qscale", 0.02, 0.01, 0.005);
		return pseudo;
	}

	private Experiment facilityCosts() {
		Experiment sunk = new ParameterSweep("sunk", "sunk:%{p.facilitySunk}",
				"kc.GameSimulation", 100)
				.addArrayParameter("facilitySunk", 0, 10, 90)
				.addFixedParameter("facilityFixed", 0.0)
				.addFixedParameter("facilityMarginalStorage", 0.0)
				.addFixedParameter("facilityMarginalTrans", 0.0);

		Experiment fixed = new ParameterSweep("fixed",
				"fixed:%{p.facilityFixed}", "kc.GameSimulation", 100)
				.addFixedParameter("facilitySunk", 0.0)
				.addArrayParameter("facilityFixed", 2.0, 4.0, 5.0)
				.addFixedParameter("facilityMarginalStorage", 0.0)
				.addFixedParameter("facilityMarginalTrans", 0.0);

		Experiment sto = new ParameterSweep("sto",
				"sto:%{p.facilityMarginalStorage}", "kc.GameSimulation", 100)
				.addFixedParameter("facilitySunk", 0.0)
				.addFixedParameter("facilityFixed", 0.0)
				.addArrayParameter("facilityMarginalStorage", 0.005, 0.01, 0.02)
				.addFixedParameter("facilityMarginalTrans", 0.0)
				.addArrayParameter("prune", false);

		Experiment stoPrune = new ParameterSweep("stoPrune",
				"sto:%{p.facilityMarginalStorage}:prune", "kc.GameSimulation",
				100)
				.addFixedParameter("facilitySunk", 0.0)
				.addFixedParameter("facilityFixed", 0.0)
				.addArrayParameter("facilityMarginalStorage", 0.005, 0.01, 0.02)
				.addFixedParameter("facilityMarginalTrans", 0.0)
				.addArrayParameter("prune", true);

		Experiment trans = new ParameterSweep("trans",
				"trans:%{p.facilityMarginalTrans}", "kc.GameSimulation", 100)
				.addFixedParameter("facilitySunk", 0.0)
				.addFixedParameter("facilityFixed", 0.0)
				.addFixedParameter("facilityMarginalStorage", 0.0)
				.addArrayParameter("facilityMarginalTrans", 0.1, 0.25, 0.5);

		Experiment multi = new MultiExperiment("facilities", "", sunk, fixed,
				sto, trans, stoPrune);
		multi.addFixedParameter("gameClass", "kc.games.KnowledgeGame");
		multi.addFixedParameter("gathererLimit", 10);
		return multi;
	}

	private Experiment facilityCostsSub() {
		Experiment sunk = new ParameterSweep("sunk",
				"sunk:%{p.facilitySunk}:%{p.analystProfile}",
				"kc.GameSimulation", 100)
				.addArrayParameter("facilitySunk", 0, 10, 90)
				.addFixedParameter("facilityFixed", 0.0)
				.addFixedParameter("facilityMarginalStorage", 0.0)
				.addFixedParameter("facilityMarginalTrans", 0.0);

		Experiment fixed = new ParameterSweep("fixed",
				"fixed:%{p.facilityFixed}:%{p.analystProfile}",
				"kc.GameSimulation", 100)
				.addFixedParameter("facilitySunk", 0.0)
				.addArrayParameter("facilityFixed", 2.0, 4.0, 5.0)
				.addFixedParameter("facilityMarginalStorage", 0.0)
				.addFixedParameter("facilityMarginalTrans", 0.0);

		Experiment sto = new ParameterSweep("sto",
				"sto:%{p.facilityMarginalStorage}:%{p.analystProfile}",
				"kc.GameSimulation", 100)
				.addFixedParameter("facilitySunk", 0.0)
				.addFixedParameter("facilityFixed", 0.0)
				.addArrayParameter("facilityMarginalStorage", 0.005, 0.01, 0.02)
				.addFixedParameter("facilityMarginalTrans", 0.0);

		Experiment trans = new ParameterSweep("trans",
				"trans:%{p.facilityMarginalTrans}:%{p.analystProfile}",
				"kc.GameSimulation", 100)
				.addFixedParameter("facilitySunk", 0.0)
				.addFixedParameter("facilityFixed", 0.0)
				.addFixedParameter("facilityMarginalStorage", 0.0)
				.addArrayParameter("facilityMarginalTrans", 0.1, 0.25, 0.5);

		Experiment multi = new MultiExperiment("facilitysub", "", sunk, fixed,
				sto, trans);
		multi.addFixedParameter("gameClass", "kc.games.KnowledgeGame");
		multi.addFixedParameter("numStrategies", 50);
		multi.addFixedParameter("gathererLimit", 10);
		multi.addArrayParameter("analystProfile", Profile.SUSTAINABLE.name(),
				Profile.PROFITABLE.name(), Profile.GREEDY);
		return multi;
	}

	private Experiment subCollective() {
		Experiment sub = new ParameterSweep("subcollective",
				"subcol:%{p.analystProfile}:%{p.greedyConsumers}",
				"kc.GameSimulation", 100)
				.addFixedParameter("facilityFixed", 2.0)
				.addFixedParameter("facilityMarginalTrans", 0.05)
				.addFixedParameter("gameClass", "kc.games.KnowledgeGame")
				.addFixedParameter("gathererLimit", 10)
				.addArrayParameter("analystProfile",
						Profile.SUSTAINABLE.name(), Profile.PROFITABLE.name(),
						Profile.GREEDY.name())
				.addFixedParameter("consumerProfile",
						Profile.SUSTAINABLE.name())
				.addRangeParameter("greedyConsumers", 0, 10, 1);
		return sub;
	}

	private Experiment building() {
		Experiment basic = new ParameterSweep("basic",
				"basic:%{p.facilityCostProfile}", "kc.FullSimulation", 200)
				.addArrayParameter("measuringCost", 0);
		Experiment sub = new ParameterSweep("sub",
				"sub:%{p.facilityCostProfile}:%{p.initiatorProfile}",
				"kc.FullSimulation", 200)
				.addArrayParameter("measuringCost", 0)
				.addFixedParameter("subscription", true)
				.addArrayParameter("initiatorProfile",
						Profile.SUSTAINABLE.name(), Profile.PROFITABLE.name(),
						Profile.GREEDY.name());
		Experiment payApp = new ParameterSweep(
				"payApp",
				"payApp:%{p.facilityCostProfile}:%{p.initiatorProfile}:%{p.separateAnalyst}",
				"kc.FullSimulation", 200)
				.addArrayParameter("measuringCost", 0)
				.addFixedParameter("subscription", true)
				.addFixedParameter("payOnApp", true)
				.addArrayParameter("initiatorProfile",
						Profile.SUSTAINABLE.name(), Profile.PROFITABLE.name(),
						Profile.GREEDY.name())
				.addArrayParameter("separateAnalyst", true, false);
		Experiment measureCost = new ParameterSweep(
				"measureCost",
				"mcost:%{p.facilityCostProfile}:%{p.initiatorProfile}:%{p.nNcProsumers}",
				"kc.FullSimulation", 200)
				.addArrayParameter("measuringCost", 0.1)
				.addFixedParameter("subscription", true)
				.addFixedParameter("payOnApp", true)
				.addFixedParameter("initiatorProfile",
						Profile.SUSTAINABLE.name())
				.addArrayParameter("nNcProsumers", 2, 6, 8);

		Experiment market = new ParameterSweep(
				"market",
				"market:%{p.facilityCostProfile}:%{p.analystProfile}:%{p.consumerProfile}",
				"kc.FullSimulation", 200)
				.addFixedParameter("type", "market")
				.addArrayParameter("measuringCost", 0)
				.addFixedParameter("payOnApp", true)
				.addFixedParameter("payOnProv", true)
				.addArrayParameter("analystProfile",
						Profile.SUSTAINABLE.name(), Profile.PROFITABLE,
						Profile.GREEDY.name())
				.addArrayParameter("consumerProfile",
						Profile.SUSTAINABLE.name(), Profile.PROFITABLE,
						Profile.GREEDY.name());

		Experiment marketNC = new ParameterSweep(
				"marketNC",
				"marketnc:%{p.facilityCostProfile}:%{p.analystProfile}:%{p.nNcProsumers}:%{p.measuringCost}",
				"kc.FullSimulation", 200)
				.addFixedParameter("type", "market")
				.addArrayParameter("measuringCost", 0, 0.1)
				.addFixedParameter("payOnApp", true)
				.addFixedParameter("payOnProv", true)
				.addArrayParameter("analystProfile",
						Profile.SUSTAINABLE.name(), Profile.PROFITABLE,
						Profile.GREEDY.name())
				.addArrayParameter("consumerProfile",
						Profile.SUSTAINABLE.name())
				.addArrayParameter("nNcProsumers", 2, 6, 8);

		Experiment collective = new ParameterSweep(
				"collective",
				"collect:%{p.facilityCostProfile}:%{p.analystProfile}:%{p.nNcProsumers}:%{p.measuringCost}",
				"kc.FullSimulation", 200)
				.addFixedParameter("type", "collective")
				.addArrayParameter("measuringCost", 0, 0.1)
				.addArrayParameter("analystProfile",
						Profile.SUSTAINABLE.name(), Profile.PROFITABLE,
						Profile.GREEDY.name())
				.addArrayParameter("nNcProsumers", 2, 6, 8);

		Experiment multi = new MultiExperiment("building", "", basic, sub,
				payApp, measureCost, market, marketNC, collective)
				.addArrayParameter("facilityCostProfile", 0, 1)
				.addFixedParameter("nProsumers", 10)
				.addFixedParameter("initiatorCredit", 0);
		return multi;
	}

	private Experiment supply() {
		Experiment supply = new ParameterSweep(
				"supply",
				"supply:%{p.facilityCostProfile}:%{p.subscription}:%{p.payOnApp}",
				"kc.FullSimulation", 200)
				.addArrayParameter("facilityCostProfile", 0, 1)
				.addFixedParameter("type", "supply")
				.addFixedParameter("nProsumers", 10)
				.addFixedParameter("initiatorCredit", 0)
				.addArrayParameter("payOnApp", false, true)
				.addArrayParameter("subscription", false, true)
				.addFixedParameter("measuringCost", 0);
		Experiment prov = new ParameterSweep(
				"supply",
				"supplyprov:%{p.facilityCostProfile}:%{p.payOnProv}:%{p.measuringCost}:%{p.nNcProsumers}",
				"kc.FullSimulation", 200)
				.addArrayParameter("facilityCostProfile", 0, 1)
				.addFixedParameter("type", "supply")
				.addFixedParameter("nProsumers", 10)
				.addArrayParameter("nNcProsumers", 2, 5)
				.addFixedParameter("initiatorCredit", 0)
				.addArrayParameter("payOnProv", false, true)
				.addFixedParameter("payOnApp", true)
				.addFixedParameter("subscription", true)
				.addFixedParameter("measuringCost", 0.1);
		Experiment provExtra = new ParameterSweep(
				"supply",
				"supplyprovex:%{p.facilityCostProfile}:%{p.payOnProv}:%{p.measuringCost}:%{p.nNcProsumers}",
				"kc.FullSimulation", 200)
				.addArrayParameter("facilityCostProfile", 0, 1)
				.addFixedParameter("type", "supply")
				.addFixedParameter("nProsumers", 10)
				.addArrayParameter("nNcProsumers", 2, 5)
				.addFixedParameter("initiatorCredit", 0)
				.addFixedParameter("payOnProv", true)
				.addFixedParameter("payOnApp", true)
				.addFixedParameter("subscription", true)
				.addFixedParameter("measuringCost", 0.1)
				.addFixedParameter("extraAnalystPay", true);
		Experiment market = new ParameterSweep(
				"market",
				"market:%{p.facilityCostProfile}:%{p.measuringCost}",
				"kc.FullSimulation", 200)
				.addArrayParameter("facilityCostProfile", 0, 1)
				.addFixedParameter("type", "market")
				.addFixedParameter("nProsumers", 10)
				.addFixedParameter("initiatorCredit", 0)
				.addFixedParameter("payOnProv", true)
				.addFixedParameter("payOnApp", true)
				.addFixedParameter("subscription", true)
				.addArrayParameter("measuringCost", 0, 0.1);
		Experiment collective = new ParameterSweep(
				"collective",
				"coll:%{p.facilityCostProfile}:%{p.measuringCost}",
				"kc.FullSimulation", 200)
				.addArrayParameter("facilityCostProfile", 0, 1)
				.addFixedParameter("type", "collective")
				.addFixedParameter("nProsumers", 10)
				.addFixedParameter("initiatorCredit", 0)
				.addArrayParameter("measuringCost", 0, 0.1);
		Experiment central = new ParameterSweep(
				"central",
				"central:%{p.facilityCostProfile}:%{p.measuringCost}",
				"kc.FullSimulation", 200)
				.addArrayParameter("facilityCostProfile", 0, 1)
				.addFixedParameter("nProsumers", 10)
				.addFixedParameter("initiatorCredit", 0)
				.addFixedParameter("payOnProv", true)
				.addFixedParameter("payOnApp", true)
				.addFixedParameter("subscription", true)
				.addArrayParameter("measuringCost", 0, 0.1);
		return new MultiExperiment("supply", "", /*supply,*/ prov, provExtra/*, market, collective, central*/);
	}
}
