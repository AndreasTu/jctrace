package de.turban.deadlock.tracer.runtime.datacollection;

import de.turban.deadlock.tracer.runtime.IFieldDescriptor;

public interface IFieldThreadEntry extends  IThreadEntry {

    /**
     * @return returns the owner object of the field.
     */
    Object getOwner();

    /**
     * @return The location id. Could also be {@link IFieldDescriptor#INVALID_FIELD_DESCRIPTOR_ID}.
     * @see LocationCache
     */
    int getFieldDescriptorId();

}
