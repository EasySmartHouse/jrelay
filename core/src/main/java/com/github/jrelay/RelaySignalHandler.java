package com.github.jrelay;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Signal;
import sun.misc.SignalHandler;

/**
 * Created by nightingale on 24.04.16.
 */
@SuppressWarnings("restriction")
public class RelaySignalHandler implements SignalHandler {

    private static final Logger LOG = LoggerFactory.getLogger(RelaySignalHandler.class);

    private RelayDeallocator deallocator = null;

    private SignalHandler handler = null;

    public RelaySignalHandler() {
        handler = Signal.handle(new Signal("TERM"), this);
    }

    @Override
    public void handle(Signal signal) {
        LOG.warn("Detected signal {} {}, calling deallocator", signal.getName(), signal.getNumber());

        // do nothing on "signal default" or "signal ignore"
        if (handler == SIG_DFL || handler == SIG_IGN) {
            return;
        }

        try {
            deallocator.deallocate();
        } finally {
            handler.handle(signal);
        }

    }

    public void set(RelayDeallocator deallocator) {
        this.deallocator = deallocator;
    }

    public RelayDeallocator get() {
        return this.deallocator;
    }

    public void reset() {
        this.deallocator = null;
    }
}
