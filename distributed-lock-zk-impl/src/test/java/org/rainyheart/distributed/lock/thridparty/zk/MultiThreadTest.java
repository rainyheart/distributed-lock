package org.rainyheart.distributed.lock.thridparty.zk;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.rainyheart.distributed.lock.api.DistributedLockApi;
import org.rainyheart.distributed.lock.api.Lock;
import org.rainyheart.distributed.lock.api.LockLevel;
import org.rainyheart.distributed.lock.api.impl.LockImpl;
import org.rainyheart.distributed.lock.thridparty.zk.utils.TestExceptionUtils;
import org.rainyheart.distributed.lock.thridparty.zkserver.EmbeddedZooKeeperServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * You need to update the test-zookeeper.properties to point to a real ZK server
 * for multiple VM test. And also remove the "extends EmbeddedZooKeeperServer"
 * in this class definition
 * 
 * @author Ken Ye
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath*:distributed-lock-spring.xml" })
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MultiThreadTest extends EmbeddedZooKeeperServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiThreadTest.class);

    private static final int THREAD_NUMBER = 30;

    private static final String TEST_ID = "testId";

    @Autowired
    DistributedLockApi api;

    @Autowired
    ZooKeeperManager zkManager;

    Lock lock;

    private Callable<Lock> newCallable(long timeout) {
        Callable<Lock> call = new Callable<Lock>() {
            @Override
            public Lock call() throws Exception {
                Lock lock = new LockImpl(TEST_ID, Thread.currentThread().getName().getBytes(), LockLevel.GLOBAL);
                api.lock(lock, timeout);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    LOGGER.error(e.getMessage(), e);
                }
                if (!api.unlock(lock)) {
                    TestExceptionUtils.addException(new AssertionError());
                }
                return lock;
            }
        };

        return call;
    }

    @Before
    public void setup() throws InterruptedException, ZkServerConnectionException, IOException, KeeperException {
        if (zkManager.getStat(ZkLockConstant.CONFIGURATION_ROOT) == null) {
            System.out.println("It is very probable a maven test !! because the connection may be destroyed !!");
        }
        clearRootPath(ZkLockConstant.APP_REGISTER_ROOT);
        clearRootPath(ZkLockConstant.SERVER_REGISTER_ROOT);
        clearRootPath(ZkLockConstant.DISTRIBUTED_LOCK_ROOT);

        zkManager.createNodeIfNotExist(ZkLockConstant.APP_REGISTER_ROOT, MultiThreadTest.class.getSimpleName().getBytes());
        zkManager.createNodeIfNotExist(ZkLockConstant.SERVER_REGISTER_ROOT,
                MultiThreadTest.class.getSimpleName().getBytes());
        zkManager.createNodeIfNotExist(ZkLockConstant.DISTRIBUTED_LOCK_ROOT,
                MultiThreadTest.class.getSimpleName().getBytes());
        TestExceptionUtils.clearException();
    }

    private void clearRootPath(String rootPath) throws KeeperException, InterruptedException {
        Stat stat = zkManager.getZk().exists(rootPath, false);
        if(stat != null) {
            List<String> children = zkManager.getZk().getChildren(rootPath, false);
            for(String child : children) {
                zkManager.getZk().delete(rootPath + ZkLockConstant.SLASH + child, -1);
            }
            zkManager.getZk().delete(rootPath, -1);
        }
    }

    @After
    public void clear() throws ZkServerConnectionException {
        zkManager.deleteNodeIfExist(ZkLockConstant.APP_REGISTER_ROOT);
        zkManager.deleteNodeIfExist(ZkLockConstant.SERVER_REGISTER_ROOT);
        zkManager.deleteNodeIfExist(ZkLockConstant.DISTRIBUTED_LOCK_ROOT);
    }

    @Test
    public void test() throws UnknownHostException, InterruptedException, ZkServerConnectionException {
        startWorkerThreads(-1);
        Stat stat = zkManager.getStat(ZkLockConstant.DISTRIBUTED_LOCK_ROOT);
        int cversion = stat.getCversion();
        System.out.println(cversion);
        assertTrue(cversion >= THREAD_NUMBER * 2); // create + delete
        // if multiple VM is started, then the number should be <= VM * 2 *
        // THREAD_NUMBER
        assertFalse(TestExceptionUtils.hasException());
    }

    private void startWorkerThreads(long timeout) throws InterruptedException {
        ExecutorService es = Executors.newFixedThreadPool(20);
        List<Callable<Lock>> threads = new ArrayList<>();
        for (int i = 0; i < THREAD_NUMBER; i++) {
            threads.add(newCallable(timeout));
        }

        List<Future<Lock>> resultList = es.invokeAll(threads);
        List<Future<Lock>> doneList = new ArrayList<>();

        while (resultList.size() != doneList.size()) {
            for (Future<Lock> result : resultList) {
                if (result.isDone() && !doneList.contains(result)) {
                    doneList.add(result);
                }
            }
            Thread.sleep(1000);
        }
    }

}
