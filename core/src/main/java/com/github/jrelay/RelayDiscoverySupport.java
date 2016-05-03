package com.github.jrelay;

/**
 * Created by nightingale on 24.04.16.
 */
/**
 * This interface should be implemented by all relay drivers which would like to support relay
 * devices discovery mechanism.
 *
 */
public interface RelayDiscoverySupport {

    /**
     * Default relay discovery scan interval in milliseconds.
     */
    public static final long DEFAULT_SCAN_INTERVAL = 3000;

    /**
     * Get interval between next discovery scans. Time interval is given in milliseconds.
     *
     * @return Time interval between next scans
     */
    long getScanInterval();

    /**
     * Check if scan is possible. In some cases, even if driver support devices discovery, there can
     * be a situation when due to various factors, scan cannot be executed (e.g. devices are busy,
     * network is unavailable, devices registry not responding, etc). In general this method should
     * return true.
     *
     * @return True if scan possible, false otherwise
     */
    boolean isScanPossible();
}
