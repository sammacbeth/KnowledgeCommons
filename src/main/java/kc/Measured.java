package kc;

public class Measured {

	final State state;
	final int strategy;
	final double payoff;
	final int t;

	public Measured(State state, int strategy, double payoff, int t) {
		super();
		this.state = state;
		this.strategy = strategy;
		this.payoff = payoff;
		this.t = t;
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
