package de.turban.deadlock.tracer;

import de.turban.deadlock.tracer.transformation.ClassRedefiner;
import de.turban.deadlock.tracer.transformation.DeadlockTracerTransformer;

import java.lang.instrument.Instrumentation;

public final class DeadlockTracerAgent {

    private DeadlockTracerAgent() {

    }

    public static void premain(String agentArgument, Instrumentation instrumentation) {
        DeadlockTracerTransformer transformer = new DeadlockTracerTransformer();
        ClassRedefiner.redefineClasses(instrumentation, transformer);
        instrumentation.addTransformer(transformer, false);
    }
}
