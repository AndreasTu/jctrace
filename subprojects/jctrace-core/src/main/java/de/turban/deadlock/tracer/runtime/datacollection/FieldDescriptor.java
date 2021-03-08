package de.turban.deadlock.tracer.runtime.datacollection;

import de.turban.deadlock.tracer.runtime.IFieldDescriptor;
import org.objectweb.asm.Opcodes;

import javax.annotation.concurrent.ThreadSafe;
import java.io.Serializable;

@ThreadSafe
public class FieldDescriptor implements IFieldDescriptor, Serializable {
    private static final long serialVersionUID = 1L;

    private final int id;
    private volatile boolean isVolatile;
    private volatile boolean isStatic;
    private final String fieldClass;
    private final String fieldName;
    private final String desc;


    FieldDescriptor(int id, String fieldClass, String fieldName, String desc) {
        this.fieldClass = fieldClass;
        this.fieldName = fieldName;
        this.desc = desc;
        this.id = id;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getFieldName() {
        return fieldName;
    }

    @Override
    public String getFieldClass() {
        return fieldClass;
    }

    @Override
    public boolean isVolatile() {
        return isVolatile;
    }

    @Override
    public boolean isStatic() {
        return isStatic;
    }

    void setAccessFlags(int asmAccessFlags) {
        isVolatile = (asmAccessFlags & Opcodes.ACC_VOLATILE) != 0;
        isStatic = (asmAccessFlags & Opcodes.ACC_STATIC) != 0;
    }

    @Override
    public String getDesc() {
        return desc;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FieldDescriptor that = (FieldDescriptor) o;

        if (!fieldClass.equals(that.fieldClass)) return false;
        return fieldName.equals(that.fieldName);
    }

    @Override
    public int compareTo(IFieldDescriptor o) {
        int id1 = this.getId();
        int id2 = o.getId();
        return Integer.compare(id1, id2);
    }

    @Override
    public int hashCode() {
        int result = fieldClass.hashCode();
        result = 31 * result + fieldName.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "FieldDescriptor [id=" + getId() + ", fieldName=" + fieldClass + "." + fieldName + "]";
    }
}
