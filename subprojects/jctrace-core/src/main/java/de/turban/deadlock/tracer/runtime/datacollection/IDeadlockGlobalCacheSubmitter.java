package de.turban.deadlock.tracer.runtime.datacollection;

public interface IDeadlockGlobalCacheSubmitter {

    void waitForProcessing();

    void newLockMonitorEnter(ILockThreadEntry lockEntry, ILockThreadEntry[] heldLocks);

    void newFieldGet(IFieldThreadEntry fieldThreadEntry, ILockThreadEntry[] heldLocks);

    void newFieldSet(IFieldThreadEntry fieldThreadEntry, ILockThreadEntry[] heldLocks);

    void newLockCreated(ILockThreadEntry lockEntry, ILockThreadEntry[] heldLocks);

}
