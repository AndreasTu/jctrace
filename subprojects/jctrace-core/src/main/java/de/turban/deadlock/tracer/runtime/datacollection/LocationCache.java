package de.turban.deadlock.tracer.runtime.datacollection;

import de.turban.deadlock.tracer.runtime.ILocationCache;
import de.turban.deadlock.tracer.runtime.serdata.ISerializableData;
import de.turban.deadlock.tracer.runtime.serdata.ISerializationSnapshotCreator;
import de.turban.deadlock.tracer.runtime.serdata.LockerLocationCacheSerSnapshot;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import java.util.*;

import static de.turban.deadlock.tracer.runtime.JctraceUtil.ensureArgument;
import static de.turban.deadlock.tracer.runtime.JctraceUtil.ensureIsNull;

@ThreadSafe
public final class LocationCache implements ILocationCache, ISerializationSnapshotCreator {

    @GuardedBy("locationMap")
    private int nextLocationId = INVALID_LOCATION_ID;

    @GuardedBy("locationMap")
    private final TIntObjectMap<StackTraceElement> locationMap = new TIntObjectHashMap<>();

    @GuardedBy("locationMap")
    private final TObjectIntMap<StackTraceElement> locationToIdMap = new TObjectIntHashMap<>();

    @GuardedBy("enabledStacktracing")
    private final HashSet<StackTraceElement> enabledStacktracing = new HashSet<>();

    @GuardedBy("enabledStacktracing")
    private final HashSet<String> enabledStacktracingbyClass = new HashSet<>();

    @GuardedBy("locationMap")
    private int lastSerializedId = INVALID_LOCATION_ID;

    LocationCache() {

        enabledStacktracing.add(new StackTraceElement("de.turban.deadlock.tests.tracer.TestSync", "testReadWrite", "TestSync.java", 59));

        enabledStacktracingbyClass.addAll(createStacktracingClassnames());
    }

    private static List<String> createStacktracingClassnames() {
        String list = System.getProperty("de.turban.DeadLockTracer.stacktracingClasses");
        if (list == null) {
            return Collections.emptyList();
        }
        List<String> blackList = new ArrayList<>();
        for (String pkg : list.trim().split(";")) {
            if(pkg != null) {
                String pkgTrimmed = pkg.trim();
                if (!pkgTrimmed.isEmpty()) {
                    blackList.add(pkgTrimmed);
                }
            }
        }
        return blackList;
    }

    @Override
    public StackTraceElement getLocationById(int id) {
        StackTraceElement stackTraceElement;
        synchronized (locationMap) {
            stackTraceElement = locationMap.get(id);
        }
        if (stackTraceElement == null) {
            throw new IllegalArgumentException("There is no location information for ID: " + id);
        }
        return stackTraceElement;
    }

    public int getIdByLocation(StackTraceElement element) {
        synchronized (locationMap) {
            return locationToIdMap.get(element);
        }
    }

     int getOrCreateIdByLocation(StackTraceElement element) {
        synchronized (locationMap) {
            int id = locationToIdMap.get(element);
            if (id == INVALID_LOCATION_ID) {
                id = newLocation(element);
            }
            return id;
        }
    }

    public int newLocation(StackTraceElement stackTraceElement) {
        Objects.requireNonNull(stackTraceElement);
        synchronized (locationMap) {
            int id = locationToIdMap.get(stackTraceElement);
            if( id != INVALID_LOCATION_ID){
                return id;
            }
            nextLocationId++;
            id = nextLocationId;
            ensureValidId(id);
            ensureIsNull(locationMap.put(id, stackTraceElement));
            ensureArgument(locationToIdMap.put(stackTraceElement, id) == INVALID_LOCATION_ID);
            return id;
        }
    }

    private void ensureValidId(int id) {
        if (id == INVALID_LOCATION_ID) {
            throw new IllegalArgumentException();
        }
    }

    public void updateLocation(int tracerLocationId, StackTraceElement newElement) {
        Objects.requireNonNull(newElement);
        ensureValidId(tracerLocationId);
        synchronized (locationMap) {
            StackTraceElement traceElement = locationMap.get(tracerLocationId);
            if (!traceElement.getClassName().equals(newElement.getClassName())) {
                throw new IllegalArgumentException();
            }
            if (!traceElement.getMethodName().equals(newElement.getMethodName())) {
                throw new IllegalArgumentException();
            }
            StackTraceElement old = locationMap.get(tracerLocationId);
            if (old != null) {
                locationToIdMap.remove(old);
            }
            locationMap.put(tracerLocationId, newElement);
            ensureArgument(locationToIdMap.put(newElement, tracerLocationId) == INVALID_LOCATION_ID);

        }
    }

    @Override
    public ISerializableData createSerializationSnapshot(int revision) {
        HashMap<Integer, StackTraceElement> serMap = new HashMap<>();
        synchronized (locationMap) {
            int[] biggestId = new int[1];
            locationMap.forEachEntry((int id, StackTraceElement th) -> {
                if (id > lastSerializedId) {
                    serMap.put(id, th);
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
        return new LockerLocationCacheSerSnapshot(revision, serMap);
    }

     boolean isStacktracingEnabledForLocation(int locationId) {
        StackTraceElement loc = getLocationById(locationId);
        synchronized (enabledStacktracing) {
            if (enabledStacktracing.contains(loc)) {
                return true;
            }
        }
        synchronized (enabledStacktracingbyClass) {
            return enabledStacktracingbyClass.contains(loc.getClassName());
        }
    }

}
