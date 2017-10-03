package de.turban.deadlock.tracer.runtime.datacollection;

public final class DeadlockCollectBindingResolver implements IDeadlockCollectBindingResolver {

    public static final IDeadlockCollectBindingResolver INSTANCE = new DeadlockCollectBindingResolver();

    private volatile DeadlockGlobalCache cacheInternal;

    private final LocationCache locationCache;

    private final ThreadCache threadCache;

    private final FieldDescriptorCache fieldDescriptorCache;

    private DeadlockCollectBindingResolver() {
        locationCache = new LocationCache();
        fieldDescriptorCache = new FieldDescriptorCache();
        threadCache = new ThreadCache();
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
        return DeadlockGlobalCache.create(this, CachingStrategyConcurrentMapEqualStrategy::new);
    }

    @Override
    public IDeadlockGlobalCache getLockCache() {
        return getGlobalCachePrivate();
    }

    @Override
    public IDeadlockGlobalCache getFieldCache() {
        return getGlobalCachePrivate();
    }

    @Override
    public IDeadlockGlobalCacheSubmitter getCacheSubmitter() {
        return getGlobalCachePrivate();
    }

    @Override
    public LocationCache getLocationCache() {
        return locationCache;
    }

    @Override
    public ThreadCache getThreadCache() {
        return threadCache;
    }

    @Override
    public FieldDescriptorCache getFieldDescriptorCache() {
        return fieldDescriptorCache;
    }

}
