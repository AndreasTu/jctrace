package de.turban.deadlock.tracer.transformation;

import de.turban.deadlock.tracer.DeadlockTracerClassBinding;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

class FieldGetSetMethodVisitor extends MethodVisitor {

    private static final String INT_VOID_DESC = "(I)V";
    static final String OBJ_STR_STR_INT_VOID_DESC = "(Ljava/lang/Object;II)V";
    static final String OBJ_VOID_DESC = "(Ljava/lang/Object;)V";

    static final String TRACER_STATIC_CLASS = DeadlockTracerClassBinding.TRACER_STATIC_CLASS;

    @SuppressWarnings("unused")
    private String className;

    private String classNameJavaStyle;

    private String methodName;

    private String sourceFile;

    private int currentLine = 1;

    FieldGetSetMethodVisitor(MethodVisitor mv, String className, String classNameJavaStyle, String sourceFile, final int access, final String name,
                             final String desc) {
        super(JctraceAsmUtil.ASM_VERSION, mv);
        this.className = className;
        this.classNameJavaStyle = classNameJavaStyle;
        this.sourceFile = sourceFile;
        this.methodName = name;

    }

    @Override
    public void visitLineNumber(int line, Label start) {
        this.currentLine = line;
        super.visitLineNumber(line, start);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        if (opcode == Opcodes.PUTFIELD) {
            putField(owner, name, desc);
        } else if (opcode == Opcodes.GETFIELD) {
            getField(owner, name, desc);
        } else if (opcode == Opcodes.PUTSTATIC) {
            // putField(owner, name, desc);
        } else if (opcode == Opcodes.GETSTATIC) {
            // getField(owner, name, desc);
        }
        super.visitFieldInsn(opcode, owner, name, desc);
    }


    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        super.visitMaxs(maxStack + 2, maxLocals);
    }


    private void getField(String owner, String name, String desc) {
        visitInsn(Opcodes.DUP);
        createAndVisitFieldDescriptor(owner, name, desc);
        createAndVisitNewTracerLocationInteger();
        newGetFieldMethodCall();
    }

    private void putField(String owner, String name, String desc) {
        Type type = Type.getType(desc);
        if (type == Type.LONG_TYPE || type == Type.DOUBLE_TYPE) {
            visitInsn(Opcodes.DUP2_X1);
            visitInsn(Opcodes.POP2);
            visitInsn(Opcodes.DUP_X2);
        } else {
            visitInsn(Opcodes.DUP2);
            visitInsn(Opcodes.POP);
        }
        createAndVisitFieldDescriptor(owner, name, desc);
        createAndVisitNewTracerLocationInteger();
        newPutFieldMethodCall();
    }

    private void createAndVisitNewTracerLocationInteger() {
        Integer location = DeadlockTracerClassBinding.newLocation(classNameJavaStyle, methodName, sourceFile, currentLine);
        visitLdcInsn(location);
    }

    private void createAndVisitFieldDescriptor(String owner, String name, String desc) {
        Integer location = DeadlockTracerClassBinding.newField(DeadlockTraceClassVisitor.classNameJavaStyle(owner), name, desc, 0, false);
        visitLdcInsn(location);
    }

    private void newGetFieldMethodCall() {
        visitMethodInsn(Opcodes.INVOKESTATIC, TRACER_STATIC_CLASS, "getField", OBJ_STR_STR_INT_VOID_DESC, false);
    }

    private void newPutFieldMethodCall() {
        visitMethodInsn(Opcodes.INVOKESTATIC, TRACER_STATIC_CLASS, "putField", OBJ_STR_STR_INT_VOID_DESC, false);
    }
}
