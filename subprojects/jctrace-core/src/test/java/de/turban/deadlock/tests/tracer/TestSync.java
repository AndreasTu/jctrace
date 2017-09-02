package de.turban.deadlock.tests.tracer;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;

public class TestSync {

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private static int hookCounter;

    public static synchronized int testStaticInt() {
        return ++hookCounter;
    }


    public synchronized void test2() {

    }

    public void test(int x, List<Integer> args, double d, long y, float f, byte b, byte[][][] s) {
        Number num = null;

        try {
            if (x == 1) {
                num = Double.valueOf("4");
            } else if (x == 3) {
                num = Integer.valueOf(2);
            }
            num = Integer.valueOf(2);
        } finally {
            num.toString();
        }

        num.toString();
    }

    public int test() {
        int x = 1;
        synchronized (this) {

        }

        synchronized (this.getClass()) {
            synchronized (this) {

            }
        }

        Lock l = new ReentrantLock();
        l.lock();
        try {

        } finally {
            l.unlock();
        }

        return x;
    }

    public void testReadWrite() {

        lock.readLock().lock();
        try {

        } finally {
            ReadLock r = lock.readLock();
            r.unlock();

        }

        lock.writeLock().lock();
        try {

        } finally {
            Lock r = lock.writeLock();
            r.unlock();

        }

    }

    //    public int test() {
    //        int x = 1;
    //        synchronized (this) {
    //            syncTest();
    //            syncStaticTest();
    //            conLockTest();
    //            conTryLockTest();
    //            add("adas");
    //            x = 2;
    //        }
    //
    //        synchronized (this.getClass()) {
    //            synchronized (this) {
    //
    //            }
    //        }
    //        return x;
    //    }
    //
    //    public void conLockTest() {
    //
    //        ReadLock readLock = lock.readLock();
    //        readLock.lock();
    //        try {
    //
    //        } finally {
    //            readLock.unlock();
    //        }
    //
    //        WriteLock writeLock = lock.writeLock();
    //        writeLock.lock();
    //        try {
    //
    //        } finally {
    //            writeLock.unlock();
    //        }
    //
    //    }
    //
    //    public void add(String permission) {
    //
    //        if (isReadOnly())
    //            throw new SecurityException("attempt to add a Permission to a readonly PermissionCollection");
    //
    //        synchronized (this) {
    //            conLockTest();
    //        }
    //    }
    //
    //    private boolean isReadOnly() {
    //        return false;
    //    }
    //
    //    public void conTryLockTest() {
    //        System.out.println("tryLock test enter");
    //        ReadLock readLock = lock.readLock();
    //        try {
    //            if (readLock.tryLock(50, TimeUnit.MILLISECONDS)) {
    //                System.out.println("tryLock was successful");
    //                readLock.unlock();
    //            } else {
    //                System.out.println("tryLock has failed");
    //            }
    //        } catch (InterruptedException e) {
    //            // TODO Auto-generated catch block
    //            e.printStackTrace();
    //        }
    //        System.out.println("tryLock test exit");
    //    }
    //
    //    public synchronized void syncTest() {
    //
    //    }
    //
    //    public static synchronized void syncStaticTest() {
    //
    //    }
    //
    //    public void test2() {
    //
    //        synchronized (this) {
    //            try {
    //                DeadlockTracer.monitorEnter(this, TestSync.class);
    //                System.out.println();
    //            } finally {
    //                DeadlockTracer.monitorExit(this, TestSync.class);
    //            }
    //        }
    //
    //    }
    //
    //    public void test3() {
    //        synchronized (this) {
    //            System.out.println();
    //        }
    //    }
    //
    //    public void test4() {
    //        System.out.println();
    //    }

    @Override
    public String toString() {
        synchronized (this) {
            return "TestSync []";
        }
    }

}
