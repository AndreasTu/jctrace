package de.turban.deadlock.tracer.transformation;

import de.turban.deadlock.tracer.DeadlockTracerClassBinding;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

import java.util.ArrayList;
import java.util.List;

public class SyncMethodVisitor extends AdviceAdapter {

    private static final String JAVA_LANG_THROWABLE = "java/lang/Throwable";
    public static final String OBJ_INT_VOID_DESC = MonitorEnterExitMethodVisitor.OBJ_INT_VOID_DESC;
    public static final String OBJ_VOID_DESC = MonitorEnterExitMethodVisitor.OBJ_VOID_DESC;

    public static final String TRACER_STATIC_CLASS = MonitorEnterExitMethodVisitor.TRACER_STATIC_CLASS;

    private final Label before = new Label();

    private final Label handler = new Label();

    private boolean isStatic;

    private int version;

    private String className;

    private String desc;

    private String classNameJavaStyle;

    private String sourceFile;

    private String methodName;

    private int currentLine = 1;

    private int tracerLocation;
    private boolean newTracerLocationGenerated;

    public SyncMethodVisitor(int version, final MethodVisitor writer, String className, String classNameJavaStyle, String sourceFile, final int access, final String name,
                             final String desc) {
        super(JctraceAsmUtil.ASM_VERSION, writer, access, name, desc);
        this.version = version;
        this.className = className;
        this.classNameJavaStyle = classNameJavaStyle;
        this.sourceFile = sourceFile;
        this.methodName = name;
        this.desc = desc;
        isStatic = (access & Opcodes.ACC_STATIC) != 0;
    }

    @Override
    public void visitCode() {
        super.visitCode();

        newMonitorEnterMethodCall();

        this.mv.visitTryCatchBlock(this.before, this.handler, this.handler, null);
        this.mv.visitLabel(this.before);
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        this.currentLine = line;
        if (currentLine > 2 && newTracerLocationGenerated) {
            updateTracerLocation(currentLine - 1); // sub 1, because the method name is normally one line above, this is only an estimate.
            newTracerLocationGenerated = false;

        }
        super.visitLineNumber(line, start);
    }

    private void updateTracerLocation(int newLineNr) {
        DeadlockTracerClassBinding.updateLocation(tracerLocation, classNameJavaStyle, methodName, sourceFile, newLineNr);
    }

    private Type newLdcClassType() {
        Type cls = Type.getType("L" + className + ";");
        return cls;
    }

    private void newMonitorEnterMethodCall() {
        generateCallArgsOpCodes(true);
        this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACER_STATIC_CLASS, "monitorEnter", OBJ_INT_VOID_DESC, false);
    }

    private void newMonitorExitMethodCall() {
        generateCallArgsOpCodes(false);
        this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, TRACER_STATIC_CLASS, "monitorExit", OBJ_VOID_DESC, false);
    }

    private void generateCallArgsOpCodes(boolean isMonitorEntry) {
        if (!isStatic) {
            this.mv.visitVarInsn(ALOAD, 0);
        } else {
            this.mv.visitLdcInsn(newLdcClassType());
        }
        if (isMonitorEntry) {
            tracerLocation = DeadlockTracerClassBinding.newLocation(classNameJavaStyle, methodName, sourceFile, currentLine);
            newTracerLocationGenerated = true;
            visitLdcInsn(Integer.valueOf(tracerLocation));
        }
    }

    @Override
    public void visitMaxs(int maxStack, final int maxLocals) {

        this.mv.visitLabel(this.handler);
        Object[] argsArr = buildArgArray();
        if (version >= Opcodes.V1_6) {
            mv.visitFrame(Opcodes.F_NEW, argsArr.length, argsArr, 1, new Object[]{JAVA_LANG_THROWABLE});
        }

        generateCacthBlockCode();

        this.mv.visitMaxs(maxStack + 2, maxLocals);
    }

    private void generateCacthBlockCode() {
        newMonitorExitMethodCall();

        this.mv.visitInsn(ATHROW);
    }

    private Object[] buildArgArray() {
        Type methodType = Type.getMethodType(desc);
        Type[] argumentTypes = methodType.getArgumentTypes();
        List<Object> args = new ArrayList<>();
        if (!isStatic) {
            args.add(className);
        }
        for (Type type : argumentTypes) {
            addOpcodeByTypeToList(type, args);
        }
        return args.toArray();
    }

    private void addOpcodeByTypeToList(Type type, List<Object> locals) {
        switch (type.getSort()) {
            case Type.BOOLEAN:
            case Type.CHAR:
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:
                locals.add(Opcodes.INTEGER);
                break;
            case Type.FLOAT:
                locals.add(Opcodes.FLOAT);
                break;
            case Type.LONG:
                locals.add(Opcodes.LONG);
                break;
            case Type.DOUBLE:
                locals.add(Opcodes.DOUBLE);
                break;
            case Type.ARRAY:
                locals.add(type.getDescriptor());
                break;
            // case Type.OBJECT:
            default:
                locals.add(type.getInternalName());
        }
    }

    @Override
    protected void onMethodExit(final int opcode) {
        // do not generate for throws instr, because this is generated in
        // visitMaxs()
        if (opcode != ATHROW) {
            newMonitorExitMethodCall();
        }
    }

}
