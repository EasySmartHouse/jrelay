package com.github.jrelay.dummy;

import com.github.jrelay.RelayDevice;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by nightingale on 29.04.16.
 */
public class RelayDummyDevice implements RelayDevice {

    private AtomicBoolean open = new AtomicBoolean(false);

    private final String name;

    public RelayDummyDevice(int number) {
        this.name = "Dummy relay " + number;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void open() {
        if (open.compareAndSet(false, true)) {
            // ...
        }
    }

    @Override
    public void close() {
        if (open.compareAndSet(true, false)) {
            // ...
        }
    }

    @Override
    public void dispose() {
        close();
    }

    @Override
    public boolean isOpen() {
        return open.get();
    }
}
