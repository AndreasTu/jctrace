package de.turban.deadlock.tracer.runtime;

import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public interface IThreadCache {

    int INVALID_THREAD_ID = 0;

    String getThreadDescriptionById(int id);

}
