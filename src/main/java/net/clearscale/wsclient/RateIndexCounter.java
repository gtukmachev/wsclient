package net.clearscale.wsclient;

import org.apache.log4j.Logger;

import java.util.Timer;
import java.util.TimerTask;

/*
 * We do not use this class outside the package, so we do not need any javadocs for it
 * @author Yuriy Pankratyev
 */
class RateIndexCounter {

    private static final Logger logger = Logger.getLogger(RateIndexCounter.class);
    private static final int DEFAULT_RATE = 10;
    private static final int A_SECOND = 1000;

    private final int rate;
    private final Timer resetTimer = new Timer(true);

    //GuardedBy("this")
    private int requestsNumber;
    private long lastResetTimestamp;

    public RateIndexCounter() {
        this.rate = DEFAULT_RATE;
    }

    public void start() {
        lastResetTimestamp = System.currentTimeMillis();
        TimerTask reset = new TimerTask() {
            @Override
            public void run() {
                reset(this.scheduledExecutionTime());
            }
        };
        resetTimer.scheduleAtFixedRate(reset, 0, A_SECOND);
    }

    private synchronized void reset(long scheduledExecutionTimestamp) {
        long diff = scheduledExecutionTimestamp - lastResetTimestamp;
        boolean isStale = diff < 1000;
        if (!isStale) {
            logger.info("---------- " + requestsNumber + " => 0 ---------- from last reset " + diff + " ms has gone");
            lastResetTimestamp = System.currentTimeMillis();
            requestsNumber = 0;
        }
    }

    public synchronized void inc() {
        requestsNumber++;
    }

    public synchronized boolean canI() {
        return requestsNumber < rate && isSameSecond();
    }

    private boolean isSameSecond() {
        return System.currentTimeMillis() - lastResetTimestamp < A_SECOND;
    }

    public synchronized int get() {
        return requestsNumber;
    }

}