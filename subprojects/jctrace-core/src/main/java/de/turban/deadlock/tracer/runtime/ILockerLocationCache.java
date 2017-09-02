package de.turban.deadlock.tracer.runtime;

public interface ILockerLocationCache {

    StackTraceElement getLocationById(int id);

    int getIdByLocation(StackTraceElement element);

}
