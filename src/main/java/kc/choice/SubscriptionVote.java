package kc.choice;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import kc.DataInstitution;
import kc.agents.BallotHandler;
import kc.agents.Profile;
import uk.ac.imperial.einst.Actor;
import uk.ac.imperial.einst.access.AccessControl;
import uk.ac.imperial.einst.ipower.IPower;
import uk.ac.imperial.einst.micropay.MicroPayments;
import uk.ac.imperial.einst.vote.Ballot;
import uk.ac.imperial.einst.vote.Preferences;
import uk.ac.imperial.einst.vote.Vote;
import uk.ac.imperial.einst.vote.Voting;

public class SubscriptionVote implements BallotHandler {

	final Actor self;
	final IPower pow;
	final Voting voting;
	final AccessControl ac;
	final MicroPayments pay;

	final Profile type;

	public SubscriptionVote(Actor a, Profile type, IPower pow, Voting voting,
			AccessControl ac, MicroPayments pay) {
		super();
		this.type = type;
		this.self = a;
		this.pow = pow;
		this.voting = voting;
		this.ac = ac;
		this.pay = pay;
	}

	@Override
	public boolean canHandle(Ballot b) {
		return b.getIssue() instanceof SubscriptionFee;
	}

	@Override
	public Vote getVote(Ballot b) {
		SubscriptionFee issue = (SubscriptionFee) b.getIssue();
		DataInstitution i = (DataInstitution) b.getIssue().getInst();

		// determine if agent pays this fee, or if they are
		// responsible for institution costs
		Set<String> currentRoles = ac.getRoles(self, i);
		boolean payer = !Collections.disjoint(currentRoles, issue.getRoles());
		boolean initiator = currentRoles.contains("initiator");

		// accumulate vote preferences
		Map<Object, AtomicInteger> preferences = new HashMap<Object, AtomicInteger>();
		final Object[] options = b.getOptions();
		for (Object o : options) {
			preferences.put(o, new AtomicInteger());
		}
		// new voting
		double instBalance = i.getAccount().getBalance();
		double instProfit = i.getProfit();
		Set<Actor> payers = new HashSet<Actor>();
		for (String role : issue.getRoles()) {
			payers.addAll(ac.getActorsInRole(role, i));
		}
		int numPayers = payers.size();
		if (type == Profile.GREEDY) {
			// greedy: initator votes for max, payer votes for
			// min value.
			if (initiator) {
				for (int j = 0; j < 2; j++) {
					preferences.get(options[options.length - j - 1]).addAndGet(
							3 - j);
				}
			}
			if (payer) {
				for (int j = 0; j < 2; j++) {
					preferences.get(options[j]).addAndGet(3 - j);
				}
			}
			Preferences votePref = Preferences.generate(issue.getMethod(),
					preferences, false, 4);
			return new Vote(self, i, b, votePref);
		} else if (type == Profile.SUSTAINABLE) {
			// choose suitable fee to move inst balance towards
			// target
			final int projectionLength = 10;
			final double target = 0;
			double baseProfit = instProfit - issue.getFee() * numPayers;
			Map<Object, Double> projected = new HashMap<Object, Double>();
			for (Object o : options) {
				double fee = Double.parseDouble(o.toString());
				if (fee > 0.65)
					projected.put(o, 100.0);
				else {
					double profit = baseProfit + fee * (double) numPayers;
					projected.put(o, Math.abs(target
							- (instBalance + projectionLength * profit)));
				}
			}
			Preferences votePref = Preferences.generate(issue.getMethod(),
					projected, true, 3);
			return new Vote(self, i, b, votePref);
		} else if (type == Profile.PROFITABLE) {
			if (initiator) {
				// aim to take 0.6 per agent per timestep.
				double baseProfit = instProfit - issue.getFee() * numPayers;
				double targetProfit = Math.max(baseProfit + numPayers * 0.6, 0);
				Map<Object, Double> projectedProfit = new HashMap<Object, Double>();
				for (Object o : options) {
					double fee = Double.parseDouble(o.toString());
					double profit = baseProfit + fee * numPayers;
					projectedProfit.put(o, Math.abs(targetProfit - profit));
				}
				Preferences votePref = Preferences.generate(issue.getMethod(),
						projectedProfit, true, 3);
				return new Vote(self, i, b, votePref);
			} else if (payer) {
				// prefer lower fees
				for (int j = 0; j < 2; j++) {
					preferences.get(options[j]).addAndGet(3 - j);
				}
				Preferences votePref = Preferences.generate(issue.getMethod(),
						preferences, false, 4);
				return new Vote(self, i, b, votePref);
			}

		}
		return null;
	}

}