package kc.choice;

import java.util.Set;

import kc.MeteredPool;
import uk.ac.imperial.einst.resource.Pool;
import uk.ac.imperial.einst.vote.Issue;
import uk.ac.imperial.einst.vote.Plurality;
import uk.ac.imperial.einst.vote.VoteMethod;

public class PoolFee extends Issue {

	final MeteredPool pool;
	final String role;
	final double incrementValue;
	static Object[] options = new Integer[] { -1, 0, 1 };

	public PoolFee(MeteredPool pool, String role, Set<String> cfvRoles,
			Set<String> voteRoles, double incrementValue) {
		super(pool.getInst(), "poolfee-" + role, cfvRoles, voteRoles,
				VoteMethod.PREFERENCE, options, Plurality.NAME);
		this.pool = pool;
		this.role = role;
		this.incrementValue = incrementValue;
	}

	public Pool getPool() {
		return pool;
	}

	public String getRole() {
		return role;
	}

	public void updateFees(Object winner) {
		double current = getCurrentFee();
		int opt;
		try {
			opt = Integer.parseInt(winner.toString());
		} catch(NumberFormatException e) {
			return;
		}
		switch (opt) {
		case -1:
			current = Math.max(0.0, current - incrementValue);
			break;
		case 1:
			current = current + incrementValue;
			break;
		}
		pool.getAppropriationFees().put(role, current);
	}

	private double getCurrentFee() {
		if (pool.getAppropriationFees().containsKey(role))
			return pool.getAppropriationFees().get(role);
		else {
			pool.getAppropriationFees().put(role, 0.0);
			return 0;
		}
	}

}
