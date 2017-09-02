package de.turban.deadlock.tracer.transformation;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.ASMifier;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.ProtectionDomain;

import static de.turban.deadlock.tracer.transformation.TransformationConstants.CLASS_FILE_SUFFIX;

public class DeadlockTracerTransformer implements ClassFileTransformer {

    private static Logger logger = LoggerFactory.getLogger(DeadlockTracerTransformer.class);

    private static boolean TRACE_CLASS_TRANSFORMATION = false;

    private static boolean CHECK_CLASS_VISITOR = false;

    private static final String TRACE_CLASS_FOLDER_NAME = "DeadlockTracerTransformer_Classes";

    private final Path tmpFolder;


    static {
        if (TRACE_CLASS_TRANSFORMATION == false) {
            TRACE_CLASS_TRANSFORMATION = logger.isTraceEnabled();
        }
    }


    public DeadlockTracerTransformer() {
        Path tmpFolderLoc = null;
        if (TRACE_CLASS_TRANSFORMATION) {
            tmpFolderLoc = resolveTempFolder();
        }
        tmpFolder = tmpFolderLoc;
    }


    private Path resolveTempFolder() {
        Path tmpFolderLoc;
        try {
            tmpFolderLoc = Files.createTempDirectory(TRACE_CLASS_FOLDER_NAME);
            logger.info("Using TRACE_CLASS_TRANSFORMATION temp folder: " + tmpFolderLoc.toAbsolutePath());
        } catch (IOException e) {
            logger.error("Could not create TRACE_CLASS_TRANSFORMATION temp folder. Using working dir.");
            tmpFolderLoc = Paths.get("");
        }
        return tmpFolderLoc;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer)
            throws IllegalClassFormatException {

        if (className == null) {
            return classfileBuffer;
        }
        if (className.startsWith("java/lang")) {
            return classfileBuffer;
        }
        if (className.startsWith("java/security")) {
            return classfileBuffer;
        }
        if (className.startsWith("java/io")) {
            return classfileBuffer;
        }
        if (className.startsWith("sun/misc")) {
            return classfileBuffer;
        }

        if (className.startsWith("de/turban/deadlock/tracer")) {
            return classfileBuffer;
        }

        //Never ignore the package java/util/concurrent/locks because otherwise ReentrantLocks are not measured.
        if (!className.startsWith("java/util/concurrent/locks")) {


            if (TransformationBlacklist.isClassBlacklisted(className)) {
                return classfileBuffer;
            }
        }

        //        if (!className.startsWith("de/turban") && !className.startsWith("java/util/concurrent/locks")) {
        //            return classfileBuffer;
        //        }

        if (TRACE_CLASS_TRANSFORMATION) {
            logger.trace("Transforming class: " + className);
            writeTempFile(classfileBuffer, className + "_ORG");
            writeDeasmTempFile(classfileBuffer, className + "_ORG");
        }

        PrintWriter traceFile = null;
        PrintWriter traceFile2 = null;
        try {
            try {

                ClassReader reader = new ClassReader(classfileBuffer);

                ClassWriter writer = createClassWriter(reader);

                ClassVisitor cv = writer;

                if (CHECK_CLASS_VISITOR) {
                    cv = new CheckClassAdapter(writer, true);
                }
                if (TRACE_CLASS_TRANSFORMATION) {
                    traceFile = createTraceFileWriter(className, ".traceASM");
                    traceFile2 = createTraceFileWriter(className, ".traceASMifier");
                    TraceClassVisitor trace = new TraceClassVisitor(cv, traceFile);
                    trace = new TraceClassVisitor(trace, new ASMifier(), traceFile2);
                    cv = trace;
                }

                DeadlockTraceClassVisitor visitor = new DeadlockTraceClassVisitor(cv);

                reader.accept(visitor, ClassReader.EXPAND_FRAMES);

                byte[] newClass = writer.toByteArray();

                if (TRACE_CLASS_TRANSFORMATION) {
                    writeTempFile(newClass, className);
                    writeDeasmTempFile(newClass, className);
                }

                if (logger.isTraceEnabled()) {
                    logger.trace("Transformation of class " + className + " finished.");
                }
                return newClass;
            } finally {
                if (traceFile != null) {
                    traceFile.close();
                }
                if (traceFile2 != null) {
                    traceFile2.close();
                }
            }
        } catch (Throwable ex) {
            logger.error("Could not transform class: " + className, ex);
            return classfileBuffer;
        }

    }

    private ClassWriter createClassWriter(ClassReader reader) {
        return new ClassWriter(reader, ClassWriter.COMPUTE_MAXS) {
            @Override
            protected String getCommonSuperClass(final String type1, final String type2) {
                throw new UnsupportedOperationException(
                        "getCommonSuperClass() shall never be called, because we calculate the frames and max values on our own");
            }
        };
    }

    private void writeDeasmTempFile(byte[] newClass, String className) {
        try {
            Path classFileTempPath = resolveTempFile(className, CLASS_FILE_SUFFIX);
            Path classdeasmTempPath = resolveTempFile(className, ".deasm");

            String command = "cmd.exe /c javap -v -p " + classFileTempPath.toAbsolutePath() + " > " + classdeasmTempPath.toAbsolutePath();
            Runtime.getRuntime().exec(command);
            return;
        } catch (IOException ex) {
            logger.error("Could not create deasm file for class " + className + " tried to call javap (windows style).", ex);
        }
    }

    private void writeTempFile(byte[] newClass, String className) {
        try {
            Path classFileTempPath = resolveTempFile(className, CLASS_FILE_SUFFIX);
            Files.write(classFileTempPath, newClass, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);

        } catch (IOException ex) {
            logger.error("Could not write Trace temp file for class " + className, ex);
        }
    }

    private Path resolveTempFile(String className, String suffix) throws IOException {
        Path classFileTempPath = tmpFolder.resolve(className + suffix);

        Files.createDirectories(classFileTempPath.getParent());
        return classFileTempPath;
    }

    private PrintWriter createTraceFileWriter(String className, String suffix) throws IOException {
        PrintWriter traceFile;
        Path classAsmFileTempPath = resolveTempFile(className, suffix);
        BufferedWriter traceFileWriter = Files.newBufferedWriter(classAsmFileTempPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING);
        traceFile = new PrintWriter(traceFileWriter);
        return traceFile;
    }

}
