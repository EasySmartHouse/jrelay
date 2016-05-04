package com.github.jrelay.jssc.impl;

/**
 * Created by nightingale on 04.05.16.
 */
public interface DeviceDiscovery<H> {

    public boolean isDeviceAvailable(H handler);

}
