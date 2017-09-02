package de.turban.deadlock.tracer.runtime.datacollection;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import de.turban.deadlock.tracer.runtime.ILockCacheEntry;

public class LockCachingStrategyConcurrentMapEqualStrategy implements ILockCachingStrategy {

    private final ConcurrentMap<EqualStrategy, ILockCacheEntry> cache = new ConcurrentHashMap<>();
    private final Function<ILockThreadEntry, ILockCacheEntry> factory;

    LockCachingStrategyConcurrentMapEqualStrategy(Function<ILockThreadEntry, ILockCacheEntry> factory) {
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
        ILockCacheEntry lockEntry = cache.get(new EqualStrategy(lockThreadEntry.getLock()));
        return lockEntry;
    }

    private ILockCacheEntry createNewEntry(ILockThreadEntry lockThreadEntry) {
        ILockCacheEntry newLockEntry = factory.apply(lockThreadEntry);
        LockWeakRef weakRef = lockThreadEntry.getLockWeakReference();
        ILockCacheEntry oldlockEntry = cache.putIfAbsent(new EqualStrategy(weakRef), newLockEntry);
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
        for (Iterator<Entry<EqualStrategy, ILockCacheEntry>> iterator = cache.entrySet().iterator(); iterator.hasNext();) {
            Entry<EqualStrategy, ILockCacheEntry> entry = iterator.next();
            EqualStrategy key = entry.getKey();
            ILockCacheEntry value = entry.getValue();
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
