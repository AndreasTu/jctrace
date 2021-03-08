package de.turban.deadlock.tracer.runtime.datacollection;

import de.turban.deadlock.tracer.runtime.ICacheEntry;

import java.util.List;
import java.util.function.Function;

public interface ICachingStrategy<C extends ICacheEntry, T extends IThreadEntry> {

    C getCacheEntryOrCreate(T threadEntry);

    C getCacheEntryUnsafe(T threadEntry);

    List<C> getEntriesExpungeStallEntries();

    List<C> getEntries();


    @FunctionalInterface
    interface ICachingStrategyCreator<C extends ICacheEntry, T extends IThreadEntry> extends Function<Function<T, C>, ICachingStrategy<C, T>> {

    }
}
