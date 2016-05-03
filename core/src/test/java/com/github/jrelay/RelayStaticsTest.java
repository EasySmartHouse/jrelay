package com.github.jrelay;

import com.github.jrelay.dummy.RelayDummyDriver;
import com.github.jrelay.test.DummyDriver;
import com.github.jrelay.test.DummyDriver2;
import com.github.jrelay.test.DummyDriver3;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

/**
 * Created by nightingale on 30.04.16.
 */
public class RelayStaticsTest {

    @Before
    public void prepare() {
        Relay.resetDriver();
        System.out.println(Thread.currentThread().getName() + ": Register dummy driver");
        Relay.registerDriver(DummyDriver.class);
    }

    @After
    public void cleanup() {
        System.out.println(Thread.currentThread().getName() + ": Reset driver");
        for (Relay relay :  Relay.getRelays()) {
            relay.close();
        }
        Relay.resetDriver();
    }

    @Test
    public void testGetRelays() {

        System.out.println(Thread.currentThread().getName() + ": testGetRelay() start");

        List<Relay> relays = Relay.getRelays();
        List<RelayDevice> devices = DummyDriver.getInstance().getDevices();

        Assert.assertTrue(relays.size() > 0);
        Assert.assertEquals(devices.size(), relays.size());

        System.out.println(Thread.currentThread().getName() + ": testGetRelay() end");
    }

    @Test
    public void testGetDefault() {

        System.out.println(Thread.currentThread().getName() + ": testGetDefault() start");

        List<Relay> relays = Relay.getRelays();
        List<RelayDevice> devices = DummyDriver.getInstance().getDevices();

        Assert.assertNotNull(Relay.getDefault());
        Assert.assertSame(relays.get(0), Relay.getDefault());
        Assert.assertSame(devices.get(0), Relay.getDefault().getDevice());

        System.out.println(Thread.currentThread().getName() + ": testGetDefault() end");
    }

    @Test
    public void test_open() {

        System.out.println(Thread.currentThread().getName() + ": test_open() start");

        Relay relay = Relay.getDefault();
        relay.open();

        Assert.assertTrue(relay.isOpen());
        relay.open();
        Assert.assertTrue(relay.isOpen());

        System.out.println(Thread.currentThread().getName() + ": testOpen() end");
    }

    @Test
    public void testClose() {

        System.out.println(Thread.currentThread().getName() + ": testClose() start");

        Relay relay = Relay.getDefault();
        relay.open();

        Assert.assertSame(DummyDriver.class, Relay.getDriver().getClass());

        Assert.assertTrue(relay.isOpen());
        relay.close();
        Assert.assertFalse(relay.isOpen());
        relay.close();
        Assert.assertFalse(relay.isOpen());

        System.out.println(Thread.currentThread().getName() + ": test_close() end");
    }

    @Test
    public void test_setDriver() throws InstantiationException {

        Relay.setDriver(DummyDriver2.class);
        RelayDriver driver2 = Relay.getDriver();

        Assert.assertSame(DummyDriver2.class, driver2.getClass());

        RelayDriver driver3 = new DummyDriver3();
        Relay.setDriver(driver3);

        Assert.assertSame(driver3, Relay.getDriver());
    }

    @Test
    public void test_registerDriver() {

        Relay.resetDriver();

        Relay.registerDriver(DummyDriver.class);
        Relay.getRelays();
        RelayDriver driver = Relay.getDriver();

        Assert.assertSame(DummyDriver.class, driver.getClass());
    }

    @Test
    public void testGetRelayByName() throws InstantiationException {
        Relay.setDriver(new DummyDriver());
        for (Relay relay : Relay.getRelays()) {
            Assert.assertEquals(relay.getName(), Relay.getRelayByName(relay.getName()).getName());
        }
    }


    @Test(expected = IllegalArgumentException.class)
    public void testGetRelayByNameWithNullArgument() throws InstantiationException {
        Relay.setDriver(new DummyDriver());
        Relay.getRelayByName(null);
    }

}
