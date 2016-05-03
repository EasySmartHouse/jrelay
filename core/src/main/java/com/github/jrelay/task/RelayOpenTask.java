package com.github.jrelay.task;

import com.github.jrelay.RelayDevice;
import com.github.jrelay.RelayDriver;
import com.github.jrelay.RelayTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by nightingale on 29.04.16.
 */
public class RelayOpenTask extends RelayTask {

    private static final Logger LOG = LoggerFactory.getLogger(RelayOpenTask.class);

    public RelayOpenTask(RelayDriver driver, RelayDevice device) {
        super(driver, device);
    }

    public void open() throws InterruptedException {
        process();
    }

    @Override
    protected void handle() {

        RelayDevice device = getDevice();

        if (device.isOpen()) {
            return;
        }

        LOG.info("Opening webcam {}", device.getName());

        device.open();
    }

}
