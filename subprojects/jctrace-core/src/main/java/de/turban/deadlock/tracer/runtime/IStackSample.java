package de.turban.deadlock.tracer.runtime;

import java.util.List;

/**
 * {@link IStackSample} represents one measurement sample of a certain stack location, including the stack frames held locks and current thread.
 */
public interface IStackSample {

    int getLocationId();

    int getThreadId();

    List<StackTraceElement> getStackTrace();

    int[] getDependentLocks();

}
