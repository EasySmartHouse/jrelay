package com.github.jrelay.test;

import com.github.jrelay.RelayDevice;
import com.github.jrelay.RelayDriver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by nightingale on 30.04.16.
 */
public class DummyDriver implements RelayDriver {

    private static final List<RelayDevice> DEVICES = new ArrayList<RelayDevice>(Arrays.asList(new RelayDevice[]{
            new DummyDevice(),
            new DummyDevice(),
            new DummyDevice(),
            new DummyDevice(),
    }));

    private static DummyDriver instance = null;

    public DummyDriver() throws InstantiationException {
        if (instance == null) {
            instance = this;
        }
    }

    public static DummyDriver getInstance() {
        return instance;
    }

    @Override
    public List<RelayDevice> getDevices() {
        return DEVICES;
    }

    @Override
    public boolean isThreadSafe() {
        return false;
    }

}
