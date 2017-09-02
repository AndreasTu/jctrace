package de.turban.deadlock.tracer.runtime.datacollection;

public interface ILockThreadEntry {

    /**
     * @return returns the lock object.
     */
    Object getLock();

    /**
     * @return The location id. Could also be {@link LockerLocationCache#INVALID_ID}.
     * @see LockerLocationCache
     */
    int getLockerLocationId();

    /**
     * @return The Thread id. Could also be {@link LockerThreadCache#INVALID_ID}.
     * @see LockerThreadCache
     */
    int getLockerThreadId();

    LockWeakRef getLockWeakReference();
}
