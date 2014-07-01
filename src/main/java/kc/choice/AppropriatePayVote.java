package kc.choice;

import java.util.Set;

import uk.ac.imperial.einst.Actor;
import uk.ac.imperial.einst.access.AccessControl;
import uk.ac.imperial.einst.micropay.Account;
import uk.ac.imperial.einst.micropay.MicroPayments;
import uk.ac.imperial.einst.resource.ProvisionAppropriationSystem;
import uk.ac.imperial.einst.resource.ProvisionAppropriationSystem.PoolUsage;
import uk.ac.imperial.einst.vote.Ballot;
import uk.ac.imperial.einst.vote.Vote;
import kc.DataInstitution;
import kc.agents.BallotHandler;
import kc.agents.Profile;

public class AppropriatePayVote implements BallotHandler {

	final Actor self;
	final Profile type;
	final ProvisionAppropriationSystem pas;
	final MicroPayments pay;
	final AccessControl ac;

	public AppropriatePayVote(Actor self, Profile type,
			ProvisionAppropriationSystem pas, MicroPayments pay,
			AccessControl ac) {
		super();
		this.self = self;
		this.type = type;
		this.pas = pas;
		this.pay = pay;
		this.ac = ac;
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
		final boolean user = usage.appropriations > 0;
		final boolean beneficiary = usage.provisionsAppropriated > usage.appropriations;
		final boolean initiator = currentRoles.contains("initiator");
		final double current = issue.getFee();
		final Account account = pay.getAccount(self);
		final double profit = account.getProfit();
		final double instBalance = i.getAccount().getBalance();
		final double instProfit = i.getProfit();

		Object choice = null;
		if (beneficiary) {
			switch (type) {
			case GREEDY:
				choice = ChangeFeeIssue.INCREASE;
				break;
			case PROFITABLE:
				if(current > 0.1)
					choice = ChangeFeeIssue.DECREASE;
				else
					choice = ChangeFeeIssue.NOCHANGE;
				break;
			case SUSTAINABLE:
				if (initiator && instBalance < 0
						&& account.getBalance() < -1 * instBalance)
					choice = ChangeFeeIssue.INCREASE; // need to pay off inst
														// debts
				else if (!issue.paidByAppropriators) {
					// ensure inst can bare cost
					if (instProfit < 0 || instBalance < 0)
						choice = ChangeFeeIssue.DECREASE;
					else if (instProfit > 0 && instBalance > 0 && profit < 0.1)
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
		} else if (user) {
			switch (type) {
			case GREEDY:
				choice = ChangeFeeIssue.DECREASE;
				break;
			case PROFITABLE:
				if(current > 0.2)
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
					if (current < 0.1)
						choice = ChangeFeeIssue.INCREASE;
					else if (current > 0.3)
						choice = ChangeFeeIssue.DECREASE;
					else
						choice = ChangeFeeIssue.NOCHANGE;
				}
				break;
			}
		}
		return new Vote(self, i, b, choice);
	}

}
