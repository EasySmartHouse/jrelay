package com.github.jrelay;

import java.util.List;

/**
 * Relay drivers abstraction. This is a factory for specific relay device implementations.
 *
 * @author nightingale
 */
public interface RelayDriver {

    /**
     * Return all registered relay devices.
     *
     * @return List of relay devices
     */
    List<RelayDevice> getDevices();

    /**
     * Is driver thread-safe. Thread safe drivers operations does not have to be
     * synchronized.
     *
     * @return True in case if driver is thread-safe, false otherwise
     */
    boolean isThreadSafe();

}
