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
import uk.ac.imperial.presage2.core.environment.EnvironmentService;
import uk.ac.imperial.presage2.core.environment.EnvironmentSharedStateAccess;
import uk.ac.imperial.presage2.core.plugin.Plugin;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class InstitutionService extends EnvironmentService implements Plugin {

	int t = 0;
	EInstSession session;
	IPower ipower;
	MicroPayments payments;

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
		this.session = new EInstSession(modules);
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
		if (sto != null)
			sto.insertDroolsSnapshot(t, session.getObjects());
	}

}
