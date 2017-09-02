package de.turban.deadlock.tracer.transformation;

import static de.turban.deadlock.tracer.transformation.TransformationConstants.CLASS_FILE_SUFFIX;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClassRedefiner {
    private static Logger logger = LoggerFactory.getLogger(ClassRedefiner.class);

    private static final String BOOT_PATH_PROPERTY = "sun.boot.class.path";
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
                } catch (ClassNotFoundException | UnmodifiableClassException | IllegalClassFormatException | IOException e) {
                    logger.error("Could not redefine Class ReentrantLock.", e);
                }
            }

        }
    }

    private static byte[] getBytes(Class<?> reentLock) throws IOException {

        String property = System.getProperty(BOOT_PATH_PROPERTY);
        if (property == null || property.isEmpty()) {
            throw new IllegalStateException("Could not find System property " + BOOT_PATH_PROPERTY + ".");
        }
        String[] split = property.split(";");
        for (String str : split) {
            if (str.endsWith(".jar")) {
                Path jarPath = Paths.get(str);
                if (Files.isRegularFile(jarPath)) {
                    try (JarFile jar = new JarFile(jarPath.toFile())) {
                        ZipEntry entry = jar.getEntry(REENTRANT_LOCK_CLASS + CLASS_FILE_SUFFIX);
                        if (entry != null) {
                            InputStream is = jar.getInputStream(entry);
                            return getBytesFromStream(is);
                        }
                    }
                }
            }
        }
        throw new IllegalStateException("Could not find rt.jar file in " + BOOT_PATH_PROPERTY
                + ". The Rt.jar is needed for ReentrantLock instrumentation. Current content: " + property);
    }

    public static byte[] getBytesFromStream(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
        }
        return output.toByteArray();
    }
}
