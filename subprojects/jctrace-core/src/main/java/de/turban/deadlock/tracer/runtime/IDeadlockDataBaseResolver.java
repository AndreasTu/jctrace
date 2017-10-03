package de.turban.deadlock.tracer.runtime;

/**
 * 
 */
public interface IDeadlockDataBaseResolver {

    IFieldCache getFieldCache();

    ILockCache getLockCache();

    ILocationCache getLocationCache();

    IThreadCache getThreadCache();

    IFieldDescriptorCache getFieldDescriptorCache();
}
