package org.rainyheart.distributed.lock.thridparty.zk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.zookeeper.KeeperException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.rainyheart.distributed.lock.api.Lock;
import org.rainyheart.distributed.lock.api.LockLevel;
import org.rainyheart.distributed.lock.api.impl.LockImpl;
import org.rainyheart.distributed.lock.thridparty.zkserver.EmbeddedZooKeeperServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath*:distributed-lock-spring.xml" })
@ActiveProfiles(value="default")
public class ZookeeperManagerTest extends EmbeddedZooKeeperServer {

    @Autowired
    ZooKeeperManager zooKeeperManager;

    @Test
    public void test() throws UnknownHostException, ZkServerConnectionException {
        if (zooKeeperManager.getStat(ZkLockConstant.CONFIGURATION_ROOT) == null) {
            System.out.println("It is very probable a maven test !! because the connection may be destroyed !!");
        }

        byte[] result = zooKeeperManager.getData(zooKeeperManager.getServerLockRootPath());
        assertEquals(InetAddress.getLocalHost().getHostName(), new String(result));

        result = zooKeeperManager
                .getData(ZkLockConstant.CONFIGURATION_ROOT + ZkLockConstant.SLASH + ZkLockConstant.DEBUG);
        String debug = new String(result);
        assertTrue("true".equalsIgnoreCase(debug) || "false".equalsIgnoreCase(debug));

        result = zooKeeperManager.getData(zooKeeperManager.getGlobalLockRootPath());
        assertEquals(ZooKeeperManager.class.getSimpleName(), new String(result));

        result = zooKeeperManager.getData(zooKeeperManager.getAppLockRootPath());
        assertEquals(zooKeeperManager.getAppName(), new String(result));
    }

    @Test
    public void testWorstCaseApp() throws ZkServerConnectionException, InterruptedException, KeeperException {
        Lock lock = new LockImpl("testId", "".getBytes(), LockLevel.APPLICATION);
        zooKeeperManager.lock(lock, -1);
        zooKeeperManager.unlock(lock);

        // simulate some hacker hack into the server and delete the root path
        zooKeeperManager.getZk().delete(zooKeeperManager.getAppLockRootPath(), -1);
        zooKeeperManager.getZk().delete(ZkLockConstant.APP_REGISTER_ROOT, -1);

        zooKeeperManager.lock(lock, -1);
        zooKeeperManager.unlock(lock);
    }

    @Test
    public void testWorstCaseServer() throws ZkServerConnectionException, InterruptedException, KeeperException {
        Lock lock = new LockImpl("testId", "".getBytes(), LockLevel.SERVER);
        zooKeeperManager.lock(lock, -1);
        zooKeeperManager.unlock(lock);

        // simulate some hacker hack into the server and delete the root path
        zooKeeperManager.getZk().delete(zooKeeperManager.getServerLockRootPath(), -1);
        zooKeeperManager.getZk().delete(ZkLockConstant.SERVER_REGISTER_ROOT, -1);

        zooKeeperManager.lock(lock, -1);
        zooKeeperManager.unlock(lock);
    }

    @Test
    public void testWorstCaseGlobal() throws ZkServerConnectionException, InterruptedException, KeeperException {
        Lock lock = new LockImpl("testId", "".getBytes(), LockLevel.GLOBAL);
        zooKeeperManager.lock(lock, -1);
        zooKeeperManager.unlock(lock);

        // simulate some hacker hack into the server and delete the root path
        zooKeeperManager.getZk().delete(zooKeeperManager.getGlobalLockRootPath(), -1);

        zooKeeperManager.lock(lock, -1);
        zooKeeperManager.unlock(lock);
    }
}
