package de.turban.deadlock.tracer.runtime.serdata;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;

public class DataDeserializer {

    byte[] buffer = new byte[65536];

    public DataDeserializer() {

    }

    public List<ISerializableData> readData(Path file) {
        if (!Files.isRegularFile(file)) {
            throw new IllegalStateException("Could not read Data file: " + file);
        }

        try (InputStream is = Files.newInputStream(file, StandardOpenOption.READ)) {
            return readData(is);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create Data file: " + file, e);
        }
    }

    public List<ISerializableData> readData(InputStream is) {
        List<ISerializableData> deserialize = deserialize(is);

        return deserialize;
    }

    private List<ISerializableData> deserialize(InputStream is) {

        List<ISerializableData> entries = new ArrayList<>();
        try {
            byte[] bytesFromStream = getBytesFromStream(is, buffer);
            int pos = 0;

            List<Future<ISerializableData>> jobs = new ArrayList<>();

            while (pos < bytesFromStream.length) {
                ByteBuffer put = ByteBuffer.allocate(4).put(bytesFromStream, pos, 4);
                put.flip();
                int len = put.getInt();
                pos += 4;
                int objPos = pos;

                jobs.add(ForkJoinPool.commonPool().submit(() -> {
                    try {
                        ObjectInputStream oIs = new ObjectInputStream(new ByteArrayInputStream(bytesFromStream, objPos, len));

                        return (ISerializableData) oIs.readObject();
                    } catch (IOException ex) {
                        throw new UncheckedIOException(ex);
                    }
                }));

                pos += len;

            }

            for (Future<ISerializableData> job : jobs) {
                entries.add(job.get());
            }

            return entries;
        } catch (IOException | InterruptedException | ExecutionException e) {
            throw new IllegalStateException("Could not read Database content.", e);
        }

    }

    public static byte[] getBytesFromStream(InputStream input, byte[] buffer) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
        }
        return output.toByteArray();
    }
}
