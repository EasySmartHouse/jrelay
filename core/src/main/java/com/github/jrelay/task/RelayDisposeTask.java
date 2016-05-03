package com.github.jrelay.task;

import com.github.jrelay.RelayDevice;
import com.github.jrelay.RelayDriver;
import com.github.jrelay.RelayTask;

/**
 * Created by nightingale on 29.04.16.
 */
public class RelayDisposeTask extends RelayTask {

    public RelayDisposeTask(RelayDriver driver, RelayDevice device) {
        super(driver, device);
    }

    public void dispose() throws InterruptedException {
        process();
    }

    @Override
    protected void handle() {
        getDevice().dispose();
    }

}
