package de.turban.deadlock.tracer.runtime;

import java.util.List;

public interface ICacheEntry {

    int INVALID_ID = 0;

    int getId();

    int[] getLocationIds();

    int[] getThreadIds();

    List<IStackSample> getStackSamples();
}
