package kc;

import java.util.HashSet;
import java.util.Set;

import kc.prediction.Predictor;
import uk.ac.imperial.einst.EInstSession;
import uk.ac.imperial.einst.Institution;
import uk.ac.imperial.einst.access.RoleOf;
import uk.ac.imperial.einst.resource.ArtifactTypeMatcher;
import uk.ac.imperial.einst.resource.Pool;
import uk.ac.imperial.einst.resource.facility.Facility;

public class InstitutionBuilder {

	final EInstSession session;
	final DataInstitution inst;
	Set<Pool> pools = new HashSet<Pool>();

	Set<String> gatherers = RoleOf.roleSet("gatherer");
	Set<String> analysts = RoleOf.roleSet("analyst");
	Set<String> initiator = RoleOf.roleSet("initiator");
	Set<String> consumers = RoleOf.roleSet("consumer");
	Set<String> managers = RoleOf.roleSet("manager");

	public InstitutionBuilder(EInstSession session, String name,
			double borrowLimit) {
		super();
		this.session = session;
		inst = new DataInstitution(name, borrowLimit);
	}

	public InstitutionBuilder addMeasuredPool(double fee) {
		MeteredPool p = new MeteredPool(inst, gatherers, analysts, managers,
				new ArtifactTypeMatcher(Measured.class));
		pools.add(p);
		if (fee > 0)
			p.getAppropriationFees().put("analyst", fee);
		return this;
	}

	public InstitutionBuilder addPredictorPool(double fee) {
		MeteredPool p = new MeteredPool(inst, analysts, consumers, managers,
				new ArtifactTypeMatcher(Predictor.class));
		pools.add(p);
		if (fee > 0)
			p.getAppropriationFees().put("consumer", fee);
		return this;
	}

	public InstitutionBuilder addFacility(double sunk, double fixed,
			double marginalStorage, double marginalTrans) {
		session.insert(new Facility(inst, pools, sunk, fixed, marginalStorage,
				marginalTrans));
		return this;
	}

	public Institution build() {
		for (Pool p : pools) {
			session.insert(p);
		}
		session.insert(inst);
		session.insert(inst.getAccount());
		return inst;
	}

}
