package com.github.jrelay.examples;

import com.github.jrelay.Relay;
import com.github.jrelay.jssc.JsscRelayDriver;
import com.github.jrelay.usbhid.UsbHidRelayDriver;

import java.util.List;

/**
 * Created by nightingale on 05.05.16.
 */
public class ManyRelaysExample {

    static {
        // set drivers
        Relay.setDrivers(UsbHidRelayDriver.class, JsscRelayDriver.class);
    }

    public static void main(String[] args) throws Exception{
        // get all available relays
        List<Relay> relays = Relay.getRelays();
        for(Relay relay: relays){
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
