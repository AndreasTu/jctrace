package de.turban.deadlock.tracer;

import de.turban.deadlock.tracer.runtime.datacollection.DeadlockCollectBindingResolver;
import de.turban.deadlock.tracer.runtime.datacollection.IDeadlockCollectBindingResolver;
import de.turban.deadlock.tracer.runtime.datacollection.LockerLocationCache;
import de.turban.deadlock.tracer.runtime.datacollection.PerThreadData;
import de.turban.deadlock.tracer.transformation.DeadlockTracerTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

public class DeadlockTracerClassBinding {

    public static final String TRACER_PKG = "de.turban.deadlock.tracer";
    public static final String TRACER_STATIC_CLASS = "de/turban/deadlock/tracer/DeadlockTracerClassBinding";


    private static boolean LOGGER_TRACE_STATIC_ENABLED = false;

    private static Logger logger;

    private static Logger log() {
        if (logger == null) {
            logger = LoggerFactory.getLogger(DeadlockTracerClassBinding.class);
        }
        return logger;
    }

    private static ThreadLocal<PerThreadData> running = new ThreadLocal<PerThreadData>() {
        @Override
        protected PerThreadData initialValue() {
            return new PerThreadData(getBindingResolver());
        }
    };

    static IDeadlockCollectBindingResolver getBindingResolver() {
        return DeadlockCollectBindingResolver.INSTANCE;
    }

    /**
     * This method shall only be called by the {@link DeadlockTracerTransformer}. Never called by runtime code! That is the reason that it has no exception
     * handling.
     *
     * @param className
     * @param methodName
     * @param fileName
     * @param lineNr
     * @return a new location id as integer
     * @see LockerLocationCache
     */
    public static int newLocation(String className, String methodName, String fileName, int lineNr) {
        return getBindingResolver().getLocationCache().newLocation(new StackTraceElement(className, methodName, fileName, lineNr));
    }

    public static <T> T runWithTracingDisabled(Supplier<T> code) {
        PerThreadData info = running.get();
        boolean alreadyRunning = info.startTraceIfNotAlready();
        try {
            return code.get();
        } catch (Throwable ex) {
            handleException(info, "runCodeWithNoTracing", ex);
            throw ex;
        } finally {
            if (!alreadyRunning) {
                info.stopTrace();
            }
        }
    }

    public static void updateLocation(int tracerLocation, String className, String methodName, String fileName, int lineNr) {
        PerThreadData info = running.get();
        if (info.startTraceIfNotAlready()) {
            return;
        }
        try {
            getBindingResolver().getLocationCache().updateLocation(tracerLocation, new StackTraceElement(className, methodName, fileName, lineNr));
        } catch (Throwable ex) {
            handleException(info, "updateLocation", ex);
        } finally {
            info.stopTrace();
        }
    }

    public static void checkForOccuredException() {
        PerThreadData info = running.get();
        if (info.startTraceIfNotAlready()) {
            return;
        }
        try {
            info.checkForOccurredException();
        } finally {
            info.stopTrace();
        }
    }

    /*
     * -------------------
     * Concurrent Locks
     * --------------------
     */

    public static void lockCreated(Object lockObj) {
        PerThreadData info = running.get();
        if (info.startTraceIfNotAlready()) {
            return;
        }
        try {
            info.lockCreated(lockObj);
        } catch (Throwable ex) {
            handleException(info, "lockCreate", ex);
        } finally {
            info.stopTrace();
        }
    }


    public static void lockUpdateThreadlocalLocationId(Object lockObj, int lockerLocationId) {
        PerThreadData info = running.get();
        if (info.startTraceIfNotAlready()) {
            return;
        }
        try {

            info.lockNewLocationId(lockObj, lockerLocationId);
        } catch (Throwable ex) {
            handleException(info, "lockUpdateThreadlocalLocationId", ex);
        } finally {
            info.stopTrace();
        }
    }

    public static void lockEnter(Object lockObj) {
        PerThreadData info = running.get();
        if (info.startTraceIfNotAlready()) {
            return;
        }
        try {
            if (LOGGER_TRACE_STATIC_ENABLED) {
                log().trace("LockEnter " + lockObj);
            }

            info.lockEnter(lockObj);
        } catch (Throwable ex) {
            handleException(info, "LockEnter", ex);
        } finally {
            info.stopTrace();
        }
    }

    public static void lockExit(Object lockObj) {
        PerThreadData info = running.get();
        if (info.startTraceIfNotAlready()) {
            return;
        }
        try {
            if (LOGGER_TRACE_STATIC_ENABLED) {
                log().trace("LockExit " + lockObj);
            }

            info.lockExit(lockObj);
        } catch (Throwable ex) {
            handleException(info, "lockExit", ex);
        } finally {
            info.stopTrace();
        }
    }

    /*
     * -------------------
     * Intrinsic monitors
     * --------------------
     */

    public static void monitorEnter(Object lockObj, int lockerLocationId) {
        PerThreadData info = running.get();
        if (info.startTraceIfNotAlready()) {
            return;
        }
        try {
            if (LOGGER_TRACE_STATIC_ENABLED) {
                log().trace("MonitorEnter " + lockObj + " locationId: " + lockerLocationId);
            }

            info.monitorEnter(lockObj, lockerLocationId);
        } catch (Throwable ex) {
            handleException(info, "monitorEnter", ex);
        } finally {
            info.stopTrace();
        }
    }

    public static void monitorExit(Object lockObj) {
        PerThreadData info = running.get();
        if (info.startTraceIfNotAlready()) {
            return;
        }
        try {
            if (LOGGER_TRACE_STATIC_ENABLED) {
                log().trace("MonitorExit " + lockObj);
            }

            info.monitorExit(lockObj);
        } catch (Throwable ex) {
            handleException(info, "monitorExit", ex);
        } finally {
            info.stopTrace();
        }
    }

    private static void handleException(PerThreadData info, String method, Throwable ex) {
        if (info != null) {
            info.assignLastExceptionForThisThread(ex);
        }
        log().error("Could not process class " + method + "(). Exception occurred", ex);
    }

}
