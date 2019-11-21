package org.rainyheart.distributed.lock.thridparty.zk;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.UnknownHostException;

import org.junit.After;
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
public class DistributedLockApiAppLevelTest extends EmbeddedZooKeeperServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistributedLockApiAppLevelTest.class);

    private static final String TEST_ID = "testId";

    @Autowired
    DistributedLockApi api;

    Lock lock;

    private Thread newThread(long timeout) {
        Thread thread1 = new Thread(new Runnable() {

            @Override
            public void run() {
                lock = new LockImpl(TEST_ID, Thread.currentThread().getName().getBytes(), LockLevel.APPLICATION);
                try {
                    synchronized (DistributedLockApiAppLevelTest.class) {
                        if (!(api.lock(lock, timeout))) {
                            TestExceptionUtils.addException(new AssertionError());
                        }
                        if (!(api.unlock(lock))) {
                            TestExceptionUtils.addException(new AssertionError());
                        }
                    }
                } catch (DistributedLockException e) {
                    LOGGER.error(e.getMessage(), e);
                    TestExceptionUtils.addException(e);
                }
            }
        });
        return thread1;
    }

    @Before
    public void setup() {
        lock = new LockImpl(TEST_ID, Thread.currentThread().getName().getBytes(), LockLevel.APPLICATION);
        TestExceptionUtils.clearException();
    }

    private void startAndWaitFor2Threads(long timeout) throws InterruptedException {
        Thread thread1 = newThread(timeout);
        Thread thread2 = newThread(timeout);

        thread1.start();
        thread2.start();

        while ((thread1.isAlive() || thread2.isAlive()) && timeout >= 0) {
            Thread.sleep(1000);
        }
    }

    @Test
    public void testAppLockWithoutTimeout()
            throws UnknownHostException, InterruptedException, DistributedLockException {

        startAndWaitFor2Threads(0);

        assertFalse(api.unlock(lock));
    }

    @Test
    public void testAppLockWithTimeout() throws UnknownHostException, InterruptedException, DistributedLockException {

        startAndWaitFor2Threads(1000);

        assertFalse(api.unlock(lock));
    }

    @Test
    public void testAppLockWithUnlimitedTimeout()
            throws UnknownHostException, InterruptedException, DistributedLockException {

        startAndWaitFor2Threads(-1);

        assertFalse(api.unlock(lock));
    }

    @Test
    public void testAppTryLock() throws UnknownHostException, DistributedLockException {
        assertTrue(api.tryLock(lock));
        assertFalse(api.tryLock(lock));

        assertTrue(api.unlock(lock));
        assertFalse(api.unlock(lock));
    }
    
    @After
    public void checkException() {
        assertFalse(TestExceptionUtils.hasException());
    }
}
