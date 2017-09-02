package de.turban.deadlock.tracer;

import java.lang.instrument.Instrumentation;

import de.turban.deadlock.tracer.transformation.ClassRedefiner;
import de.turban.deadlock.tracer.transformation.DeadlockTracerTransformer;

public class DeadlockTracerAgent {

    public static void premain(String agentArgument, Instrumentation instrumentation) {
        DeadlockTracerTransformer transformer = new DeadlockTracerTransformer();
        ClassRedefiner.redefineClasses(instrumentation, transformer);
        instrumentation.addTransformer(transformer, false);
    }

}
