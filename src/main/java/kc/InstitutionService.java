package kc;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

import kc.util.KCStorage;
import uk.ac.imperial.einst.Action;
import uk.ac.imperial.einst.EInstSession;
import uk.ac.imperial.einst.Module;
import uk.ac.imperial.einst.UnavailableModuleException;
import uk.ac.imperial.einst.access.AccessControl;
import uk.ac.imperial.einst.ipower.IPower;
import uk.ac.imperial.einst.micropay.MicroPayments;
import uk.ac.imperial.einst.resource.ProvisionAppropriationSystem;
import uk.ac.imperial.einst.resource.facility.Facilities;
import uk.ac.imperial.einst.vote.Voting;
import uk.ac.imperial.presage2.core.environment.EnvironmentService;
import uk.ac.imperial.presage2.core.environment.EnvironmentSharedStateAccess;
import uk.ac.imperial.presage2.core.plugin.Plugin;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class InstitutionService extends EnvironmentService implements Plugin {

	int t = 0;
	int tminus1 = 0;
	EInstSession session;
	IPower ipower;
	MicroPayments payments;
	KnowledgeCommons kc;

	KCStorage sto;

	@Inject
	public InstitutionService(EnvironmentSharedStateAccess sharedState)
			throws NoSuchMethodException, SecurityException,
			InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException,
			UnavailableModuleException {
		super(sharedState);
		Set<Class<? extends Module>> modules = new HashSet<Class<? extends Module>>();
		modules.add(AccessControl.class);
		modules.add(IPower.class);
		modules.add(ProvisionAppropriationSystem.class);
		modules.add(MicroPayments.class);
		modules.add(Facilities.class);
		modules.add(KnowledgeCommons.class);
		modules.add(Voting.class);
		modules.add(CollectiveChoice.class);
		this.session = new EInstSession(modules);
		this.ipower = this.session.getModule(IPower.class);
		this.payments = this.session.getModule(MicroPayments.class);
		this.kc = this.session.getModule(KnowledgeCommons.class);
	}

	/**
	 * Copy ctor for tests.
	 * 
	 * @param iService
	 * @throws UnavailableModuleException
	 */
	InstitutionService(InstitutionService iService)
			throws UnavailableModuleException {
		super(iService.sharedState);
		this.session = new EInstSession(iService.session);
		this.ipower = this.session.getModule(IPower.class);
		this.payments = this.session.getModule(MicroPayments.class);
	}

	@Inject(optional = true)
	public void setStorage(KCStorage sto) {
		this.sto = sto;
	}

	public EInstSession getSession() {
		return session;
	}

	public void act(Action act) {
		session.insert(act);
	}

	@Override
	public void incrementTime() {
		tminus1 = t - 1;
		if (sto != null && session.getActionLog().containsKey(tminus1)) {
			sto.insertActions(tminus1, session.getActionLog().get(tminus1));
		}
		t++;
	}

	@Override
	public void initialise() {
	}

	@Override
	public void execute() {
	}

	@Override
	public void onSimulationComplete() {
		session.printActionLog();
		if (sto != null) {
			sto.insertDroolsSnapshot(t, session.getObjects());
			for (int t = ++tminus1; t <= this.t; t++) {
				if (session.getActionLog().containsKey(t)) {
					sto.insertActions(t, session.getActionLog().get(t));
				}
			}
		}
	}

}
