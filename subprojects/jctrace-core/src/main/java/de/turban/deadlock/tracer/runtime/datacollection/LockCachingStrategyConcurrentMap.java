package de.turban.deadlock.tracer.runtime.datacollection;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import de.turban.deadlock.tracer.runtime.ILockCacheEntry;

public class LockCachingStrategyConcurrentMap implements ILockCachingStrategy {

    private final ConcurrentMap<LockWeakRef, ILockCacheEntry> cache = new ConcurrentHashMap<>();
    private final Function<ILockThreadEntry, ILockCacheEntry> factory;

    LockCachingStrategyConcurrentMap(Function<ILockThreadEntry, ILockCacheEntry> factory) {
        this.factory = factory;
    }

    @Override
    public ILockCacheEntry getLockCacheEntryOrCreate(ILockThreadEntry lockThreadEntry) {
        ILockCacheEntry lockEntry = getLockCacheEntryUnsafe(lockThreadEntry);
        if (lockEntry == null) {
            lockEntry = createNewEntry(lockThreadEntry);
        }
        return lockEntry;
    }

    @Override
    public ILockCacheEntry getLockCacheEntryUnsafe(ILockThreadEntry lockThreadEntry) {
        LockWeakRef weakRef = lockThreadEntry.getLockWeakReference();
        ILockCacheEntry lockEntry = cache.get(weakRef);
        return lockEntry;
    }

    private ILockCacheEntry createNewEntry(ILockThreadEntry lockThreadEntry) {
        ILockCacheEntry newLockEntry = factory.apply(lockThreadEntry);
        LockWeakRef weakRef = lockThreadEntry.getLockWeakReference();
        ILockCacheEntry oldlockEntry = cache.putIfAbsent(weakRef, newLockEntry);
        if (oldlockEntry != null) {
            return oldlockEntry;
        }
        return newLockEntry;
    }

    @Override
    public List<ILockCacheEntry> getLockEntries() {
        return new ArrayList<>(cache.values());
    }

    @Override
    public List<ILockCacheEntry> getLockEntriesExpungeStallEntries() {

        List<ILockCacheEntry> list = new ArrayList<>();
        for (Iterator<Entry<LockWeakRef, ILockCacheEntry>> iterator = cache.entrySet().iterator(); iterator.hasNext();) {
            Entry<LockWeakRef, ILockCacheEntry> entry = iterator.next();
            LockWeakRef wRef = entry.getKey();
            ILockCacheEntry value = entry.getValue();
            list.add(value);
            if (wRef.get() == null) {
                iterator.remove();
            }
        }
        return list;
    }
}
