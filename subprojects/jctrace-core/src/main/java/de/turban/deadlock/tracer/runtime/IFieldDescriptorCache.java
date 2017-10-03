package de.turban.deadlock.tracer.runtime;

import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public interface IFieldDescriptorCache {

    IFieldDescriptor getFieldDescriptorById(int id);
}
