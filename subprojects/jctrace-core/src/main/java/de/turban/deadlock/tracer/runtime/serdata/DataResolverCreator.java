package de.turban.deadlock.tracer.runtime.serdata;

import de.turban.deadlock.tracer.runtime.ICacheEntry;
import de.turban.deadlock.tracer.runtime.IDeadlockDataResolver;
import de.turban.deadlock.tracer.runtime.IFieldCacheEntry;
import de.turban.deadlock.tracer.runtime.IFieldDescriptor;
import de.turban.deadlock.tracer.runtime.ILocationCache;
import de.turban.deadlock.tracer.runtime.ILockCacheEntry;
import de.turban.deadlock.tracer.runtime.IReportFieldCache;
import de.turban.deadlock.tracer.runtime.IReportFieldDescriptorCache;
import de.turban.deadlock.tracer.runtime.IReportLockCache;
import de.turban.deadlock.tracer.runtime.IThreadCache;
import de.turban.deadlock.tracer.runtime.JctraceUtil;
import de.turban.deadlock.tracer.runtime.serdata.IHasRevision.RevisionComparator;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiFunction;

import static java.util.stream.Collectors.toList;

public class DataResolverCreator {

    public IDeadlockDataResolver resolveData(List<ISerializableData> data) {

        List<LockCacheEntrySer> locksSer = new ArrayList<>();
        List<FieldCacheEntrySer> fieldsSer = new ArrayList<>();
        List<LockerThreadCacheSerSnapshot> threadSer = new ArrayList<>();
        List<LockerLocationCacheSerSnapshot> locationSer = new ArrayList<>();
        List<FieldDescriptorCacheSerSnapshot> fieldDescSer = new ArrayList<>();

        fillListsByType(data, locksSer, fieldsSer, threadSer, locationSer, fieldDescSer);

        IReportLockCache lockCache = buildLockCache(locksSer);
        IReportFieldCache fieldCache = buildFieldCache(fieldsSer);
        IThreadCache threadCache = buildThreadCache(threadSer);
        ILocationCache locationCache = buildLocationCache(locationSer);
        IReportFieldDescriptorCache fieldDescriptorCache = buildFieldDescriptorCache(fieldDescSer);
        return new DeadlockDataResolver(lockCache, fieldCache, threadCache, locationCache, fieldDescriptorCache);
    }


    private void fillListsByType(List<ISerializableData> data,
                                 List<LockCacheEntrySer> locksSer,
                                 List<FieldCacheEntrySer> fieldsSer,
                                 List<LockerThreadCacheSerSnapshot> threadSer,
                                 List<LockerLocationCacheSerSnapshot> locationSer,
                                 List<FieldDescriptorCacheSerSnapshot> fieldDescSer) {
        for (ISerializableData d : data) {
            if (d instanceof LockCacheEntrySer) {
                locksSer.add((LockCacheEntrySer) d);
            } else if (d instanceof FieldCacheEntrySer) {
                fieldsSer.add((FieldCacheEntrySer) d);
            } else if (d instanceof LockerThreadCacheSerSnapshot) {
                threadSer.add((LockerThreadCacheSerSnapshot) d);
            } else if (d instanceof LockerLocationCacheSerSnapshot) {
                locationSer.add((LockerLocationCacheSerSnapshot) d);
            } else if (d instanceof FieldDescriptorCacheSerSnapshot) {
                fieldDescSer.add((FieldDescriptorCacheSerSnapshot) d);
            } else {
                throw new IllegalArgumentException("Unknown type found " + d + " Class:" + d.getClass());
            }
        }
    }

    private ILocationCache buildLocationCache(List<LockerLocationCacheSerSnapshot> locationSer) {

        TIntObjectHashMap<StackTraceElement> map = new TIntObjectHashMap<>();
        locationSer.sort(RevisionComparator.INSTANCE);

        for (LockerLocationCacheSerSnapshot l : locationSer) {
            for (Entry<Integer, StackTraceElement> entry : l.getMap().entrySet()) {
                map.put(entry.getKey(), entry.getValue());
            }
        }

        return new ILocationCache() {

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

    private IReportFieldDescriptorCache buildFieldDescriptorCache(List<FieldDescriptorCacheSerSnapshot> fieldDescSer) {
        TIntObjectHashMap<IFieldDescriptor> map = new TIntObjectHashMap<>();
        fieldDescSer.sort(RevisionComparator.INSTANCE);

        for (FieldDescriptorCacheSerSnapshot l : fieldDescSer) {
            for (Entry<Integer, IFieldDescriptor> entry : l.getMap().entrySet()) {
                map.put(entry.getKey(), entry.getValue());
            }
        }

        List<IFieldDescriptor> unmodifiableList = Collections.unmodifiableList(
            map.valueCollection()
                .stream()
                .sorted()
                .collect(toList())
        );

        return new IReportFieldDescriptorCache() {

            @Override
            public IFieldDescriptor getFieldDescriptorById(int id) {
                IFieldDescriptor desc = map.get(id);
                if (desc == null) {
                    throw new IllegalArgumentException("No Field Descriptor found for id " + id);
                }
                return desc;
            }

            @Override
            public List<IFieldDescriptor> getFieldDescriptors() {
                return unmodifiableList;
            }
        };
    }


    private IThreadCache buildThreadCache(List<LockerThreadCacheSerSnapshot> threadSer) {
        TIntObjectHashMap<String> map = new TIntObjectHashMap<>();
        threadSer.sort(RevisionComparator.INSTANCE);

        for (LockerThreadCacheSerSnapshot t : threadSer) {
            for (Entry<Integer, String> entry : t.getMap().entrySet()) {
                map.put(entry.getKey(), entry.getValue());
            }
        }

        //noinspection Convert2Lambda
        return new IThreadCache() {

            @Override
            public String getThreadDescriptionById(int id) {
                String thread = map.get(id);
                if (thread == null) {
                    throw new IllegalArgumentException("There is no ThreadDescription for ID: " + id);
                }
                return thread;
            }

        };
    }

    private IReportLockCache buildLockCache(List<LockCacheEntrySer> locksSer) {

        return buildCache(locksSer, (list, map) ->
            new IReportLockCache() {

                @Override
                public List<ILockCacheEntry> getLockEntries() {
                    return JctraceUtil.uncheckedCast(list);
                }

                @Override
                public ILockCacheEntry getLockById(int id) {
                    return map.get(id);
                }
            });
    }

    private IReportFieldCache buildFieldCache(List<FieldCacheEntrySer> fieldSer) {

        return buildCache(fieldSer, (list, map) ->
            new IReportFieldCache() {

                @Override
                public List<IFieldCacheEntry> getFieldEntries() {
                    return JctraceUtil.uncheckedCast(list);
                }

                @Override
                public IFieldCacheEntry getFieldById(int id) {
                    return map.get(id);
                }
            });
    }

    private <T extends ICacheEntry & IHasRevision & Comparable & IHasStacksamples, R> R buildCache(List<T> ser, BiFunction<List<T>, TIntObjectHashMap<T>, R> cacheCreator) {
        Map<T, T> map = new HashMap<>();
        for (T e : ser) {
            T existing = map.get(e);
            if (existing == null) {
                map.put(e, e);
            } else {
                if (e.getRevision() > existing.getRevision()) {
                    map.remove(existing);
                    e.addStacks(existing.getStackSamples());
                    map.put(e, e);
                }
            }
        }

        List<T> lst = new ArrayList<>(map.values());
        Collections.sort(JctraceUtil.uncheckedCast(lst));
        List<T> unmodifiableList = Collections.unmodifiableList(lst);
        TIntObjectHashMap<T> lockMapLoc = new TIntObjectHashMap<>();
        unmodifiableList.forEach(e -> lockMapLoc.put(e.getId(), e));

        return cacheCreator.apply(unmodifiableList, lockMapLoc);
    }

    private static class DeadlockDataResolver implements IDeadlockDataResolver {

        private final IReportLockCache lockCache;
        private final IReportFieldCache fieldCache;
        private final IThreadCache threadCache;
        private final ILocationCache locationCache;
        private final IReportFieldDescriptorCache fieldDescriptorCache;

        DeadlockDataResolver(IReportLockCache lockCache,
                             IReportFieldCache fieldCache,
                             IThreadCache threadCache,
                             ILocationCache locationCache,
                             IReportFieldDescriptorCache fieldDescriptorCache) {
            this.lockCache = lockCache;
            this.fieldCache = fieldCache;
            this.threadCache = threadCache;
            this.locationCache = locationCache;
            this.fieldDescriptorCache = fieldDescriptorCache;

        }

        @Override
        public IReportLockCache getLockCache() {
            return lockCache;
        }


        @Override
        public IReportFieldCache getFieldCache() {
            return fieldCache;
        }


        @Override
        public ILocationCache getLocationCache() {
            return locationCache;
        }

        @Override
        public IThreadCache getThreadCache() {
            return threadCache;
        }

        @Override
        public IReportFieldDescriptorCache getFieldDescriptorCache() {
            return fieldDescriptorCache;
        }
    }
}
