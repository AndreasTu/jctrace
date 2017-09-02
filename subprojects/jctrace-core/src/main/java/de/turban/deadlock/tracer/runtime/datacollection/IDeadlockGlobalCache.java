package de.turban.deadlock.tracer.runtime.datacollection;

import java.util.List;

import de.turban.deadlock.tracer.runtime.ILockCache;
import de.turban.deadlock.tracer.runtime.ILockCacheEntry;

public interface IDeadlockGlobalCache extends ILockCache {

    List<ILockCacheEntry> getLockEntriesExpungeStallEntries();

}
