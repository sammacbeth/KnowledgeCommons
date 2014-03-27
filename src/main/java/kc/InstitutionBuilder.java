package kc;

import java.util.HashSet;
import java.util.Set;

import kc.choice.PoolFee;
import kc.prediction.Predictor;
import uk.ac.imperial.einst.EInstSession;
import uk.ac.imperial.einst.Institution;
import uk.ac.imperial.einst.access.RoleOf;
import uk.ac.imperial.einst.resource.ArtifactMatcher;
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

	static ArtifactMatcher measuredMatcher = new ArtifactTypeMatcher(
			Measured.class);
	static ArtifactMatcher predictorMatcher = new ArtifactTypeMatcher(
			Predictor.class);

	public InstitutionBuilder(EInstSession session, String name,
			double borrowLimit) {
		super();
		this.session = session;
		inst = new DataInstitution(name, borrowLimit);
	}

	public InstitutionBuilder addSubscription(String role, double fee) {
		this.inst.getSubscriptionFees().put(role, fee);
		return this;
	}

	public PoolBuilder addPool(Set<String> contrib, Set<String> extract,
			Set<String> remove, ArtifactMatcher matcher) {
		return new PoolBuilder(new MeteredPool(inst, contrib, extract, remove,
				matcher));
	}

	public PoolBuilder addMeasuredPool() {
		return addPool(gatherers, analysts, managers, measuredMatcher);
	}

	public InstitutionBuilder addMeasuredPool(double fee) {
		return addMeasuredPool().withFee("analyst", fee).end();
	}

	public InstitutionBuilder addMeasuredPool(double fee, Set<String> cfvRoles,
			Set<String> voteRoles, double incrementValue) {
		return addMeasuredPool().withDynamicFee("analyst", fee, cfvRoles,
				voteRoles, incrementValue).end();
	}

	public PoolBuilder addPredictorPool() {
		return addPool(analysts, consumers, managers, predictorMatcher);
	}

	public InstitutionBuilder addPredictorPool(double fee) {
		return addPredictorPool().withFee("consumer", fee).end();
	}

	public InstitutionBuilder addFacility(double sunk, double fixed,
			double marginalStorage, double marginalTrans) {
		session.insert(new Facility(inst, pools, sunk, fixed, marginalStorage,
				marginalTrans));
		return this;
	}

	public Institution build() {
		session.insert(inst);
		session.insert(inst.getAccount());
		return inst;
	}

	class PoolBuilder {

		final MeteredPool pool;

		PoolBuilder(MeteredPool pool) {
			super();
			this.pool = pool;
		}

		public PoolBuilder withFee(String role, double fee) {
			pool.getAppropriationFees().put("analyst", fee);
			return this;
		}

		public PoolBuilder withDynamicFee(String role, double fee,
				Set<String> cfv, Set<String> vote, double incrementValue) {
			pool.getAppropriationFees().put("analyst", fee);
			session.insert(new PoolFee(pool, role, cfv, vote, incrementValue));
			return this;
		}

		public InstitutionBuilder end() {
			session.insert(pool);
			pools.add(pool);
			return InstitutionBuilder.this;
		}

	}

}
