package de.turban.deadlock.tracer.runtime;

import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public interface IFieldDescriptor extends  Comparable<IFieldDescriptor>{

    int INVALID_FIELD_DESCRIPTOR_ID = 0;

    int getId();

    String getFieldName();

    String getFieldClass();

    String getDesc();

    boolean isVolatile();

    boolean isStatic();

}
