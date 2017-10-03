package de.turban.deadlock.tracer.runtime.datacollection;

import de.turban.deadlock.tracer.runtime.ICacheEntry;
import gnu.trove.map.hash.THashMap;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

public class CachingStrategyTHashMap<C extends ICacheEntry, T extends IThreadEntry> implements ICachingStrategy<C, T> {

    private final THashMap<Object, C> cache = new THashMap<Object, C>() {

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
    private final Function<T, C> factory;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock writeLock = lock.writeLock();
    private final Lock readLock = lock.readLock();

    CachingStrategyTHashMap(Function<T, C> factory) {
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
        readLock.lock();
        try {
            C res = cache.get(threadEntry.getObject());
            return res;
        } finally {
            readLock.unlock();
        }
    }

    private C createNewEntry(T threadEntry) {
        writeLock.lock();
        try {

            C newLockEntry = factory.apply(threadEntry);
            LockWeakRef lockWeakRef = threadEntry.getLockWeakReference();
            C oldlockEntry = cache.put(lockWeakRef, newLockEntry);
            if (oldlockEntry != null) {
                throw new IllegalStateException();
            }
            return newLockEntry;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public List<C> getEntries() {
        readLock.lock();
        try {
            return new ArrayList<>(cache.values());
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public List<C> getEntriesExpungeStallEntries() {
        throw new UnsupportedOperationException();
    }

}
