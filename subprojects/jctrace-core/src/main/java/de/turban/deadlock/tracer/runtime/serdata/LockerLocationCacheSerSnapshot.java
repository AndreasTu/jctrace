package de.turban.deadlock.tracer.runtime.serdata;

import java.util.HashMap;
import java.util.Map;

public final class LockerLocationCacheSerSnapshot implements ISerializableData, IHasRevision {
    private static final long serialVersionUID = -7512779107587139482L;

    private final Map<Integer, StackTraceElement> serMap;

    private final int revision;

    public LockerLocationCacheSerSnapshot(int revision, HashMap<Integer, StackTraceElement> serMap) {
        this.revision = revision;
        this.serMap = serMap;

    }

    @Override
    public int getRevision() {
        return revision;
    }

    public Map<Integer, StackTraceElement> getMap() {
        return serMap;
    }

}
