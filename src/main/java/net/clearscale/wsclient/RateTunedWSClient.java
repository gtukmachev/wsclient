package net.clearscale.wsclient;

import java.util.Stack;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * A client for REST WS.
 * <p/>
 * The main feature of the client is that a number of requests is limited by throughput rate index which is defined manually.
 * Rate index is measured in requests per seconds.
 * This class is threadsafe.
 *
 * @author Yuriy Pankratyev
 */
public class RateTunedWSClient {

    private static final RateIndexCounter rateIndexCounter = new RateIndexCounter();
    private static final int NUMBER_OF_WS_CLIENT_THREADS = 50;
    private static final Executor requestExecutor = Executors.newFixedThreadPool(NUMBER_OF_WS_CLIENT_THREADS);

    private static final Stack<RateLimitedGetTask> stackOfTasks = new Stack<RateLimitedGetTask>();
    public static final String URL = "http://catalog.api.2gis.ru/profile?version=1.3&key=ruuxah6217";

    public static void main(String[] args) throws InterruptedException {
        rateIndexCounter.start();

        //create 100 tasks
        for (int i = 0; i < 100; i++) {
            RateLimitedGetTask getTask = new RateLimitedGetTask(URL, rateIndexCounter, stackOfTasks);
            //link task as last element
            stackOfTasks.add(getTask);
        }

        while(!stackOfTasks.isEmpty()) {
            RateLimitedGetTask task = stackOfTasks.pop();
            requestExecutor.execute(task);
        }


    }
}
