package com.github.jrelay.jssc;

import com.github.jrelay.RelayDevice;
import com.github.jrelay.RelayDiscoverySupport;
import com.github.jrelay.RelayDriver;
import com.github.jrelay.RelayExceptionHandler;
import com.github.jrelay.jssc.impl.DeviceDiscovery;
import com.github.jrelay.jssc.impl.Relay2ChannelsDeviceDiscovery;
import com.github.jrelay.jssc.impl.Relay4ChannelsDeviceDiscovery;
import com.github.jrelay.jssc.impl.util.SerialPortHelper;
import jssc.SerialPort;
import jssc.SerialPortList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by nightingale on 04.05.16.
 */
public class JsscRelayDriver implements RelayDriver, RelayDiscoverySupport {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(JsscRelayDriver.class);

    private static final DeviceDiscovery<SerialPort> relay2ChannelsDeviceDiscovery = new Relay2ChannelsDeviceDiscovery();
    //TODO not supported yet
    private static final DeviceDiscovery<SerialPort> relay4ChannelsDeviceDiscovery = new Relay4ChannelsDeviceDiscovery();


    /**
     * Thread factory.
     *
     * @author Bartosz Firyn (sarxos)
     */
    private static class DeviceCheckThreadFactory implements ThreadFactory {

        /**
         * Next number for created thread.
         */
        private AtomicInteger number = new AtomicInteger();

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "relay-check-" + number.incrementAndGet());
            t.setUncaughtExceptionHandler(RelayExceptionHandler.getInstance());
            t.setDaemon(true);
            return t;
        }
    }


    /**
     * Thread factory.
     */
    private static final ThreadFactory THREAD_FACTORY = new DeviceCheckThreadFactory();

    /**
     * The callable to query single relay device. Callable getter will return device if it's
     * available or null if it's not.
     */
    private static class DeviceAvailableCheck implements Callable<JsscRelayDevice> {

        private final CountDownLatch latch;

        private final JsscRelayDevice device;

        /**
         * The callable to query port device.
         *
         * @param device  the device to check
         * @param latch the count down latch
         */
        public DeviceAvailableCheck(JsscRelayDevice device, CountDownLatch latch) {
            this.device = device;
            this.latch = latch;
        }

        @Override
        public JsscRelayDevice call() throws Exception {
            try {
                return relay2ChannelsDeviceDiscovery.isDeviceAvailable(device.getSerialPort())
                        ? device : null;
            } finally {
                latch.countDown();
            }
        }
    }

    /**
     * Discovery scan interval in milliseconds.
     */
    private volatile long scanInterval = 5000;

    /**
     * Discovery scan timeout in milliseconds. This is maximum time which executor will wait for
     * online detection to succeed.
     */
    private volatile long scanTimeout = 5000;

    /**
     * Is discovery scanning possible.
     */
    private volatile boolean scanning = false;

    /**
     * Execution service.
     */
    private final ExecutorService executor = Executors.newCachedThreadPool(THREAD_FACTORY);


    public JsscRelayDriver(){
    }

    public JsscRelayDriver(boolean scanning){
        this.scanning = scanning;
    }

    private List<JsscRelayDevice> getAllJsscDevices(){
        List<JsscRelayDevice> devices = new ArrayList<JsscRelayDevice>();

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
     * Return all registered relay devices.
     *
     * @return List of relay devices
     */
    @Override
    public List<RelayDevice> getDevices() {

        // in case when scanning is disabled (by default) this method will
        // return all registered devices

        if (!isScanPossible()) {
            return Collections.unmodifiableList((List<? extends RelayDevice>) getAllJsscDevices());
        }

        // if scanning is enabled, this method will first perform lookup
        // for every JSSC device and only available devices will be returned
        List<JsscRelayDevice> devices = getAllJsscDevices();
        CountDownLatch latch = new CountDownLatch(devices.size());
        List<Future<JsscRelayDevice>> futures = new ArrayList<Future<JsscRelayDevice>>(devices.size());

        for (JsscRelayDevice device : devices) {
            futures.add(executor.submit(new DeviceAvailableCheck(device, latch)));
        }

        try {
            if (!latch.await(scanTimeout, TimeUnit.MILLISECONDS)) {
                for (Future<JsscRelayDevice> future : futures) {
                    if (!future.isDone()) {
                        future.cancel(true);
                    }
                }
            }
        } catch (InterruptedException e1) {
            return null;
        }

        List<JsscRelayDevice> available = new ArrayList<JsscRelayDevice>(devices.size());

        for (Future<JsscRelayDevice> future : futures) {

            JsscRelayDevice device = null;
            try {
                if ((device = future.get()) != null) {
                    available.add(device);
                }
            } catch (InterruptedException e) {
                LOG.debug(e.getMessage(), e);
            } catch (CancellationException e) {
                continue;
            } catch (ExecutionException e) {
                LOG.error(e.getMessage(), e);
            }
        }

        return Collections.unmodifiableList((List<? extends RelayDevice>) available);
    }

        @Override
        public long getScanInterval() {
            return scanInterval;
        }

        /**
         * Set new scan interval. Value must be given in milliseconds and shall not be negative.
         *
         * @param scanInterval
         */
        public void setScanInterval(long scanInterval) {
            if (scanInterval > 0) {
                this.scanInterval = scanInterval;
            } else {
                throw new IllegalArgumentException("Scan interval for IP camera cannot be negative");
            }
        }

        @Override
        public boolean isScanPossible() {
            return scanning;
        }

        /**
         * Set discovery scanning possible.
         *
         * @param scanning
         */
        public void setScanPossible(boolean scanning) {
            this.scanning = scanning;
        }

        /**
         * @return Scan timeout in milliseconds
         */
        public long getScanTimeout() {
            return scanTimeout;
        }

        /**
         * Set new scan timeout. This value cannot be less than 1000 milliseconds (which equals 1
         * second).
         *
         * @param scanTimeout the scan timeout in milliseconds
         */
        public void setScanTimeout(long scanTimeout) {
            if (scanTimeout < 1000) {
                scanTimeout = 1000;
            }
            this.scanTimeout = scanTimeout;
        }

    /**
     * Is driver thread-safe. Thread safe drivers operations does not have to be
     * synchronized.
     *
     * @return True in case if driver is thread-safe, false otherwise
     */
    @Override
    public boolean isThreadSafe() {
        return true;
    }
}
