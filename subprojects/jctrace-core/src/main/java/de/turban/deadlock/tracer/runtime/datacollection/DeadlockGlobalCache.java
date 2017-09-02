package de.turban.deadlock.tracer.runtime.datacollection;

import de.turban.deadlock.tracer.DeadlockTracerClassBinding;
import de.turban.deadlock.tracer.runtime.ILockCacheEntry;
import de.turban.deadlock.tracer.runtime.ILockStackEntry;
import de.turban.deadlock.tracer.runtime.serdata.ISerializableData;
import de.turban.deadlock.tracer.runtime.serdata.ISerializationSnapshotCreator;
import de.turban.deadlock.tracer.runtime.serdata.LockCacheEntrySer;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public final class DeadlockGlobalCache implements IDeadlockGlobalCacheSubmitter, IDeadlockGlobalCache {

    private final ILockCachingStrategy cache;

    @SuppressWarnings("unused")
    private final DeadlockCollector fileCollector;

    private final IDeadlockCollectBindingResolver resolver;


    static DeadlockGlobalCache create(IDeadlockCollectBindingResolver resolver,
                                      Function<Function<ILockThreadEntry, ILockCacheEntry>, ILockCachingStrategy> strategy) {
        Function<ILockThreadEntry, ILockCacheEntry> factory = (x) -> new LockCacheEntry(x);
        return new DeadlockGlobalCache(resolver, strategy.apply(factory));
    }

    private DeadlockGlobalCache(IDeadlockCollectBindingResolver resolver, ILockCachingStrategy cache) {
        this.resolver = resolver;
        this.cache = cache;
        this.fileCollector = new DeadlockCollector(resolver);

    }



    @Override
    public void waitForProcessing() {
    }

    @Override
    public void newLockMonitorEnter(ILockThreadEntry lockThreadEntry, ILockThreadEntry[] heldLocks) {
        LockCacheEntry lockEntry = (LockCacheEntry) cache.getLockCacheEntryOrCreate(lockThreadEntry);
        lockEntry.updateLockCacheEntry(resolver, cache, lockThreadEntry, heldLocks);

    }

    @Override
    public void newLockCreated(ILockThreadEntry lockThreadEntry, ILockThreadEntry[] heldLocks) {
        LockCacheEntry lockEntry = (LockCacheEntry) cache.getLockCacheEntryOrCreate(lockThreadEntry);
        lockEntry.newLockCreatedCacheEntry(resolver, cache, lockThreadEntry, heldLocks);
    }

    @Override
    public List<ILockCacheEntry> getLockEntries() {
        return cache.getLockEntries();
    }

    @Override
    public List<ILockCacheEntry> getLockEntriesExpungeStallEntries() {
        return cache.getLockEntriesExpungeStallEntries();
    }

    static final class LockStackEntry implements ILockStackEntry, Serializable {


        private static final long serialVersionUID = -4085775546411117928L;

        private final int lockerLocationId;
        private final int lockerThreadId;
        private final int[] dependentLocks;
        private final List<StackTraceElement> stackTrace;

        private transient int hash;

        LockStackEntry(int lockerLocationId, int lockerThreadId, TIntList dependentLocksLst) {
            this.lockerLocationId = lockerLocationId;
            this.lockerThreadId = lockerThreadId;
            this.dependentLocks = dependentLocksLst.toArray();
            this.stackTrace = createStack();
        }

        private List<StackTraceElement> createStack() {
            StackTraceElement[] stackTraceLoc = new Throwable().getStackTrace();
            List<StackTraceElement> lst = new ArrayList<>();
            for (int i = 0; i < stackTraceLoc.length; i++) {
                StackTraceElement e = stackTraceLoc[i];
                if (!e.getClassName().startsWith(DeadlockTracerClassBinding.TRACER_PKG)) {
                    lst.add(e);
                }
            }
            return Collections.unmodifiableList(lst);
        }

        @Override
        public int getLockerLocationId() {
            return lockerLocationId;
        }

        @Override
        public int getLockerThreadId() {
            return lockerThreadId;
        }

        @Override
        public List<StackTraceElement> getStackTrace() {
            return stackTrace;
        }

        @Override
        public int[] getDependentLocks() {
            return dependentLocks.clone();
        }

        @Override
        public int hashCode() {
            if (hash == 0) {
                this.hash = calcHashcode();
            }
            return this.hash;
        }

        private int calcHashcode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + Arrays.hashCode(dependentLocks);
            result = prime * result + lockerLocationId;
            result = prime * result + lockerThreadId;
            result = prime * result + ((stackTrace == null) ? 0 : stackTrace.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            LockStackEntry other = (LockStackEntry) obj;

            if (!Arrays.equals(dependentLocks, other.dependentLocks))
                return false;
            if (lockerLocationId != other.lockerLocationId)
                return false;
            if (lockerThreadId != other.lockerThreadId)
                return false;
            if (stackTrace == null) {
                if (other.stackTrace != null)
                    return false;
            } else if (!stackTrace.equals(other.stackTrace))
                return false;
            return true;
        }
    }

    @ThreadSafe
    static final class LockCacheEntry implements ILockCacheEntry, ISerializationSnapshotCreator {
        private static final AtomicInteger globalIdCounter = new AtomicInteger(0);

        private final String lockClass;

        private final int id;

        @GuardedBy("this")
        private long lockedCount = 0;

        @GuardedBy("this")
        private final TIntSet lockerLocationIds = new TIntHashSet();

        @GuardedBy("this")
        private final TIntSet lockerThreadIds = new TIntHashSet();

        @GuardedBy("this")
        private final TIntSet dependentLocks = new TIntHashSet();

        @GuardedBy("this")
        private Set<LockStackEntry> stackEntries = null;

        @GuardedBy("this")
        private boolean changedSinceLastSnapshot;

        LockCacheEntry(ILockThreadEntry entry) {
            Objects.requireNonNull(entry);
            Object lock = entry.getLock();
            Objects.requireNonNull(lock);
            if (lock instanceof Class) {
                Class<?> clazz = (Class<?>) lock;
                lockClass = "(ClassLock) " + clazz.getName();
            } else {
                lockClass = lock.getClass().getName();
            }
            id = globalIdCounter.incrementAndGet();
            if (id <= 0) {
                throw new IllegalStateException("LockCacheEntry global id count overflow");
            }
            changedSinceLastSnapshot = true;

        }

        @Override
        public ISerializableData createSerializationSnapshot(int revision) {
            synchronized (this) {
                if (changedSinceLastSnapshot) {
                    changedSinceLastSnapshot = false;
                    LockCacheEntrySer ser = new LockCacheEntrySer(this, revision);
                    if (stackEntries != null) {
                        stackEntries.clear();
                    }
                    return ser;

                }
                return null;
            }
        }

        void newLockCreatedCacheEntry(IDeadlockCollectBindingResolver resolver, ILockCachingStrategy cache, ILockThreadEntry lockThreadEntry,
                                      ILockThreadEntry[] heldLocks) {
            synchronized (this) {
                changedSinceLastSnapshot = true;
                int locationId = lockThreadEntry.getLockerLocationId();
                int threadId = lockThreadEntry.getLockerThreadId();
                boolean newLocationAdded = addLockerLocationId(locationId);
                if (newLocationAdded) {
                    checkNewLocationForStacktracing(resolver, locationId);
                }
                addLockerThreadId(threadId);
            }
        }

        void updateLockCacheEntry(IDeadlockCollectBindingResolver resolver, ILockCachingStrategy cache, ILockThreadEntry lockThreadEntry,
                                  ILockThreadEntry[] heldLocks) {
            synchronized (this) {
                changedSinceLastSnapshot = true;
                int locationId = lockThreadEntry.getLockerLocationId();
                int threadId = lockThreadEntry.getLockerThreadId();
                boolean newLocationAdded = addLockerLocationId(locationId);
                if (newLocationAdded) {
                    checkNewLocationForStacktracing(resolver, locationId);
                }
                addLockerThreadId(threadId);
                incrementLockCount();
                TIntList dependentLocksLst = addDependentLocks(cache, lockThreadEntry, heldLocks);
                addStackEntry(locationId, threadId, dependentLocksLst);
            }
        }

        private void checkNewLocationForStacktracing(IDeadlockCollectBindingResolver resolver, int locationId) {
            if (resolver.getLocationCache().isStacktracingEnabledForLocation(locationId)) {
                if (stackEntries == null) {
                    stackEntries = new LinkedHashSet<>();
                }
            }
        }

        private void addStackEntry(int locationId, int threadId, TIntList dependentLocksLst) {
            if (isStackCapturingEnabled()) {
                LockStackEntry stack = new LockStackEntry(locationId, threadId, dependentLocksLst);
                stackEntries.add(stack);
            }
        }

        private boolean isStackCapturingEnabled() {
            return stackEntries != null;
        }

        private void addLockerThreadId(int lockerThreadId) {
            if (lockerThreadId != LockerThreadCache.INVALID_THREAD_ID) {
                lockerThreadIds.add(lockerThreadId);
            }
        }

        private TIntList addDependentLocks(ILockCachingStrategy cache, ILockThreadEntry lockThreadEntry, ILockThreadEntry[] heldLocks) {

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
                ILockCacheEntry lockEntry = cache.getLockCacheEntryUnsafe(lockThObj);
                if (lockEntry == null) {
                    ILockCacheEntry lockEntry2 = cache.getLockCacheEntryUnsafe(lockThObj);
                    throw new IllegalStateException("There must be a ILockCacheEntry for the lock " + lockThObj + " second try " + lockEntry2);
                }
                addDependentLocks(lockEntry);
                if (isStackCapturingEnabled()) {
                    dependentLocks.add(lockEntry.getId());
                }
            }
            return dependentLocks;
        }

        private boolean addLockerLocationId(int lockerLocationId) {
            if (lockerLocationId != LockerLocationCache.INVALID_ID) {
                return lockerLocationIds.add(lockerLocationId);
            }
            return false;
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
            synchronized (dependentLocks) {
                return dependentLocks.contains(lockId);
            }
        }

        @Override
        public int hashCode() {
            return id;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            LockCacheEntry other = (LockCacheEntry) obj;
            if (id != other.id)
                return false;
            return true;
        }

        @Override
        public int compareTo(ILockCacheEntry o) {
            int id1 = this.getId();
            int id2 = o.getId();
            if (id1 == id2) {
                return 0;
            }
            if (id1 < id2) {
                return -1;
            }
            return 1;
        }

        @Override
        public int getId() {
            return id;
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
        public int[] getLockerLocationIds() {
            synchronized (lockerLocationIds) {
                return lockerLocationIds.toArray();
            }
        }

        @Override
        public int[] getLockerThreadIds() {
            synchronized (lockerThreadIds) {
                return lockerThreadIds.toArray();
            }
        }

        @Override
        public int[] getDependentLocks() {
            synchronized (dependentLocks) {
                return dependentLocks.toArray();
            }
        }

        @Override
        public List<ILockStackEntry> getStackEntries() {
            synchronized (dependentLocks) {
                if (!isStackCapturingEnabled()) {
                    return Collections.emptyList();
                }
                return new ArrayList<>(stackEntries);
            }
        }

        @Override
        public String toString() {
            return "LockCacheEntry [id=" + id + ", lockClass=" + lockClass + ", lockedCount=" + lockedCount + "]";
        }

    }

}