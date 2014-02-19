package kc.agents;

import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

import kc.Measured;
import kc.util.MultiUserQueue;
import uk.ac.imperial.einst.Action;
import uk.ac.imperial.einst.Institution;
import uk.ac.imperial.einst.UnavailableModuleException;
import uk.ac.imperial.einst.ipower.IPower;
import uk.ac.imperial.einst.ipower.PowerReactive;
import uk.ac.imperial.einst.resource.Appropriate;
import uk.ac.imperial.einst.resource.AppropriationsListener;
import uk.ac.imperial.einst.resource.ProvisionAppropriationSystem;
import uk.ac.imperial.einst.resource.Request;

public class ConsumerBehaviour implements Behaviour, PowerReactive,
		AppropriationsListener {

	private static final String TYPE = "consumer";

	final AbstractAgent s;
	IPower pow;
	ProvisionAppropriationSystem sys;
	Set<Institution> appTargets = new HashSet<Institution>();
	boolean checkTargets = false;

	int lastRequest = -1;
	MultiUserQueue<Measured> appropriatedMeasured = new MultiUserQueue<Measured>();

	public ConsumerBehaviour(AbstractAgent s) {
		super();
		this.s = s;
	}

	@Override
	public String getType() {
		return TYPE;
	}

	@Override
	public void initialise() {
		try {
			pow = s.inst.getSession().getModule(IPower.class);
			sys = s.inst.getSession().getModule(
					ProvisionAppropriationSystem.class);
		} catch (UnavailableModuleException e) {
			throw new RuntimeException(e);
		}
		pow.registerPowerListener(s, new Appropriate(s, null, new Measured()),
				this);
		sys.registerForAppropriations(s, this);
	}

	@Override
	public void doBehaviour() {
		if (checkTargets) {
			for (Action act : pow.powList(s, new Appropriate(s, null,
					new Measured()))) {
				appTargets.add(act.getInst());
			}
			checkTargets = false;
		}
		for (Institution i : appTargets) {
			s.inst.act(new Request(s, i, new MeasuredMatcher()
					.setNewerThan(lastRequest), 10));
		}
	}

	@Override
	public void onPower(Action act) {
		appTargets.add(act.getInst());
	}

	@Override
	public void onPowerRetraction(Action act) {
		// rebuild list
		appTargets.clear();
		checkTargets = true;
	}

	@Override
	public void onAppropriation(Object artifact) {
		if (artifact instanceof Measured) {
			Measured m = (Measured) artifact;
			this.lastRequest = Math.max(this.lastRequest, m.getT());
			appropriatedMeasured.publish(m);
		}
	}

	void measuredQueueSubscribe(Queue<Measured> q) {
		appropriatedMeasured.subscribe(q);
	}

}
