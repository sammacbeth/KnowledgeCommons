package kc;

import java.util.HashMap;
import java.util.Map;

import uk.ac.imperial.einst.Institution;
import uk.ac.imperial.einst.micropay.Account;

public class DataInstitution implements Institution {

	final String name;
	final Account account;
	double profit = 0;

	String payRole = "initiator";
	final Map<String, Double> subscriptionFees = new HashMap<String, Double>();

	DataInstitution(String name, double borrowLimit) {
		super();
		this.name = name;
		this.account = new Account(this, 0, borrowLimit);
	}

	@Override
	public String toString() {
		return name;
	}

	public String getPayRole() {
		return payRole;
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
