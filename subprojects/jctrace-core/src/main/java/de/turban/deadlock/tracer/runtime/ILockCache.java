package de.turban.deadlock.tracer.runtime;

import java.util.List;

public interface ILockCache {

    List<ILockCacheEntry> getLockEntries();

}
