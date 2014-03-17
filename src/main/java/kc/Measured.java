package kc;

public class Measured {

	final String by;
	final State state;
	final int strategy;
	final double payoff;
	final int t;

	public Measured() {
		super();
		by = null;
		state = null;
		strategy = 0;
		payoff = 0;
		t = -1;
	}

	public Measured(String by, State state, int strategy, double payoff, int t) {
		super();
		this.by = by;
		this.state = state;
		this.strategy = strategy;
		this.payoff = payoff;
		this.t = t;
	}

	public String getBy() {
		return by;
	}

	public State getState() {
		return state;
	}

	public int getStrategy() {
		return strategy;
	}

	public double getPayoff() {
		return payoff;
	}

	public int getT() {
		return t;
	}

	@Override
	public String toString() {
		return "Measured [state=" + state + ", strategy=" + strategy
				+ ", payoff=" + payoff + ", t=" + t + "]";
	}

}
