package de.turban.deadlock.tracer.runtime.datacollection.disruptor;

import com.lmax.disruptor.EventHandler;
import de.turban.deadlock.tracer.runtime.datacollection.IDeadlockGlobalCacheSubmitter;
import de.turban.deadlock.tracer.runtime.datacollection.ILockThreadEntry;

class LockEventHandler implements EventHandler<LockEvent> {

    private IDeadlockGlobalCacheSubmitter cache;

    LockEventHandler(IDeadlockGlobalCacheSubmitter cache) {
        this.cache = cache;

    }

    @Override
    public void onEvent(LockEvent event, long sequence, boolean endOfBatch) {
        ILockThreadEntry lockObj = event.getLockObj();
        cache.newLockMonitorEnter(lockObj, event.getHeldLocks());
    }

}
