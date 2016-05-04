package com.github.jrelay.usbhid;

import com.github.jrelay.Relay;

/**
 * Created by nightingale on 03.05.16.
 */
public class UsbHidRelayDriverExample {

    static{
       Relay.setDriver(UsbHidRelayDriver.class);
    }


    public static void main(String[] args) throws Exception{
        // get default relay
        Relay relay = Relay.getDefault();

        //open relay
        relay.open();

        //wait...
        Thread.sleep(2000l);

        //close relay
        relay.close();
    }
}
