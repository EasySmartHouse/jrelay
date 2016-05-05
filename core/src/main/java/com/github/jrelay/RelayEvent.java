package com.github.jrelay;

import java.util.EventObject;

/**
 * Created by m.rusakovich on 26.04.2016.
 */
public class RelayEvent extends EventObject {

    private static final long serialVersionUID = 1L;

    private RelayEventType type = null;

    public RelayEvent(RelayEventType type, Relay relay) {
        super(relay);
        this.type = type;
    }

    @Override
    public Relay getSource() {
        return (Relay) super.getSource();
    }

    public RelayEventType getType(){
        return type;
    }

    public String toString(){
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append("Event: ")
                .append(getType().name())
                .append(", source: ")
                .append(getSource().getDevice().getName());
        return strBuilder.toString();
    }

}
