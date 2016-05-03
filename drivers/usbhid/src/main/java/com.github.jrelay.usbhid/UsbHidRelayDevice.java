package com.github.jrelay.usbhid;

import by.creepid.jusbrelay.*;
import com.github.jrelay.RelayDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by nightingale on 01.05.16.
 */
public class UsbHidRelayDevice implements RelayDevice {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(UsbHidRelayDevice.class);

    private AtomicBoolean open = new AtomicBoolean(false);
    private AtomicBoolean disposed = new AtomicBoolean(false);
    private AtomicBoolean initialized = new AtomicBoolean(false);

    private final UsbRelayDeviceInfo deviceInfo;
    private final UsbRelayManager manager;
    private int channel;

    private UsbRelayDeviceHandler handler = null;

    /**
     * Initialize device.
     */
    private synchronized void init() {
        if (!initialized.compareAndSet(false, true)) {
            return;
        }

        LOG.debug("UsbHidRelay device initialization");

        try {
            this.handler = manager.deviceOpen(deviceInfo.getSerialNumber());
        }catch(UsbRelayException ex){
            LOG.error(String.format("Error while device opening %s", getName()), ex);
        }
    }

    public UsbHidRelayDevice(UsbRelayDeviceInfo deviceInfo, UsbRelayManager manager, int channel) {
        this.deviceInfo = deviceInfo;
        this.manager = manager;
        this.channel = channel;
    }

    /**
     * Get device name.
     *
     * @return Device name
     */
    @Override
    public String getName() {
        StringBuilder nameBuilder = new StringBuilder("USB > ")
                .append(deviceInfo.getDevicePath())
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
            if (handler != null) {
                try {
                    manager.openRelayChannel(handler, channel);
                } catch (UsbRelayException ex) {
                    LOG.error(String.format("Error while device opening %s", getName()), ex);
                    open.compareAndSet(true, false);
                }
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
            if (handler != null) {
                try {
                    manager.closeRelayChannel(handler, channel);
                } catch (UsbRelayException ex) {
                    LOG.error(String.format("Error while device channel closing %s", getName()), ex);
                    open.compareAndSet(false, true);
                }
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

        LOG.debug(String.format("Disposing UsbHidRelay device %s", getName()));

        close();

        synchronized(this) {
            if (handler != null) {
                try {
                    manager.closeRelay(handler);
                } catch (UsbRelayException ex) {
                    LOG.debug(String.format("Closing UsbHidRelay device channel %s", getName()));
                }
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
