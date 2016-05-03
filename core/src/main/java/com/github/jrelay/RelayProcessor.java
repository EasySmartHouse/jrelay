package com.github.jrelay;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by nightingale on 29.04.16.
 */
public class RelayProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(RelayProcessor.class);

    /**
     * Thread doing supersync processing.
     *
     * @author sarxos
     */
    public static final class ProcessorThread extends Thread {

        private static final AtomicInteger N = new AtomicInteger(0);

        public ProcessorThread(Runnable r) {
            super(r, String.format("atomic-processor-%d", N.incrementAndGet()));
        }
    }

    /**
     * Thread factory for processor.
     *
     * @author Bartosz Firyn (SarXos)
     */
    private static final class ProcessorThreadFactory implements ThreadFactory {

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new ProcessorThread(r);
            t.setUncaughtExceptionHandler(RelayExceptionHandler.getInstance());
            t.setDaemon(true);
            return t;
        }
    }

    /**
     * Heart of overall processing system. This class process all native calls wrapped in tasks, by
     * doing this all tasks executions are super-synchronized.
     *
     * @author Bartosz Firyn (SarXos)
     */
    private static final class AtomicProcessor implements Runnable {

        private SynchronousQueue<RelayTask> inbound = new SynchronousQueue<RelayTask>(true);
        private SynchronousQueue<RelayTask> outbound = new SynchronousQueue<RelayTask>(true);

        /**
         * Process task.
         *
         * @param task the task to be processed
         * @throws InterruptedException when thread has been interrupted
         */
        public void process(RelayTask task) throws InterruptedException {
            inbound.put(task);

            Throwable t = outbound.take().getThrowable();
            if (t != null) {
                throw new RelayException("Cannot execute task", t);
            }
        }

        @Override
        public void run() {
            while (true) {
                RelayTask t = null;
                try {
                    (t = inbound.take()).handle();
                } catch (InterruptedException e) {
                    break;
                } catch (Throwable e) {
                    if (t != null) {
                        t.setThrowable(e);
                    }
                } finally {
                    if (t != null) {
                        try {
                            outbound.put(t);
                        } catch (InterruptedException e) {
                            break;
                        } catch (Exception e) {
                            throw new RuntimeException("Cannot put task into outbound queue", e);
                        }
                    }
                }
            }
        }
    }

    /**
     * Is processor started?
     */
    private static final AtomicBoolean started = new AtomicBoolean(false);

    /**
     * Execution service.
     */
    private static ExecutorService runner = null;

    /**
     * Static processor.
     */
    private static final AtomicProcessor processor = new AtomicProcessor();

    /**
     * Singleton instance.
     */
    private static final RelayProcessor INSTANCE = new RelayProcessor();;

    private RelayProcessor() {
    }

    /**
     * Process single relay task.
     *
     * @param task the task to be processed
     * @throws InterruptedException when thread has been interrupted
     */
    public void process(RelayTask task) throws InterruptedException {

        if (started.compareAndSet(false, true)) {
            runner = Executors.newSingleThreadExecutor(new ProcessorThreadFactory());
            runner.execute(processor);
        }

        if (!runner.isShutdown()) {
            processor.process(task);
        } else {
            throw new RejectedExecutionException("Cannot process because processor runner has been already shut down");
        }
    }

    public void shutdown() {
        if (started.compareAndSet(true, false)) {

            LOG.debug("Shutting down relay processor");

            runner.shutdown();

            LOG.debug("Awaiting tasks termination");

            while (runner.isTerminated()) {

                try {
                    runner.awaitTermination(100, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    return;
                }

                runner.shutdownNow();
            }

            LOG.debug("All tasks has been terminated");
        }

    }

    public static synchronized RelayProcessor getInstance() {
        return INSTANCE;
    }
}
