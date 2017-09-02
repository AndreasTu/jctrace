package de.turban.deadlock.tracer.runtime;

import java.util.List;


public interface IReportLockCache extends ILockCache {

    /**
     * Returns a sorted immutable list of all {@link ILockCacheEntry} in the loaded database.
     *
     * @return the list
     */
    List<ILockCacheEntry> getLockEntries();

    ILockCacheEntry getLockById(int id);
}
