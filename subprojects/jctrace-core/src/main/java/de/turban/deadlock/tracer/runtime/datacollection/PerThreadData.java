package de.turban.deadlock.tracer.runtime.datacollection;

import de.turban.deadlock.tracer.DeadlockTracerClassBinding;
import de.turban.deadlock.tracer.transformation.TransformationBlacklist;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@NotThreadSafe // But ThreadLocal Data
public class PerThreadData {

    private final IDeadlockCollectBindingResolver resolver;

    private Throwable lastException;

    private boolean traceRunning = false;

    private int threadId = LockerThreadCache.INVALID_THREAD_ID;

    private int currentLockerLocationId = LockerLocationCache.INVALID_ID;

    private WeakReference<Object> currentLockerLockObj = null;

    private final LinkedList<ILockThreadEntry> heldLocks = new LinkedList<>();

    public PerThreadData(IDeadlockCollectBindingResolver resolver) {
        this.resolver = resolver;
    }

    private IDeadlockCollectBindingResolver getBindingResolver() {
        return resolver;
    }

    private static class LockThreadEntry implements ILockThreadEntry {
        final Object lock;
        final int lockerLocationId; // Note: could also be LockerLocationCache.INVALID_ID
        final int lockerThreadId;
        int lockCount = 0;
        volatile LockWeakRef lockWeak;

        public LockThreadEntry(Object lock, int lockerLocationId, int lockerThreadId) {
            this.lock = lock;
            this.lockerLocationId = lockerLocationId;
            this.lockerThreadId = lockerThreadId;
        }

        @Override
        public Object getLock() {
            return lock;
        }

        @Override
        public int getLockerLocationId() {
            return lockerLocationId;
        }

        @Override
        public int getLockerThreadId() {
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
        if (threadId == LockerThreadCache.INVALID_THREAD_ID) {
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
        traceRunning = false;
    }

    /**
     * @param lockObj
     * @param lockerLocationId the location id {@link LockerLocationCache#INVALID_ID} is permitted.
     */
    public void monitorEnter(Object lockObj, int lockerLocationId) {
        Objects.requireNonNull(lockObj);
        LockThreadEntry entry = findElement(lockObj, lockerLocationId, true);
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
        Objects.requireNonNull(lockObj);

        LockThreadEntry lockEntry = findElement(lockObj, 0, false);
        Objects.requireNonNull(lockEntry);
        lockEntry.lockCount--;
        int lockC = lockEntry.lockCount;
        if (lockC == 0) {
            removeLockFromList(lockEntry);
        }
    }

    private void newLockMonitorEnter(LockThreadEntry lockEntry) {
        ILockThreadEntry[] array = heldLocks.toArray(new LockThreadEntry[heldLocks.size()]);
        getBindingResolver().getCacheSubmitter().newLockMonitorEnter(lockEntry, array);

    }

    private void newLockCreated(LockThreadEntry lockEntry) {
        ILockThreadEntry[] array = heldLocks.toArray(new LockThreadEntry[heldLocks.size()]);
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
        if (lockerLocationId != LockerLocationCache.INVALID_ID) {
            currentLockerLocationId = lockerLocationId;
            if (lockObj instanceof ReentrantReadWriteLock.ReadLock || lockObj instanceof ReentrantReadWriteLock.WriteLock) {
                lockObj = getSyncObjOfReadWriteLock(lockObj);
            }
            currentLockerLockObj = new WeakReference<Object>(lockObj);
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
        int lockLocationId = currentLockerLocationId;
        if (currentLockerLockObj == null || currentLockerLockObj.get() != lockObj) {
            //Wrong lock so the location does not match
            lockLocationId = LockerLocationCache.INVALID_ID;
        }
        // Note currentLockerLocationId could also be LockerLocationCache.INVALID_ID
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
        if (creationLocationId == LockerLocationCache.INVALID_ID) {
            return;
        }
        LockThreadEntry newEntry = new LockThreadEntry(lockObj, creationLocationId, threadId);
        newLockCreated(newEntry);
    }

    @Nullable
    private StackTraceElement createStackFrame() {
        StackTraceElement[] stackTraceLoc = new Throwable().getStackTrace();
        List<StackTraceElement> lst = new ArrayList<>();
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
