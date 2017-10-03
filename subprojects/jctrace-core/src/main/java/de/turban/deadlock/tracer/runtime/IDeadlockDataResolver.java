package de.turban.deadlock.tracer.runtime;

public interface IDeadlockDataResolver extends IDeadlockDataBaseResolver {

    IReportLockCache getLockCache();

    IReportFieldCache getFieldCache();

    IReportFieldDescriptorCache getFieldDescriptorCache();
}
