package kc;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

import kc.agents.NonPlayerAgent;
import kc.util.KCStorage;
import uk.ac.imperial.einst.Action;
import uk.ac.imperial.einst.EInstSession;
import uk.ac.imperial.einst.Institution;
import uk.ac.imperial.einst.Module;
import uk.ac.imperial.einst.UnavailableModuleException;
import uk.ac.imperial.einst.access.AccessControl;
import uk.ac.imperial.einst.ipower.IPower;
import uk.ac.imperial.einst.micropay.Account;
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

	Set<DataInstitution> institutions = new HashSet<DataInstitution>();

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
		if (sto != null) {
			for (Account ac : payments.getAccounts()) {
				if (ac.getHolder() instanceof NonPlayerAgent)
					sto.insertPlayerGameRound(t, ac.getHolder().toString(), 0,
							0, ac.getBalance());
			}
			for (DataInstitution i : institutions) {
				sto.insertPlayerGameRound(t, i.name, 0, 0,
						i.account.getBalance());
			}
		}
		t++;
	}

	@Override
	public void initialise() {
		if (sto != null) {
			Set<Object> sessionObjects = session.getObjects();
			sto.insertInitialState(sessionObjects);
			for (Object o : sessionObjects) {
				if (o instanceof DataInstitution) {
					institutions.add((DataInstitution) o);
				}
			}
		}
	}

	@Override
	public void execute() {
	}

	@Override
	public void onSimulationComplete() {
		// session.printActionLog();
		if (sto != null) {
			sto.insertDroolsSnapshot(t, session.getObjects());
			for (int t = ++tminus1; t <= this.t; t++) {
				if (session.getActionLog().containsKey(t)) {
					sto.insertActions(t, session.getActionLog().get(t));
				}
			}
		}
	}

	public double getBalance(Institution i) {
		return payments.getAccount(i).getBalance();
	}

}
