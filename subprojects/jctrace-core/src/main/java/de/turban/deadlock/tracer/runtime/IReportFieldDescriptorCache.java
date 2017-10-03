package de.turban.deadlock.tracer.runtime;

import java.util.List;

public interface IReportFieldDescriptorCache extends  IFieldDescriptorCache {

    /**
     * Returns a sorted immutable list of all {@link IFieldDescriptor} in the loaded database.
     *
     * @return the list
     */
    List<IFieldDescriptor> getFieldDescriptors();
}
