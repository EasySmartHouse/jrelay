package com.github.jrelay.usbhid;

import com.github.jrelay.Relay;

/**
 * Created by nightingale on 03.05.16.
 */
public class UsbHidRelayDriverExample {

    static{
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
