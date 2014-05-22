package kc.choice;

import java.util.Set;

import kc.MeteredPool;
import uk.ac.imperial.einst.vote.Plurality;
import uk.ac.imperial.einst.vote.VoteMethod;

public class PoolAppropriatePay extends ChangeFeeIssue {

	final MeteredPool pool;
	boolean paidByAppropriators = false;

	protected PoolAppropriatePay(MeteredPool pool, Set<String> cfvRoles,
			Set<String> voteRoles, Set<String> roles, double incrementValue,
			boolean paidByAppropriators) {
		super(pool.getInst(), "poolpay-" + pool.toString(), cfvRoles,
				voteRoles, VoteMethod.SINGLE, Plurality.NAME, roles,
				incrementValue);
		this.pool = pool;
		this.paidByAppropriators = paidByAppropriators;
	}

	@Override
	public double getFee() {
		return pool.getPayOnAppropriation();
	}

	@Override
	public void setFee(double value) {
		pool.setPayOnAppropriation(value);
		if (paidByAppropriators) {
			for (String r : roles) {
				pool.getAppropriationFees().put(r, value);
			}
		}
	}

}
