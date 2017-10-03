package de.turban.deadlock.tracer.transformation;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.TryCatchBlockSorter;

class DeadlockTraceClassVisitor extends ClassVisitor {

    private int version;
    private String className;
    private String sourceFile;
    private String classNameJavaStyle;

    DeadlockTraceClassVisitor(ClassVisitor cv) {
        super(Opcodes.ASM6, cv);

    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.version = version;
        if (version < Opcodes.V1_5) {
            this.version = Opcodes.V1_5;
        } else {
            this.version = version;
        }

        this.className = name;
        this.classNameJavaStyle = classNameJavaStyle(className);

        super.visit(this.version, access, name, signature, superName, interfaces);
    }

    static String classNameJavaStyle(String className) {
        return className.replace('/', '.');
    }

    @Override
    public void visitSource(String sourceFile, String debug) {
        this.sourceFile = sourceFile;
        super.visitSource(sourceFile, debug);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        FieldVisitor fv = super.visitField(access, name, desc, signature, value);
        fv = new FieldCollectVisitor(fv,className, classNameJavaStyle, sourceFile, access, name, desc);

        return fv;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        mv = new MonitorEnterExitMethodVisitor(mv, className, classNameJavaStyle, sourceFile, access, name, desc);
        mv = new FieldGetSetMethodVisitor(mv, className, classNameJavaStyle, sourceFile, access, name, desc);
        boolean hasSyncFlag = (access & Opcodes.ACC_SYNCHRONIZED) != 0;
        if (hasSyncFlag) {
            TryCatchBlockSorter tryCatchSorter = new TryCatchBlockSorter(mv, access, name, desc, signature, exceptions);
            mv = new SyncMethodVisitor(version, tryCatchSorter, className, classNameJavaStyle, sourceFile, access, name, desc);
        }
        if (ReadWriteLockClassMethodVisitor.CLASSES_TO_TRANSFORM.contains(className)) {
            mv = new ReadWriteLockClassMethodVisitor(mv, className, classNameJavaStyle, sourceFile, access, name, desc);
        }
        return mv;
    }



}
