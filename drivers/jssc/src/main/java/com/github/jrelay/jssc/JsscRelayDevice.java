package com.github.jrelay.jssc;

import com.github.jrelay.RelayDevice;
import com.github.jrelay.jssc.impl.CommandReturn;
import com.github.jrelay.jssc.impl.ControlCommand;
import com.github.jrelay.jssc.impl.JsscRelayCommandBuilder;
import com.github.jrelay.jssc.impl.util.SerialPortHelper;
import jssc.SerialPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import jssc.*;

/**
 * Created by nightingale on 04.05.16.
 */
public class JsscRelayDevice implements RelayDevice {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(JsscRelayDevice.class);

    private static final int READ_TIMEOUT = 300;
    private final byte channel;
    private final SerialPort serialPort;

    private AtomicBoolean open = new AtomicBoolean(false);
    private AtomicBoolean disposed = new AtomicBoolean(false);
    private AtomicBoolean initialized = new AtomicBoolean(false);


    public JsscRelayDevice(byte channel, SerialPort serialPort) {
        this.channel = channel;
        this.serialPort = serialPort;
    }

    private synchronized void init() {

        if (!initialized.compareAndSet(false, true)) {
            return;
        }

        LOG.debug("JsscRelay device initialization");

        SerialPortHelper.initPort(serialPort,
                SerialPort.BAUDRATE_9600,
                SerialPort.DATABITS_8,
                SerialPort.STOPBITS_1,
                SerialPort.PARITY_NONE);
    }

    /**
     * Get device name.
     *
     * @return Device name
     */
    @Override
    public String getName() {
        StringBuilder nameBuilder = new StringBuilder(serialPort.getPortName())
                .append(" > channel: ")
                .append(channel);
        return nameBuilder.toString();
    }

    /**
     * Open device, it can be closed any time.
     */
    @Override
    public void open() {
        if (!open.compareAndSet(false, true)) {
            return;
        }

        LOG.debug(String.format("Opening UsbHidRelay device %s", getName()));

        init();

        synchronized(this) {
            try {
                byte[] cmd = JsscRelayCommandBuilder.INSTANCE_4CH.getControlCommand(ControlCommand.OPEN, channel);
                this.serialPort.writeBytes(cmd);

                byte[] actualResp = serialPort.readBytes(8, READ_TIMEOUT);
                byte[] expectedResp = JsscRelayCommandBuilder.INSTANCE_4CH.getReturnCommand(CommandReturn.OPEN, channel);

                if (!Arrays.equals(actualResp, expectedResp)) {
                    throw new RuntimeException("Device response error " + this.getName());
                }
            } catch (SerialPortException | SerialPortTimeoutException ex) {
                LOG.error(String.format("Error while device opening %s", getName()), ex);
                open.compareAndSet(true, false);
            }
        }

    }

    /**
     * Close device, however it can be open again.
     */
    @Override
    public void close() {
        if (!open.compareAndSet(true, false)) {
            return;
        }

        LOG.debug(String.format("Closing UsbHidRelay device channel %s", getName()));

        synchronized(this) {
            try {
                byte[] cmd = JsscRelayCommandBuilder.INSTANCE_4CH.getControlCommand(ControlCommand.CLOSE, channel);
                this.serialPort.writeBytes(cmd);

                byte[] actualResp = serialPort.readBytes(8, READ_TIMEOUT);
                byte[] expectedResp = JsscRelayCommandBuilder.INSTANCE_4CH.getReturnCommand(CommandReturn.CLOSE, channel);

                if (!Arrays.equals(actualResp, expectedResp)) {
                    throw new RuntimeException("Device response error " + this.getName());
                }
            } catch (SerialPortException | SerialPortTimeoutException ex) {
                LOG.error(String.format("Error while device opening %s", getName()), ex);
                open.compareAndSet(true, false);
            }
        }
    }

    /**
     * Dispose device. After device is disposed it cannot be open again.
     */
    @Override
    public void dispose() {
        if (!disposed.compareAndSet(false, true)) {
            return;
        }

        LOG.debug(String.format("Disposing JsscRelay device %s", getName()));

        close();

        synchronized(this) {
            try {
                serialPort.closePort();
            } catch (SerialPortException ex) {
                LOG.debug(String.format("Closing JsscRelay device channel %s", getName()), ex);
            }
        }
    }

    /**
     * Is relay device open?
     *
     * @return True if relay device is open, false otherwise
     */
    @Override
    public boolean isOpen() {
        return open.get();
    }
}
