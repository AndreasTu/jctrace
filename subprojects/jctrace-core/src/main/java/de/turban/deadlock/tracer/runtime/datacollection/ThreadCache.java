package de.turban.deadlock.tracer.runtime.datacollection;

import de.turban.deadlock.tracer.runtime.IThreadCache;
import de.turban.deadlock.tracer.runtime.serdata.ISerializableData;
import de.turban.deadlock.tracer.runtime.serdata.ISerializationSnapshotCreator;
import de.turban.deadlock.tracer.runtime.serdata.LockerThreadCacheSerSnapshot;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import java.util.HashMap;

@ThreadSafe
public final class ThreadCache implements IThreadCache, ISerializationSnapshotCreator {

    @GuardedBy("threadMap")
    private int nextThreadId = INVALID_THREAD_ID;

    @GuardedBy("threadMap")
    private final TIntObjectMap<String> threadMap = new TIntObjectHashMap<>();

    @GuardedBy("threadMap")
    private int lastSerializedId = INVALID_THREAD_ID;

    ThreadCache() {

    }

    @Override
    public String getThreadDescriptionById(int id) {
        String threadDesc = threadMap.get(id);
        if (threadDesc == null) {
            throw new IllegalArgumentException("There is no ThreadDescription for ID: " + id);
        }
        return threadDesc;
    }

    public int newThreadIdOfCurrentThread() {
        Thread currentThread = Thread.currentThread();
        String threadDesc = buildThreadDescription(currentThread);
        synchronized (threadMap) {
            nextThreadId++;
            int id = nextThreadId;
            ensureValidId(id);
            threadMap.put(id, threadDesc);
            return id;
        }
    }

    @Override
    public ISerializableData createSerializationSnapshot(int revision) {
        HashMap<Integer, String> serMap = new HashMap<>();
        synchronized (threadMap) {
            int[] biggestId = new int[1];
            threadMap.forEachEntry((int id, String th) -> {
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
        return new LockerThreadCacheSerSnapshot(revision, serMap);
    }

    private String buildThreadDescription(Thread currentThread) {
        return currentThread.getName() + " (JavaId: " + currentThread.getId() + ")";
    }

    private void ensureValidId(int id) {
        if (id == INVALID_THREAD_ID) {
            throw new IllegalArgumentException();
        }
    }

}
