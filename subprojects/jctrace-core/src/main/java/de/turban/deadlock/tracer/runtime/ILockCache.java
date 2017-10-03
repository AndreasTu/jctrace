package de.turban.deadlock.tracer.runtime;

import de.turban.deadlock.tracer.runtime.datacollection.ILockThreadEntry;

import java.util.List;

public interface ILockCache {

    List<ILockCacheEntry> getLockEntries();

}
