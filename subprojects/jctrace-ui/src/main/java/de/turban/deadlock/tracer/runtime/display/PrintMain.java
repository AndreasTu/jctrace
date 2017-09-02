package de.turban.deadlock.tracer.runtime.display;

import de.turban.deadlock.tracer.runtime.IDeadlockDataResolver;

import java.io.File;
import java.nio.file.Path;

public class PrintMain {

    public static void main(String[] args) {
        System.out.println("Deadlock Tracer Print main started...");

        long startTime = System.currentTimeMillis();

        Path file = new File(args[0]).toPath();
        IDeadlockDataResolver resolver = new DataVisualizationLoader().loadDatabase(file);

        Path outfile = new File(args[1]).toPath();

        startTime = System.currentTimeMillis();
        new DeadlockCacheXmlPrinter(resolver).printLocks(outfile);
        long stopTime = System.currentTimeMillis();
        System.out.println("Printing report took " + (stopTime - startTime) + "ms");
    }

}
