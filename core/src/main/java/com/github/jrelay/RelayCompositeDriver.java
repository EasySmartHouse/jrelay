package com.github.jrelay;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nightingale on 24.04.16.
 */
public class RelayCompositeDriver implements RelayDriver {

    private List<RelayDriver> drivers = new ArrayList<RelayDriver>();

    public RelayCompositeDriver(RelayDriver... drivers) {
        for (RelayDriver driver : drivers) {
            this.drivers.add(driver);
        }
    }

    public void add(RelayDriver driver) {
        drivers.add(driver);
    }

    public List<RelayDriver> getDrivers() {
        return drivers;
    }

    @Override
    public List<RelayDevice> getDevices() {
        List<RelayDevice> all = new ArrayList<RelayDevice>();
        for (RelayDriver driver : drivers) {
            all.addAll(driver.getDevices());
        }
        return all;
    }

    @Override
    public boolean isThreadSafe() {
        boolean safe = true;
        for (RelayDriver driver : drivers) {
            safe &= driver.isThreadSafe();
            if (!safe) {
                break;
            }
        }
        return safe;
    }

}
