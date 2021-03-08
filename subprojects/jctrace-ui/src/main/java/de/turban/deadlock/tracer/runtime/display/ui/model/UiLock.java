package de.turban.deadlock.tracer.runtime.display.ui.model;

import com.google.common.collect.Lists;
import de.turban.deadlock.tracer.runtime.IDeadlockDataResolver;
import de.turban.deadlock.tracer.runtime.ILockCacheEntry;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class UiLock {

    private final String className;

    private final IDeadlockDataResolver resolver;

    private final ILockCacheEntry lockCache;

    UiLock(IDeadlockDataResolver resolver, ILockCacheEntry lockCache) {
        Objects.nonNull(resolver);
        Objects.nonNull(lockCache);
        this.resolver = resolver;
        this.lockCache = lockCache;
        className = getClassName(lockCache);
    }


    private String getClassName(ILockCacheEntry lockCache) {
        String lockClass = lockCache.getLockClass();
        if (lockClass.startsWith("java.util.concurrent")) {
            if (lockCache.getLocationIds().length > 0) {
                StackTraceElement firstLocation = resolver.getLocationCache().getLocationById(lockCache.getLocationIds()[0]);
                lockClass = firstLocation.getClassName();
            }
        }
        int idx = lockClass.lastIndexOf(".");
        if (idx > 0) {
            return lockClass.substring(idx + 1, lockClass.length());
        } else {
            return lockClass;
        }
    }


    public List<UiLock> getDependentLocks() {
        List<UiLock> locks = Lists.newArrayList();
        for (int id : getLockCache().getDependentLocks()) {
            ILockCacheEntry l = resolver.getLockCache().getLockById(id);
            locks.add(new UiLock(resolver, l));
        }
        return locks;
    }

    public List<UiLock> getPossibleDeadLockWithLocks() {
        List<UiLock> locks = Lists.newArrayList();
        for (int id : getLockCache().getDependentLocks()) {
            ILockCacheEntry l = resolver.getLockCache().getLockById(id);
            if (l.hasDependentLock(getLockCache().getId())) {
                locks.add(new UiLock(resolver, l));
            }
        }
        return locks;
    }

    public ObservableList<String> getLocations() {
        List<String> list = Arrays
            .stream(lockCache.getLocationIds())
            .mapToObj(id -> resolver.getLocationCache().getLocationById(id).toString())
            .collect(Collectors.toList());
        return FXCollections.observableList(list);
    }

    public ObservableList<String> getThreads() {
        List<String> list = Arrays
            .stream(lockCache.getThreadIds())
            .mapToObj(id -> resolver.getThreadCache().getThreadDescriptionById(id))
            .collect(Collectors.toList());

        return FXCollections.observableList(list);
    }


    public ILockCacheEntry getLockCache() {
        return lockCache;
    }

    @Override
    public String toString() {
        return className + " (id: " + lockCache.getId() + ")";
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UiLock uiLock = (UiLock) o;

        if (!resolver.equals(uiLock.resolver)) {
            return false;
        }
        return lockCache.equals(uiLock.lockCache);
    }

    @Override
    public int hashCode() {
        int result = resolver.hashCode();
        result = 31 * result + lockCache.hashCode();
        return result;
    }

    public IDeadlockDataResolver getResolver() {
        return resolver;
    }
}
