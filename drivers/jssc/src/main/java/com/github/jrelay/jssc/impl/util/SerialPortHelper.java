package com.github.jrelay.jssc.impl.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jssc.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Created by nightingale on 04.05.16.
 */
public class SerialPortHelper {

    private static final Logger LOG = LoggerFactory.getLogger(SerialPortHelper .class);
    private final static Pattern COM_PORT_PATTERN = Pattern.compile("^COM\\d{1,2}$");

    private SerialPortHelper() {
    }

    public static void initPort(SerialPort port, int baudRate, int dataBits, int stopBits, int parity) {
        try {
            if (!port.isOpened()) {
                port.openPort();
            }

            port.setParams(baudRate, dataBits, stopBits, parity);

        } catch (SerialPortException ex) {
            LOG.error(ex.getMessage());
        }
    }

    public static void closePort(SerialPort port) {
        try {
            if (port.isOpened()) {
                port.closePort();
            }

        } catch (SerialPortException ex) {
            LOG.error(ex.getMessage());
        }
    }

    public static int getPortNumber(String portName) {
        Matcher portMatcher = COM_PORT_PATTERN.matcher(portName);
        if (portMatcher.matches()) {
            return Integer.valueOf(portName.replaceFirst("COM", ""));
        }
        return -1;
    }

}
