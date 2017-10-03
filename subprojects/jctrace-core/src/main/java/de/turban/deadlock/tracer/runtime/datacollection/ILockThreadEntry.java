package de.turban.deadlock.tracer.runtime.datacollection;

public interface ILockThreadEntry extends IThreadEntry {

    /**
     * @return returns the lock object.
     */
    Object getLock();


}
