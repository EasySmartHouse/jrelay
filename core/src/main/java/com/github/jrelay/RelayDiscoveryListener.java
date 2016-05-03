package com.github.jrelay;

/**
 * Created by nightingale on 24.04.16.
 */
public interface RelayDiscoveryListener {

    void relayFound(RelayDiscoveryEvent event);

    void relayGone(RelayDiscoveryEvent event);

}
