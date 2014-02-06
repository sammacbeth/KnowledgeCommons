package kc;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

import kc.agents.AbstractAgent;
import uk.ac.imperial.einst.EInstSession;
import uk.ac.imperial.einst.Module;
import uk.ac.imperial.einst.access.AccessControl;
import uk.ac.imperial.einst.ipower.IPower;
import uk.ac.imperial.presage2.core.environment.EnvironmentService;
import uk.ac.imperial.presage2.core.environment.EnvironmentSharedStateAccess;

import com.google.inject.Inject;

public class InstitutionService extends EnvironmentService {

	EInstSession session;

	@Inject
	public InstitutionService(EnvironmentSharedStateAccess sharedState)
			throws NoSuchMethodException, SecurityException,
			InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException {
		super(sharedState);
		Set<Class<? extends Module>> modules = new HashSet<Class<? extends Module>>();
		modules.add(AccessControl.class);
		modules.add(IPower.class);
		this.session = new EInstSession(modules);
	}

	public EInstSession getSession() {
		return session;
	}

	public void registerAgent(AbstractAgent a) {
		
	}

}
