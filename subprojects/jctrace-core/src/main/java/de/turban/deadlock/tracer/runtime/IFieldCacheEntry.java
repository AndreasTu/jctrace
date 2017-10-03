package de.turban.deadlock.tracer.runtime;

public interface IFieldCacheEntry extends Comparable<IFieldCacheEntry>, ICacheEntry {

    int getOwnerIdentityHash();

    int getFieldDescriptorId();

    long getReadCount();

    long getWriteCount();

}
