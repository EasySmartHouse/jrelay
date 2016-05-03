package com.github.jrelay.dummy;

import com.github.jrelay.RelayDevice;
import com.github.jrelay.RelayDiscoverySupport;
import com.github.jrelay.RelayDriver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by nightingale on 29.04.16.
 */
public class RelayDummyDriver implements RelayDriver, RelayDiscoverySupport {
    private int count;

    public RelayDummyDriver(int count) {
        this.count = count;
    }

    @Override
    public long getScanInterval() {
        return 10000;
    }

    @Override
    public boolean isScanPossible() {
        return true;
    }

    @Override
    public List<RelayDevice> getDevices() {
        List<RelayDevice> devices = new ArrayList<RelayDevice>();
        for (int i = 0; i < count; i++) {
            devices.add(new RelayDummyDevice(i));
        }
        return Collections.unmodifiableList(devices);
    }

    @Override
    public boolean isThreadSafe() {
        return false;
    }
}
