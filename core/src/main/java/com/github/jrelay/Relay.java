package com.github.jrelay;

import com.github.jrelay.dummy.RelayDummyDevice;
import com.github.jrelay.dummy.RelayDummyDriver;
import com.github.jrelay.task.RelayCloseTask;
import com.github.jrelay.task.RelayDisposeTask;
import com.github.jrelay.task.RelayOpenTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by nightingale on 24.04.16.
 */
public class Relay {

    /**
     * Logger instance.
     */
    private static final Logger LOG = LoggerFactory.getLogger(Relay.class);

    /**
     * List of driver classes names to search for.
     */
    private static final List<String> DRIVERS_LIST = new ArrayList<String>();

    /**
     * List of driver classes to search for.
     */
    private static final List<Class<?>> DRIVERS_CLASS_LIST = new ArrayList<Class<?>>();

    /**
     * Discovery listeners.
     */
    private static final List<RelayDiscoveryListener> DISCOVERY_LISTENERS = Collections.synchronizedList(new ArrayList<RelayDiscoveryListener>());

    /**
     * Relay driver
     */
    private static volatile RelayDriver driver = null;

    /**
     * Relay discovery service.
     */
    private static volatile RelayDiscoveryService discovery = null;


    /**
     * Is automated deallocation on TERM signal enabled.
     */
    private static boolean deallocOnTermSignal = false;

    /**
     * Is auto-open feature enabled?
     */
    private static boolean autoOpen = false;

    /**
     * Relay listeners.
     */
    private List<RelayListener> listeners = new CopyOnWriteArrayList<RelayListener>();

    /**
     * Shutdown hook.
     */
    private RelayShutdownHook hook = null;

    /**
     * Is relay open?
     */
    private AtomicBoolean open = new AtomicBoolean(false);

    /**
     * Is relay already disposed?
     */
    private AtomicBoolean disposed = new AtomicBoolean(false);

    /**
     * Is non-blocking (asynchronous) access enabled?
     */
    private volatile boolean asynchronous = false;

    /**
     * Underlying relay device.
     */
    private RelayDevice device = new RelayDummyDevice(Integer.MIN_VALUE);

    /**
     * Lock which denies access to the given relay when it's already in use by other
     * API process or thread.
     */
    private RelayLock lock = null;

    /**
     * Executor service for notifications.
     */
    private ExecutorService notificator = null;

    private final class NotificationThreadFactory implements ThreadFactory {

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, String.format("notificator-[%s]", getName()));
            t.setUncaughtExceptionHandler(RelayExceptionHandler.getInstance());
            t.setDaemon(true);
            return t;
        }
    }

    /**
     * Relay class.
     *
     * @param device - device to be used as relay
     * @throws IllegalArgumentException when device argument is null
     */
    protected Relay(RelayDevice device) {
        if (device == null) {
            throw new IllegalArgumentException("Relay device cannot be null");
        }
        this.device = device;
        this.lock = new RelayLock(this);
    }

    /**
     * Open the relay in blocking (synchronous) mode.
     *
     * @return True if relay has been open, false otherwise
     * @see #open(boolean)
     * @throws RelayException when something went wrong
     */
    public boolean open() {
        return open(false);
    }


    /**
     * Open the relay in either blocking (synchronous) or non-blocking
     * (asynchronous) mode.
     * @param async true for non-blocking mode, false for blocking
     *
     * @return True if relay has been open
     * @throws RelayException when something went wrong
     */
    public boolean open(boolean async) {

        if (open.compareAndSet(false, true)) {
            assert lock != null;

            notificator = Executors.newSingleThreadExecutor(new NotificationThreadFactory());

            // lock relay for other Java (only) processes

            lock.lock();

            // open relay device
            RelayOpenTask task = new RelayOpenTask(driver, device);
            try {
                task.open();
            } catch (InterruptedException e) {
                lock.unlock();
                open.set(false);
                LOG.debug("Thread has been interrupted in the middle of relay opening process!", e);
                return false;
            } catch (RelayException e) {
                lock.unlock();
                open.set(false);
                LOG.debug("Relay exception when opening", e);
                throw e;
            }

            LOG.debug("Relay is now open {}", getName());

            // install shutdown hook
            try {
                Runtime.getRuntime().addShutdownHook(hook = new RelayShutdownHook(this));
            } catch (IllegalStateException e) {

                LOG.debug("Shutdown in progress, do not open device");
                LOG.trace(e.getMessage(), e);

                close();

                return false;
            }


            // notify listeners
            RelayEvent we = new RelayEvent(RelayEventType.OPEN, this);
            Iterator<RelayListener> wli = listeners.iterator();
            RelayListener l = null;

            while (wli.hasNext()) {
                l = wli.next();
                try {
                    l.relayOpen(we);
                } catch (Exception e) {
                    LOG.error(String.format("Notify relay open, exception when calling listener %s", l.getClass()), e);
                }
            }

        } else {
            LOG.debug("Relay is already open {}", getName());
        }

        return true;
    }

    /**
     * Close the relay.
     *
     * @return True if relay has been open, false otherwise
     */
    public boolean close() {

        if (open.compareAndSet(true, false)) {

            LOG.debug("Closing relay {}", getName());

            assert lock != null;

            // close relay
            RelayCloseTask task = new RelayCloseTask(driver, device);
            try {
                task.close();
            } catch (InterruptedException e) {
                open.set(true);
                LOG.debug("Thread has been interrupted before relay was closed!", e);
                return false;
            } catch (RelayException e) {
                open.set(true);
                throw e;
            }


            // remove shutdown hook (it's not more necessary)
            removeShutdownHook();

            // unlock relay so other Java processes can start using it
            lock.unlock();

            // notify listeners

            RelayEvent we = new RelayEvent(RelayEventType.CLOSED, this);
            Iterator<RelayListener> wli = listeners.iterator();
            RelayListener l = null;

            while (wli.hasNext()) {
                l = wli.next();
                try {
                    l.relayClosed(we);
                } catch (Exception e) {
                    LOG.error(String.format("Notify relay closed, exception when calling %s listener", l.getClass()), e);
                }
            }

            notificator.shutdown();
            while (!notificator.isTerminated()) {
                try {
                    notificator.awaitTermination(100, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    return false;
                }
            }

            LOG.debug("Relay {} has been closed", getName());

        } else {
            LOG.debug("Relay {} is already closed", getName());
        }

        return true;
    }


    /**
     * Completely dispose capture device. After this operation relay cannot be used any more and
     * full reinstantiation is required.
     */
    protected void dispose(){
        assert disposed != null;
        assert open != null;
        assert driver != null;
        assert device != null;
        assert listeners != null;

        if (!disposed.compareAndSet(false, true)) {
            return;
        }

        open.set(false);

        LOG.info("Disposing relay {}", getName());

        RelayDisposeTask task = new RelayDisposeTask(driver, device);
        try {
            task.dispose();
        } catch (InterruptedException e) {
            LOG.error("Processor has been interrupted before relay was disposed!", e);
            return;
        }

        RelayEvent we = new RelayEvent(RelayEventType.DISPOSED, this);
        Iterator<RelayListener> wli = listeners.iterator();
        RelayListener l = null;

        while (wli.hasNext()) {
            l = wli.next();
            try {
                l.relayClosed(we);
                l.relayDisposed(we);
            } catch (Exception e) {
                LOG.error(String.format("Notify relay disposed, exception when calling %s listener", l.getClass()), e);
            }
        }

        removeShutdownHook();

        LOG.debug("relay disposed {}", getName());
    }

    private void removeShutdownHook() {

        // hook can be null because there is a possibility that relay has never
        // been open and therefore hook was not created

        if (hook != null) {
            try {
                Runtime.getRuntime().removeShutdownHook(hook);
            } catch (IllegalStateException e) {
                LOG.trace("Shutdown in progress, cannot remove hook");
            }
        }
    }

    /**
     * Is relay open?
     *
     * @return true if open, false otherwise
     */
    public boolean isOpen() {
        return open.get();
    }

    /**
     * If the underlying device implements Configurable interface, specified
     * parameters are passed to it. May be called before the open method or
     * later in dependence of the device implementation.
     *
     * @param parameters - Map of parameters changing device defaults
     * @see RelayDevice.Configurable
     */
    public void setParameters(Map<String, ?> parameters) {
        RelayDevice device = getDevice();
        if (device instanceof RelayDevice.Configurable) {
            ((RelayDevice.Configurable) device).setParameters(parameters);
        } else {
            LOG.debug("Relay device {} is not configurable", device);
        }
    }

    /**
     * Is relay ready to be read.
     *
     * @return True if ready, false otherwise
     */
    private boolean isReady() {

        assert disposed != null;
        assert open != null;

        if (disposed.get()) {
            LOG.warn("Cannot get relay state, relay has been already disposed");
            return false;
        }

        if (!open.get()) {
            if (autoOpen) {
                open();
            } else {
                return false;
            }
        }

        return true;
    }

    /**
     * Get list of relays to use. This method will wait predefined time interval for relay devices
     * to be discovered. By default this time is set to 1 minute.
     *
     * @return List of relays existing in the system
     * @throws RelayException when something is wrong
     * @see Relay#getRelays(long, TimeUnit)
     */
    public static List<Relay> getRelays() throws RelayException {

        // timeout exception below will never be caught since user would have to
        // wait around three hundreds billion years for it to occur

        try {
            return getRelays(Long.MAX_VALUE);
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get list of relays to use. This method will wait given time interval for relay devices to
     * be discovered. Time argument is given in milliseconds.
     *
     * @param timeout the time to wait for relay devices to be discovered
     * @return List of relay existing in the system
     * @throws TimeoutException when timeout occurs
     * @throws RelayException when something is wrong
     * @throws IllegalArgumentException when timeout is negative
     * @see Relay#getRelays(long, TimeUnit)
     */
    public static List<Relay> getRelays(long timeout) throws TimeoutException, RelayException {
        if (timeout < 0) {
            throw new IllegalArgumentException(String.format("Timeout cannot be negative (%d)", timeout));
        }
        return getRelays(timeout, TimeUnit.MILLISECONDS);
    }

    /**
     * Get list of relays to use. This method will wait given time interval for relay devices to
     * be discovered.
     *
     * @param timeout the devices discovery timeout
     * @param tunit the time unit
     * @return List of relays
     * @throws TimeoutException when timeout has been exceeded
     * @throws RelayException when something is wrong
     * @throws IllegalArgumentException when timeout is negative or tunit null
     */
    public static synchronized List<Relay> getRelays(long timeout, TimeUnit tunit) throws TimeoutException, RelayException {

        if (timeout < 0) {
            throw new IllegalArgumentException(String.format("Timeout cannot be negative (%d)", timeout));
        }
        if (tunit == null) {
            throw new IllegalArgumentException("Time unit cannot be null!");
        }

        RelayDiscoveryService discovery = getDiscoveryService();

        assert discovery != null;

        List<Relay> relays = discovery.getRelays(timeout, tunit);
        if (!discovery.isRunning()) {
            discovery.start();
        }

        return relays;
    }


    /**
     * Will discover and return first relay available in the system.
     *
     * @return Default webcam (first from the list)
     * @throws RelayException if something is really wrong
     * @see Relay#getRelays()
     */
    public static Relay getDefault() throws RelayException {
        try {
            return getDefault(Long.MAX_VALUE);
        } catch (TimeoutException e) {
            // this should never happen since user would have to wait 300000000
            // years for it to occur
            throw new RuntimeException(e);
        }
    }

    /**
     * Will discover and return first relay available in the system.
     *
     * @param timeout the relay discovery timeout (1 minute by default)
     * @return Default relay (first from the list)
     * @throws TimeoutException when discovery timeout has been exceeded
     * @throws RelayException if something is really wrong
     * @throws IllegalArgumentException when timeout is negative
     * @see Relay#getRelays(long)
     */
    public static Relay getDefault(long timeout) throws TimeoutException, RelayException {
        if (timeout < 0) {
            throw new IllegalArgumentException(String.format("Timeout cannot be negative (%d)", timeout));
        }
        return getDefault(timeout, TimeUnit.MILLISECONDS);
    }

    /**
     * Will discover and return first relay available in the system.
     *
     * @param timeout the relay discovery timeout (1 minute by default)
     * @param tunit the time unit
     * @return Default relay (first from the list)
     * @throws TimeoutException when discovery timeout has been exceeded
     * @throws RelayException if something is really wrong
     * @throws IllegalArgumentException when timeout is negative or tunit null
     * @see Relay#getRelays(long, TimeUnit)
     */
    public static Relay getDefault(long timeout, TimeUnit tunit) throws TimeoutException, RelayException {

        if (timeout < 0) {
            throw new IllegalArgumentException(String.format("Timeout cannot be negative (%d)", timeout));
        }
        if (tunit == null) {
            throw new IllegalArgumentException("Time unit cannot be null!");
        }

        List<Relay> relays = getRelays(timeout, tunit);

        assert relays != null;

        if (!relays.isEmpty()) {
            return relays.get(0);
        }

        LOG.warn("No relays has been detected!");

        return null;
    }



    /**
     * Return underlying relay device. Depending on the driver used to discover devices, this
     * method can return instances of different class. By default {@link RelayDummyDevice} is
     * returned when no external driver is used.
     *
     * @return Underlying relay device instance
     */
    public RelayDevice getDevice() {
        assert device != null;
        return device;
    }

    /**
     * Is TERM signal handler enabled.
     *
     * @return True if enabled, false otherwise
     */
    public static boolean isHandleTermSignal() {
        return deallocOnTermSignal;
    }

    /**
     * Add new relay discovery listener.
     *
     * @param l the listener to be added
     * @return True, if listeners list size has been changed, false otherwise
     * @throws IllegalArgumentException when argument is null
     */
    public static boolean addDiscoveryListener(RelayDiscoveryListener l) {
        if (l == null) {
            throw new IllegalArgumentException("Relay discovery listener cannot be null!");
        }
        return DISCOVERY_LISTENERS.add(l);
    }

    public static RelayDiscoveryListener[] getDiscoveryListeners() {
        return DISCOVERY_LISTENERS.toArray(new RelayDiscoveryListener[DISCOVERY_LISTENERS.size()]);
    }

    /**
     * Remove discovery listener
     *
     * @param l the listener to be removed
     * @return True if listeners list contained the specified element
     */
    public static boolean removeDiscoveryListener(RelayDiscoveryListener l) {
        return DISCOVERY_LISTENERS.remove(l);
    }

    /**
     * Get relay name (device name). The name of device depends on the value returned by the
     * underlying data source, so in some cases it can be human-readable value and sometimes it can
     * be some strange number.
     *
     * @return Name
     */
    public String getName() {
        assert device != null;
        return device.getName();
    }

    @Override
    public String toString() {
        return String.format("Relay %s", getName());
    }

    /**
     * Add relay listener.
     *
     * @param l the listener to be added
     * @return True if listener has been added, false if it was already there
     * @throws IllegalArgumentException when argument is null
     */
    public boolean addRelayListener(RelayListener l) {
        if (l == null) {
            throw new IllegalArgumentException("Relay listener cannot be null!");
        }
        assert listeners != null;
        return listeners.add(l);
    }

    /**
     * @return All relay listeners
     */
    public RelayListener[] getRelaysListeners() {
        assert listeners != null;
        return listeners.toArray(new RelayListener[listeners.size()]);
    }

    /**
     * @return Number of relays listeners
     */
    public int getRelayListenersCount() {
        assert listeners != null;
        return listeners.size();
    }

    /**
     * Removes relays listener.
     *
     * @param l the listener to be removed
     * @return True if listener has been removed, false otherwise
     */
    public boolean removeRelaysListener(RelayListener l) {
        assert listeners != null;
        return listeners.remove(l);
    }

    /**
     * Return relay driver. Perform search if necessary.<br>
     * <br>
     * <b>This method is not thread-safe!</b>
     *
     * @return RelayDriver
     */
    public static synchronized RelayDriver getDriver() {

        if (driver != null) {
            return driver;
        }

        if (driver == null) {
            driver = RelayDriverUtils.findDriver(DRIVERS_LIST, DRIVERS_CLASS_LIST);
        }
        if (driver == null) {
            driver = new RelayDummyDriver(Integer.MIN_VALUE);
        }

        LOG.info("{} capture driver will be used", driver.getClass().getSimpleName());

        return driver;
    }

    /**
     * Set new video driver to be used by relay.<br>
     * <br>
     * <b>This method is not thread-safe!</b>
     *
     * @param wd new relay driver to be used
     * @throws IllegalArgumentException when argument is null
     */
    public static void setDriver(RelayDriver wd) {

        if (wd == null) {
            throw new IllegalArgumentException("Relay driver cannot be null!");
        }

        LOG.debug("Setting new driver {}", wd);

        resetDriver();

        driver = wd;
    }

    /**
     * Set new driver class to be used by relay. Class given in the argument shall extend
     * {@link RelayDriver} interface and should have public default constructor, so instance can be
     * created by reflection.<br>
     * <br>
     * <b>This method is not thread-safe!</b>
     *
     * @param driverClass new video driver class to use
     * @throws IllegalArgumentException when argument is null
     */
    public static void setDriver(Class<? extends RelayDriver> driverClass) {

        if (driverClass == null) {
            throw new IllegalArgumentException("Relay driver class cannot be null!");
        }

        resetDriver();

        try {
            driver = driverClass.newInstance();
        } catch (InstantiationException e) {
            throw new RelayException(e);
        } catch (IllegalAccessException e) {
            throw new RelayException(e);
        }
    }

    /**
     * Reset relay driver.<br>
     * <br>
     * <b>This method is not thread-safe!</b>
     */
    public static void resetDriver() {

        synchronized (DRIVERS_LIST) {
            DRIVERS_LIST.clear();
        }

        if (discovery != null) {
            discovery.shutdown();
            discovery = null;
        }

        driver = null;
    }

    /**
     * Register new relay video driver.
     *
     * @param clazz relay video driver class
     * @throws IllegalArgumentException when argument is null
     */
    public static void registerDriver(Class<? extends RelayDriver> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("Relay driver class to register cannot be null!");
        }
        DRIVERS_CLASS_LIST.add(clazz);
        registerDriver(clazz.getCanonicalName());
    }

    /**
     * Register new relay driver.
     *
     * @param clazzName relay driver class name
     * @throws IllegalArgumentException when argument is null
     */
    public static void registerDriver(String clazzName) {
        if (clazzName == null) {
            throw new IllegalArgumentException("Relay driver class name to register cannot be null!");
        }
        DRIVERS_LIST.add(clazzName);
    }

    /**
     * <b>CAUTION!!!</b><br>
     * <br>
     * This is experimental feature to be used mostly in in development phase. After you set handle
     * term signal to true, and fetch devices, Relay API will listen for TERM
     * signal and try to close all devices after it has been received. <b>This feature can be
     * unstable on some systems!</b>
     *
     * @param on signal handling will be enabled if true, disabled otherwise
     */
    public static void setHandleTermSignal(boolean on) {
        if (on) {
            LOG.warn("Automated deallocation on TERM signal is now enabled! Make sure to not use it in production!");
        }
        deallocOnTermSignal = on;
    }


    /**
     * Switch all relays to auto open mode. In this mode, each relay will be automatically open
     * Please be aware of some side effects! In case of multi-threaded applications, there is no guarantee
     * that one thread will not try to open relay even if it was manually closed in different
     * thread.
     *
     * @param on true to enable, false to disable
     */
    public static void setAutoOpenMode(boolean on) {
        autoOpen = on;
    }

    /**
     * Is auto open mode enabled. Auto open mode will will automatically open relay whenever user
     * will try to get state from instance which has not yet been open. Please be aware of some side
     * effects! In case of multi-threaded applications, there is no guarantee that one thread will
     * not try to open relay even if it was manually closed in different thread.
     *
     * @return True if mode is enabled, false otherwise
     */
    public static boolean isAutoOpenMode() {
        return autoOpen;
    }

    /**
     * Return discovery service.
     *
     * @return Discovery service
     */
    public static synchronized RelayDiscoveryService getDiscoveryService() {
        if (discovery == null) {
            discovery = new RelayDiscoveryService(getDriver());
        }
        return discovery;
    }

    /**
     * Return discovery service without creating it if not exists.
     *
     * @return Discovery service or null if not yet created
     */
    public static synchronized RelayDiscoveryService getDiscoveryServiceRef() {
        return discovery;
    }

    /**
     * Return relay lock.
     *
     * @return Relay lock
     */
    public RelayLock getLock() {
        return lock;
    }

    /**
     * Shutdown relay framework. This method should be used <b>ONLY</b> when you
     * are exiting JVM, but please <b>do not invoke it</b> if you really don't
     * need to.
     */
    protected static void shutdown() {

        // stop discovery service
        RelayDiscoveryService discovery = getDiscoveryServiceRef();
        if (discovery != null) {
            discovery.stop();
        }

        // stop processor
        RelayProcessor.getInstance().shutdown();
    }

    /**
     * Return relay with given name or null if no device with given name has
     * been found. Please note that specific relay name may depend on the order
     * it was connected to the USB port (e.g. /dev/video0 vs /dev/video1).
     *
     * @param name the relay name
     * @return Relay with given name or null if not found
     * @throws IllegalArgumentException when name is null
     */
    public static Relay getRelayByName(String name) {

        if (name == null) {
            throw new IllegalArgumentException("Relay name cannot be null");
        }

        for (Relay relay : getRelays()) {
            if (relay.getName().equals(name)) {
                return relay;
            }
        }

        return null;
    }

}
