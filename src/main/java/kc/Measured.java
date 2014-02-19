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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((by == null) ? 0 : by.hashCode());
		long temp;
		temp = Double.doubleToLongBits(payoff);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((state == null) ? 0 : state.hashCode());
		result = prime * result + strategy;
		result = prime * result + t;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Measured other = (Measured) obj;
		if (by == null) {
			if (other.by != null)
				return false;
		} else if (!by.equals(other.by))
			return false;
		if (Double.doubleToLongBits(payoff) != Double
				.doubleToLongBits(other.payoff))
			return false;
		if (state == null) {
			if (other.state != null)
				return false;
		} else if (!state.equals(other.state))
			return false;
		if (strategy != other.strategy)
			return false;
		if (t != other.t)
			return false;
		return true;
	}

}
