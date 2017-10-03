package de.turban.deadlock.tracer.runtime;


public interface ILockCacheEntry extends Comparable<ILockCacheEntry>, ICacheEntry {

    long getLockedCount();

    String getLockClass();

    int[] getDependentLocks();

    boolean hasDependentLock(ILockCacheEntry lock);

    boolean hasDependentLock(int lockId);

}