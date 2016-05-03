package com.github.jrelay;

/**
 * Created by m.rusakovich on 26.04.2016.
 */
public interface RelayListener {

    void relayOpen(RelayEvent relayEvent);

    void relayClosed(RelayEvent relayEvent);

    void relayDisposed(RelayEvent relayEvent);

}
