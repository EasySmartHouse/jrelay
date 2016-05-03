package com.github.jrelay;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.NOPLoggerFactory;

/**
 * Created by nightingale on 24.04.16.
 */
public class RelayExceptionHandler implements Thread.UncaughtExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(RelayExceptionHandler.class);

    private static final RelayExceptionHandler INSTANCE = new RelayExceptionHandler();

    private RelayExceptionHandler() {
        // singleton
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        Object context = LoggerFactory.getILoggerFactory();
        if (context instanceof NOPLoggerFactory) {
            System.err.println(String.format("Exception in thread %s", t.getName()));
            e.printStackTrace();
        } else {
            LOG.error(String.format("Exception in thread %s", t.getName()), e);
        }
    }

    public static void handle(Throwable e) {
        INSTANCE.uncaughtException(Thread.currentThread(), e);
    }

    public static final RelayExceptionHandler getInstance() {
        return INSTANCE;
    }

}
