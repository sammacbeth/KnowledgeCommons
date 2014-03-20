package kc;

import org.drools.runtime.StatefulKnowledgeSession;

import uk.ac.imperial.einst.EInstSession;
import uk.ac.imperial.einst.Module;
import uk.ac.imperial.einst.RuleResources;

@RuleResources({ "kc.drl" })
public class KnowledgeCommons implements Module {

	StatefulKnowledgeSession session;

	@Override
	public void initialise(EInstSession eInstSession,
			StatefulKnowledgeSession session) {
		this.session = session;
	}

	public boolean isBankrupt(DataInstitution i) {
		return this.session.getQueryResults("isBankrupt", i).size() > 0;
	}

}
