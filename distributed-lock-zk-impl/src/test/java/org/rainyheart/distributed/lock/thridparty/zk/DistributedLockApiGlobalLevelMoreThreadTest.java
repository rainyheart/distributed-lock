package org.rainyheart.distributed.lock.thridparty.zk;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.rainyheart.distributed.lock.api.DistributedLockApi;
import org.rainyheart.distributed.lock.api.Lock;
import org.rainyheart.distributed.lock.api.LockLevel;
import org.rainyheart.distributed.lock.api.exception.DistributedLockException;
import org.rainyheart.distributed.lock.api.impl.LockImpl;
import org.rainyheart.distributed.lock.thridparty.zk.utils.TestExceptionUtils;
import org.rainyheart.distributed.lock.thridparty.zkserver.EmbeddedZooKeeperServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath*:distributed-lock-spring.xml" })
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DistributedLockApiGlobalLevelMoreThreadTest extends EmbeddedZooKeeperServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistributedLockApiGlobalLevelMoreThreadTest.class);

    private static final String TEST_ID = "testId";

    public static AtomicInteger count = new AtomicInteger(0);
    
    @Autowired
    DistributedLockApi api;

    Lock lock;

    @Before
    public void setup() {
        lock = new LockImpl(TEST_ID, Thread.currentThread().getName().getBytes(), LockLevel.GLOBAL);
        TestExceptionUtils.clearException();
        DistributedLockApiGlobalLevelMoreThreadTest.count.set(0);
    }

    @Test
    public void testGlobalLockWithTimeout()
            throws UnknownHostException, InterruptedException, DistributedLockException {

        startAndWaitFor5Threads(120000);

        int total = DistributedLockApiGlobalLevelMoreThreadTest.count.intValue();
        Assert.assertTrue(total >= 1 && total <= 5);
        assertFalse(api.unlock(lock));

        assertFalse(TestExceptionUtils.hasException());
    }
    
    @Test
    public void testGlobalLockWithUnlimitedTimeout()
            throws UnknownHostException, InterruptedException, DistributedLockException {

        startAndWaitFor5Threads(-1);

        int total = DistributedLockApiGlobalLevelMoreThreadTest.count.intValue();
        Assert.assertEquals(5, total);
        assertFalse(api.unlock(lock));
        assertFalse(TestExceptionUtils.hasException());
    }

    @Test
    public void testGlobalLockWithoutTimeout()
            throws UnknownHostException, InterruptedException, DistributedLockException {

        startAndWaitFor5Threads(0);

        int total = DistributedLockApiGlobalLevelMoreThreadTest.count.intValue();
        Assert.assertEquals(1, total);
        assertFalse(api.unlock(lock));

        assertTrue(TestExceptionUtils.hasException());
    }

    private void startAndWaitFor5Threads(long timeout) throws InterruptedException {
        Thread thread1 = newThread(timeout);
        Thread thread2 = newThread(timeout);
        Thread thread3 = newThread(timeout);
        Thread thread4 = newThread(timeout);
        Thread thread5 = newThread(timeout);

        thread1.start();
        thread2.start();
        thread3.start();
        thread4.start();
        thread5.start();
        
        while (isAlive(thread1, thread2, thread3, thread4, thread5)) {
            Thread.sleep(1000);
        }
    }

    private boolean isAlive(Thread... threads) {
        for (Thread thread : threads) {
            if(thread.isAlive()) {
                return true;
            }
        }
        return false;
    }

    private Thread newThread(long timeout) {
        Thread thread1 = new Thread(new Runnable() {

            @Override
            public void run() {
                lock = new LockImpl(TEST_ID, Thread.currentThread().getName().getBytes(), LockLevel.GLOBAL);
                try {
                    if (!(api.lock(lock, timeout))) {
                        TestExceptionUtils.addException(new AssertionError());
                        return;
                    }
                    Thread.sleep(1000);
                    DistributedLockApiGlobalLevelMoreThreadTest.count.incrementAndGet();
                } catch (DistributedLockException | InterruptedException e) {
                    LOGGER.error(e.getMessage(), e);
                    TestExceptionUtils.addException(e);
                } finally {
                    try {
                        if (!(api.unlock(lock))) {
                            TestExceptionUtils.addException(new AssertionError());
                        }
                    } catch (DistributedLockException e) {
                        LOGGER.error(e.getMessage(), e);
                        TestExceptionUtils.addException(e);
                    }
                }
            }
        });
        return thread1;
    }
    
}
