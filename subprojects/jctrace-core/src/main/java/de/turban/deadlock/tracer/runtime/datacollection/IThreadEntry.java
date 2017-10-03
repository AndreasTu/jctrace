package de.turban.deadlock.tracer.runtime.datacollection;

import de.turban.deadlock.tracer.runtime.ILocationCache;
import de.turban.deadlock.tracer.runtime.IThreadCache;

public interface IThreadEntry {

    LockWeakRef getLockWeakReference();

    /**
     * @return returns the object for which this thread event was recorded.
     */
    Object getObject();

    /**
     * @return The location id. Could also be {@link ILocationCache#INVALID_LOCATION_ID}.
     * @see LocationCache
     */
    int getLocationId();

    /**
     * @return The Thread id. Could also be {@link IThreadCache#INVALID_THREAD_ID}.
     * @see ThreadCache
     */
    int getThreadId();
}
