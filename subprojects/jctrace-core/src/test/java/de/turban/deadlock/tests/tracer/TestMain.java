package de.turban.deadlock.tests.tracer;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TestMain {

    public static void main(String[] args) throws InterruptedException {

        System.out.println("TestStartet");
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
