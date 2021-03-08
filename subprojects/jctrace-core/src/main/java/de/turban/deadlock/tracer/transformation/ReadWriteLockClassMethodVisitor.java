package de.turban.deadlock.tracer.transformation;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;

import java.util.HashSet;
import java.util.Set;

public class ReadWriteLockClassMethodVisitor extends AdviceAdapter {

    public static final String TRACER_STATIC_CLASS = MonitorEnterExitMethodVisitor.TRACER_STATIC_CLASS;

    public static final String OBJ_VOID_DESC = MonitorEnterExitMethodVisitor.OBJ_VOID_DESC;

    private static final String REENTRANT_SYNC_DESCRIPTOR = "Ljava/util/concurrent/locks/ReentrantReadWriteLock$Sync;";

    private static final String LOCK = "java/util/concurrent/locks/Lock";

    private static final String REENTRANT_LOCK = "java/util/concurrent/locks/ReentrantLock";

    private static final String REENTRANT_READ_LOCK = "java/util/concurrent/locks/ReentrantReadWriteLock$ReadLock";

    private static final String REENTRANT_WRITE_LOCK = "java/util/concurrent/locks/ReentrantReadWriteLock$WriteLock";

    @SuppressWarnings("unused")
    private static final String STAMPED_LOCK = "java/util/concurrent/locks/StampedLock";

    @SuppressWarnings("unused")
    private static final String STAMPED_READ_LOCK = "java/util/concurrent/locks/StampedLock$ReadLockView";

    @SuppressWarnings("unused")
    private static final String STAMPED_WRITE_LOCK = "java/util/concurrent/locks/StampedLock$WriteLockView";

    private static final Set<String> CLASSES_CLIENT_SIDE_CALLS;
    public static final Set<String> CLASSES_TO_TRANSFORM;

    private static final Set<String> CLASSES_REENTRANT_READ_WRITE;

    static {
        Set<String> clientside = new HashSet<>();
        Set<String> readWrite = new HashSet<>();
        Set<String> transform = new HashSet<>();

        clientside.add(REENTRANT_WRITE_LOCK);
        transform.add(REENTRANT_WRITE_LOCK);
        readWrite.add(REENTRANT_WRITE_LOCK);

        clientside.add(REENTRANT_READ_LOCK);
        transform.add(REENTRANT_READ_LOCK);
        readWrite.add(REENTRANT_READ_LOCK);

        clientside.add(LOCK);

        clientside.add(REENTRANT_LOCK);
        transform.add(REENTRANT_LOCK);

        //        clientside.add(STAMPED_LOCK);
        //        transform.add(STAMPED_LOCK);
        //
        //        clientside.add(STAMPED_WRITE_LOCK);
        //        transform.add(STAMPED_WRITE_LOCK);
        //
        //        clientside.add(STAMPED_READ_LOCK);
        //        transform.add(STAMPED_READ_LOCK);

        CLASSES_CLIENT_SIDE_CALLS = java.util.Collections.unmodifiableSet(clientside);
        CLASSES_REENTRANT_READ_WRITE = java.util.Collections.unmodifiableSet(readWrite);
        CLASSES_TO_TRANSFORM = java.util.Collections.unmodifiableSet(transform);
    }

    private String className;

    private String method;

    @SuppressWarnings("unused")
    private String classNameJavaStyle;

    @SuppressWarnings("unused")
    private String sourceFile;

    public ReadWriteLockClassMethodVisitor(final MethodVisitor writer, String className, String classNameJavaStyle, String sourceFile, final int access,
                                           final String name, final String desc) {
        super(JctraceAsmUtil.ASM_VERSION, writer, access, name, desc);
        this.className = className;
        this.classNameJavaStyle = classNameJavaStyle;
        this.sourceFile = sourceFile;
        this.method = name;

    }

    @Override
    protected void onMethodEnter() {

        if (method.equals("unlock")) {
            visitCodeForLockMethodArgument();
            newLockExitMethodCall();
        }
    }

    @Override
    protected void onMethodExit(int opcode) {
        if (opcode != Opcodes.ATHROW) {
            if (method.equals("<init>")) {
                visitCodeForLockMethodArgument();
                newLockCreateMethodCall();
            } else if (method.equals("lock")) {
                newLockEnterMethodCall();
            } else if (method.equals("readLock")) {
                newLockEnterMethodCall();
            } else if (method.equals("writeLock")) {
                newLockEnterMethodCall();
            } else if (method.equals("lockInterruptibly")) {
                newLockEnterMethodCall();
            } else if (method.equals("tryLock")) {
                if (opcode == Opcodes.IRETURN) {
                    mv.visitInsn(DUP);
                    Label l2 = new Label();
                    mv.visitJumpInsn(IFEQ, l2);
                    newLockEnterMethodCall();
                    mv.visitLabel(l2);
                }
            }
        }
    }

    private void visitCodeForLockMethodArgument() {
        this.mv.visitVarInsn(Opcodes.ALOAD, 0);
        if (CLASSES_REENTRANT_READ_WRITE.contains(className)) {
            // If we are a read write lock, we use the sync object instead of this, because the both locks shall be considered as the same
            this.mv.visitFieldInsn(GETFIELD, className, "sync", REENTRANT_SYNC_DESCRIPTOR);
        }
    }

    public static boolean isInvocationALockEnter(String owner, String method, String desc) {
        if (CLASSES_CLIENT_SIDE_CALLS.contains(owner)) {
            if (method.equals("lock")) {
                return true;
            } else if (method.equals("lockInterruptibly")) {
                return true;
            } else if (method.equals("tryLock")) {
                return true;
            }
        }
        return false;
    }

    private void newLockCreateMethodCall() {
        this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACER_STATIC_CLASS, "lockCreated", OBJ_VOID_DESC, false);
    }

    private void newLockExitMethodCall() {
        this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACER_STATIC_CLASS, "lockExit", OBJ_VOID_DESC, false);
    }

    private void newLockEnterMethodCall() {
        visitCodeForLockMethodArgument();
        this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACER_STATIC_CLASS, "lockEnter", OBJ_VOID_DESC, false);
    }

}
