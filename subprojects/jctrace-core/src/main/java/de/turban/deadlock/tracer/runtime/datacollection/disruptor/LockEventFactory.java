package de.turban.deadlock.tracer.runtime.datacollection.disruptor;

import com.lmax.disruptor.EventFactory;

class LockEventFactory implements EventFactory<LockEvent> {

    @Override
    public LockEvent newInstance() {
        return new LockEvent();
    }

}
