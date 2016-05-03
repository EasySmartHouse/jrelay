package com.github.jrelay;

/**
 * Created by nightingale on 29.04.16.
 */
public abstract class RelayTask {

    private boolean doSync = true;
    private RelayProcessor processor = null;
    private RelayDevice device = null;
    private Throwable throwable = null;

    public RelayTask(boolean threadSafe, RelayDevice device) {
        this.doSync = !threadSafe;
        this.device = device;
        this.processor = RelayProcessor.getInstance();
    }

    public RelayTask(RelayDriver driver, RelayDevice device) {
        this(driver.isThreadSafe(), device);
    }

    public RelayTask(RelayDevice device) {
        this(false, device);
    }

    public RelayDevice getDevice() {
        return device;
    }

    /**
     * Process task by processor thread.
     *
     * @throws InterruptedException when thread has been interrupted
     */
    public void process() throws InterruptedException {

        boolean alreadyInSync = Thread.currentThread() instanceof RelayProcessor.ProcessorThread;

        if (alreadyInSync) {
            handle();
        } else {
            if (doSync) {
                if (processor == null) {
                    throw new RuntimeException("Driver should be synchronized, but processor is null");
                }
                processor.process(this);
            } else {
                handle();
            }
        }
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public void setThrowable(Throwable t) {
        this.throwable = t;
    }

    protected abstract void handle();
}
