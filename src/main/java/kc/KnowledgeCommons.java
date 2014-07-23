package kc;

import java.util.Map;
import java.util.Set;

import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.rule.QueryResults;
import org.drools.runtime.rule.QueryResultsRow;

import uk.ac.imperial.einst.EInstSession;
import uk.ac.imperial.einst.Institution;
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

	public double getProvisionPay(Institution i, Object artifact) {
		QueryResults res = this.session.getQueryResults("getProvisionPay", i, artifact);
		if(res.size() == 1) {
			QueryResultsRow row = res.iterator().next();
			return (Double) row.get("$pay");
		}
		return 0;
	}
	
	public double getAppropriationFee(Institution i, Object artifact, String role) {
		QueryResults res = this.session.getQueryResults("getAppropriationFee", i, artifact);
		if(res.size() == 1) {
			@SuppressWarnings("unchecked")
			Map<String, Double> fees = (Map<String, Double>) res.iterator().next().get("$fees");
			if(fees.containsKey(role)) {
				return fees.get(role);
			}
		}
		return 0;
	}

}
