package de.turban.deadlock.tracer.runtime;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.List;

public interface ILockCacheEntry extends Comparable<ILockCacheEntry> {

    public static int INVALID_LOCK_ID = 0;

    int getId();

    long getLockedCount();

    String getLockClass();

    int[] getLockerLocationIds();

    int[] getLockerThreadIds();

    int[] getDependentLocks();

    boolean hasDependentLock(ILockCacheEntry lock);

    boolean hasDependentLock( int lockId);

    List<ILockStackEntry> getStackEntries();

}