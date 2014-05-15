package kc.choice;

import java.util.Set;

import uk.ac.imperial.einst.Institution;
import uk.ac.imperial.einst.vote.Issue;
import uk.ac.imperial.einst.vote.VoteMethod;

public abstract class FeeIssue extends Issue {

	protected final Set<String> roles;
	protected final double incrementValue;
	double minValue = 0.0;
	double maxValue = 1.0;

	public FeeIssue(Institution inst, String name, Set<String> cfvRoles,
			Set<String> voteRoles, VoteMethod method, String wdm,
			Object[] options, Set<String> roles, double incrementValue) {
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

	public abstract void updateFee(Object winner);

	public abstract double getFee();

	public abstract void setFee(double value);

}