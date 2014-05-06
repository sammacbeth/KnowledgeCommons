package kc.choice;

import java.util.Set;

import uk.ac.imperial.einst.Institution;
import uk.ac.imperial.einst.vote.Issue;
import uk.ac.imperial.einst.vote.VoteMethod;

public abstract class FeeIssue extends Issue {

	final Set<String> roles;
	final double incrementValue;
	static Object[] options = new Integer[] { -1, 0, 1 };

	protected FeeIssue(Institution inst, String name, Set<String> cfvRoles,
			Set<String> voteRoles, VoteMethod method, String wdm,
			Set<String> roles, double incrementValue) {
		super(inst, name, cfvRoles, voteRoles, method, options, wdm);
		this.roles = roles;
		this.incrementValue = incrementValue;
	}

	public Set<String> getRoles() {
		return roles;
	}

	public double getIncrementValue() {
		return incrementValue;
	}

	public static Object[] getOptions() {
		return options;
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
			current = Math.max(0.0, current - incrementValue);
			break;
		case 1:
			current = current + incrementValue;
			break;
		}
		setFee(current);
	}

	protected abstract double getFee();

	public abstract void setFee(double value);

}
