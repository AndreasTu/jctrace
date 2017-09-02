package de.turban.deadlock.tracer.runtime.serdata;

import java.util.HashMap;
import java.util.Map;

public final class LockerThreadCacheSerSnapshot implements ISerializableData, IHasRevision {

    private static final long serialVersionUID = -4266949445540311751L;

    private final Map<Integer, String> serMap;

    private final int revision;

    public LockerThreadCacheSerSnapshot(int revision, HashMap<Integer, String> serMap) {
        this.revision = revision;
        this.serMap = serMap;

    }

    @Override
    public int getRevision() {
        return revision;
    }

    public Map<Integer, String> getMap() {
        return serMap;
    }
}
