package de.turban.deadlock.tracer.runtime;

/**
 * 
 */
public interface IDeadlockDataBaseResolver {

    ILockCache getLockCache();

    ILockerLocationCache getLocationCache();

    ILockerThreadCache getThreadCache();
}
