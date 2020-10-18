package test;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.leekwars.generator.fight.StartOrder;
import com.leekwars.generator.Generator;
import com.leekwars.generator.fight.Fight;
import com.leekwars.generator.fight.Order;
import com.leekwars.generator.fight.entity.Entity;
import com.leekwars.generator.leek.Leek;

public class TestOrder {

	private Generator generator;
	private Fight fight;

	@Before
	public void setUp() throws Exception {

		generator = new Generator();
		fight = new Fight(generator);
	}

	@Test
	public void orderTest() {
		// Test de l'ordre de jeu
		Order order = new Order();
		Leek l1, l2, l3, l5;
		order.addEntity(l1 = new Leek(1, "J1"));
		order.addEntity(l2 = new Leek(2, "J2"));
		order.addEntity(l3 = new Leek(3, "J3"));
		order.addEntity(new Leek(4, "J4"));
		l5 = new Leek(5, "J5");

		Assert.assertEquals(order.current().getId(), 1);
		Assert.assertEquals(order.getTurn(), 1);
		order.next();
		Assert.assertEquals(order.current().getId(), 2);
		order.next();
		Assert.assertEquals(order.current().getId(), 3);
		order.next();
		Assert.assertEquals(order.current().getId(), 4);
		Assert.assertEquals(order.getTurn(), 1);
		order.next();
		Assert.assertEquals(order.current().getId(), 1);
		Assert.assertEquals(order.getTurn(), 2);
		order.removeEntity(l1);
		Assert.assertEquals(order.current().getId(), 4);
		order.next();
		Assert.assertEquals(order.current().getId(), 2);
		order.removeEntity(l3);
		Assert.assertEquals(order.current().getId(), 2);
		order.next();
		Assert.assertEquals(order.current().getId(), 4);
		order.removeEntity(l2);
		Assert.assertEquals(order.current().getId(), 4);
		order.next();
		Assert.assertEquals(order.current().getId(), 4);
		order.addEntity(l5);
		Assert.assertEquals(order.current().getId(), 4);
		order.next();
		Assert.assertEquals(order.current().getId(), 5);
		order.next();
		Assert.assertEquals(order.current().getId(), 4);
	}

	@Test
	@Ignore
	public void bootOrderTest() {

		StartOrder order = new StartOrder();

		// Ordre complet
		Leek l = new Leek(1, "J1 T1");
		l.setTeam(1);
		l.getBaseStats().setStat(Entity.CHARAC_FREQUENCY, 500);
		order.addEntity(l);
		Leek l2 = new Leek(2, "J2 T1");
		l2.setTeam(1);
		l2.getBaseStats().setStat(Entity.CHARAC_FREQUENCY, 800);
		order.addEntity(l2);
		Leek l3 = new Leek(3, "J3 T1");
		l3.setTeam(1);
		l3.getBaseStats().setStat(Entity.CHARAC_FREQUENCY, 480);
		order.addEntity(l3);
		Leek l4 = new Leek(4, "J1 T2");
		l4.setTeam(2);
		l4.getBaseStats().setStat(Entity.CHARAC_FREQUENCY, 360);
		order.addEntity(l4);
		Leek l5 = new Leek(5, "J2 T2");
		l5.setTeam(2);
		l5.getBaseStats().setStat(Entity.CHARAC_FREQUENCY, 100);
		order.addEntity(l5);
		Leek l6 = new Leek(6, "J3 T2");
		l6.setTeam(2);
		l6.getBaseStats().setStat(Entity.CHARAC_FREQUENCY, 130);
		order.addEntity(l6);
		List<Entity> leeks = order.compute(fight);
		Assert.assertEquals(leeks.get(0).getId(), 2);
		Assert.assertEquals(leeks.get(1).getId(), 4);
		Assert.assertEquals(leeks.get(2).getId(), 1);
		Assert.assertEquals(leeks.get(3).getId(), 6);
		Assert.assertEquals(leeks.get(4).getId(), 3);
		Assert.assertEquals(leeks.get(5).getId(), 5);

		// Order avec plus de joueurs en team2
		order = new StartOrder();
		order.addEntity(l2);
		order.addEntity(l3);
		order.addEntity(l4);
		order.addEntity(l5);
		order.addEntity(l6);
		leeks = order.compute(fight);
		Assert.assertEquals(leeks.get(0).getId(), 2);
		Assert.assertEquals(leeks.get(1).getId(), 4);
		Assert.assertEquals(leeks.get(2).getId(), 3);
		Assert.assertEquals(leeks.get(3).getId(), 6);
		Assert.assertEquals(leeks.get(4).getId(), 5);

		// Order avec plus de joueurs en team1
		order = new StartOrder();
		order.addEntity(l);
		order.addEntity(l2);
		order.addEntity(l3);
		order.addEntity(l5);
		order.addEntity(l6);
		leeks = order.compute(fight);
		Assert.assertEquals(leeks.get(0).getId(), 2);
		Assert.assertEquals(leeks.get(1).getId(), 6);
		Assert.assertEquals(leeks.get(2).getId(), 1);
		Assert.assertEquals(leeks.get(3).getId(), 5);
		Assert.assertEquals(leeks.get(4).getId(), 3);
	}
}
