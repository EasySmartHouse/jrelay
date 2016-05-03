package com.github.jrelay;

import org.easymock.EasyMockSupport;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import org.easymock.EasyMock;
import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(EasyMockRunner.class)
public class RelayTest extends EasyMockSupport {

    @Mock
    RelayDriver driver;

    @Mock
    RelayDevice device;

    @Test
    public void testOpen() {

        EasyMock.expect(device.getName())
                .andReturn("Relay Mock Device")
                .anyTimes();

        EasyMock.expect(device.isOpen())
                .andReturn(false)
                .once();

        device.open();
        EasyMock.expectLastCall()
                .once();

        device.dispose();
        EasyMock.expectLastCall()
                .anyTimes();

        EasyMock.expect(driver.getDevices())
                .andReturn(new ArrayList<RelayDevice>(Arrays.asList(device)))
                .anyTimes();

        EasyMock.expect(driver.isThreadSafe())
                .andReturn(true)
                .anyTimes();

        replayAll();

        Relay.setDriver(driver);

        Relay relay =  Relay.getDefault();
        relay.open();

        verifyAll();
    }
}