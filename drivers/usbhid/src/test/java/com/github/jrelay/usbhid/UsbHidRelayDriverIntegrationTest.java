package com.github.jrelay.usbhid;

import com.github.jrelay.RelayDevice;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class UsbHidRelayDriverIntegrationTest {

    @Test
    public void testGetDevices() throws Exception {
        UsbHidRelayDriver relayDriver = new  UsbHidRelayDriver();
        List<RelayDevice> relays = relayDriver.getDevices();
        for(RelayDevice device:relays){
            assertNotNull(device);
            device.open();
            device.close();
        }
    }

}