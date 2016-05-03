package com.github.jrelay;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

/**
 * Created by nightingale on 24.04.16.
 */
final class RelayDeallocator {

    private static final RelaySignalHandler HANDLER = new RelaySignalHandler();

    private final Relay[] relays;

    /**
     * This constructor is used internally to create new deallocator for the
     * given devices array.
     *
     * @param relays the devices to be stored in deallocator
     */
    private RelayDeallocator(Relay[] relays) {
        this.relays = relays;
    }

    /**
     * Store devices to be deallocated when TERM signal has been received.
     *
     * @param relays the relays array to be stored in deallocator
     */
    protected static void store(Relay[] relays) {
        if (HANDLER.get() == null) {
            HANDLER.set(new RelayDeallocator(relays));
        } else {
            throw new IllegalStateException("Deallocator is already set!");
        }
    }

    protected static void unstore() {
        HANDLER.reset();
    }

    protected void deallocate() {
        for(Relay relay : relays) {
            try {
                relay.dispose();
            } catch (Throwable t) {
                caugh(t);
            }
        }
    }

    private void caugh(Throwable t) {
        File f = new File(String.format("jrelay-hs-%s", System.currentTimeMillis()));
        PrintStream ps = null;
        try {
            t.printStackTrace(ps = new PrintStream(f));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (ps != null) {
                ps.close();
            }
        }
    }

}
