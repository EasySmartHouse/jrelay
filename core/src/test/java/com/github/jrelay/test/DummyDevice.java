package com.github.jrelay.test;

import com.github.jrelay.RelayDevice;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by nightingale on 30.04.16.
 */
public class DummyDevice implements RelayDevice {

    private static final AtomicInteger INSTANCE_NUM = new AtomicInteger(0);


    private String name = DummyDevice.class.getSimpleName() + "-" + INSTANCE_NUM.incrementAndGet();
    private boolean open = false;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void open() {
        open = true;
    }

    @Override
    public void close() {
        open = false;
    }

    public boolean isOpen() {
        return open;
    }

    @Override
    public void dispose() {
        // do nothing
    }
}
