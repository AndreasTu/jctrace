package de.turban.deadlock.tracer.runtime;

public interface ILocationCache {

    int INVALID_LOCATION_ID = 0;

    StackTraceElement getLocationById(int id);

    int getIdByLocation(StackTraceElement element);

}
