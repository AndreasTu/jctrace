package de.turban.deadlock.tracer.transformation;

import org.objectweb.asm.FieldVisitor;

public class FieldAccessVisitor extends FieldVisitor {

    public FieldAccessVisitor(int api, FieldVisitor fv) {
        super(api, fv);
    }
}
