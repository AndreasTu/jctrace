package de.turban.deadlock.tracer.runtime.datacollection;

import de.turban.deadlock.tracer.runtime.ICacheEntry;
import de.turban.deadlock.tracer.runtime.IFieldCacheEntry;
import de.turban.deadlock.tracer.runtime.ILockCacheEntry;
import de.turban.deadlock.tracer.runtime.JctraceUtil;
import de.turban.deadlock.tracer.runtime.serdata.FieldCacheEntrySer;
import de.turban.deadlock.tracer.runtime.serdata.ISerializableData;
import de.turban.deadlock.tracer.runtime.serdata.LockCacheEntrySer;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Objects.requireNonNull;

public final class DeadlockGlobalCache implements IDeadlockGlobalCacheSubmitter, IDeadlockGlobalCache {

    private final ICachingStrategy<ILockCacheEntry, ILockThreadEntry> lockCache;

    private final ICachingStrategy<IFieldCacheEntry, IFieldThreadEntry> fieldCache;

    @SuppressWarnings({"unused", "FieldCanBeLocal"})
    private final DeadlockCollector fileCollector;

    private final IDeadlockCollectBindingResolver resolver;


    static DeadlockGlobalCache
    create(IDeadlockCollectBindingResolver resolver, ICachingStrategy.ICachingStrategyCreator<ICacheEntry, IThreadEntry> strategyCreator) {

        ICachingStrategy<ILockCacheEntry, ILockThreadEntry> lockCache = JctraceUtil.uncheckedCast(
            strategyCreator.apply(e -> new LockCacheEntry((ILockThreadEntry) e)));
        ICachingStrategy<IFieldCacheEntry, IFieldThreadEntry> fieldCache = JctraceUtil.uncheckedCast(
            strategyCreator.apply(e -> new FieldCacheEntry((IFieldThreadEntry) e)));

        return new DeadlockGlobalCache(resolver, lockCache, fieldCache);
    }

    private DeadlockGlobalCache(IDeadlockCollectBindingResolver resolver,
                                ICachingStrategy<ILockCacheEntry, ILockThreadEntry> lockCache,
                                ICachingStrategy<IFieldCacheEntry, IFieldThreadEntry> fieldCache) {
        this.resolver = resolver;
        this.lockCache = lockCache;
        this.fieldCache = fieldCache;
        this.fileCollector = new DeadlockCollector(resolver);

    }

    @Override
    public void waitForProcessing() {
    }

    @Nullable
    @Override
    public ILockCacheEntry getLockCacheEntryForThreadUnsafe(ILockThreadEntry threadEntry) {
        return lockCache.getCacheEntryUnsafe(threadEntry);
    }

    @Override
    public void newLockMonitorEnter(ILockThreadEntry lockThreadEntry, ILockThreadEntry[] heldLocks) {
        LockCacheEntry lockEntry = (LockCacheEntry) lockCache.getCacheEntryOrCreate(lockThreadEntry);
        lockEntry.updateLockCacheEntry(resolver, lockThreadEntry, heldLocks);

    }

    @Override
    public void newFieldGet(IFieldThreadEntry fieldThreadEntry, ILockThreadEntry[] heldLocks) {
        FieldCacheEntry fieldEntry = (FieldCacheEntry) fieldCache.getCacheEntryOrCreate(fieldThreadEntry);
        fieldEntry.fieldGetCacheEntry(resolver, fieldThreadEntry, heldLocks);
    }

    @Override
    public void newFieldSet(IFieldThreadEntry fieldThreadEntry, ILockThreadEntry[] heldLocks) {
        FieldCacheEntry fieldEntry = (FieldCacheEntry) fieldCache.getCacheEntryOrCreate(fieldThreadEntry);
        fieldEntry.fieldSetCacheEntry(resolver, fieldThreadEntry, heldLocks);
    }

    @Override
    public void newLockCreated(ILockThreadEntry lockThreadEntry, ILockThreadEntry[] heldLocks) {
        LockCacheEntry lockEntry = (LockCacheEntry) lockCache.getCacheEntryOrCreate(lockThreadEntry);
        lockEntry.newLockCreatedCacheEntry(resolver, lockThreadEntry, heldLocks);
    }

    @Override
    public List<ILockCacheEntry> getLockEntries() {
        return lockCache.getEntries();
    }

    @Override
    public List<IFieldCacheEntry> getFieldEntries() {
        return fieldCache.getEntries();
    }

    @Override
    public List<ILockCacheEntry> getLockEntriesExpungeStallEntries() {
        return lockCache.getEntriesExpungeStallEntries();
    }

    @Override
    public List<IFieldCacheEntry> getFieldEntriesExpungeStallEntries() {
        return fieldCache.getEntriesExpungeStallEntries();
    }


    @ThreadSafe
    static final class LockCacheEntry extends AbstractCacheEntry implements ILockCacheEntry {
        private static final AtomicInteger globalIdCounter = new AtomicInteger(0);

        private final String lockClass;

        @GuardedBy("this")
        private long lockedCount = 0;

        @GuardedBy("this")
        private final TIntSet dependentLocks = new TIntHashSet();


        LockCacheEntry(ILockThreadEntry entry) {
            super(globalIdCounter.incrementAndGet());
            requireNonNull(entry);
            Object lock = entry.getLock();
            requireNonNull(lock);
            if (lock instanceof Class) {
                Class<?> clazz = (Class<?>) lock;
                lockClass = "(ClassLock) " + clazz.getName();
            } else {
                lockClass = lock.getClass().getName();
            }

        }

        protected ISerializableData createSerObjInternal(int revision) {
            return new LockCacheEntrySer(this, revision);
        }

        void newLockCreatedCacheEntry(IDeadlockCollectBindingResolver resolver,
                                      ILockThreadEntry lockThreadEntry,
                                      ILockThreadEntry[] heldLocks) {
            synchronized (this) {
                setChangedSinceLastSnapshot();
                int locationId = lockThreadEntry.getLocationId();
                int threadId = lockThreadEntry.getThreadId();
                boolean newLocationAdded = addLocationId(locationId);
                if (newLocationAdded) {
                    checkNewLocationForStacktracing(resolver, locationId);
                }
                addThreadId(threadId);
            }
        }

        void updateLockCacheEntry(IDeadlockCollectBindingResolver resolver,
                                  ILockThreadEntry lockThreadEntry,
                                  ILockThreadEntry[] heldLocks) {
            synchronized (this) {
                setChangedSinceLastSnapshot();
                int locationId = lockThreadEntry.getLocationId();
                int threadId = lockThreadEntry.getThreadId();
                boolean newLocationAdded = addLocationId(locationId);
                if (newLocationAdded) {
                    checkNewLocationForStacktracing(resolver, locationId);
                }
                addThreadId(threadId);
                incrementLockCount();
                TIntList dependentLocksLst = addDependentLocks(resolver, lockThreadEntry, heldLocks);
                addStackEntry(locationId, threadId, dependentLocksLst);
            }
        }

        private TIntList addDependentLocks(IDeadlockCollectBindingResolver resolver, ILockThreadEntry lockThreadEntry, ILockThreadEntry[] heldLocks) {

            TIntList dependentLocks = null;
            if (isStackCapturingEnabled()) {
                dependentLocks = new TIntArrayList();
            }
            for (ILockThreadEntry lockThObj : heldLocks) {
                if (lockThObj == null) {
                    continue;
                }
                if (lockThObj == lockThreadEntry) {
                    continue;
                }
                ILockCacheEntry lockEntry = resolver.getLockCache().getLockCacheEntryForThreadUnsafe(lockThObj);
                if (lockEntry == null) {
                    ILockCacheEntry lockEntry2 = resolver.getLockCache().getLockCacheEntryForThreadUnsafe(lockThObj);
                    throw new IllegalStateException("There must be a ILockCacheEntry for the lock " + lockThObj + " second try " + lockEntry2);
                }
                addDependentLocks(lockEntry);
                if (isStackCapturingEnabled()) {
                    requireNonNull(dependentLocks).add(lockEntry.getId());
                }
            }
            return dependentLocks;
        }

        private void incrementLockCount() {
            lockedCount++;
        }

        private void addDependentLocks(ILockCacheEntry lock) {
            dependentLocks.add(lock.getId());
        }

        @Override
        public boolean hasDependentLock(ILockCacheEntry lock) {
            return hasDependentLock(lock.getId());
        }

        @Override
        public boolean hasDependentLock(int lockId) {
            synchronized (this) {
                return dependentLocks.contains(lockId);
            }
        }

        @Override
        public int compareTo(@Nonnull ILockCacheEntry o) {
            int id1 = this.getId();
            int id2 = o.getId();
            return Integer.compare(id1, id2);
        }

        @Override
        public long getLockedCount() {
            return lockedCount;
        }

        @Override
        public String getLockClass() {
            return lockClass;
        }

        @Override
        public int[] getDependentLocks() {
            synchronized (this) {
                return dependentLocks.toArray();
            }
        }

        @Override
        public String toString() {
            return "LockCacheEntry [id=" + getId() + ", lockClass=" + lockClass + ", lockedCount=" + lockedCount + "]";
        }

    }


    @ThreadSafe
    static final class FieldCacheEntry extends AbstractCacheEntry implements IFieldCacheEntry {
        private static final AtomicInteger globalIdCounter = new AtomicInteger(0);

        private final int fieldDescriptorId;

        @GuardedBy("this")
        private long readCount = 0;

        @GuardedBy("this")
        private long writeCount = 0;

        private final int ownerIdentityHash;

        FieldCacheEntry(IFieldThreadEntry entry) {
            super(globalIdCounter.incrementAndGet());

            requireNonNull(entry);
            Object owner = entry.getOwner();
            requireNonNull(owner);
            ownerIdentityHash = System.identityHashCode(owner);
            fieldDescriptorId = entry.getFieldDescriptorId();
        }

        protected ISerializableData createSerObjInternal(int revision) {
            return new FieldCacheEntrySer(this, revision);
        }

        void fieldGetCacheEntry(IDeadlockCollectBindingResolver resolver,
                                IFieldThreadEntry fieldThreadEntry,
                                ILockThreadEntry[] heldLocks) {
            synchronized (this) {
                fieldUsage(resolver, fieldThreadEntry, heldLocks);
                this.incrementReadCount();
            }
        }

        void fieldSetCacheEntry(IDeadlockCollectBindingResolver resolver,
                                IFieldThreadEntry fieldThreadEntry,
                                ILockThreadEntry[] heldLocks) {
            synchronized (this) {
                fieldUsage(resolver, fieldThreadEntry, heldLocks);
                this.incrementWriteCount();
            }
        }

        private void fieldUsage(IDeadlockCollectBindingResolver resolver, IFieldThreadEntry fieldThreadEntry, ILockThreadEntry[] heldLocks) {
            setChangedSinceLastSnapshot();
            int locationId = fieldThreadEntry.getLocationId();
            int threadId = fieldThreadEntry.getThreadId();
            boolean newLocationAdded = addLocationId(locationId);
            if (newLocationAdded) {
                checkNewLocationForStacktracing(resolver, locationId);
            }
            addThreadId(threadId);
            TIntList dependentLocksList = addDependentLocks(resolver, heldLocks);
            addStackEntry(locationId, threadId, dependentLocksList);
        }

        private TIntList addDependentLocks(IDeadlockCollectBindingResolver resolver, ILockThreadEntry[] heldLocks) {

            TIntList dependentLocks = null;
            if (isStackCapturingEnabled()) {
                dependentLocks = new TIntArrayList();
            }
            for (ILockThreadEntry lockThObj : heldLocks) {
                if (lockThObj == null) {
                    continue;
                }
                ILockCacheEntry lockEntry = resolver.getLockCache().getLockCacheEntryForThreadUnsafe(lockThObj);
                if (lockEntry == null) {
                    ILockCacheEntry lockEntry2 = resolver.getLockCache().getLockCacheEntryForThreadUnsafe(lockThObj);
                    throw new IllegalStateException("There must be a ILockCacheEntry for the lock " + lockThObj + " second try " + lockEntry2);
                }
                //addDependentLocks(lockEntry);
                if (isStackCapturingEnabled()) {
                    requireNonNull(dependentLocks).add(lockEntry.getId());
                }
            }
            return dependentLocks;
        }

        private void incrementWriteCount() {
            writeCount++;
        }

        private void incrementReadCount() {
            readCount++;
        }

        @Override
        public int compareTo(@Nonnull IFieldCacheEntry o) {
            int id1 = this.getId();
            int id2 = o.getId();
            return Integer.compare(id1, id2);
        }


        @Override
        public int getFieldDescriptorId() {
            return fieldDescriptorId;
        }

        @Override
        public long getReadCount() {
            synchronized (this) {
                return readCount;
            }
        }

        @Override
        public long getWriteCount() {
            synchronized (this) {
                return writeCount;
            }
        }

        public int getOwnerIdentityHash() {
            return ownerIdentityHash;
        }

        @Override
        public String toString() {
            return "FieldCacheEntry [id=" + getId() + ", fieldDescriptorId=" + fieldDescriptorId + "]";
        }

    }

}
