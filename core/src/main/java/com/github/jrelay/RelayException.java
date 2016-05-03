package com.github.jrelay;

/**
 * Created by m.rusakovich on 26.04.2016.
 */
public class RelayException extends RuntimeException {

    private static final long serialVersionUID = 4305046981807594375L;

    public RelayException(String message) {
        super(message);
    }

    public RelayException(String message, Throwable cause) {
        super(message, cause);
    }

    public RelayException(Throwable cause) {
        super(cause);
    }

}
