package de.turban.deadlock.tracer.runtime.datacollection.disruptor;

import de.turban.deadlock.tracer.runtime.datacollection.ILockThreadEntry;

class LockEvent {
    private ILockThreadEntry lockEntry;
    private ILockThreadEntry[] heldLocks;

    public LockEvent() {

    }

    public ILockThreadEntry getLockObj() {
        return lockEntry;
    }

    public void setLockObj(ILockThreadEntry lockEntry) {
        this.lockEntry = lockEntry;
    }

    public ILockThreadEntry[] getHeldLocks() {
        return heldLocks;
    }

    public void setHeldLocks(ILockThreadEntry[] heldLocks) {
        this.heldLocks = heldLocks;
    }

}
