package com.github.jrelay.examples;

import com.github.jrelay.Relay;
import com.github.jrelay.usbhid.UsbHidRelayDriver;

/**
 * Created by nightingale on 03.05.16.
 */
public class ManyRelaysExample {

    static {
        // set driver
        Relay.setDriver(UsbHidRelayDriver.class);
    }

    public static void main(String[] args) {
        // get default relay
        Relay relay = Relay.getDefault();

        //open relay
        relay.open();

        //wait...
        try {
            Thread.sleep(2000l);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        relay.close();
    }

}
