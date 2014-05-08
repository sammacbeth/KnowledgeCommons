package kc;

import java.util.HashSet;
import java.util.Set;

import kc.choice.PoolFee;
import kc.choice.SubscriptionFee;
import kc.prediction.Predictor;
import uk.ac.imperial.einst.EInstSession;
import uk.ac.imperial.einst.Institution;
import uk.ac.imperial.einst.access.RoleOf;
import uk.ac.imperial.einst.resource.ArtifactMatcher;
import uk.ac.imperial.einst.resource.ArtifactTypeMatcher;
import uk.ac.imperial.einst.resource.Pool;
import uk.ac.imperial.einst.resource.facility.Facility;
import uk.ac.imperial.einst.vote.Plurality;
import uk.ac.imperial.einst.vote.VoteMethod;

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

	public InstitutionBuilder addDynamicSubscription(Set<String> roles,
			double fee, Set<String> cfv, Set<String> vote, double incrementValue) {
		SubscriptionFee issue = new SubscriptionFee(inst, cfv, vote,
				VoteMethod.SINGLE, Plurality.NAME, roles, incrementValue);
		issue.setFee(fee);
		session.insert(issue);
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
		return addMeasuredPool().withDynamicFee(RoleOf.roleSet("analyst"), fee,
				cfvRoles, voteRoles, incrementValue).end();
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
			pool.getAppropriationFees().put(role, fee);
			return this;
		}

		public PoolBuilder withDynamicFee(Set<String> roles, double fee,
				Set<String> cfv, Set<String> vote, double incrementValue) {
			PoolFee issue = new PoolFee(pool, roles, cfv, vote, incrementValue);
			issue.setFee(fee);
			session.insert(issue);
			return this;
		}

		public InstitutionBuilder end() {
			session.insert(pool);
			pools.add(pool);
			return InstitutionBuilder.this;
		}

	}

}
