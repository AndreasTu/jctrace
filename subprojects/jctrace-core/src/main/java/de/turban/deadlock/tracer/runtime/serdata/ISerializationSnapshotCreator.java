package de.turban.deadlock.tracer.runtime.serdata;

import javax.annotation.Nullable;

public interface ISerializationSnapshotCreator {

    @Nullable
    ISerializableData createSerializationSnapshot(int revision);
}
