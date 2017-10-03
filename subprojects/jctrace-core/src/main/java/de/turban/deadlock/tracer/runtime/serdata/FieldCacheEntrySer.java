package de.turban.deadlock.tracer.runtime.serdata;

import de.turban.deadlock.tracer.runtime.IFieldCacheEntry;
import de.turban.deadlock.tracer.runtime.IStackSample;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Immutable
public final class FieldCacheEntrySer implements IFieldCacheEntry, ISerializableData, IHasStacksamples {
    private static final long serialVersionUID = 1L;

    private final int revision;

    private final int id;

    private final int fieldDescriptorId;

    private final long readCount;

    private final long writeCount;

    private final int[] lockerLocationIds;

    private final int[] lockerThreadIds;

    private final int ownerIdentityHash;

    private final List<IStackSample> stackSamples;

    public FieldCacheEntrySer(IFieldCacheEntry entry, int revision) {
        Objects.requireNonNull(entry);
        this.id = entry.getId();
        this.revision = revision;
        this.fieldDescriptorId = entry.getFieldDescriptorId();
        this.readCount = entry.getReadCount();
        this.writeCount = entry.getWriteCount();
        this.lockerLocationIds = entry.getLocationIds();
        this.lockerThreadIds = entry.getThreadIds();
        this.stackSamples = entry.getStackSamples();
        this.ownerIdentityHash = entry.getOwnerIdentityHash();
    }

    public int getOwnerIdentityHash() {
        return ownerIdentityHash;
    }

    @Override
    public int getFieldDescriptorId() {
        return fieldDescriptorId;
    }

    @Override
    public long getReadCount() {
        return readCount;
    }

    @Override
    public long getWriteCount() {
        return writeCount;
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
        if (!(obj instanceof IFieldCacheEntry)) {
            return false;
        }
        IFieldCacheEntry other = (IFieldCacheEntry) obj;
        return id == other.getId();
    }

    @Override
    public int compareTo(@Nonnull IFieldCacheEntry o) {
        int id1 = this.getId();
        int id2 = o.getId();
        return Integer.compare(id1, id2);
    }

    @Override
    public int getId() {
        return id;
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
    public List<IStackSample> getStackSamples() {
        return Collections.unmodifiableList(stackSamples);
    }

    @Override
    public String toString() {
        return "FieldCacheEntrySer [id=" + id + ", fieldDescId=" + getFieldDescriptorId() + "]";
    }

    public void addStacks(List<? extends IStackSample> stackEntriesFromOld) {
        for (IStackSample e : stackEntriesFromOld) {
            if (!stackSamples.contains(e)) {
                stackSamples.add(e);
            }
        }
    }

}
