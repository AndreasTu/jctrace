package de.turban.deadlock.tracer.transformation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;

public class ClassRedefiner {
    private static final Logger logger = LoggerFactory.getLogger(ClassRedefiner.class);
    private static final String REENTRANT_LOCK_CLASS = "java/util/concurrent/locks/ReentrantLock";
    private static final String REENTRANT_LOCK_JAVA = "java.util.concurrent.locks.ReentrantLock";

    public static void redefineClasses(Instrumentation instrumentation, DeadlockTracerTransformer transformer) {
        redefineReentrantLock(instrumentation, transformer);
    }

    private static void redefineReentrantLock(Instrumentation instrumentation, DeadlockTracerTransformer transformer) {
        boolean redefineClassesSupported = instrumentation.isRedefineClassesSupported();
        if (redefineClassesSupported) {
            Class<?>[] allLoadedClasses = instrumentation.getAllLoadedClasses();
            Class<?> reentLock = null;
            for (Class<?> c : allLoadedClasses) {
                if (c.getName().equals(REENTRANT_LOCK_JAVA)) {
                    reentLock = c;
                }
            }
            if (reentLock != null) {
                try {
                    byte[] bytes = getBytes(reentLock);
                    if (bytes != null) {
                        bytes = transformer.transform(null, REENTRANT_LOCK_CLASS, reentLock, null, bytes);
                        ClassDefinition def = new ClassDefinition(reentLock, bytes);

                        instrumentation.redefineClasses(def);

                    }
                } catch (ClassNotFoundException | UnmodifiableClassException | IllegalClassFormatException |
                         IOException e) {
                    logger.error("Could not redefine Class ReentrantLock.", e);
                }
            }

        }
    }

    private static byte[] getBytes(Class<?> reentLock) throws IOException {

        InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream(REENTRANT_LOCK_CLASS + TransformationConstants.CLASS_FILE_SUFFIX);
        if (is == null) {
            throw new IllegalStateException("Could not find ReentrantLock Class data in running Java VM."
                + ". The data is needed for ReentrantLock instrumentation.");
        }
        try {
            return getBytesFromStream(is);
        } finally {
            is.close();
        }
    }

    private static byte[] getBytesFromStream(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
        }
        return output.toByteArray();
    }
}
