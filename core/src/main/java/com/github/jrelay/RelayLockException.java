package com.github.jrelay;

/**
 * Created by m.rusakovich on 27.04.2016.
 */
public class RelayLockException extends RelayException{

    private static final long serialVersionUID = 1L;

    public RelayLockException(String message, Throwable cause) {
        super(message, cause);
    }

    public RelayLockException(String message) {
        super(message);
    }

    public RelayLockException(Throwable cause) {
        super(cause);
    }
}
