package kc;

import java.util.HashSet;
import java.util.Set;

import kc.choice.PoolAppropriatePay;
import kc.choice.PoolFee;
import kc.choice.SubscriptionFee;
import kc.prediction.Predictor;
import uk.ac.imperial.einst.EInstSession;
import uk.ac.imperial.einst.Institution;
import uk.ac.imperial.einst.access.RoleOf;
import uk.ac.imperial.einst.access.Roles;
import uk.ac.imperial.einst.resource.ArtifactMatcher;
import uk.ac.imperial.einst.resource.ArtifactTypeMatcher;
import uk.ac.imperial.einst.resource.Pool;
import uk.ac.imperial.einst.resource.facility.Facility;
import uk.ac.imperial.einst.vote.Borda;
import uk.ac.imperial.einst.vote.VoteMethod;

public class InstitutionBuilder {

	final EInstSession session;
	final DataInstitution inst;
	Set<Pool> pools = new HashSet<Pool>();

	Set<String> gatherers = Roles.set("gatherer");
	Set<String> analysts = Roles.set("analyst");
	Set<String> initiator = Roles.set("initiator");
	Set<String> consumers = Roles.set("consumer");
	Set<String> managers = Roles.set("manager");
	Set<String> evaluator = Roles.set("evaluator");

	static ArtifactMatcher measuredMatcher = new ArtifactTypeMatcher(
			Measured.class);
	static ArtifactMatcher predictorMatcher = new ArtifactTypeMatcher(
			Predictor.class);
	static ArtifactMatcher reviewMatcher = new ArtifactTypeMatcher(Review.class);

	public InstitutionBuilder(EInstSession session, String name,
			double borrowLimit, String... payRoles) {
		super();
		this.session = session;
		inst = new DataInstitution(name, borrowLimit, payRoles);
	}

	public InstitutionBuilder addSubscription(String role, double fee) {
		this.inst.getSubscriptionFees().put(role, fee);
		return this;
	}

	public InstitutionBuilder addDynamicSubscription(Set<String> roles,
			double fee, Set<String> cfv, Set<String> vote, double incrementValue) {
		SubscriptionFee issue = new SubscriptionFee(inst, cfv, vote,
				VoteMethod.RANK_ORDER, Borda.NAME, roles, incrementValue);
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

	public PoolBuilder addReviewPool() {
		return addPool(evaluator, Roles.union(evaluator, consumers), managers,
				reviewMatcher);
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

		public PoolBuilder setPayOnProvision(double payOnProvision) {
			pool.payOnProvision = payOnProvision;
			return this;
		}

		public PoolBuilder setPayOnAppropriation(double payOnAppropriation) {
			pool.payOnAppropriation = payOnAppropriation;
			return this;
		}

		public PoolBuilder dynamicPayOnAppropriation(Set<String> roles,
				double fee, Set<String> cfv, Set<String> vote,
				double incrementValue, boolean paidByAppropriators) {
			PoolAppropriatePay issue = new PoolAppropriatePay(pool, cfv, vote,
					roles, incrementValue, paidByAppropriators);
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
