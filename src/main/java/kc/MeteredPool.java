package kc;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import uk.ac.imperial.einst.Institution;
import uk.ac.imperial.einst.resource.ArtifactMatcher;
import uk.ac.imperial.einst.resource.Pool;

public class MeteredPool extends Pool {

	final Map<String, Double> appropriationFees = new HashMap<String, Double>();
	
	public MeteredPool(Institution inst, Set<String> contribRoles,
			Set<String> extractRoles, Set<String> removeRoles, ArtifactMatcher artifactMatcher) {
		super(inst, contribRoles, extractRoles, removeRoles, artifactMatcher);
	}

	public Map<String, Double> getAppropriationFees() {
		return appropriationFees;
	}

}