package de.turban.deadlock.tracer.runtime.datacollection.asyncqueue;

import de.turban.deadlock.tracer.runtime.datacollection.IDeadlockGlobalCacheSubmitter;
import de.turban.deadlock.tracer.runtime.datacollection.ILockThreadEntry;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DeadlockQueue implements IDeadlockGlobalCacheSubmitter {

    private volatile ExecutorService executor;
    private final IDeadlockGlobalCacheSubmitter cache;

    public DeadlockQueue(IDeadlockGlobalCacheSubmitter cache) {
        this.cache = cache;
        executor = newExecutor();

    }

    private ExecutorService newExecutor() {
        return Executors.newFixedThreadPool(4);
    }

    @Override
    public void newLockMonitorEnter(ILockThreadEntry lockEntry, ILockThreadEntry[] lst) {
        executor.submit(() -> {
            cache.newLockMonitorEnter(lockEntry, lst);
        });
    }

    @Override
    public void newLockCreated(ILockThreadEntry lockEntry, ILockThreadEntry[] lst) {
        executor.submit(() -> {
            cache.newLockCreated(lockEntry, lst);
        });
    }

    @Override
    public void waitForProcessing() {
        ExecutorService old = executor;
        executor = newExecutor();
        old.shutdown();
        cache.waitForProcessing();
    }

}
