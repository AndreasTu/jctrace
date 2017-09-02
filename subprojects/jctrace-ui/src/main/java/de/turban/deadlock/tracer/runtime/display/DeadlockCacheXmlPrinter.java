package de.turban.deadlock.tracer.runtime.display;

import de.turban.deadlock.tracer.runtime.IDeadlockDataResolver;
import de.turban.deadlock.tracer.runtime.ILockCacheEntry;
import de.turban.deadlock.tracer.runtime.ILockStackEntry;
import de.turban.deadlock.tracer.runtime.ILockerThreadCache;
import de.turban.deadlock.tracer.runtime.datacollection.LockerLocationCache;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DeadlockCacheXmlPrinter {

    private final IDeadlockDataResolver resolver;

    private BufferedWriter writer;

    public DeadlockCacheXmlPrinter(IDeadlockDataResolver resolver) {
        this.resolver = resolver;

    }

    public void printLocks(Path file) {
        file = file.toAbsolutePath();
        DeadlockCalculator calc = new DeadlockCalculator(resolver);
        calc.calculateDeadlocks();
        List<ILockCacheEntry> locks = calc.getAllLocksSorted();
        List<ILockCacheEntry> possibleDeadLocksSorted = calc.getPossibleDeadLocks();

        long startTime = System.currentTimeMillis();

        System.out.println("Printing possible DeadLock into XML file: " + file.toFile().getAbsolutePath());
        try (BufferedWriter writer = Files.newBufferedWriter(file,
                Charset.forName("UTF-8"), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            this.writer = writer;
            appendLine("<DeadLockReport>");
            printPossibleDeadLocks(possibleDeadLocksSorted, true);
            appendLine("</DeadLockReport>");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        long stopTime = System.currentTimeMillis();
        System.out.println("DeadLock-XML took " + (stopTime - startTime) + "ms");

        startTime = System.currentTimeMillis();
        Path allLocksFile = file.getParent().resolve(file.toFile().getName() + ".AllLocks.xml");
        System.out.println("Printing all found Locks into XML file: " + allLocksFile.toFile().getAbsolutePath());
        try (BufferedWriter writer = Files.newBufferedWriter(allLocksFile,
                Charset.forName("UTF-8"), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            this.writer = writer;
            appendLine("<DeadLockReport>");
            printMeasuredLocks(locks);
            printPossibleDeadLocks(possibleDeadLocksSorted, false);
            appendLine("</DeadLockReport>");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        stopTime = System.currentTimeMillis();
        System.out.println("AllLock-XML took " + (stopTime - startTime) + "ms");

    }

    private void printPossibleDeadLocks(List<ILockCacheEntry> possibleDeadLocksSorted, boolean filterLockLocations) throws IOException {
        appendLine("<PossibleDeadLocks>");
        for (ILockCacheEntry lock : possibleDeadLocksSorted) {

            if (filterLockLocations) {
                if (lock.getLockerLocationIds().length == 0) {
                    //Filter locks, which have no location
                    continue;
                }
            }

            printLock(lock);
        }

        appendLine("</PossibleDeadLocks>");
    }

    private void printLock(ILockCacheEntry lock) throws IOException {
        appendLine("<Lock>");

        append("<LockId>");
        append(lock.getId());
        appendLine("</LockId>");

        append("<LockClass>");
        appendEsc(lock.getLockClass());
        appendLine("</LockClass>");

        append("<LockCount>");
        append(lock.getLockedCount());
        appendLine("</LockCount>");

        appendLine("<LockerLocations>");
        for (String cls : getLockerLocations(lock)) {
            append("<Location>");
            appendEsc(cls);
            appendLine("</Location>");
        }
        appendLine("</LockerLocations>");

        appendLine("<LockerThreads>");
        for (String thread : getLockerThreadLocations(lock)) {
            append("<LockerThread>");
            appendEsc(thread);
            appendLine("</LockerThread>");
        }
        appendLine("</LockerThreads>");

        printDependentLocks(lock.getDependentLocks(), lock);
        printPossibleDependentDeadLocks(lock.getDependentLocks(), lock);

        printStackTraces(lock);

        appendLine("</Lock>");
    }

    private void printDependentLocks(int[] dependentLocks, ILockCacheEntry lock) throws IOException {
        append("<DependentLocks>");
        int[] dependentLocksSorted = dependentLocks.clone();
        Arrays.sort(dependentLocksSorted);
        boolean first = true;
        for (int lockId : dependentLocksSorted) {
            ILockCacheEntry depLock = resolver.getLockCache().getLockById(lockId);
            if (first) {
                appendLine("");
                first = false;
            }
            append("<DependentLock");
            if (depLock == null) {

                append(" lockNotFound=\"true\"");
                append(">");
                append(lockId);
            } else {
                if (!containsLock(depLock, lock.getId())) {
                    append(" lockNotReferencingThis=\"true\"");
                }
                if (depLock.getLockerLocationIds().length == 0) {
                    append(" lockHasNoLocationInfo=\"true\"");
                }

                append(">");
                append(depLock.getId());

            }

            appendLine("</DependentLock>");
        }

        appendLine("</DependentLocks>");
    }

    private boolean hasLockPossibleDeadLocks(int[] dependentLocks, ILockCacheEntry lock) {
        for (int lockId : dependentLocks) {
            ILockCacheEntry depLock = resolver.getLockCache().getLockById(lockId);

            if (depLock != null) {
                if (!containsLock(depLock, lock.getId())) {
                    continue;
                }
                return true;

            }

        }
        return false;
    }

    private void printPossibleDependentDeadLocks(int[] dependentLocks, ILockCacheEntry lock) throws IOException {
        append("<PossibleDeadLockWithLocks>");
        int[] dependentLocksSorted = dependentLocks.clone();
        Arrays.sort(dependentLocksSorted);
        for (int lockId : dependentLocksSorted) {
            ILockCacheEntry depLock = resolver.getLockCache().getLockById(lockId);

            if (depLock == null) {
                continue;
            } else {
                if (!containsLock(depLock, lock.getId())) {
                    continue;
                }
                appendLine("");
                append("<DependentLock");
                if (depLock.getLockerLocationIds().length == 0) {
                    append(" lockHasNoLocationInfo=\"true\"");
                }

                append(">");
                append(depLock.getId());

            }

            appendLine("</DependentLock>");
        }

        appendLine("</PossibleDeadLockWithLocks>");
    }

    private boolean containsLock(ILockCacheEntry depLock, int idToSearch) {
        return depLock.hasDependentLock(idToSearch);
    }


    private void printMeasuredLocks(List<ILockCacheEntry> locks) throws IOException {
        appendLine("<MeasuredLocks>");
        for (ILockCacheEntry lock : locks) {

            if (lock.getLockerLocationIds().length == 0) {
                //Filter locks, which have no location
                continue;
            }

            appendLine("<Lock>");

            append("<LockId>");
            append(lock.getId());
            appendLine("</LockId>");

            append("<LockClass>");
            appendEsc(lock.getLockClass());
            appendLine("</LockClass>");

            append("<LockCount>");
            append(lock.getLockedCount());
            appendLine("</LockCount>");

            appendLine("<LockerLocations>");
            for (String cls : getLockerLocations(lock)) {
                append("<Location>");
                appendEsc(cls);
                appendLine("</Location>");
            }
            appendLine("</LockerLocations>");

            appendLine("<LockerThreads>");
            for (String thread : getLockerThreadLocations(lock)) {
                append("<LockerThread>");
                appendEsc(thread);
                appendLine("</LockerThread>");
            }
            appendLine("</LockerThreads>");

            printDependentLocks(lock.getDependentLocks(), lock);

            printStackTraces(lock);

            appendLine("</Lock>");
        }
        appendLine("</MeasuredLocks>");
    }

    private void printStackTraces(ILockCacheEntry lock) throws IOException {
        List<ILockStackEntry> stacks = lock.getStackEntries();
        if (!stacks.isEmpty()) {
            appendLine("<Callstacks>");
            appendLine("<PossibleDeadLockCallstacks>");
            for (ILockStackEntry stack : stacks) {
                if (hasLockPossibleDeadLocks(stack.getDependentLocks(), lock)) {
                    printStackEntry(lock, stack);
                }

            }
            appendLine("</PossibleDeadLockCallstacks>");

            appendLine("<AllCallstacks>");
            for (ILockStackEntry stack : stacks) {
                printStackEntry(lock, stack);

            }
            appendLine("</AllCallstacks>");
            appendLine("</Callstacks>");
        }
    }

    private void printStackEntry(ILockCacheEntry lock, ILockStackEntry stack) throws IOException {
        appendLine("<CallstackEntry>");
        int lockerLocationId = stack.getLockerLocationId();
        if (lockerLocationId != LockerLocationCache.INVALID_ID) {
            append("<Location>");
            appendEsc(getLocationById(lockerLocationId).toString());
            appendLine("</Location>");

        }


        int lockerThreadId = stack.getLockerThreadId();
        if (lockerThreadId != LockerLocationCache.INVALID_ID) {
            append("<LockerThread>");
            appendEsc(getThreadById(lockerThreadId));
            appendLine("</LockerThread>");
        }

        printDependentLocks(stack.getDependentLocks(), lock);
        printPossibleDependentDeadLocks(stack.getDependentLocks(), lock);

        append("<Stacktrace>");
        for (StackTraceElement elem : stack.getStackTrace()) {
            if (elem == null) {
                continue;
            }
            appendLine("");
            append("<Frame");


            append(">");
            appendEsc(elem.toString());

            depLockSearch:
            for (int depLockId : stack.getDependentLocks()) {
                ILockCacheEntry depLock = resolver.getLockCache().getLockById(depLockId);
                if (depLock != null) {
                    for (int locId : depLock.getLockerLocationIds()) {
                        StackTraceElement traceElement = getLocationById(locId);
                        if (traceElement != null) {
                            if (elem.getClassName().equals(traceElement.getClassName())) {
                                if (elem.getFileName().equals(traceElement.getFileName())) {
                                    if (elem.getMethodName().equals(traceElement.getMethodName())) {
                                        append("<HoldingLock id=\"" + depLockId + "\"/>");
                                        continue depLockSearch;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            append("</Frame>");

        }
        append("</Stacktrace>");
        appendLine("</CallstackEntry>");
    }

    private void append(String content) throws IOException {
        writer.append(content);
    }

    private void appendEsc(String content) throws IOException {
        writer.append(escapeXml(content));
    }

    private String escapeXml(String content) {
        return content.replace("<", "&lt;").replace(">", "&gt;");
    }

    private void append(int content) throws IOException {
        writer.append(Integer.toString(content));
    }


    private void append(long content) throws IOException {
        writer.append(Long.toString(content));
    }

    private void appendLine(String content) throws IOException {
        writer.append(content);
        writer.append("\r\n");
    }

    private void appendLineEsc(String content) throws IOException {
        writer.append(escapeXml(content));
        writer.append("\r\n");
    }

    private void appendLine(int content) throws IOException {
        writer.append(Integer.toString(content));
        writer.append("\r\n");
    }

    private void appendLine(long content) throws IOException {
        writer.append(Long.toString(content));
        writer.append("\r\n");
    }

    private List<String> getLockerThreadLocations(ILockCacheEntry lock) {
        ILockerThreadCache cache = resolver.getThreadCache();
        int[] lockerThreadIds = lock.getLockerThreadIds();
        List<String> lst = new ArrayList<>(lockerThreadIds.length);

        for (int id : lockerThreadIds) {
            lst.add(cache.getThreadDescriptionById(id));
        }

        Collections.sort(lst);
        return lst;

    }

    private List<String> getLockerLocations(ILockCacheEntry lock) {
        int[] lockerLocationIds = lock.getLockerLocationIds();
        List<String> lst = new ArrayList<>(lockerLocationIds.length);

        for (int id : lockerLocationIds) {
            lst.add(getLocationById(id).toString());
        }

        Collections.sort(lst);
        return lst;
    }

    private StackTraceElement getLocationById(int id) {
        return resolver.getLocationCache().getLocationById(id);
    }

    private String getThreadById(int id) {
        return resolver.getThreadCache().getThreadDescriptionById(id);
    }

}
