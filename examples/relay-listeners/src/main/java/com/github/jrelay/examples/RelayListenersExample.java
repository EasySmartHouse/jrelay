package com.github.jrelay.examples;

import com.github.jrelay.Relay;
import com.github.jrelay.RelayEvent;
import com.github.jrelay.RelayListener;
import com.github.jrelay.jssc.JsscRelayDriver;
import com.github.jrelay.usbhid.UsbHidRelayDriver;

import java.util.List;

/**
 * Created by nightingale on 05.05.16.
 */
public class RelayListenersExample {

    static {
        // set drivers
        Relay.setDrivers(UsbHidRelayDriver.class, JsscRelayDriver.class);
    }

    private static class GlobalRelayListener implements RelayListener{

        @Override
        public void relayOpen(RelayEvent relayEvent) {
            System.out.println("Relay opened: " + relayEvent.toString());
        }

        @Override
        public void relayClosed(RelayEvent relayEvent) {
            System.out.println("Relay closed: " + relayEvent.toString());
        }

        @Override
        public void relayDisposed(RelayEvent relayEvent) {
            System.out.println("Relay disposed: " + relayEvent.toString());

        }
    }

    public static void main(String[] args) throws Exception{
        GlobalRelayListener globalListener = new GlobalRelayListener();

        // get all available relays
        List<Relay> relays = Relay.getRelays();
        for(Relay relay: relays){
            //add relay listener
            relay.addRelayListener(globalListener);
            //open relay
            relay.open();
            //wait
            Thread.sleep(2000l);
            //relay close
            relay.close();
            //wait again..
            Thread.sleep(2000l);
        }
    }

}
