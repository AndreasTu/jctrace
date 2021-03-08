package de.turban.deadlock.tracer.runtime.display;

import de.turban.deadlock.tracer.runtime.IDeadlockDataResolver;
import de.turban.deadlock.tracer.runtime.serdata.DataDeserializer;
import de.turban.deadlock.tracer.runtime.serdata.DataResolverCreator;
import de.turban.deadlock.tracer.runtime.serdata.ISerializableData;

import java.nio.file.Path;
import java.util.List;

/**
 *
 */
public class DataVisualizationLoader {

    public IDeadlockDataResolver loadDatabase(Path file) {
        long startTime = System.currentTimeMillis();

        DataResolverCreator resolv = new DataResolverCreator();

        List<ISerializableData> readData = new DataDeserializer().readData(file);
        long stopTime = System.currentTimeMillis();
        System.out.println("Load of raw DB took " + (stopTime - startTime) + "ms");

        startTime = System.currentTimeMillis();
        IDeadlockDataResolver resolver = resolv.resolveData(readData);
        stopTime = System.currentTimeMillis();
        System.out.println("DB resolution took " + (stopTime - startTime) + "ms");

        return resolver;
    }
}
