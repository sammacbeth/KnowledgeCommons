package kc.choice;

import java.util.Set;

import uk.ac.imperial.einst.Institution;
import uk.ac.imperial.einst.vote.VoteMethod;

public abstract class ChangeFeeIssue extends FeeIssue {

	public static int DOUBLE_DECREASE = -2;
	public static int DECREASE = -1;
	public static int NOCHANGE = 0;
	public static int INCREASE = 1;
	public static int DOUBLE_INCREASE = 2;

	protected static Object[] options = new Integer[] { DOUBLE_DECREASE, DECREASE, NOCHANGE,
			INCREASE, DOUBLE_INCREASE};

	protected ChangeFeeIssue(Institution inst, String name,
			Set<String> cfvRoles, Set<String> voteRoles, VoteMethod method,
			String wdm, Set<String> roles, double min, double max, double incrementValue) {
		super(inst, name, cfvRoles, voteRoles, method, wdm, options, roles,
				incrementValue);
		this.minValue = min;
		this.maxValue = max;
	}

	public void updateFee(Object winner) {
		double current = getFee();
		int opt;
		try {
			opt = Integer.parseInt(winner.toString());
		} catch (NumberFormatException e) {
			return;
		}
		current += opt * incrementValue;
		current = Math.max(minValue, current);
		current = Math.min(maxValue, current);
		setFee(current);
	}

}
