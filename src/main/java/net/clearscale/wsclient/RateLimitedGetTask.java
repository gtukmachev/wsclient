package net.clearscale.wsclient;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.Stack;
import java.util.concurrent.CountDownLatch;

/*
 * @author Yuriy Pankratyev
 */
class RateLimitedGetTask implements Runnable {
    
    private static final Logger logger = Logger.getLogger(RateLimitedGetTask.class);

    private final RateIndexCounter rateIndexCounter;
    private final String url;
    private final Stack<RateLimitedGetTask> staskOfTasks;

    public RateLimitedGetTask(String url, RateIndexCounter rateIndexCounter, Stack<RateLimitedGetTask> stackOfTasks) {
        this.rateIndexCounter = rateIndexCounter;
        this.url = url;
        this.staskOfTasks = stackOfTasks;
    }


    public void run() {
        CloseableHttpAsyncClient httpclient = HttpAsyncClients.createDefault();
        httpclient.start();
        boolean isRequestStarted = false;
        final CountDownLatch latch = new CountDownLatch(1);
        final HttpGet httpGet = new HttpGet(url);
        try {
            synchronized (rateIndexCounter) {
                if (rateIndexCounter.canI()) {
                    int before = rateIndexCounter.get();
                    rateIndexCounter.inc();
                    int after = rateIndexCounter.get();

                    logger.info("---------- " + before + " => " + after + "----------");

                    //request with a callback
                    httpclient.execute(httpGet, new FutureCallback<HttpResponse>() {

                        public void completed(final HttpResponse response2) {
                            latch.countDown();
                        }

                        public void failed(final Exception ex) {
                            latch.countDown();
                        }

                        public void cancelled() {
                            latch.countDown();
                        }

                    });
                    isRequestStarted = true;

                } else {
                    //return the task to the stackOfTasks
                    staskOfTasks.push(this);
                }
            }
            //wait till request will be done
            if (isRequestStarted) {
                latch.await();
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                httpclient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
