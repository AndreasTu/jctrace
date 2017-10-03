package de.turban.deadlock.tracer.runtime.serdata;

import de.turban.deadlock.tracer.runtime.ILockCacheEntry;
import de.turban.deadlock.tracer.runtime.IStackSample;
import gnu.trove.set.hash.TIntHashSet;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Immutable
public final class LockCacheEntrySer implements ILockCacheEntry, ISerializableData, IHasStacksamples {
    private static final long serialVersionUID = 1L;

    private final String lockClass;

    private final int revision;

    private final int id;

    private final long lockedCount;

    private final int[] lockerLocationIds;

    private final int[] lockerThreadIds;

    private final int[] dependentLocks;

    private final List<IStackSample> stackSamples;

    private transient TIntHashSet dependentLockSet;

    public LockCacheEntrySer(ILockCacheEntry entry, int revision) {
        Objects.requireNonNull(entry);
        this.id = entry.getId();
        this.lockClass = entry.getLockClass();
        this.revision = revision;
        this.lockedCount = entry.getLockedCount();
        this.lockerLocationIds = entry.getLocationIds();
        this.lockerThreadIds = entry.getThreadIds();
        this.dependentLocks = entry.getDependentLocks();
        this.stackSamples = entry.getStackSamples();

    }

    LockCacheEntrySer(int id, String lockClass, int lockedCount, int revision) {
        this.id = id;
        this.lockClass = lockClass;
        this.revision = revision;
        this.lockedCount = lockedCount;
        this.lockerLocationIds = new int[0];
        this.lockerThreadIds = new int[0];
        this.dependentLocks = new int[0];
        this.stackSamples = new ArrayList<>();

    }

    @Override
    public boolean hasDependentLock(ILockCacheEntry lock) {
        initDependentLockSet();
        return dependentLockSet.contains(lock.getId());
    }

    @Override
    public boolean hasDependentLock(int lockId) {
        initDependentLockSet();
        return dependentLockSet.contains(lockId);
    }

    private void initDependentLockSet() {
        if (dependentLockSet == null) {
            dependentLockSet = new TIntHashSet();
            for (int i : dependentLocks) {
                dependentLockSet.add(i);
            }
        }
    }

    @Override
    public int getRevision() {
        return revision;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ILockCacheEntry)) {
            return false;
        }
        ILockCacheEntry other = (ILockCacheEntry) obj;
        return id == other.getId();
    }

    @Override
    public int compareTo(@Nonnull  ILockCacheEntry o) {
        int id1 = this.getId();
        int id2 = o.getId();
        return Integer.compare(id1, id2);
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
    public int[] getLocationIds() {
        return lockerLocationIds.clone();
    }

    @Override
    public int[] getThreadIds() {
        return lockerThreadIds.clone();
    }

    @Override
    public int[] getDependentLocks() {
        return dependentLocks.clone();
    }

    @Override
    public List<IStackSample> getStackSamples() {
        return Collections.unmodifiableList(stackSamples);
    }

    @Override
    public String toString() {
        return "LockCacheEntrySer [id=" + id + ", lockClass=" + lockClass + ", lockedCount=" + lockedCount + "]";
    }

    public void addStacks(List<? extends IStackSample> stackEntriesFromOld) {
        for (IStackSample stack : stackEntriesFromOld) {
            if (!stackSamples.contains(stack)) {
                stackSamples.add(stack);
            }
        }
    }
}
