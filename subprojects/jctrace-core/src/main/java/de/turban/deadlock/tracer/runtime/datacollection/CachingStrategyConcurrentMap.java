package de.turban.deadlock.tracer.runtime.datacollection;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import de.turban.deadlock.tracer.runtime.ICacheEntry;
import de.turban.deadlock.tracer.runtime.ILockCacheEntry;

public class CachingStrategyConcurrentMap<C extends ICacheEntry,T extends IThreadEntry> implements ICachingStrategy<C,T> {

    private final ConcurrentMap<LockWeakRef, C> lockCache = new ConcurrentHashMap<>();
    private final Function<T, C> factory;

    CachingStrategyConcurrentMap(Function<T, C> factory) {
        this.factory = factory;
    }

    @Override
    public C getCacheEntryOrCreate(T threadEntry) {
        C lockEntry = getCacheEntryUnsafe(threadEntry);
        if (lockEntry == null) {
            lockEntry = createNewEntry(threadEntry);
        }
        return lockEntry;
    }

    @Override
    public C getCacheEntryUnsafe(T threadEntry) {
        LockWeakRef weakRef = threadEntry.getLockWeakReference();
        C lockEntry = lockCache.get(weakRef);
        return lockEntry;
    }

    private C createNewEntry(T threadEntry) {
        C newLockEntry = factory.apply(threadEntry);
        LockWeakRef weakRef = threadEntry.getLockWeakReference();
        C oldlockEntry = lockCache.putIfAbsent(weakRef, newLockEntry);
        if (oldlockEntry != null) {
            return oldlockEntry;
        }
        return newLockEntry;
    }

    @Override
    public List<C> getEntries() {
        return new ArrayList<>(lockCache.values());
    }

    @Override
    public List<C> getEntriesExpungeStallEntries() {

        List<C> list = new ArrayList<>();
        for (Iterator<Entry<LockWeakRef, C>> iterator = lockCache.entrySet().iterator(); iterator.hasNext();) {
            Entry<LockWeakRef, C> entry = iterator.next();
            LockWeakRef wRef = entry.getKey();
            C value = entry.getValue();
            list.add(value);
            if (wRef.get() == null) {
                iterator.remove();
            }
        }
        return list;
    }
}
