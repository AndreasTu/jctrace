package de.turban.deadlock.tracer.transformation;

import de.turban.deadlock.tracer.DeadlockTracerClassBinding;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class MonitorEnterExitMethodVisitor extends MethodVisitor {

    private static final String INT_VOID_DESC = "(I)V";
    public static final String OBJ_INT_VOID_DESC = "(Ljava/lang/Object;I)V";
    public static final String OBJ_VOID_DESC = "(Ljava/lang/Object;)V";

    public static final String TRACER_STATIC_CLASS = de.turban.deadlock.tracer.DeadlockTracerClassBinding.TRACER_STATIC_CLASS;

    @SuppressWarnings("unused")
    private String className;

    private String classNameJavaStyle;

    private String methodName;

    private String sourceFile;

    private int currentLine = 1;

    public MonitorEnterExitMethodVisitor(MethodVisitor mv, String className, String classNameJavaStyle, String sourceFile, final int access, final String name,
                                         final String desc) {
        super(Opcodes.ASM5, mv);
        this.className = className;
        this.classNameJavaStyle = classNameJavaStyle;
        this.sourceFile = sourceFile;
        this.methodName = name;

    }

    @Override
    public void visitInsn(int opcode) {
        if (opcode == Opcodes.MONITORENTER) {
            monitorEnter();
        } else if (opcode == Opcodes.MONITOREXIT) {
            monitorExit();
        } else {
            super.visitInsn(opcode);
        }
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        if (ReadWriteLockClassMethodVisitor.isInvocationALockEnter(owner, name, desc)) {
            newLockNewLocationIdMethodCall();
        }
        super.visitMethodInsn(opcode, owner, name, desc, itf);
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        this.currentLine = line;
        super.visitLineNumber(line, start);
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        super.visitMaxs(maxStack + 2, maxLocals);
    }

    private void monitorEnter() {
        visitInsn(Opcodes.DUP);
        super.visitInsn(Opcodes.MONITORENTER);
        createAndVisitNewTracerLocationInteger();

        //visitLdcInsn(newLdcClassType());
        newMonitorEnterMethodCall();

    }

    private void createAndVisitNewTracerLocationInteger() {
        int tracerLocation = DeadlockTracerClassBinding.newLocation(classNameJavaStyle, methodName, sourceFile, currentLine);
        visitLdcInsn(Integer.valueOf(tracerLocation));
    }

    private void monitorExit() {
        // System.out.println("MonitorExit found: " + className);

        visitInsn(Opcodes.DUP);
        newMonitorExitMethodCall();
        super.visitInsn(Opcodes.MONITOREXIT);
    }

    private void newMonitorEnterMethodCall() {
        visitMethodInsn(Opcodes.INVOKESTATIC, TRACER_STATIC_CLASS, "monitorEnter", OBJ_INT_VOID_DESC, false);
    }

    private void newMonitorExitMethodCall() {
        visitMethodInsn(Opcodes.INVOKESTATIC, TRACER_STATIC_CLASS, "monitorExit", OBJ_VOID_DESC, false);
    }

    private void newLockNewLocationIdMethodCall() {
        visitInsn(Opcodes.DUP);
        createAndVisitNewTracerLocationInteger();
        visitMethodInsn(Opcodes.INVOKESTATIC, TRACER_STATIC_CLASS, "lockUpdateThreadlocalLocationId", OBJ_INT_VOID_DESC, false);
    }

}
