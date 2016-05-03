package com.github.jrelay;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by nightingale on 24.04.16.
 */
public class RelayDiscoveryService implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(RelayDiscoveryService.class);

    private static final class RelayDiscovery implements Callable<List<Relay>>, ThreadFactory {

        private final RelayDriver driver;

        public RelayDiscovery(RelayDriver driver) {
            this.driver = driver;
        }

        @Override
        public List<Relay> call() throws Exception {
            return toRelays(driver.getDevices());
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "relay-discovery-service");
            t.setDaemon(true);
            t.setUncaughtExceptionHandler(RelayExceptionHandler.getInstance());
            return t;
        }
    }

    private final RelayDriver driver;
    private final RelayDiscoverySupport support;

    private volatile List<Relay> relays = null;

    private AtomicBoolean running = new AtomicBoolean(false);
    private AtomicBoolean enabled = new AtomicBoolean(true);

    private Thread runner = null;

    protected RelayDiscoveryService(RelayDriver driver) {

        if (driver == null) {
            throw new IllegalArgumentException("Driver cannot be null!");
        }

        this.driver = driver;
        this.support = (RelayDiscoverySupport) (driver instanceof RelayDiscoverySupport ? driver : null);
    }

    private static List<Relay> toRelays(List<RelayDevice> devices) {
        List<Relay> relays = new ArrayList<Relay>();
        for (RelayDevice device : devices) {
            relays.add(new Relay(device));
        }
        return relays;
    }

    /**
     * Get list of devices used by relays.
     *
     * @return List of relay devices
     */
    private static List<RelayDevice> getDevices(List<Relay> relays) {
        List<RelayDevice> devices = new ArrayList<RelayDevice>();
        for (Relay relay : relays) {
            devices.add(relay.getDevice());
        }
        return devices;
    }

    public List<Relay> getRelays(long timeout, TimeUnit tunit) throws TimeoutException {

        if (timeout < 0) {
            throw new IllegalArgumentException("Timeout cannot be negative");
        }

        if (tunit == null) {
            throw new IllegalArgumentException("Time unit cannot be null!");
        }

        List<Relay> tmp = null;

        synchronized (Relay.class) {

            if (relays == null) {

                RelayDiscovery discovery = new RelayDiscovery(driver);
                ExecutorService executor = Executors.newSingleThreadExecutor(discovery);
                Future<List<Relay>> future = executor.submit(discovery);

                executor.shutdown();

                try {

                    executor.awaitTermination(timeout, tunit);

                    if (future.isDone()) {
                        relays = future.get();
                    } else {
                        future.cancel(true);
                    }

                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (ExecutionException e) {
                    throw new RelayException(e);
                }

                if (relays == null) {
                    throw new TimeoutException(String.format("Webcams discovery timeout (%d ms) has been exceeded", timeout));
                }

                tmp = new ArrayList<Relay>(relays);
                if (Relay.isHandleTermSignal()) {
                    RelayDeallocator.store(relays.toArray(new Relay[relays.size()]));
                }
            }
        }

        if (tmp != null) {
            RelayDiscoveryListener[] listeners = Relay.getDiscoveryListeners();
            for (Relay relay : tmp) {
                notifyRelayFound(relay, listeners);
            }
        }

        return Collections.unmodifiableList(relays);
    }

    /**
     * Scan for newly added or already removed relays.
     */
    public void scan() {

        RelayDiscoveryListener[] listeners = Relay.getDiscoveryListeners();

        List<RelayDevice> tmpnew = driver.getDevices();
        List<RelayDevice> tmpold = null;

        try {
            tmpold = getDevices(getRelays(Long.MAX_VALUE, TimeUnit.MILLISECONDS));
        } catch (TimeoutException e) {
            throw new RelayException(e);
        }

        // convert to linked list due to O(1) on remove operation on
        // iterator versus O(n) for the same operation in array list
        List<RelayDevice> oldones = new LinkedList<RelayDevice>(tmpold);
        List<RelayDevice> newones = new LinkedList<RelayDevice>(tmpnew);

        Iterator<RelayDevice> oi = oldones.iterator();
        Iterator<RelayDevice> ni = null;

        RelayDevice od = null; // old device
        RelayDevice nd = null; // new device

        // reduce lists
        while (oi.hasNext()) {

            od = oi.next();
            ni = newones.iterator();

            while (ni.hasNext()) {

                nd = ni.next();

                // remove both elements, if device name is the same, which
                // actually means that device is exactly the same
                if (nd.getName().equals(od.getName())) {
                    ni.remove();
                    oi.remove();
                    break;
                }
            }
        }

        // if any left in old ones it means that devices has been removed
        if (oldones.size() > 0) {

            List<Relay> notified = new ArrayList<Relay>();

            for (RelayDevice device : oldones) {
                for (Relay relay : relays) {
                    if (relay.getDevice().getName().equals(device.getName())) {
                        notified.add(relay);
                        break;
                    }
                }
            }

            setCurrentRelays(tmpnew);

            for (Relay relay : notified) {
                notifyRelayGone(relay, listeners);
                relay.dispose();
            }
        }

        // if any left in new ones it means that devices has been added
        if (newones.size() > 0) {

            setCurrentRelays(tmpnew);

            for (RelayDevice device : newones) {
                for (Relay webcam : relays) {
                    if (webcam.getDevice().getName().equals(device.getName())) {
                        notifyRelayFound(webcam, listeners);
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void run() {

        // do not run if driver does not support discovery
        if (support == null) {
            return;
        }
        if (!support.isScanPossible()) {
            return;
        }

        // wait initial time interval since devices has been initially
        // discovered
        Object monitor = new Object();
        do {
            synchronized (monitor) {
                try {
                    monitor.wait(support.getScanInterval());
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    throw new RuntimeException("Problem waiting on monitor", e);
                }
            }
            scan();

        } while (running.get());

        LOG.debug("Relay discovery service loop has been stopped");
    }


    private void setCurrentRelays(List<RelayDevice> devices) {
        relays = toRelays(devices);
        if (Relay.isHandleTermSignal()) {
            RelayDeallocator.unstore();
            RelayDeallocator.store(relays.toArray(new Relay[relays.size()]));
        }
    }

    private static void notifyRelayGone(Relay relay, RelayDiscoveryListener[] listeners) {
        RelayDiscoveryEvent event = new RelayDiscoveryEvent(relay, RelayDiscoveryEvent.REMOVED);
        for (RelayDiscoveryListener l : listeners) {
            try {
                l.relayGone(event);
            } catch (Exception e) {
                LOG.error(String.format("Relay gone, exception when calling listener %s", l.getClass()), e);
            }
        }
    }

    private static void notifyRelayFound(Relay relay, RelayDiscoveryListener[] listeners) {
        RelayDiscoveryEvent event = new RelayDiscoveryEvent(relay, RelayDiscoveryEvent.ADDED);
        for (RelayDiscoveryListener l : listeners) {
            try {
                l.relayFound(event);
            } catch (Exception e) {
                LOG.error(String.format("Webcam found, exception when calling listener %s", l.getClass()), e);
            }
        }
    }

    /**
     * Is discovery service running?
     *
     * @return True or false
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Start discovery service.
     */
    public void start() {

        // if configured to not start, then simply return

        if (!enabled.get()) {
            LOG.info("Discovery service has been disabled and thus it will not be started");
            return;
        }

        // capture driver does not support discovery - nothing to do

        if (support == null) {
            LOG.info("Discovery will not run - driver {} does not support this feature", driver.getClass().getSimpleName());
            return;
        }

        // return if already running

        if (!running.compareAndSet(false, true)) {
            return;
        }

        // start discovery service runner

        runner = new Thread(this, "relay-discovery-service");
        runner.setUncaughtExceptionHandler(RelayExceptionHandler.getInstance());
        runner.setDaemon(true);
        runner.start();
    }


    /**
     * Stop discovery service.
     */
    public void stop() {

        // return if not running

        if (!running.compareAndSet(true, false)) {
            return;
        }

        try {
            runner.join();
        } catch (InterruptedException e) {
            throw new RelayException("Joint interrupted");
        }

        LOG.debug("Discovery service has been stopped");

        runner = null;
    }

    /**
     * Cleanup.
     */
    protected void shutdown() {

        stop();

        // dispose all relays

        Iterator<Relay> wi = relays.iterator();
        while (wi.hasNext()) {
            Relay relay = wi.next();
            relay.dispose();
        }

        synchronized (Relay.class) {

            // clear relays list

            relays.clear();

            // unassign relays from deallocator

            if (Relay.isHandleTermSignal()) {
                RelayDeallocator.unstore();
            }
        }
    }

}
