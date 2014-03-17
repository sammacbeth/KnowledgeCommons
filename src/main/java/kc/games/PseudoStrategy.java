package kc.games;

import kc.Strategy;
import kc.prediction.PseudoPredictor;

public class PseudoStrategy extends Strategy {

	final PseudoPredictor source;

	public PseudoStrategy(PseudoPredictor source) {
		super(0, true);
		this.source = source;
	}

	@Override
	public String toString() {
		return "PseudoStrategy [" + source + "]";
	}

}
