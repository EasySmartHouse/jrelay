package com.github.jrelay.examples;

import com.github.jrelay.*;
import com.github.jrelay.jssc.JsscRelayDriver;
import com.github.jrelay.usbhid.UsbHidRelayDriver;

import java.util.List;

/**
 * Created by nightingale on 05.05.16.
 */
public class DiscoveryListenerExample {

    static {
        // set discovery support driver
        JsscRelayDriver jsscRelayDriver = new JsscRelayDriver(true);
        Relay.setDriver(jsscRelayDriver);
    }

    private static class GlobalDiscoveryListener implements RelayDiscoveryListener{
        @Override
        public void relayFound(RelayDiscoveryEvent event) {
            System.out.println("Relay found:" + event.toString());
        }

        @Override
        public void relayGone(RelayDiscoveryEvent event) {
            System.out.println("Relay gone:" + event.toString());
        }
    }

    public static void main(String[] args) throws Exception{
        GlobalDiscoveryListener discoveryListener = new GlobalDiscoveryListener();
        Relay.addDiscoveryListener(discoveryListener);

        System.out.println("Relays available: " + Relay.getRelays().size());
        System.out.println("Please, unplug the device...");
        Thread.sleep(10000l);

        System.out.println("Relays available: " + Relay.getRelays().size());
    }

}
