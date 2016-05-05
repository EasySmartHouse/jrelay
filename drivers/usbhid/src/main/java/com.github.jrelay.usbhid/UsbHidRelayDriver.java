package com.github.jrelay.usbhid;

import by.creepid.jusbrelay.NativeUsbRelayManager;
import by.creepid.jusbrelay.UsbRelayDeviceInfo;
import by.creepid.jusbrelay.UsbRelayException;
import by.creepid.jusbrelay.UsbRelayManager;
import com.github.jrelay.RelayDevice;
import com.github.jrelay.RelayDriver;
import com.github.jrelay.RelayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by nightingale on 01.05.16.
 */
public class UsbHidRelayDriver implements RelayDriver {

    private static final Logger LOG = LoggerFactory.getLogger(UsbHidRelayDriver.class);

    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    private static final UsbRelayManager manager = NativeUsbRelayManager.getInstance();

    private static final class UsbHidRelayShutdownHook extends Thread {

        public UsbHidRelayShutdownHook() {
            super("usbhidrelay-shutdown-hook");
        }

        @Override
        public void run() {
            LOG.debug("UsbHidRelayDriver deinitialization");
            try {
                manager.relayExit();
            }catch(UsbRelayException ex){
                LOG.error("UsbHidRelayDriver exit error", ex);
            }
        }
    }

    private final void init() {
        LOG.debug("UsbHidRelayDriver initialization");

        try {
            manager.relayInit();
        }catch(UsbRelayException ex){
            LOG.error("UsbHidRelayDriver init error", ex);
        }

        Runtime.getRuntime().addShutdownHook(new UsbHidRelayShutdownHook());
    }

    public UsbHidRelayDriver(){
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
        try {
            List<RelayDevice> devices = new ArrayList<RelayDevice>();
            UsbRelayDeviceInfo[] deviceInfos = manager.deviceEnumerate();
            for (int i = 0; i < deviceInfos.length; i++) {
                UsbRelayDeviceInfo deviceInfo = deviceInfos[i];

                int channels = deviceInfo.getDeviceType().getChannels();
                for (int ch = 0; ch < channels; ch++) {
                    devices.add(new UsbHidRelayDevice(deviceInfo, manager, ch));
                }
            }
            return devices;
        }catch(Exception ex){
            LOG.error("UsbHidRelayDriver error", ex);
            return Collections.EMPTY_LIST;
        }
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

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
