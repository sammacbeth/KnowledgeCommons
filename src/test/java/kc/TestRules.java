package kc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.junit.Before;
import org.junit.Test;

import uk.ac.imperial.einst.Action;
import uk.ac.imperial.einst.Actor;
import uk.ac.imperial.einst.EInstSession;
import uk.ac.imperial.einst.StubActor;
import uk.ac.imperial.einst.access.RoleOf;
import uk.ac.imperial.einst.ipower.Obl;
import uk.ac.imperial.einst.micropay.Account;
import uk.ac.imperial.einst.micropay.Invoice;
import uk.ac.imperial.einst.micropay.Transfer;
import uk.ac.imperial.einst.resource.Pool;
import uk.ac.imperial.einst.resource.facility.Facility;
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
		DataInstitution i = new DataInstitution("i1", BORROW_LIM, "test");
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

	@Test
	public void testInstitutionProfit() {
		final double BORROW_LIM = 10;
		final double SUB_FEE = 1.0;

		session.LOG_WM = false;
		DataInstitution i = new DataInstitution("i1", BORROW_LIM);
		i.subscriptionFees.put("test", SUB_FEE);
		session.insert(i);
		session.insert(i.getAccount());
		session.insert(new Facility(i, Collections.<Pool> emptySet(), 5, 1, 0,
				0));

		Actor a1 = new StubActor("a1");
		Account ac1 = new Account(a1, 100);
		Actor a2 = new StubActor("a2");
		Account ac2 = new Account(a2, 100);
		session.insert(new RoleOf(a1, i, "test"));
		session.insert(new RoleOf(a2, i, "test"));
		session.insert(ac1);
		session.insert(ac2);

		session.incrementTime();
		
		DescriptiveStatistics paid = new DescriptiveStatistics(10);
		paid.addValue(5 + 1);
		DescriptiveStatistics received = new DescriptiveStatistics(10);
		received.addValue(0);
		Set<Obl> added = new HashSet<Obl>();
		for (int j = 0; j < 8; j++) {
			Set<Obl> obls = new HashSet<Obl>();
			obls.addAll(inst.ipower.getObligations(a1));
			obls.addAll(inst.ipower.getObligations(a2));
			obls.removeAll(added);
			for (Obl o : obls) {
				session.insert(o.getAction());
				added.add(o);
			}
			received.addValue(2);
			paid.addValue(1);
			session.incrementTime();
			double balance = received.getSum() - paid.getSum();
			assertEquals(balance, i.account.getBalance(), 0.00001);
			assertEquals(balance / (1.0+j), i.profit, 0.00001);
		}
		assertTrue(true);
	}
}
