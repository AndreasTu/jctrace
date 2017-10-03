package de.turban.deadlock.tracer.runtime.display;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import de.turban.deadlock.tracer.runtime.*;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.*;


public class DeadlockCalculator {

    private final IDeadlockDataResolver resolver;

    private ImmutableList<ILockCacheEntry> possibleDeadLocksSorted;

    private ImmutableList<ILockCacheEntry> allLocksSorted;

    private TIntObjectHashMap<ILockCacheEntry> lockMap;

    public DeadlockCalculator(IDeadlockDataResolver resolver) {
        this.resolver = Preconditions.checkNotNull(resolver);
    }


    public IDeadlockDataResolver getResolver() {
        return resolver;
    }

    public void calculateDeadlocks() {
        long startTime = System.currentTimeMillis();

        ILockCache globalCache = resolver.getLockCache();

        long stopTime = System.currentTimeMillis();
        System.out.println("Database Lock collecting and sort took " + (stopTime - startTime) + "ms");

        startTime = System.currentTimeMillis();
        Set<ILockCacheEntry> possibleDeadLocks = new LinkedHashSet<>();
        calculatePossibleDeadLocks(globalCache.getLockEntries(), possibleDeadLocks);
        stopTime = System.currentTimeMillis();
        System.out.println("Deadlock calculation took " + (stopTime - startTime) + "ms");
        List<ILockCacheEntry> possibleDeadLocksSortedLoc = new ArrayList<>(possibleDeadLocks);
        Collections.sort(possibleDeadLocksSortedLoc);
        possibleDeadLocksSorted = ImmutableList.copyOf(possibleDeadLocksSortedLoc);
    }

    public List<ILockCacheEntry> getPossibleDeadLocks() {
        return possibleDeadLocksSorted;
    }

    public List<ILockCacheEntry> getAllLocksSorted() {
        return resolver.getLockCache().getLockEntries();
    }

    public List<IFieldCacheEntry> getAllFieldsSorted() {
        return resolver.getFieldCache().getFieldEntries();
    }


    public List<IFieldDescriptor> getAllFieldDescriptorsSorted() {
        return resolver.getFieldDescriptorCache().getFieldDescriptors();
    }

    private void calculatePossibleDeadLocks(List<ILockCacheEntry> locks, Set<ILockCacheEntry> possibleDeadLocks) {

        for (ILockCacheEntry lock : locks) {

            for (int lockId : lock.getDependentLocks()) {

                ILockCacheEntry depLock = resolver.getLockCache().getLockById(lockId);
                if (depLock != null) {
                    if (depLock.hasDependentLock(lock)) {
                        possibleDeadLocks.add(lock);
                        possibleDeadLocks.add(depLock);
                    }
                }
            }
        }
    }
}
