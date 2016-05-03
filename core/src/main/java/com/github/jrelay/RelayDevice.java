package com.github.jrelay;

import java.util.Map;

/**
 * Created by nightingale on 24.04.16.
 */
public interface RelayDevice {

    /**
     * This interface may be implemented by devices which expect any specific
     * parameters.
     *
     */
    public static interface Configurable {

        /**
         * Sets device parameters. Each device implementation may accept its own
         * set of parameters. All accepted keys, value types, possible values
         * and defaults should be reasonably documented by the implementor. May
         * be called before the open method or later in dependence of the device
         * implementation.
         *
         * @param parameters - Map of parameters changing device defaults
         * @see Relay#setParameters(Map)
         */
        void setParameters(Map<String, ?> parameters);
    }

    /**
     * Get device name.
     *
     * @return Device name
     */
    String getName();

    /**
     * Open device, it can be closed any time.
     */
    void open();

    /**
     * Close device, however it can be open again.
     */
    void close();

    /**
     * Dispose device. After device is disposed it cannot be open again.
     */
    void dispose();

    /**
     * Is relay device open?
     *
     * @return True if relay device is open, false otherwise
     */
    boolean isOpen();
}
