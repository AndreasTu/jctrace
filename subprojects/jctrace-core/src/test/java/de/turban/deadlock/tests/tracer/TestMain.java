package de.turban.deadlock.tests.tracer;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TestMain {

    private static boolean staticField;

    private boolean field;

    private long longField;

    public static void main(String[] args) throws InterruptedException {

        System.out.println("TestStartet");

        synchronized (new Object()) {

        }

        testFieldAccess();

        /*Class<?> c = TestSync.class;
        TestSync.testStaticInt();
        TestSync sync = new TestSync();
        sync.test(1, null, 0d, 0L, 0f, (byte) 0, null);
        sync.test();
        sync.test2();
        for (int x = 0; x < 2; x++) {
            sync.testReadWrite();
        }
        //sync.testReadWrite();
        //de.turban.deadlock.tracer.DeadlockGlobalCache.INSTANCE.printLocks();
        */

        //reentrantReadWriteLocks();

    }

    private static void testFieldAccess() {
        TestMain t = new TestMain();
        t.field = true;
        t.field = false;
        if (new TestMain().field) {

        }

        new TestMain().longField = 1;

        if (new TestMain().longField == 1) {

        }

        staticField = true;
        if (staticField) {

        }
    }

    private static void reentrantReadWriteLocks() throws InterruptedException {
        ReentrantReadWriteLock l = new ReentrantReadWriteLock();
        Thread.sleep(1000);
        ReentrantReadWriteLock l2 = new ReentrantReadWriteLock();

        l.readLock().lock();
        try {

            l2.readLock().lock();
            try {

            } finally {
                l2.readLock().unlock();
            }

        } finally {
            l.readLock().unlock();
        }


        l2.readLock().lock();
        try {

            l.readLock().lock();
            try {

            } finally {
                l.readLock().unlock();
            }

        } finally {
            l2.readLock().unlock();
        }
    }


}
