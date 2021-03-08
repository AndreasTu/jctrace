package de.turban.deadlock.tracer.runtime.datacollection;

import de.turban.deadlock.tracer.DeadlockTracerClassBinding;
import de.turban.deadlock.tracer.runtime.ILocationCache;
import de.turban.deadlock.tracer.runtime.IThreadCache;
import de.turban.deadlock.tracer.transformation.TransformationBlacklist;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.util.Objects.requireNonNull;

@NotThreadSafe // But ThreadLocal Data
public class PerThreadData {

    private final IDeadlockCollectBindingResolver resolver;

    private Throwable lastException;

    private boolean traceRunning;

    private boolean isTraceCollectorThread;

    private int threadId = IThreadCache.INVALID_THREAD_ID;

    private int currentLocationId = ILocationCache.INVALID_LOCATION_ID;

    private WeakReference<Object> currentLockerLockObj = null;

    private final LinkedList<ILockThreadEntry> heldLocks = new LinkedList<>();

    public PerThreadData(IDeadlockCollectBindingResolver resolver) {

        this.resolver = resolver;
        this.isTraceCollectorThread = Thread.currentThread() instanceof DeadlockCollector.CollectorThread;
        this.traceRunning = isTraceCollectorThread;
    }

    private IDeadlockCollectBindingResolver getBindingResolver() {
        return resolver;
    }

    private static class LockThreadEntry implements ILockThreadEntry {
        final Object lock;
        final int lockerLocationId; // Note: could also be ILocationCache.INVALID_LOCATION_ID
        final int lockerThreadId;
        int lockCount = 0;
        volatile LockWeakRef lockWeak;

        LockThreadEntry(Object lock, int lockerLocationId, int lockerThreadId) {
            this.lock = lock;
            this.lockerLocationId = lockerLocationId;
            this.lockerThreadId = lockerThreadId;
        }

        @Override
        public Object getLock() {
            return lock;
        }

        @Override
        public Object getObject() {
            return getLock();
        }

        @Override
        public int getLocationId() {
            return lockerLocationId;
        }

        @Override
        public int getThreadId() {
            return lockerThreadId;
        }

        @Override
        public LockWeakRef getLockWeakReference() {
            if (lockWeak == null) {
                this.lockWeak = new LockWeakRef(lock);
            }
            return lockWeak;
        }

        @Override
        public String toString() {
            return "LockThreadEntry [lock=" + lock + ", lockerLocationId=" + lockerLocationId + ", lockerThreadId=" + lockerThreadId + ", lockCount="
                + lockCount + "]";
        }

    }

    private static class FieldThreadEntry implements IFieldThreadEntry {
        final Object owner;
        final int fieldDescriptorId;
        final int locationId; // Note: could also be ILocationCache.INVALID_LOCATION_ID
        final int threadId;
        volatile LockWeakRef lockWeak;

        FieldThreadEntry(Object owner, int fieldDescriptorId, int locationId, int threadId) {
            this.owner = owner;
            this.fieldDescriptorId = fieldDescriptorId;
            this.locationId = locationId;
            this.threadId = threadId;
        }

        @Override
        public Object getOwner() {
            return owner;
        }

        @Override
        public Object getObject() {
            return getOwner();
        }

        @Override
        public int getLocationId() {
            return locationId;
        }

        @Override
        public int getThreadId() {
            return threadId;
        }

        @Override
        public int getFieldDescriptorId() {
            return fieldDescriptorId;
        }

        @Override
        public LockWeakRef getLockWeakReference() {
            if (lockWeak == null) {
                this.lockWeak = new LockWeakRef(getOwner());
            }
            return lockWeak;
        }

        @Override
        public String toString() {
            return "FieldThreadEntry [owner=" + getOwner() + ", fieldDescriptorId=" + fieldDescriptorId + ", lockerLocationId=" + locationId + ", lockerThreadId=" + threadId + "]";
        }

    }

    @SuppressWarnings("WeakerAccess")
    public boolean isTraceRunning() {
        return traceRunning;
    }

    public boolean startTraceIfNotAlready() {
        if (isTraceRunning()) {
            return true;
        }
        traceRunning = true;
        processThreadInfo();
        return false;
    }

    private void processThreadInfo() {
        if (threadId == IThreadCache.INVALID_THREAD_ID) {
            publishThreadInfo();
        }
    }

    private void publishThreadInfo() {
        threadId = getBindingResolver().getThreadCache().newThreadIdOfCurrentThread();
    }

    public void stopTrace() {
        if (!isTraceRunning()) {
            throw new IllegalStateException();
        }
        if (isTraceCollectorThread) {
            throw new IllegalStateException();
        }
        traceRunning = false;
    }

    /**
     * @param lockObj          the monitor where the monitor enter was called
     * @param lockerLocationId the location id {@link ILocationCache#INVALID_LOCATION_ID} is permitted.
     */
    public void monitorEnter(Object lockObj, int lockerLocationId) {
        requireNonNull(lockObj);
        LockThreadEntry entry = requireNonNull(findElement(lockObj, lockerLocationId, true));
        entry.lockCount++;
        if (entry.lockCount == 1) {
            newLockMonitorEnter(entry);
        }
    }

    private LockThreadEntry findElement(Object lockObj, int lockerLocationId, boolean createNew) {
        if (!heldLocks.isEmpty()) {
            LockThreadEntry last = (LockThreadEntry) heldLocks.getLast();
            if (last.lock == lockObj) {
                return last;
            }
            Iterator<ILockThreadEntry> it = heldLocks.descendingIterator();
            it.next();
            while (it.hasNext()) {
                LockThreadEntry next = (LockThreadEntry) it.next();
                if (next.lock == lockObj) {
                    return next;
                }
            }
        }
        if (createNew) {
            return addLockToList(lockObj, lockerLocationId);
        }
        return null;
    }

    public void monitorExit(Object lockObj) {
        requireNonNull(lockObj);

        LockThreadEntry lockEntry = findElement(lockObj, 0, false);
        requireNonNull(lockEntry);
        lockEntry.lockCount--;
        int lockC = lockEntry.lockCount;
        if (lockC == 0) {
            removeLockFromList(lockEntry);
        }
    }

    private void newLockMonitorEnter(LockThreadEntry lockEntry) {
        ILockThreadEntry[] array = heldLocks.toArray(new ILockThreadEntry[heldLocks.size()]);
        getBindingResolver().getCacheSubmitter().newLockMonitorEnter(lockEntry, array);

    }

    private void newLockCreated(LockThreadEntry lockEntry) {
        ILockThreadEntry[] array = heldLocks.toArray(new ILockThreadEntry[heldLocks.size()]);
        getBindingResolver().getCacheSubmitter().newLockCreated(lockEntry, array);

    }

    private void removeLockFromList(LockThreadEntry lockEntry) {
        //System.out.println("Lock removed: " + lockObj);
        LockThreadEntry last = (LockThreadEntry) heldLocks.getLast();
        if (last == lockEntry) {
            heldLocks.removeLast();
        } else {
            heldLocks.remove(lockEntry);
        }
    }

    private LockThreadEntry addLockToList(Object lockObj, int lockerLocationId) {
        LockThreadEntry newEntry = new LockThreadEntry(lockObj, lockerLocationId, threadId);
        heldLocks.addLast(newEntry);
        return newEntry;
    }

    public void lockNewLocationId(Object lockObj, int lockerLocationId) {
        if (lockerLocationId != ILocationCache.INVALID_LOCATION_ID) {
            currentLocationId = lockerLocationId;
            if (lockObj instanceof ReentrantReadWriteLock.ReadLock || lockObj instanceof ReentrantReadWriteLock.WriteLock) {
                lockObj = getSyncObjOfReadWriteLock(lockObj);
            }
            currentLockerLockObj = new WeakReference<>(lockObj);
        }
    }

    private Object getSyncObjOfReadWriteLock(Object lockObj) {
        try {
            Field sync = lockObj.getClass().getDeclaredField("sync");
            sync.setAccessible(true);
            lockObj = sync.get(lockObj);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return lockObj;
    }

    public void lockEnter(Object lockObj) {
        int lockLocationId = currentLocationId;
        if (currentLockerLockObj == null || currentLockerLockObj.get() != lockObj) {
            //Wrong lock so the location does not match
            lockLocationId = ILocationCache.INVALID_LOCATION_ID;
        }
        // Note currentLocationId could also be ILocationCache.INVALID_LOCATION_ID
        monitorEnter(lockObj, lockLocationId);
    }

    public void lockCreated(Object lockObj) {
        StackTraceElement elem = createStackFrame();
        if (elem == null) {
            return;
        }
        if (TransformationBlacklist.isClassBlacklisted(elem.getClassName())) {
            return;
        }
        int creationLocationId = getBindingResolver().getLocationCache().getOrCreateIdByLocation(elem);
        if (creationLocationId == LocationCache.INVALID_LOCATION_ID) {
            return;
        }
        LockThreadEntry newEntry = new LockThreadEntry(lockObj, creationLocationId, threadId);
        newLockCreated(newEntry);
    }

    @Nullable
    private StackTraceElement createStackFrame() {
        StackTraceElement[] stackTraceLoc = new Throwable().getStackTrace();
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < stackTraceLoc.length; i++) {
            StackTraceElement e = stackTraceLoc[i];
            if (!e.getClassName().startsWith(DeadlockTracerClassBinding.TRACER_PKG)) {
                if (!e.getClassName().startsWith("java.util.concurrent")) {
                    return e;
                }
            }
        }
        return null;
    }

    public void lockExit(Object lockObj) {
        monitorExit(lockObj);
    }


    public void getField(@Nullable Object owner, int fieldDescriptorId, int locationId) {
        requireNonNull(owner);
        FieldThreadEntry entry = new FieldThreadEntry(owner, fieldDescriptorId, locationId, threadId);

        ILockThreadEntry[] array = heldLocks.toArray(new ILockThreadEntry[heldLocks.size()]);
        getBindingResolver().getCacheSubmitter().newFieldGet(entry, array);
    }

    public void setField(@Nullable Object owner, int fieldDescriptorId, int locationId) {
        requireNonNull(owner);
        FieldThreadEntry entry = new FieldThreadEntry(owner, fieldDescriptorId, locationId, threadId);

        ILockThreadEntry[] array = heldLocks.toArray(new ILockThreadEntry[heldLocks.size()]);
        getBindingResolver().getCacheSubmitter().newFieldSet(entry, array);
    }

    public void checkForOccurredException() {
        if (lastException != null) {
            Throwable ex = lastException;
            lastException = null;
            throw new IllegalStateException("Exception occurred during last trace operation. ", ex);
        }
    }

    public void assignLastExceptionForThisThread(Throwable ex) {
        lastException = ex;
    }

}
