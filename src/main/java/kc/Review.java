package kc;

import uk.ac.imperial.einst.Actor;

public class Review {

	final Actor by;
	final Object of;
	final double payoff;
	final double cost;
	final int t;

	public Review(Actor by, Object of, double payoff, double cost, int t) {
		super();
		this.by = by;
		this.of = of;
		this.payoff = payoff;
		this.cost = cost;
		this.t = t;
	}

	public Actor getBy() {
		return by;
	}

	public Object getOf() {
		return of;
	}

	public double getPayoff() {
		return payoff;
	}

	public double getCost() {
		return cost;
	}

	public int getT() {
		return t;
	}

	public double getScore() {
		return payoff - cost;
	}

}
