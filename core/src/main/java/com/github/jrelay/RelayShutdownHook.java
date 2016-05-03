package com.github.jrelay;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Shutdown hook to be executed when JVM exits gracefully. This class intention
 * is to be used internally only.
 *
 * Created by nightingale on 29.04.16.
 */
public final class RelayShutdownHook extends Thread {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(RelayShutdownHook.class);

    /**
     * Number of shutdown hook instance.
     */
    private static int number = 0;

    /**
     * Webcam instance to be disposed / closed.
     */
    private Relay relay = null;

    /**
     * Create new shutdown hook instance.
     *
     * @param relay the relay for which hook is intended
     */
    protected RelayShutdownHook(Relay relay) {
        super("shutdown-hook-" + (++number));
        this.relay = relay;
        this.setUncaughtExceptionHandler(RelayExceptionHandler.getInstance());
    }

    @Override
    public void run() {
        LOG.info("Automatic {} deallocation", relay.getName());
        relay.dispose();
    }
}
