package org.rainyheart.distributed.lock.thridparty.zk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.zookeeper.KeeperException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.Before;
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
public class ZooKeeperManagerTest extends EmbeddedZooKeeperServer {

    @Autowired
    ZooKeeperManager zooKeeperManager;

    @Before
    public void setUp() {
        zooKeeperManager = new ZooKeeperManager();
    }


    @Test
    public void testIsValidHostname() {
        // Valid hostnames
        assertTrue(zooKeeperManager.isValidHostname("localhost"));
        assertTrue(zooKeeperManager.isValidHostname("example.com"));
        assertTrue(zooKeeperManager.isValidHostname("sub.example.com"));
        assertTrue(zooKeeperManager.isValidHostname("192.168.1.1"));

        // Invalid hostnames
        assertFalse(zooKeeperManager.isValidHostname("invalid_host_name"));
        assertFalse(zooKeeperManager.isValidHostname("example..com"));
        assertFalse(zooKeeperManager.isValidHostname("example-.com"));
        assertFalse(zooKeeperManager.isValidHostname("example.com-"));

        assertFalse(zooKeeperManager.isValidHostname("256.256.256.256")); // Invalid IP
        assertFalse(zooKeeperManager.isValidHostname("192.168.1")); // Incomplete IP
        assertFalse(zooKeeperManager.isValidHostname("192.168.1.1.1")); // Too many octets
        assertFalse(zooKeeperManager.isValidHostname("192.168.1."));  // Incomplete IP
        assertFalse(zooKeeperManager.isValidHostname(".192.168.1.1")); // Invalid format
        assertFalse(zooKeeperManager.isValidHostname("my host.com")); // Contains space
        assertFalse(zooKeeperManager.isValidHostname("host#.com")); // Invalid character
        assertFalse(zooKeeperManager.isValidHostname(null)); // Null input
        assertFalse(zooKeeperManager.isValidHostname("")); // Empty input
    }
}
