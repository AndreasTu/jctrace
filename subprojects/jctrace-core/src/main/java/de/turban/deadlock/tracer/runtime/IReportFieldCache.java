package de.turban.deadlock.tracer.runtime;

import java.util.List;


public interface IReportFieldCache extends IFieldCache {

    /**
     * Returns a sorted immutable list of all {@link IFieldCacheEntry} in the loaded database.
     *
     * @return the list
     */
    List<IFieldCacheEntry> getFieldEntries();

    IFieldCacheEntry getFieldById(int id);
}
