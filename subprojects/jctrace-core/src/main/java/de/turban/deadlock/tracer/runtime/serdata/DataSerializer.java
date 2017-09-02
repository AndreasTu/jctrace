package de.turban.deadlock.tracer.runtime.serdata;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import de.turban.deadlock.tracer.runtime.ILockCacheEntry;
import de.turban.deadlock.tracer.runtime.datacollection.IDeadlockCollectBindingResolver;

public class DataSerializer {
    private static AtomicInteger currentRevision = new AtomicInteger(0);

    private static int incrementAndGetRevision() {
        return currentRevision.incrementAndGet();
    }

    private final Path file;

    DataSerializer() {
        file = null;
    }

    public DataSerializer(Path file) {
        this.file = file;
        try {
            if (!Files.isRegularFile(file)) {
                if (!Files.exists(file)) {
                    Files.createFile(file);
                } else {
                    throw new IllegalStateException("Could not create Data file: " + file);
                }
            } else {
                Files.newOutputStream(file, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE).close();
            }

        } catch (IOException e) {
            throw new IllegalStateException("Could not create Data file: " + file, e);
        }
    }

    public synchronized void serialize(IDeadlockCollectBindingResolver resolver) {
        try (BufferedOutputStream os = new BufferedOutputStream(Files.newOutputStream(file, StandardOpenOption.APPEND, StandardOpenOption.WRITE))) {

            serializeAll(os, resolver);

            System.out.println("DeadLock data written to " + file.toAbsolutePath());
        } catch (IOException e) {
            throw new IllegalStateException("Could not create Data file: " + file, e);
        }
    }

    synchronized void serializeAll(OutputStream os, IDeadlockCollectBindingResolver resolver) {
        int revision = incrementAndGetRevision();
        List<ILockCacheEntry> entries = resolver.getLockCache().getLockEntriesExpungeStallEntries();
        for (ILockCacheEntry e : entries) {
            ISerializationSnapshotCreator cacheEntry = (ISerializationSnapshotCreator) e;
            ISerializableData data = cacheEntry.createSerializationSnapshot(revision);
            if (data != null) {
                serializeElement(os, data);
            }
        }

        ISerializableData threads = resolver.getThreadCache().createSerializationSnapshot(revision);
        if (threads != null) {
            serializeElement(os, threads);
        }

        ISerializableData locations = resolver.getLocationCache().createSerializationSnapshot(revision);
        if (locations != null) {
            serializeElement(os, locations);
        }
    }

    synchronized void serializeFromCreator(OutputStream os, List<? extends ISerializationSnapshotCreator> entries) {
        int revision = incrementAndGetRevision();
        for (ISerializationSnapshotCreator creator : entries) {
            ISerializableData data = creator.createSerializationSnapshot(revision);
            serializeElement(os, data);
        }

    }

    synchronized void serializeData(OutputStream os, List<? extends ISerializableData> entries) {
        for (ISerializableData data : entries) {
            serializeElement(os, data);
        }

    }

    private void serializeElement(OutputStream os, ISerializableData data) {
        try {
            ByteArrayOutputStream bs = new ByteArrayOutputStream();
            ObjectOutputStream oOut = new ObjectOutputStream(bs);
            oOut.writeObject(data);
            byte[] byteArray = bs.toByteArray();
            byte[] len = ByteBuffer.allocate(4).putInt(byteArray.length).array();
            os.write(len);
            os.write(byteArray);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not create Data file: " + file, ex);
        }
    }

}
