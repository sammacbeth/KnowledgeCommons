package kc.choice;

import java.util.Map;
import java.util.Set;

import kc.MeteredPool;
import uk.ac.imperial.einst.resource.Pool;
import uk.ac.imperial.einst.vote.Plurality;
import uk.ac.imperial.einst.vote.VoteMethod;

public class PoolFee extends ChangeFeeIssue {

	final MeteredPool pool;

	public PoolFee(MeteredPool pool, Set<String> roles, Set<String> cfvRoles,
			Set<String> voteRoles, double incrementValue) {
		super(pool.getInst(), "poolfee-" + roles.toString(), cfvRoles,
				voteRoles, VoteMethod.PREFERENCE, Plurality.NAME, roles,
				incrementValue);
		this.pool = pool;
	}

	public Pool getPool() {
		return pool;
	}

	@Override
	public double getFee() {
		double fee = 0;
		Map<String, Double> fees = pool.getAppropriationFees();
		for (String r : roles) {
			if (fees.containsKey(r))
				fee += fees.get(r);
		}
		return fee / roles.size();
	}

	@Override
	public void setFee(double value) {
		Map<String, Double> fees = pool.getAppropriationFees();
		for (String r : roles) {
			fees.put(r, value);
		}
	}

}
