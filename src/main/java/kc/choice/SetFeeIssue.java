package kc.choice;

import java.util.Set;

import uk.ac.imperial.einst.Institution;
import uk.ac.imperial.einst.vote.VoteMethod;

public abstract class SetFeeIssue extends FeeIssue {

	static Object[] options = null;

	static Object[] createOptions(double min, double max, double inc) {
		int n = (int) Math.ceil((max - min) / inc) + 1;
		options = new Double[n];
		double v = min;
		for (int i = 0; i < options.length; i++) {
			options[i] = Math.min(v, max);
			v += Math.round(inc*100)/100.0;
		}
		return options;
	}

	protected SetFeeIssue(Institution inst, String name, Set<String> cfvRoles,
			Set<String> voteRoles, VoteMethod method, String wdm, Set<String> roles, double min,
			double max, double increments) {
		super(inst, name, cfvRoles, voteRoles, method, wdm,
				createOptions(min, max, increments), roles, increments);
		this.minValue = min;
		this.maxValue = max;
	}

	@Override
	public void updateFee(Object winner) {
		try {
			double value = Double.parseDouble(winner.toString());
			setFee(value);
		} catch (NumberFormatException e) {
			throw (e);
		}
	}

}
