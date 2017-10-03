package de.turban.deadlock.tracer.runtime.datacollection;

import de.turban.deadlock.tracer.runtime.IFieldCache;
import de.turban.deadlock.tracer.runtime.IFieldCacheEntry;
import de.turban.deadlock.tracer.runtime.ILockCache;
import de.turban.deadlock.tracer.runtime.ILockCacheEntry;

import javax.annotation.Nullable;
import java.util.List;

public interface IDeadlockGlobalCache extends ILockCache, IFieldCache {

    @Nullable
    ILockCacheEntry getLockCacheEntryForThreadUnsafe(ILockThreadEntry threadEntry);

    List<ILockCacheEntry> getLockEntriesExpungeStallEntries();

    List<IFieldCacheEntry> getFieldEntriesExpungeStallEntries();

}
