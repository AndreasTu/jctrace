package de.turban.deadlock.tracer.runtime.datacollection;

public final class DeadlockCollectBindingResolver implements IDeadlockCollectBindingResolver {

    public static final IDeadlockCollectBindingResolver INSTANCE = new DeadlockCollectBindingResolver();

    private volatile DeadlockGlobalCache cacheInternal;

    private final LockerLocationCache locationCache;

    private final LockerThreadCache threadCache;

    private DeadlockCollectBindingResolver() {
        locationCache = new LockerLocationCache();
        threadCache = new LockerThreadCache();
    }

    private DeadlockGlobalCache getGlobalCachePrivate() {
        if (cacheInternal == null) {
            synchronized (this) {
                if (cacheInternal == null) {
                    cacheInternal = createGlobalCache();
                }
            }
        }
        return cacheInternal;
    }

    private DeadlockGlobalCache createGlobalCache() {
        return DeadlockGlobalCache.create(this, LockCachingStrategyConcurrentMapEqualStrategy::new);
    }

    @Override
    public IDeadlockGlobalCache getLockCache() {
        return getGlobalCachePrivate();
    }

    @Override
    public IDeadlockGlobalCacheSubmitter getCacheSubmitter() {
        return getGlobalCachePrivate();
    }

    @Override
    public LockerLocationCache getLocationCache() {
        return locationCache;
    }

    @Override
    public LockerThreadCache getThreadCache() {
        return threadCache;
    }

}
