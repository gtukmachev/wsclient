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

    private final int rate;
    private final Timer resetTimer = new Timer(true);

    //GuardedBy("this")
    private int requestsNumber;
    /**
     * timestamp of the current second
     */
    private long currentSecondTimestamp;

    public RateIndexCounter() {
        this.rate = DEFAULT_RATE;
    }

    public void start() {
        TimerTask reset = new TimerTask() {
            @Override
            public void run() {
                reset();
            }
        };
        resetTimer.schedule(reset, 0, 1000);
    }

    private synchronized void reset() {
        logger.info("---------- " + requestsNumber + " => 0----------");
        requestsNumber = 0;
        currentSecondTimestamp = System.currentTimeMillis();
    }

    public synchronized void inc() {
        requestsNumber++;
    }

    public synchronized boolean canI() {
        boolean isItStillSameSecond = System.currentTimeMillis() - currentSecondTimestamp < 1000;
        return requestsNumber < rate && isItStillSameSecond;
    }

    public synchronized int get() {
        return requestsNumber;
    }

}