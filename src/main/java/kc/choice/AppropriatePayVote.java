package kc.choice;

import uk.ac.imperial.einst.Actor;
import uk.ac.imperial.einst.vote.Ballot;
import uk.ac.imperial.einst.vote.Vote;
import kc.DataInstitution;
import kc.agents.BallotHandler;
import kc.agents.Profile;

public class AppropriatePayVote implements BallotHandler {

	final Actor self;
	final Profile type;

	public AppropriatePayVote(Actor self, Profile type) {
		super();
		this.self = self;
		this.type = type;
	}

	@Override
	public boolean canHandle(Ballot b) {
		return b.getIssue() instanceof PoolAppropriatePay;
	}

	@Override
	public Vote getVote(Ballot b) {
		PoolAppropriatePay issue = (PoolAppropriatePay) b.getIssue();
		DataInstitution i = (DataInstitution) issue.getInst();
		
		boolean user;
		boolean beneficiary;
		// TODO
		
		return null;
	}

}
