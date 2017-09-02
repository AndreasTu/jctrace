package de.turban.deadlock.tracer.runtime.datacollection.disruptor;

import com.lmax.disruptor.RingBuffer;

import de.turban.deadlock.tracer.runtime.datacollection.ILockThreadEntry;

class LockEventProducer {

    private final RingBuffer<LockEvent> ringBuffer;

    LockEventProducer(RingBuffer<LockEvent> ringBuffer) {
        this.ringBuffer = ringBuffer;
    }

    void newLockMonitorEnter(ILockThreadEntry lockEntry, ILockThreadEntry[] heldLocks) {
        long sequence = ringBuffer.next(); // Grab the next sequence
        try {
            LockEvent event = ringBuffer.get(sequence);
            event.setLockObj(lockEntry);
            event.setHeldLocks(heldLocks);
        } finally {
            ringBuffer.publish(sequence);
        }
    }

}
