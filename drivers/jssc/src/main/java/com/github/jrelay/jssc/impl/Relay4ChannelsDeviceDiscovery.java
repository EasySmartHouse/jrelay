package com.github.jrelay.jssc.impl;

/**
 * Created by nightingale on 04.05.16.
 */
public class Relay4ChannelsDeviceDiscovery extends Relay2ChannelsDeviceDiscovery {

    private static final int MAX_CHANNELS_COUNT = 4;

    @Override
    public int getChannelsCount() {
        return MAX_CHANNELS_COUNT;
    }

}
