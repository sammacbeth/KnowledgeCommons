package kc;

import org.drools.runtime.StatefulKnowledgeSession;

import uk.ac.imperial.einst.EInstSession;
import uk.ac.imperial.einst.Module;
import uk.ac.imperial.einst.RuleResources;

@RuleResources({ "collective.drl" })
public class CollectiveChoice implements Module {

	@Override
	public void initialise(EInstSession eInstSession,
			StatefulKnowledgeSession session) {

	}

}
