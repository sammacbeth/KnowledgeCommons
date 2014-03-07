package kc.agents;

import java.util.LinkedList;
import java.util.Queue;

import kc.InstitutionService;
import uk.ac.imperial.einst.UnavailableModuleException;
import uk.ac.imperial.einst.ipower.IPower;
import uk.ac.imperial.einst.ipower.Obl;
import uk.ac.imperial.einst.ipower.ObligationReactive;

public class InstitutionalBehaviour implements Behaviour, ObligationReactive {

	final AbstractAgent s;

	Queue<Obl> obligations = new LinkedList<Obl>();
	boolean meetObligations = true;

	InstitutionService inst;
	IPower pow;

	public InstitutionalBehaviour(AbstractAgent s) {
		super();
		this.s = s;
	}

	@Override
	public void initialise() {
		this.inst = s.inst;
		try {
			pow = this.inst.getSession().getModule(IPower.class);
			pow.registerObligationReactive(this, s);
		} catch (UnavailableModuleException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void doBehaviour() {
		if (meetObligations)
			processObligations();
	}

	protected void processObligations() {
		while (!obligations.isEmpty()) {
			inst.act(obligations.poll().getAction());
		}
	}

	@Override
	public void onObligation(Obl obl) {
		obligations.add(obl);
	}
}
