package com.github.jrelay.jssc.impl;

import com.github.jrelay.jssc.impl.util.SerialPortHelper;
import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Created by nightingale on 04.05.16.
 */
public class Relay2ChannelsDeviceDiscovery implements DeviceDiscovery<SerialPort>{

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger( Relay2ChannelsDeviceDiscovery.class);

    private static final int MAX_CHANNELS_COUNT = 2;

    private static final int READ_TIMEOUT = 300;

    @Override
    public boolean isDeviceAvailable(SerialPort port) {

            try {

                SerialPortHelper.initPort(port, SerialPort.BAUDRATE_9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

                synchronized(port) {
                    for (int channel = 0; channel < getChannelsCount(); channel++) {

                        byte[] checkCmd = JsscRelayCommandBuilder.INSTANCE_4CH.getControlCommand(ControlCommand.READING_STATUS, channel);

                        try {
                            port.writeBytes(checkCmd);
                            byte[] response = port.readBytes(8, READ_TIMEOUT);

                            if (!Arrays.equals(JsscRelayCommandBuilder.READING_STATE_RESPONSE, response)) {
                                return false;
                            }
                        } catch (SerialPortException ex) {

                            return false;
                        } catch (SerialPortTimeoutException ex) {

                            return false;
                        }
                    }
                }
            } finally {
                SerialPortHelper.closePort(port);
            }
            return true;

    }

    public int getChannelsCount() {
        return MAX_CHANNELS_COUNT;
    }

}
