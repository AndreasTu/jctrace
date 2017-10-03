package de.turban.deadlock.tracer.runtime.datacollection;

import de.turban.deadlock.tracer.runtime.ICacheEntry;
import de.turban.deadlock.tracer.runtime.IStackSample;
import de.turban.deadlock.tracer.runtime.serdata.ISerializableData;
import de.turban.deadlock.tracer.runtime.serdata.ISerializationSnapshotCreator;
import gnu.trove.list.TIntList;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import javax.annotation.concurrent.GuardedBy;
import java.util.*;

abstract class AbstractCacheEntry implements ICacheEntry, ISerializationSnapshotCreator {

    private final int id;

    @GuardedBy("this")
    private final TIntSet lockerLocationIds = new TIntHashSet();

    @GuardedBy("this")
    private final TIntSet lockerThreadIds = new TIntHashSet();

    @GuardedBy("this")
    private boolean changedSinceLastSnapshot;


    @GuardedBy("this")
    private Set<StackSample> stackSamples = null;


    protected AbstractCacheEntry(int id){

        if (id <= 0) {
            throw new IllegalStateException(this.getClass().getSimpleName()+ " global id count overflow");
        }
        this.id= id;
        changedSinceLastSnapshot = true;
    }


    protected boolean isStackCapturingEnabled() {
        return stackSamples != null;
    }


    protected void checkNewLocationForStacktracing(IDeadlockCollectBindingResolver resolver, int locationId) {
        if (resolver.getLocationCache().isStacktracingEnabledForLocation(locationId)) {
            if (stackSamples == null) {
                stackSamples = new LinkedHashSet<>();
            }
        }
    }

    protected void addStackEntry(int locationId, int threadId, TIntList dependentLocksLst) {
        if (isStackCapturingEnabled()) {
            StackSample stack = new StackSample(locationId, threadId, dependentLocksLst);
            stackSamples.add(stack);
        }
    }

    @GuardedBy("this")
    protected void addThreadId(int lockerThreadId) {
        if (lockerThreadId != ThreadCache.INVALID_THREAD_ID) {
            lockerThreadIds.add(lockerThreadId);
        }
    }
    @GuardedBy("this")
    protected boolean addLocationId(int lockerLocationId) {
        if (lockerLocationId != LocationCache.INVALID_LOCATION_ID) {
            return lockerLocationIds.add(lockerLocationId);
        }
        return false;
    }


    @GuardedBy("this")
    protected final void setChangedSinceLastSnapshot(){
        changedSinceLastSnapshot = true;
    }


    @Override
    public final ISerializableData createSerializationSnapshot(int revision) {
        synchronized (this) {
            if (changedSinceLastSnapshot) {
                changedSinceLastSnapshot = false;
                ISerializableData data = createSerObjInternal(revision);
                if (stackSamples != null) {
                    stackSamples.clear();
                }
                return data;
            }
            return null;
        }
    }

    @Override
    public final int hashCode() {
        return id;
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AbstractCacheEntry other = (AbstractCacheEntry) obj;
        if (id != other.id)
            return false;
        return true;
    }


    @Override
    public final int getId() {
        return id;
    }

    @Override
    public final int[] getLocationIds() {
        synchronized (this) {
            return lockerLocationIds.toArray();
        }
    }

    @Override
    public final int[] getThreadIds() {
        synchronized (this) {
            return lockerThreadIds.toArray();
        }
    }

    @Override
    public List<IStackSample> getStackSamples() {
        synchronized (this) {
            if (!isStackCapturingEnabled()) {
                return Collections.emptyList();
            }
            return new ArrayList<>(stackSamples);
        }
    }

    protected abstract ISerializableData createSerObjInternal(int revision);
}
