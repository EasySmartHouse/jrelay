package com.github.jrelay;

import org.easymock.EasyMockSupport;
import org.junit.Test;

import static org.junit.Assert.*;

import org.assertj.core.api.Assertions;
import org.easymock.EasyMock;
import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(EasyMockRunner.class)
public class RelayLockTest extends EasyMockSupport {

    Relay relay;

    @Before
    public void before() {

        relay = createNiceMock(Relay.class);

        EasyMock.expect(relay.getName())
                .andReturn("test-relay")
                .anyTimes();

        replayAll();
    }

    @Test
     public void testLock() throws Exception {
        RelayLock lock = new RelayLock(relay);
        lock.lock();

        Assertions.assertThat(lock.isLocked())
                .isTrue();

        lock.unlock();

        Assertions.assertThat(lock.isLocked())
                .isFalse();
    }

    @Test
    public void testLock2() throws Exception {

        RelayLock first = new RelayLock(relay);
        RelayLock second = new RelayLock(relay);

        first.lock();

        Assertions.assertThat(second.isLocked())
                .isTrue();

        first.unlock();
		
    }
}