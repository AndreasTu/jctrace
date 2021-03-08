package de.turban.deadlock.tracer.transformation;

import de.turban.deadlock.tracer.DeadlockTracerClassBinding;
import org.objectweb.asm.FieldVisitor;

class FieldCollectVisitor extends FieldVisitor {

    FieldCollectVisitor(FieldVisitor fv, String className, String classNameJavaStyle, String sourceFile, final int access, final String name,
                        final String desc) {
        super(JctraceAsmUtil.ASM_VERSION, fv);

        DeadlockTracerClassBinding.newField(classNameJavaStyle, name, desc, access, true);
    }

}
