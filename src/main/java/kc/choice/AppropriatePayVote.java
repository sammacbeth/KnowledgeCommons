package kc.choice;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import kc.DataInstitution;
import kc.Game;
import kc.KnowledgeCommons;
import kc.Measured;
import kc.agents.BallotHandler;
import kc.agents.Profile;
import kc.prediction.Predictor;
import uk.ac.imperial.einst.Actor;
import uk.ac.imperial.einst.access.AccessControl;
import uk.ac.imperial.einst.micropay.Account;
import uk.ac.imperial.einst.micropay.MicroPayments;
import uk.ac.imperial.einst.resource.ProvisionAppropriationSystem;
import uk.ac.imperial.einst.resource.ProvisionAppropriationSystem.PoolUsage;
import uk.ac.imperial.einst.vote.Ballot;
import uk.ac.imperial.einst.vote.Preferences;
import uk.ac.imperial.einst.vote.Vote;

public class AppropriatePayVote implements BallotHandler {

	final Actor self;
	final Profile type;
	final ProvisionAppropriationSystem pas;
	final MicroPayments pay;
	final AccessControl ac;
	final KnowledgeCommons kc;
	final Game g;

	public AppropriatePayVote(Actor self, Profile type,
			ProvisionAppropriationSystem pas, MicroPayments pay,
			AccessControl ac, KnowledgeCommons kc, Game g) {
		super();
		this.self = self;
		this.type = type;
		this.pas = pas;
		this.pay = pay;
		this.ac = ac;
		this.kc = kc;
		this.g = g;
	}

	@Override
	public boolean canHandle(Ballot b) {
		return b.getIssue() instanceof PoolAppropriatePay;
	}

	@Override
	public Vote getVote(Ballot b) {
		PoolAppropriatePay issue = (PoolAppropriatePay) b.getIssue();
		DataInstitution i = (DataInstitution) issue.getInst();

		Set<String> currentRoles = ac.getRoles(self, i);
		PoolUsage usage = pas
				.getPoolUsage(self, issue.pool, b.getStarted() - 5);
		final boolean measuredPool = issue.getPool().getContribRoles()
				.contains("gatherer");
		final boolean initiator = currentRoles.contains("initiator");
		final boolean consumer = currentRoles.contains("consumer");
		final boolean analyst = currentRoles.contains("analyst");
		final boolean user = !Collections.disjoint(issue.getPool()
				.getExtractRoles(), currentRoles);
		final boolean beneficiary = !Collections.disjoint(issue.getPool()
				.getContribRoles(), currentRoles);
		final double current = issue.getFee();
		final Account account = pay.getAccount(self);
		// final double debt = pay.getDebt(self);
		final double profit = account.getProfit();
		final double instBalance = i.getAccount().getBalance();
		final double instProfit = i.getProfit();

		Object choice = null;
		if (issue instanceof SetFeeIssue) {
			Map<Object, AtomicInteger> preferences = new HashMap<Object, AtomicInteger>();
			final Object[] options = b.getOptions();
			for (Object o : options) {
				preferences.put(o, new AtomicInteger());
			}

			if (beneficiary) {
				double nSold = (usage.provisionsAppropriated - usage.appropriations) / 6.0;
				double earnings = current * nSold;
				double baseProfit = profit - earnings;
				Map<Object, Double> rating = new HashMap<Object, Double>();
				double targetProfit = 0.5;
				double required = 0;

				switch (type) {
				case GREEDY:
					for (int j = 1; j < 4; j++) {
						preferences.get(options[options.length - j]).addAndGet(
								4 - j);
					}
					choice = Preferences.generate(issue.getMethod(),
							preferences, false, 3);
					break;
				case PROFITABLE:
					targetProfit = 0.1;
					// targetProfit = (futureT-account.getBalance())/(futureT -
					// b.getStarted());
					required = (targetProfit - baseProfit) / nSold;
					if (analyst) {
						double fee = kc.getAppropriationFee(i, new Measured(),
								"analyst");
						if (fee > required) {
							required = fee + 0.1;
						}
					} else if (consumer) {
						double fee = kc.getAppropriationFee(i, Predictor.class,
								"consumer");
						if (fee > required) {
							required = fee + 0.1;
						}
					}
					if (profit < 0.1)
						required = current + 0.1;
					else if (profit < 1)
						required = current + 0.05;
					else
						required = current;
					for (Object o : options) {
						double fee = Double.parseDouble(o.toString());
						rating.put(o, Math.abs(required - fee));
					}
					choice = Preferences.generate(issue.getMethod(), rating,
							true, 3);
					break;
				case SUSTAINABLE:
					double measuringCost = g.getMeasuringCost();
					double minimum = 0;
					targetProfit = consumer ? 0.0 : 0.3;
					// targetProfit =
					// (0.25*futureT-account.getBalance())/(futureT -
					// b.getStarted());
					required = (targetProfit - baseProfit) / nSold;
					if (profit < -0.5
							|| (profit < 0 && account.getBalance() < 0))
						required = current + 0.1;
					else if (profit > 0.8)
						required = current - 0.05;
					else if (profit > 1)
						required = current - 0.1;
					else if (profit <= 0.1 || account.getBalance() < 0)
						required = current + 0.05;
					else
						required = current;
					if (measuredPool && required < measuringCost)
						minimum = measuringCost + 0.01;
					/*
					 * if(analyst) { double fee = kc.getAppropriationFee(i, new
					 * Measured(), "analyst"); if(fee > required) { required =
					 * fee; } }
					 */
					for (Object o : options) {
						double fee = Double.parseDouble(o.toString());
						if (fee < minimum)
							rating.put(o, 5.0);
						else
							rating.put(o, Math.abs(required - fee));
					}
					choice = Preferences.generate(issue.getMethod(), rating,
							true, 3);
					break;
				}
			} else if (user) {
				switch (type) {
				case GREEDY:
				case PROFITABLE:
					for (int j = 0; j < 2; j++) {
						preferences.get(options[j]).addAndGet(3 - j);
					}
					choice = Preferences.generate(issue.getMethod(),
							preferences, true, 3);
					break;
				case SUSTAINABLE:
					double nBought = (usage.appropriations - usage.provisionsAppropriated) / 6.0;
					double cost = current * nBought;
					double baseProfit = profit - cost;
					double targetProfit = 0.25;
					double limit = (baseProfit - targetProfit) / nBought;
					double required = current;
					if (profit < -1)
						required = current - 0.1;
					else if (profit <= 0.1)
						required = current - 0.05;
					else if (profit > 1)
						required = current + 0.05;
					else if (profit > 2)
						required = current + 0.1;
					else
						required = current;
					Map<Object, Double> rating = new HashMap<Object, Double>();
					for (Object o : options) {
						double fee = Double.parseDouble(o.toString());
						rating.put(o, Math.abs(required - fee));
					}
					choice = Preferences.generate(issue.getMethod(), rating,
							true, 3);
					break;
				}
			} else if (initiator) {
				// initiator who is not invested either way in the pool
				Map<Object, Double> rating = new HashMap<Object, Double>();
				double preferred = 0;
				switch (type) {
				case GREEDY:
				case PROFITABLE:
					// preferred = 0;
					// break;
				case SUSTAINABLE:
					if (!issue.paidByAppropriators) {
						// ensure inst can bare cost
						if (instBalance < 0)
							preferred = current - 0.1;
						else if (instBalance > 0 && current < 0.1)
							preferred = current + 0.1;
						else
							preferred = current;
					} else {
						// others are paying, set a fair rate
						double measuringCost = g.getMeasuringCost();
						if (measuredPool) {
							preferred = measuringCost + 0.01;
						} else {
							preferred = 0.075 + (9 * measuringCost / 10);
						}
					}
					break;
				}
				for (Object o : options) {
					double fee = Double.parseDouble(o.toString());
					rating.put(o, Math.abs(preferred - fee));
				}
				choice = Preferences.generate(issue.getMethod(), rating, true,
						3);
			}
		} else {
			if (beneficiary) {
				switch (type) {
				case GREEDY:
					choice = ChangeFeeIssue.INCREASE;
					break;
				case PROFITABLE:
					if (profit > 1.0)
						choice = ChangeFeeIssue.DECREASE;
					else if (profit < 0.75)
						choice = ChangeFeeIssue.INCREASE;
					else
						choice = ChangeFeeIssue.NOCHANGE;
					break;
				case SUSTAINABLE:
					if (!issue.paidByAppropriators) {
						// ensure inst can bare cost
						if (instBalance < 0)
							choice = ChangeFeeIssue.DECREASE;
						else if (instBalance > 0 && profit < 0.1)
							choice = ChangeFeeIssue.INCREASE;
						else
							choice = ChangeFeeIssue.NOCHANGE;
					} else {
						// others are paying, set a fair rate
						if (profit < 0.25)
							choice = ChangeFeeIssue.INCREASE;
						else if (profit > 0.5)
							choice = ChangeFeeIssue.DECREASE;
						else
							choice = ChangeFeeIssue.NOCHANGE;
					}
					break;
				}
			} else if (user) {
				switch (type) {
				case GREEDY:
					choice = ChangeFeeIssue.DECREASE;
					break;
				case PROFITABLE:
					if (current > 0.2)
						choice = ChangeFeeIssue.DECREASE;
					break;
				case SUSTAINABLE:
					if (!issue.paidByAppropriators) {
						// ensure inst can bare cost
						if (instProfit < 0 || instBalance < 0)
							choice = ChangeFeeIssue.DECREASE;
						else if (current < 0.1)
							choice = ChangeFeeIssue.INCREASE;
						else
							choice = ChangeFeeIssue.NOCHANGE;
					} else {
						// others are paying, set a fair rate
						if (profit < 0)
							choice = ChangeFeeIssue.INCREASE;
						else if (profit > 1.0)
							choice = ChangeFeeIssue.DECREASE;
						else
							choice = ChangeFeeIssue.NOCHANGE;
					}
					break;
				}
			} else if (initiator) {
				// initiator who is not invested either way in the pool
				switch (type) {
				case GREEDY:
				case PROFITABLE:
					choice = ChangeFeeIssue.DECREASE;
					break;
				case SUSTAINABLE:
					if (!issue.paidByAppropriators) {
						// ensure inst can bare cost
						if (instBalance < 0)
							choice = ChangeFeeIssue.DECREASE;
						else if (instBalance > 0 && current < 0.1)
							choice = ChangeFeeIssue.INCREASE;
						else
							choice = ChangeFeeIssue.NOCHANGE;
					} else {
						// others are paying, set a fair rate
						if (profit < 0)
							choice = ChangeFeeIssue.INCREASE;
						else if (profit > 1.0)
							choice = ChangeFeeIssue.DECREASE;
						else
							choice = ChangeFeeIssue.NOCHANGE;
					}
					break;
				}
			}
		}
		return new Vote(self, i, b, choice);
	}

}
