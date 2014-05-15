package kc.choice;

import java.util.Map;
import java.util.Set;

import kc.DataInstitution;
import uk.ac.imperial.einst.vote.VoteMethod;

public class SubscriptionFee extends SetFeeIssue {

	final DataInstitution di;

	public SubscriptionFee(DataInstitution inst, Set<String> cfvRoles,
			Set<String> voteRoles, VoteMethod method, String wdm,
			Set<String> roles, double incrementValue) {
		super(inst, "subfee-" + roles.toString(), cfvRoles, voteRoles, method,
				wdm, roles, 0, 1, incrementValue);
		this.di = inst;
	}

	@Override
	public double getFee() {
		double fee = 0;
		Map<String, Double> fees = di.getSubscriptionFees();
		for (String r : roles) {
			if (fees.containsKey(r)) {
				fee += fees.get(r);
			}
		}
		return fee / roles.size();
	}

	@Override
	public void setFee(double value) {
		Map<String, Double> fees = di.getSubscriptionFees();
		for (String r : roles) {
			fees.put(r, value);
		}
	}

}
