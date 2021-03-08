package de.turban.deadlock.tracer;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import de.turban.deadlock.tracer.runtime.datacollection.DeadlockCollectBindingResolver;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DeadlockCachePerformanceTest {

    private static ListeningExecutorService executorService;

    private List<Object> locks = Lists.newArrayList();
    private static int numThreads;

    private static int locNr;

    @BeforeClass
    public static void setupClass() throws InterruptedException, ExecutionException {
        locNr = DeadlockTracerClassBinding.newLocation(DeadlockCachePerformanceTest.class.getName(), "lock", "", 1);
        numThreads = Runtime.getRuntime().availableProcessors();
        numThreads = 4;
        executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(numThreads));

        new DeadlockCachePerformanceTest().executeLocking(false);
    }

    @Test
    public void test() throws InterruptedException, ExecutionException {
        executeLocking(true);
    }

    private void executeLocking(boolean print) throws InterruptedException, ExecutionException {

        Stopwatch watch = Stopwatch.createStarted();

        List<ListenableFuture<?>> futures = Lists.newArrayList();
        for (int i = 0; i < numThreads; i++) {
            Object obj = new Object();
            Object obj2 = new Object();
            locks.add(obj);
            locks.add(obj2);
            futures.add(executorService.submit(() -> lockForOneThread(obj, obj2)));
        }
        ListenableFuture<List<Object>> res = Futures.allAsList(futures);
        res.get();
        DeadlockCollectBindingResolver.INSTANCE.getCacheSubmitter().waitForProcessing();
        watch.stop();
        if (print) {
            System.out.println("Took:  " + watch.elapsed(TimeUnit.MILLISECONDS) + "ms");
        }

    }

    private void lockForOneThread(Object obj, Object obj2) {
        for (int x = 0; x < 50000000; x++) {
            lock(obj, obj2);
        }
    }

    private void lock(Object obj, Object obj2) {
        synchronized (obj) {
            DeadlockTracerClassBinding.monitorEnter(obj, locNr);
            try {
                synchronized (obj2) {
                    DeadlockTracerClassBinding.monitorEnter(obj2, locNr);
                    try {
                        obj.notify();
                    } finally {
                        DeadlockTracerClassBinding.monitorExit(obj2);
                    }
                }
            } finally {
                DeadlockTracerClassBinding.monitorExit(obj);
            }
        }
        DeadlockTracerClassBinding.checkForOccurredException();
    }
}
