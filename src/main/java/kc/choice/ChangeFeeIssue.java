package kc.choice;

import java.util.Set;

import uk.ac.imperial.einst.Institution;
import uk.ac.imperial.einst.vote.VoteMethod;

public abstract class ChangeFeeIssue extends FeeIssue {

	public static int DECREASE = -1;
	public static int NOCHANGE = 0;
	public static int INCREASE = 1;

	protected static Object[] options = new Integer[] { DECREASE, NOCHANGE,
			INCREASE };

	protected ChangeFeeIssue(Institution inst, String name,
			Set<String> cfvRoles, Set<String> voteRoles, VoteMethod method,
			String wdm, Set<String> roles, double incrementValue) {
		super(inst, name, cfvRoles, voteRoles, method, wdm, options, roles,
				incrementValue);
	}

	public void updateFee(Object winner) {
		double current = getFee();
		int opt;
		try {
			opt = Integer.parseInt(winner.toString());
		} catch (NumberFormatException e) {
			return;
		}
		switch (opt) {
		case -1:
			current = Math.max(minValue, current - incrementValue);
			break;
		case 1:
			current = Math.min(maxValue, current + incrementValue);
			break;
		}
		setFee(current);
	}

}
