package kc;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import uk.ac.imperial.einst.Institution;
import uk.ac.imperial.einst.micropay.Account;

public class DataInstitution implements Institution {

	final String name;
	final Account account;
	double profit = 0;

	Set<String> payRoles = new HashSet<String>();
	final Map<String, Double> subscriptionFees = new HashMap<String, Double>();

	DataInstitution(String name, double borrowLimit, String... payRoles) {
		super();
		this.name = name;
		this.account = new Account(this, 0, borrowLimit);
		Collections.addAll(this.payRoles, payRoles);
	}

	@Override
	public String toString() {
		return name;
	}

	public Set<String> getPayRoles() {
		return payRoles;
	}

	public String getName() {
		return name;
	}

	public Account getAccount() {
		return account;
	}

	public Map<String, Double> getSubscriptionFees() {
		return subscriptionFees;
	}

	public double getProfit() {
		return profit;
	}

	public void setProfit(double profit) {
		this.profit = profit;
	}

}
