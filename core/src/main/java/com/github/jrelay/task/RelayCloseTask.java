package com.github.jrelay.task;

import com.github.jrelay.RelayDevice;
import com.github.jrelay.RelayDriver;
import com.github.jrelay.RelayTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by nightingale on 29.04.16.
 */
public class RelayCloseTask extends RelayTask {

    private static final Logger LOG = LoggerFactory.getLogger(RelayCloseTask.class);

    public  RelayCloseTask (RelayDriver driver, RelayDevice device) {
        super(driver, device);
    }

    public void close() throws InterruptedException {
        process();
    }

    @Override
    protected void handle() {

        RelayDevice device = getDevice();
        if (!device.isOpen()) {
            return;
        }

        LOG.info("Closing {}", device.getName());

        device.close();
    }

}
