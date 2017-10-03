package de.turban.deadlock.tracer.runtime.datacollection;

import de.turban.deadlock.tracer.DeadlockTracerClassBinding;
import de.turban.deadlock.tracer.runtime.IStackSample;
import gnu.trove.list.TIntList;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class StackSample implements IStackSample, Serializable {
    private static final long serialVersionUID = 1L;

    private final int locationId;
    private final int threadId;
    private final List<StackTraceElement> stackTrace;
    private int[] dependentLocks;
    private transient int hash;

    private StackSample(int locationId, int threadId, int[] dependentLocks) {
        this.locationId = locationId;
        this.threadId = threadId;
        this.dependentLocks = dependentLocks;
        this.stackTrace = createStack();
    }

    StackSample(int locationId, int threadId, TIntList dependentLocksList) {
        this(locationId, threadId, dependentLocksList.toArray());

    }

    private List<StackTraceElement> createStack() {
        StackTraceElement[] stackTraceLoc = new Throwable().getStackTrace();
        List<StackTraceElement> lst = new ArrayList<>();
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < stackTraceLoc.length; i++) {
            StackTraceElement e = stackTraceLoc[i];
            if (!e.getClassName().startsWith(DeadlockTracerClassBinding.TRACER_PKG)) {
                lst.add(e);
            }
        }
        return Collections.unmodifiableList(lst);
    }

    @Override
    public int getLocationId() {
        return locationId;
    }

    @Override
    public int getThreadId() {
        return threadId;
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
    public final int hashCode() {
        if (hash == 0) {
            this.hash = calcHashcode();
        }
        return this.hash;
    }

    private int calcHashcode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(dependentLocks);
        result = prime * result + getLocationId();
        result = prime * result + getThreadId();
        result = prime * result + ((getStackTrace() == null) ? 0 : getStackTrace().hashCode());
        return result;
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        StackSample other = (StackSample) obj;

        if (!Arrays.equals(dependentLocks, other.dependentLocks))
            return false;
        if (getLocationId() != other.getLocationId())
            return false;
        if (getThreadId() != other.getThreadId())
            return false;
        if (getStackTrace() == null) {
            if (other.getStackTrace() != null)
                return false;
        } else if (!getStackTrace().equals(other.getStackTrace()))
            return false;
        return true;
    }

}
