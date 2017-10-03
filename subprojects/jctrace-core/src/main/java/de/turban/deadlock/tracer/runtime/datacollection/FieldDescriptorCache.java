package de.turban.deadlock.tracer.runtime.datacollection;

import de.turban.deadlock.tracer.runtime.IFieldDescriptor;
import de.turban.deadlock.tracer.runtime.IFieldDescriptorCache;
import de.turban.deadlock.tracer.runtime.serdata.FieldDescriptorCacheSerSnapshot;
import de.turban.deadlock.tracer.runtime.serdata.ISerializableData;
import de.turban.deadlock.tracer.runtime.serdata.ISerializationSnapshotCreator;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static de.turban.deadlock.tracer.runtime.JctraceUtil.ensureArgument;
import static de.turban.deadlock.tracer.runtime.JctraceUtil.ensureIsNull;

@ThreadSafe
public final class FieldDescriptorCache implements IFieldDescriptorCache, ISerializationSnapshotCreator {

    private static final int INVALID_FIELD_DESCRIPTOR_ID = IFieldDescriptor.INVALID_FIELD_DESCRIPTOR_ID;

    @GuardedBy("descriptorMap")
    private int nextFieldDescriptorId = INVALID_FIELD_DESCRIPTOR_ID;

    @GuardedBy("descriptorMap")
    private final TIntObjectMap<IFieldDescriptor> descriptorMap = new TIntObjectHashMap<>();

    @GuardedBy("descriptorMap")
    private final TObjectIntMap<IFieldDescriptor> descriptorToIdMap = new TObjectIntHashMap<>();

    @GuardedBy("descriptorMap")
    private int lastSerializedId = INVALID_FIELD_DESCRIPTOR_ID;

    FieldDescriptorCache() {

    }

    @Override
    public IFieldDescriptor getFieldDescriptorById(int id) {
        IFieldDescriptor desc;
        synchronized (descriptorMap) {
            desc = descriptorMap.get(id);
        }
        if (desc == null) {
            throw new IllegalArgumentException("There is no field descriptor for ID: " + id);
        }
        return desc;
    }

    public int newFieldDescriptor(String fieldClass, String fieldName, String desc, int asmAccessFlags, boolean fromField) {
        Objects.requireNonNull(fieldClass);
        Objects.requireNonNull(fieldName);
        Objects.requireNonNull(desc);
        synchronized (descriptorMap) {
            FieldDescriptor fieldDesc = new FieldDescriptor(INVALID_FIELD_DESCRIPTOR_ID, fieldClass, fieldName, desc);
            int id = descriptorToIdMap.get(fieldDesc);
            if (id != INVALID_FIELD_DESCRIPTOR_ID) {
                if(fromField){
                    ((FieldDescriptor)descriptorMap.get(id)).setAccessFlags(asmAccessFlags);
                }
                return id;
            }
            nextFieldDescriptorId++;
            id = nextFieldDescriptorId;
            ensureValidId(id);
            fieldDesc = new FieldDescriptor(id, fieldClass, fieldName, desc);
            if(fromField) {
                fieldDesc.setAccessFlags(asmAccessFlags);
            }
            ensureIsNull(descriptorMap.put(id, fieldDesc));
            ensureArgument(descriptorToIdMap.put(fieldDesc, id) == INVALID_FIELD_DESCRIPTOR_ID);

            return id;
        }
    }

    private void ensureValidId(int id) {
        if (id == IFieldDescriptor.INVALID_FIELD_DESCRIPTOR_ID) {
            throw new IllegalArgumentException();
        }
    }


    @Override
    public ISerializableData createSerializationSnapshot(int revision) {
        HashMap<Integer, IFieldDescriptor> serMap = new HashMap<>();
        synchronized (descriptorMap) {
            int[] biggestId = new int[1];
            descriptorMap.forEachEntry((int id, IFieldDescriptor f) -> {
                if (id > lastSerializedId) {
                    serMap.put(id, f);
                }
                if (biggestId[0] < id) {
                    biggestId[0] = id;
                }
                return true;
            });
            lastSerializedId = biggestId[0];
        }
        if (serMap.isEmpty()) {
            return null;
        }
        return new FieldDescriptorCacheSerSnapshot(revision, serMap);
    }

}
