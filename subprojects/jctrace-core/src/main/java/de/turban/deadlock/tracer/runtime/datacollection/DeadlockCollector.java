package de.turban.deadlock.tracer.runtime.datacollection;

import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import de.turban.deadlock.tracer.DeadlockTracerClassBinding;
import de.turban.deadlock.tracer.runtime.serdata.DataSerializer;

public class DeadlockCollector {

    private final DataSerializer ser = new DataSerializer(Paths.get("./Deadlock.db"));

    private ScheduledExecutorService exec;

    private final IDeadlockCollectBindingResolver resolver;

    public DeadlockCollector(IDeadlockCollectBindingResolver resolver) {
        this.resolver = resolver;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            this.collect();
        }));

        exec = Executors.newScheduledThreadPool(1, (r) -> {
            Thread thread = new Thread(r);
            thread.setName("DeadlockCollectorThread" + thread.getName());
            thread.setDaemon(true);
            return thread;
        });
        exec.scheduleAtFixedRate(() -> this.collect(), 1, 1, TimeUnit.SECONDS);
    }

    public void collect() {
        DeadlockTracerClassBinding.runWithTracingDisabled(() -> {
            ser.serialize(resolver);
            return null;
        });
    }

}
