package com.github.jrelay.jssc;

import com.github.jrelay.Relay;

import java.util.List;

/**
 * Created by nightingale on 04.05.16.
 */
public class JsscRelayDriverExample {

    static{
        Relay.setDriver(JsscRelayDriver.class);
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
