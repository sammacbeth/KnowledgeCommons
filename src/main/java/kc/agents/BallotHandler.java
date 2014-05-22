package kc.agents;

import uk.ac.imperial.einst.vote.Ballot;
import uk.ac.imperial.einst.vote.Vote;

public interface BallotHandler {

	boolean canHandle(Ballot b);
	
	Vote getVote(Ballot b);
}
