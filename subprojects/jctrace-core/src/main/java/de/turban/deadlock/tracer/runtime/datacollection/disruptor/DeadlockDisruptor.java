package de.turban.deadlock.tracer.runtime.datacollection.disruptor;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.PhasedBackoffWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import de.turban.deadlock.tracer.runtime.datacollection.IDeadlockGlobalCacheSubmitter;
import de.turban.deadlock.tracer.runtime.datacollection.IFieldThreadEntry;
import de.turban.deadlock.tracer.runtime.datacollection.ILockThreadEntry;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class DeadlockDisruptor implements IDeadlockGlobalCacheSubmitter {

    private final IDeadlockGlobalCacheSubmitter cache;

    private volatile LockEventProducer producer;

    private volatile Disruptor<LockEvent> disruptor;

    private volatile ExecutorService executor;

    public DeadlockDisruptor(IDeadlockGlobalCacheSubmitter cache) {
        this.cache = cache;
        createCacheDisruptorWrapper(cache);
    }

    @Override
    public void waitForProcessing() {
        Disruptor<?> old = disruptor;
        ExecutorService oldExecutor = executor;
        createCacheDisruptorWrapper(cache);
        old.shutdown();
        oldExecutor.shutdown();

    }

    @Override
    public void newLockMonitorEnter(ILockThreadEntry lockEntry, ILockThreadEntry[] heldLocks) {
        LockEventProducer producerLoc = producer;
        if (producerLoc == null) {
            throw new IllegalStateException();
        }
        producerLoc.newLockMonitorEnter(lockEntry, heldLocks);
    }

    @Override
    public void newFieldGet(IFieldThreadEntry fieldThreadEntry, ILockThreadEntry[] heldLocks) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void newFieldSet(IFieldThreadEntry fieldThreadEntry, ILockThreadEntry[] heldLocks) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void newLockCreated(ILockThreadEntry lockEntry, ILockThreadEntry[] heldLocks) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked")
    private void createCacheDisruptorWrapper(IDeadlockGlobalCacheSubmitter cache) {

        this.executor = Executors.newSingleThreadExecutor();

        // The factory for the event
        LockEventFactory factory = new LockEventFactory();

        // Specify the size of the ring buffer, must be power of 2.
        int bufferSize = 1 * 512 * 1024; // 524288 Entries

        // Construct the Disruptor
        Disruptor<LockEvent> disruptor = new Disruptor<>(factory, bufferSize, executor, ProducerType.MULTI,
            new PhasedBackoffWaitStrategy(400, 10000, TimeUnit.MICROSECONDS, new BlockingWaitStrategy()));

        disruptor.handleExceptionsWith(new ExceptionHandler<LockEvent>() {

            @Override
            public void handleEventException(Throwable ex, long sequence, LockEvent event) {
                ex.printStackTrace();
            }

            @Override
            public void handleOnStartException(Throwable ex) {
                ex.printStackTrace();
            }

            @Override
            public void handleOnShutdownException(Throwable ex) {
                ex.printStackTrace();
            }
        });

        // Connect the handler
        disruptor.handleEventsWith(new LockEventHandler(cache));

        // Start the Disruptor, starts all threads running
        disruptor.start();

        // Get the ring buffer from the Disruptor to be used for publishing.
        RingBuffer<LockEvent> ringBuffer = disruptor.getRingBuffer();

        LockEventProducer producerLoc = new LockEventProducer(ringBuffer);

        this.disruptor = disruptor;
        this.producer = producerLoc;
    }
}
