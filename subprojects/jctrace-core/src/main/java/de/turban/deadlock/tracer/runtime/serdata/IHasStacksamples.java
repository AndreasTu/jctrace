package de.turban.deadlock.tracer.runtime.serdata;

import de.turban.deadlock.tracer.runtime.IStackSample;

import java.util.List;

public interface IHasStacksamples {

    void addStacks(List<? extends IStackSample> stackEntries);
}
