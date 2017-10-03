package de.turban.deadlock.tracer.transformation;

import de.turban.deadlock.tracer.DeadlockTracerClassBinding;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

class FieldCollectVisitor extends FieldVisitor {

    FieldCollectVisitor(FieldVisitor fv, String className, String classNameJavaStyle, String sourceFile, final int access, final String name,
                 final String desc) {
        super(Opcodes.ASM6, fv);

        DeadlockTracerClassBinding.newField(classNameJavaStyle, name, desc, access, true);
    }

}
