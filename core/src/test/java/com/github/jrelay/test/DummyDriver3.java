package com.github.jrelay.test;

import com.github.jrelay.RelayDevice;
import com.github.jrelay.RelayDriver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by nightingale on 30.04.16.
 */
public class DummyDriver3 implements RelayDriver {

    private static final List<RelayDevice> DEVICES = new ArrayList<RelayDevice>(Arrays.asList(new RelayDevice[]{
            new DummyDevice(),
            new DummyDevice(),
            new DummyDevice(),
            new DummyDevice(),
    }));

    private static DummyDriver3 instance = null;

    public DummyDriver3() throws InstantiationException {
        if (instance == null) {
            instance = this;
        }
    }

    public static DummyDriver3 getInstance() {
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
