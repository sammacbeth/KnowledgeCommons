package kc.agents;

import kc.Measured;
import uk.ac.imperial.einst.resource.ArtifactTypeMatcher;

public class MeasuredMatcher extends ArtifactTypeMatcher {

	int newerThan = 0;

	public MeasuredMatcher() {
		super(Measured.class);
	}

	public MeasuredMatcher setNewerThan(int from) {
		this.newerThan = from;
		return this;
	}

	@Override
	public boolean matches(Object artifact) {
		if (super.matches(artifact)) {
			Measured m = (Measured) artifact;
			return m.getT() > newerThan;
		}
		return false;
	}

	@Override
	public String toString() {
		return "(" + super.toString() + ", newerThan(" + newerThan + "))";
	}

}
