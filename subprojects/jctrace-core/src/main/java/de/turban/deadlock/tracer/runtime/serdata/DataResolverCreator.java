package de.turban.deadlock.tracer.runtime.serdata;

import de.turban.deadlock.tracer.runtime.*;
import de.turban.deadlock.tracer.runtime.serdata.IHasRevision.RevisionComparator;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.*;
import java.util.Map.Entry;

public class DataResolverCreator {

    public IDeadlockDataResolver resolveData(List<ISerializableData> data) {

        List<LockCacheEntrySer> locksSer = new ArrayList<>();
        List<LockerThreadCacheSerSnapshot> threadSer = new ArrayList<>();
        List<LockerLocationCacheSerSnapshot> locationSer = new ArrayList<>();

        fillListsByType(data, locksSer, threadSer, locationSer);

        IReportLockCache lockCache = buildLockCache(locksSer);
        ILockerThreadCache threadCache = buildThreadCache(threadSer);
        ILockerLocationCache locationCache = buildLocationCache(locationSer);
        return new DeadlockDataResolver(lockCache, threadCache, locationCache);
    }

    private void fillListsByType(List<ISerializableData> data, List<LockCacheEntrySer> locksSer, List<LockerThreadCacheSerSnapshot> threadSer,
                                 List<LockerLocationCacheSerSnapshot> locationSer) {
        for (ISerializableData d : data) {
            if (d instanceof LockCacheEntrySer) {
                locksSer.add((LockCacheEntrySer) d);
            } else if (d instanceof LockerThreadCacheSerSnapshot) {
                threadSer.add((LockerThreadCacheSerSnapshot) d);
            } else if (d instanceof LockerLocationCacheSerSnapshot) {
                locationSer.add((LockerLocationCacheSerSnapshot) d);
            } else {
                throw new IllegalArgumentException("Unknown type found " + d + " Class:" + d.getClass());
            }
        }
    }

    private ILockerLocationCache buildLocationCache(List<LockerLocationCacheSerSnapshot> locationSer) {

        TIntObjectHashMap<StackTraceElement> map = new TIntObjectHashMap<>();
        Collections.sort(locationSer, RevisionComparator.INSTANCE);

        for (LockerLocationCacheSerSnapshot l : locationSer) {
            for (Entry<Integer, StackTraceElement> entry : l.getMap().entrySet()) {
                map.put(entry.getKey(), entry.getValue());
            }
        }

        return new ILockerLocationCache() {

            @Override
            public StackTraceElement getLocationById(int id) {
                StackTraceElement stackTraceElement = map.get(id);
                if (stackTraceElement == null) {
                    throw new IllegalArgumentException("No Location found for id " + id);
                }
                return stackTraceElement;
            }

            @Override
            public int getIdByLocation(StackTraceElement element) {
                throw new UnsupportedOperationException("This method is not supported in retrieval mode.");
            }

        };
    }

    private ILockerThreadCache buildThreadCache(List<LockerThreadCacheSerSnapshot> threadSer) {
        TIntObjectHashMap<String> map = new TIntObjectHashMap<>();
        Collections.sort(threadSer, RevisionComparator.INSTANCE);

        for (LockerThreadCacheSerSnapshot t : threadSer) {
            for (Entry<Integer, String> entry : t.getMap().entrySet()) {
                map.put(entry.getKey(), entry.getValue());
            }
        }

        return new ILockerThreadCache() {

            @Override
            public String getThreadDescriptionById(int id) {
                String thread = map.get(id);
                if (thread == null) {
                    throw new IllegalArgumentException();
                }
                return thread;
            }

        };
    }

    private IReportLockCache buildLockCache(List<LockCacheEntrySer> locksSer) {
        Map<LockCacheEntrySer, LockCacheEntrySer> map = new HashMap<>();
        for (LockCacheEntrySer e : locksSer) {
            LockCacheEntrySer existing = map.get(e);
            if (existing == null) {
                map.put(e, e);
            } else {
                if (e.getRevision() > existing.getRevision()) {
                    map.remove(existing);
                    e.addStacks(existing.getStackEntries());
                    map.put(e, e);
                }
            }
        }

        List<ILockCacheEntry> lst = new ArrayList<>(map.values());
        Collections.sort(lst);
        List<ILockCacheEntry> unmodList = Collections.unmodifiableList(lst);
        TIntObjectHashMap<ILockCacheEntry> lockMapLoc = new TIntObjectHashMap<>();
        unmodList.forEach(e -> lockMapLoc.put(e.getId(), e));

        return new IReportLockCache() {

            @Override
            public List<ILockCacheEntry> getLockEntries() {
                return unmodList;
            }

            @Override
            public ILockCacheEntry getLockById(int id) {
                return lockMapLoc.get(id);
            }
        };
    }

    private static class DeadlockDataResolver implements IDeadlockDataResolver {

        private final IReportLockCache lockCache;
        private final ILockerThreadCache threadCache;
        private final ILockerLocationCache locationCache;

        DeadlockDataResolver(IReportLockCache lockCache, ILockerThreadCache threadCache, ILockerLocationCache locationCache) {
            this.lockCache = lockCache;
            this.threadCache = threadCache;
            this.locationCache = locationCache;

        }

        @Override
        public IReportLockCache getLockCache() {
            return lockCache;
        }

        @Override
        public ILockerLocationCache getLocationCache() {
            return locationCache;
        }

        @Override
        public ILockerThreadCache getThreadCache() {
            return threadCache;
        }

    }
}
