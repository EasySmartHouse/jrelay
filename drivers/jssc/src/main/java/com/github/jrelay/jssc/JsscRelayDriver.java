package com.github.jrelay.jssc;

import com.github.jrelay.RelayDevice;
import com.github.jrelay.RelayDriver;
import com.github.jrelay.jssc.impl.DeviceDiscovery;
import com.github.jrelay.jssc.impl.Relay2ChannelsDeviceDiscovery;
import com.github.jrelay.jssc.impl.Relay4ChannelsDeviceDiscovery;
import com.github.jrelay.jssc.impl.util.SerialPortHelper;
import jssc.SerialPort;
import jssc.SerialPortList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by nightingale on 04.05.16.
 */
public class JsscRelayDriver implements RelayDriver {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(JsscRelayDriver.class);

    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    private DeviceDiscovery<SerialPort> relay2ChannelsDeviceDiscovery = new Relay2ChannelsDeviceDiscovery();
    //TODO not supported yet
    private DeviceDiscovery<SerialPort> relay4ChannelsDeviceDiscovery = new Relay4ChannelsDeviceDiscovery();

    private static final class JsscRelayShutdownHook extends Thread {

        public JsscRelayShutdownHook() {
            super("jssc-shutdown-hook");
        }

        @Override
        public void run() {
            LOG.debug("JsscRelayDriver deinitialization");
            String[] portNames = SerialPortList.getPortNames();
            for (String portName : portNames) {
                SerialPortHelper.closePort(new SerialPort(portName));
            }
        }
    }

    private final void init() {
        LOG.debug("JsscRelayDriver initialization");

        Runtime.getRuntime().addShutdownHook(new JsscRelayShutdownHook());
    }

    public JsscRelayDriver(){
        if (INITIALIZED.compareAndSet(false, true)) {
            init();
        }
    }

    /**
     * Return all registered relay devices.
     *
     * @return List of relay devices
     */
    @Override
    public List<RelayDevice> getDevices() {
        List<RelayDevice> devices = new ArrayList<RelayDevice>();

        String[] portNames = SerialPortList.getPortNames();
        for (String portName : portNames) {
            SerialPort serialPort = new SerialPort(portName);

            if (relay2ChannelsDeviceDiscovery.isDeviceAvailable(serialPort)) {
                devices.add(new JsscRelayDevice((byte) 0, serialPort));
                devices.add(new JsscRelayDevice((byte) 1, serialPort));
            }
        }

        return devices;
    }

    /**
     * Is driver thread-safe. Thread safe drivers operations does not have to be
     * synchronized.
     *
     * @return True in case if driver is thread-safe, false otherwise
     */
    @Override
    public boolean isThreadSafe() {
        return false;
    }
}
