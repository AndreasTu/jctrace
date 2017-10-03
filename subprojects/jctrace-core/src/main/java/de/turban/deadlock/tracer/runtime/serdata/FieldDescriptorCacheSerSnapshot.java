package de.turban.deadlock.tracer.runtime.serdata;

import de.turban.deadlock.tracer.runtime.IFieldDescriptor;

import java.util.HashMap;
import java.util.Map;

public final class FieldDescriptorCacheSerSnapshot implements ISerializableData, IHasRevision {
    private static final long serialVersionUID = -1L;

    private final Map<Integer, IFieldDescriptor> serMap;

    private final int revision;

    public FieldDescriptorCacheSerSnapshot(int revision, HashMap<Integer, IFieldDescriptor> serMap) {
        this.revision = revision;
        this.serMap = serMap;

    }

    @Override
    public int getRevision() {
        return revision;
    }

    public Map<Integer, IFieldDescriptor> getMap() {
        return serMap;
    }

}
