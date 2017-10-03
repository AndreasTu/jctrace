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

public class CachingStrategyConcurrentMapEqualStrategy<C extends ICacheEntry,T extends IThreadEntry> implements ICachingStrategy<C,T> {

    private final ConcurrentMap<EqualStrategy, C> cache = new ConcurrentHashMap<>();
    private final Function<T, C> factory;

    CachingStrategyConcurrentMapEqualStrategy(Function<T, C> factory) {
        this.factory = factory;

    }

    @Override
    public C getCacheEntryOrCreate(T lockThreadEntry) {
        C lockEntry = getCacheEntryUnsafe(lockThreadEntry);
        if (lockEntry == null) {
            lockEntry = createNewEntry(lockThreadEntry);
        }
        return lockEntry;
    }

    @Override
    public C getCacheEntryUnsafe(T lockThreadEntry) {
        C lockEntry = cache.get(new EqualStrategy(lockThreadEntry.getObject()));
        return lockEntry;
    }

    private C createNewEntry(T threadEntry) {
        C newLockEntry = factory.apply(threadEntry);
        LockWeakRef weakRef = threadEntry.getLockWeakReference();
        C oldlockEntry = cache.putIfAbsent(new EqualStrategy(weakRef), newLockEntry);
        if (oldlockEntry != null) {
            return oldlockEntry;
        }
        return newLockEntry;
    }

    @Override
    public List<C> getEntries() {
        return new ArrayList<>(cache.values());
    }

    @Override
    public List<C> getEntriesExpungeStallEntries() {

        List<C> list = new ArrayList<>();
        for (Iterator<Entry<EqualStrategy, C>> iterator = cache.entrySet().iterator(); iterator.hasNext();) {
            Entry<EqualStrategy, C> entry = iterator.next();
            EqualStrategy key = entry.getKey();
            C value = entry.getValue();
            list.add(value);
            LockWeakRef wRef = (LockWeakRef) key.obj;
            if (wRef.get() == null) {
                iterator.remove();
            }
        }
        return list;
    }

    private static class EqualStrategy {

        private final Object obj;
        private final int hash;

        EqualStrategy(Object obj) {
            this.obj = obj;
            if (obj instanceof LockWeakRef) {
                hash = obj.hashCode();
            } else {
                hash = System.identityHashCode(obj);
            }
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (this.getClass() != other.getClass()) {
                return false;
            }
            Object thisObj = this.obj;
            Object otherObj = ((EqualStrategy) other).obj;
            boolean isThisWeak = thisObj instanceof LockWeakRef;
            boolean isOtherWeak = otherObj instanceof LockWeakRef;
            if (isThisWeak) {
                LockWeakRef tWeak = (LockWeakRef) thisObj;
                if (isOtherWeak) {
                    LockWeakRef oWeak = (LockWeakRef) otherObj;
                    return tWeak.get() == oWeak.get();
                } else {
                    return tWeak.get() == otherObj;
                }
            }
            if (isOtherWeak) {
                LockWeakRef oWeak = (LockWeakRef) otherObj;
                return thisObj == oWeak.get();
            }
            return thisObj == otherObj;
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }
}
