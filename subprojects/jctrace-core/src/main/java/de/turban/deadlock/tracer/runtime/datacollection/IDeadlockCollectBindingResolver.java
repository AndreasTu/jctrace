package de.turban.deadlock.tracer.runtime.datacollection;

import de.turban.deadlock.tracer.runtime.IDeadlockDataBaseResolver;

public interface IDeadlockCollectBindingResolver extends IDeadlockDataBaseResolver {

    @Override
    IDeadlockGlobalCache getLockCache();

    IDeadlockGlobalCacheSubmitter getCacheSubmitter();

    @Override
    LockerLocationCache getLocationCache();

    @Override
    LockerThreadCache getThreadCache();

}
