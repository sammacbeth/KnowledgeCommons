package kc;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

import kc.agents.AbstractAgent;
import uk.ac.imperial.einst.Action;
import uk.ac.imperial.einst.EInstSession;
import uk.ac.imperial.einst.Module;
import uk.ac.imperial.einst.UnavailableModuleException;
import uk.ac.imperial.einst.access.AccessControl;
import uk.ac.imperial.einst.ipower.IPower;
import uk.ac.imperial.einst.resource.ProvisionAppropriationSystem;
import uk.ac.imperial.presage2.core.environment.EnvironmentService;
import uk.ac.imperial.presage2.core.environment.EnvironmentSharedStateAccess;

import com.google.inject.Inject;

public class InstitutionService extends EnvironmentService {

	EInstSession session;
	IPower ipower;

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
		this.session = new EInstSession(modules);
		this.ipower = this.session.getModule(IPower.class);
	}

	public EInstSession getSession() {
		return session;
	}

	public void registerAgent(AbstractAgent a) {
		ipower.registerObligationReactive(a, a);
	}

	public void act(Action act) {
		session.insert(act);
	}

}
