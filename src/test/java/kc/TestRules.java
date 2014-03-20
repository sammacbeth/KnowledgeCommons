package kc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import uk.ac.imperial.einst.Action;
import uk.ac.imperial.einst.Actor;
import uk.ac.imperial.einst.EInstSession;
import uk.ac.imperial.einst.StubActor;
import uk.ac.imperial.einst.access.RoleOf;
import uk.ac.imperial.einst.ipower.Obl;
import uk.ac.imperial.einst.micropay.Invoice;
import uk.ac.imperial.einst.micropay.Transfer;
import uk.ac.imperial.presage2.core.environment.EnvironmentSharedStateAccess;

public class TestRules {

	InstitutionService inst = null;
	EInstSession session = null;

	@Before
	public void setUp() throws Exception {
		if (inst == null) {
			EnvironmentSharedStateAccess ss = null;
			inst = new InstitutionService(ss);
		} else
			inst = new InstitutionService(inst);
		session = inst.session;
	}

	@Test
	public void testInstitutionBankruptcy() {
		final double BORROW_LIM = 10;
		final double INVOICE_AMOUNT = 11;
		assertTrue(INVOICE_AMOUNT > BORROW_LIM);

		session.LOG_WM = false;
		DataInstitution i = new DataInstitution("i1", BORROW_LIM);
		session.insert(i);
		session.insert(i.getAccount());
		session.incrementTime();
		assertFalse(inst.kc.isBankrupt(i));

		session.insert(new Invoice("a", i, "moneh", INVOICE_AMOUNT, 1));
		session.incrementTime();
		assertTrue(inst.kc.isBankrupt(i));
	}

	@Test
	public void testInstitutionCostSharing() {
		final double BORROW_LIM = 10;
		final double NEGATIVE_BALANCE = -2;
		// ensure institution isn't bankrupt
		assertTrue(BORROW_LIM > -1 * NEGATIVE_BALANCE);

		session.LOG_WM = false;
		DataInstitution i = new DataInstitution("i1", BORROW_LIM);
		i.payRole = "test";
		i.getAccount().setBalance(NEGATIVE_BALANCE);
		session.insert(i);
		session.insert(i.getAccount());

		Actor a1 = new StubActor("a1");
		Actor a2 = new StubActor("a2");
		Actor a3 = new StubActor("a3");

		session.insert(new RoleOf(a1, i, "test"));
		session.insert(new RoleOf(a2, i, "test"));
		session.insert(new RoleOf(a3, i, "other"));
		session.incrementTime();

		List<Obl> obls = inst.ipower.getObligations(a1);
		assertEquals(1, obls.size());
		assertTrue(obls.get(0).getAction()
				.equalsIgnoreT(new Transfer(a1, i, -1 * NEGATIVE_BALANCE / 2)));

		assertEquals(0, inst.ipower.getObligations(a3).size());
	}

	@Test
	public void testSubscriptionFee() {
		final double BORROW_LIM = 10;
		final double SUB_FEE = 1.0;

		session.LOG_WM = false;
		DataInstitution i = new DataInstitution("i1", BORROW_LIM);
		i.subscriptionFees.put("test", SUB_FEE);
		session.insert(i);
		session.insert(i.getAccount());

		Actor a1 = new StubActor("a1");
		Actor a2 = new StubActor("a2");
		session.insert(new RoleOf(a1, i, "test"));
		session.insert(new RoleOf(a2, i, "other"));
		// need an action for t
		session.insert(new Action(a1, i) {
		});
		session.incrementTime();

		List<Obl> obls = inst.ipower.getObligations(a1);
		assertEquals(1, obls.size());
		assertTrue(obls.get(0).getAction()
				.equalsIgnoreT(new Transfer(a1, i, SUB_FEE)));

		assertEquals(0, inst.ipower.getObligations(a2).size());
	}
}
