package de.turban.deadlock.tracer.runtime;

import java.util.List;

public interface ILockStackEntry {

    int getLockerLocationId();

    int getLockerThreadId();

    List<StackTraceElement> getStackTrace();

    int[] getDependentLocks();
}
