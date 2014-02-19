package kc.agents;

import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

import uk.ac.imperial.einst.Action;
import uk.ac.imperial.einst.Institution;
import uk.ac.imperial.einst.UnavailableModuleException;
import uk.ac.imperial.einst.ipower.IPower;
import uk.ac.imperial.einst.ipower.PowerReactive;
import uk.ac.imperial.einst.resource.Provision;
import kc.Measured;

public class GathererBehaviour implements Behaviour, PowerReactive {

	private static final String TYPE = "gatherer";
	final AbstractAgent s;
	Queue<Measured> incMeasured;
	IPower pow;
	Set<Institution> provisionTargets = new HashSet<Institution>();
	boolean checkTargets = false;

	public GathererBehaviour(AbstractAgent s) {
		super();
		this.s = s;
	}

	@Override
	public String getType() {
		return TYPE;
	}

	@Override
	public void initialise() {
		this.incMeasured = s.game.measuredQueueSubscribe(s.getID());
		try {
			pow = s.inst.getSession().getModule(IPower.class);
		} catch (UnavailableModuleException e) {
			throw new RuntimeException(e);
		}
		pow.registerPowerListener(s, new Provision(s, null, new Measured()),
				this);
	}

	@Override
	public void doBehaviour() {
		if (checkTargets) {
			for (Action act : pow.powList(s, new Provision(s, null,
					new Measured()))) {
				provisionTargets.add(act.getInst());
			}
			checkTargets = false;
		}
		while (!incMeasured.isEmpty()) {
			Measured m = incMeasured.poll();
			for (Institution i : provisionTargets) {
				s.inst.act(new Provision(s, i, m));
			}
		}
	}

	@Override
	public void onPower(Action act) {
		provisionTargets.add(act.getInst());
	}

	@Override
	public void onPowerRetraction(Action act) {
		// rebuild list
		provisionTargets.clear();
		checkTargets = true;
	}

}
