package de.turban.deadlock.tracer.runtime.datacollection;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

import de.turban.deadlock.tracer.runtime.ILockCacheEntry;
import gnu.trove.map.hash.THashMap;

public class LockCachingStrategyTHashMap implements ILockCachingStrategy {

    private final THashMap<Object, ILockCacheEntry> cache = new THashMap<Object, ILockCacheEntry>() {

        @Override
        protected boolean equals(Object notnull, Object two) {
            if (notnull instanceof LockWeakRef) {
                return equalLock((LockWeakRef) notnull, two);
            }
            if (two instanceof LockWeakRef) {
                return equalLock((LockWeakRef) two, notnull);
            }
            throw new IllegalStateException();
        }

        private boolean equalLock(LockWeakRef t, Object obj) {
            Object thisObj = t.get();

            if (t.getClass() != obj.getClass()) {
                return thisObj == obj;
            }
            LockWeakRef other = (LockWeakRef) obj;
            Object otherObj = other.get();

            return otherObj == thisObj;
        }

        @Override
        protected int hash(Object notnull) {
            if (notnull instanceof LockWeakRef) {
                return notnull.hashCode();
            }
            return System.identityHashCode(notnull);
        }

    };
    private final Function<ILockThreadEntry, ILockCacheEntry> factory;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock writeLock = lock.writeLock();
    private final Lock readLock = lock.readLock();

    LockCachingStrategyTHashMap(Function<ILockThreadEntry, ILockCacheEntry> factory) {
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
        readLock.lock();
        try {
            ILockCacheEntry res = cache.get(lockThreadEntry.getLock());
            return res;
        } finally {
            readLock.unlock();
        }
    }

    private ILockCacheEntry createNewEntry(ILockThreadEntry lockThreadEntry) {
        writeLock.lock();
        try {

            ILockCacheEntry newLockEntry = factory.apply(lockThreadEntry);
            LockWeakRef lockWeakRef = lockThreadEntry.getLockWeakReference();
            ILockCacheEntry oldlockEntry = cache.put(lockWeakRef, newLockEntry);
            if (oldlockEntry != null) {
                throw new IllegalStateException();
            }
            return newLockEntry;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public List<ILockCacheEntry> getLockEntries() {
        readLock.lock();
        try {
            return new ArrayList<>(cache.values());
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public List<ILockCacheEntry> getLockEntriesExpungeStallEntries() {
        throw new UnsupportedOperationException();
    }

}
