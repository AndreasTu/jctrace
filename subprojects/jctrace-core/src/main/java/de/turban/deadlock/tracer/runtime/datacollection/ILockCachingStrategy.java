package de.turban.deadlock.tracer.runtime.datacollection;

import java.util.List;

import de.turban.deadlock.tracer.runtime.ILockCacheEntry;

public interface ILockCachingStrategy {

    ILockCacheEntry getLockCacheEntryOrCreate(ILockThreadEntry lockThreadEntry);

    ILockCacheEntry getLockCacheEntryUnsafe(ILockThreadEntry lockThreadEntry);

    List<ILockCacheEntry> getLockEntries();

    List<ILockCacheEntry> getLockEntriesExpungeStallEntries();

}
