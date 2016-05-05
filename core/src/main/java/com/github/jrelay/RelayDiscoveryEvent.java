package com.github.jrelay;

import java.util.EventObject;

/**
 * Created by nightingale on 24.04.16.
 *
 * This event is generated when relay has been found or lost.
 */
public class RelayDiscoveryEvent extends EventObject {

    private static final long serialVersionUID = 1L;

    /**
     * Event type informing about newly connected relay.
     */
    public static final int ADDED = 1;

    /**
     * Event type informing about lately disconnected relay.
     */
    public static final int REMOVED = 2;

    /**
     * Event type (relay connected / disconnected).
     */
    private int type = -1;

    /**
     * Create new relay discovery event.
     *
     * @param relay the relay which has been found or removed
     * @param type the event type
     * @see #ADDED
     * @see #REMOVED
     */
    public RelayDiscoveryEvent(Relay relay, int type) {
        super(relay);
        this.type = type;
    }

    /**
     * Return the relay which has been found or removed.
     *
     * @return Relay instance
     */
    public Relay getRelay() {
        return (Relay) getSource();
    }

    /**
     * Return event type (relay connected / disconnected)
     *
     * @return Integer value
     * @see #ADDED
     * @see #REMOVED
     */
    public int getType() {
        return type;
    }

    public String toString(){
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append("Event: ")
                .append(getType() == ADDED ? "ADDED" : "REMOVED")
                .append(", source: ")
                .append(getRelay().getDevice().getName());
        return strBuilder.toString();
    }

}
