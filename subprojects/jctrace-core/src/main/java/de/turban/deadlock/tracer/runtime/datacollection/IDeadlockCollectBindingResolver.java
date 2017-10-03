package de.turban.deadlock.tracer.runtime.datacollection;

import de.turban.deadlock.tracer.runtime.IDeadlockDataBaseResolver;

public interface IDeadlockCollectBindingResolver extends IDeadlockDataBaseResolver {

    @Override
    IDeadlockGlobalCache getFieldCache();

    @Override
    IDeadlockGlobalCache getLockCache();

    IDeadlockGlobalCacheSubmitter getCacheSubmitter();

    @Override
    LocationCache getLocationCache();

    @Override
    ThreadCache getThreadCache();

    @Override
    FieldDescriptorCache getFieldDescriptorCache();

}
