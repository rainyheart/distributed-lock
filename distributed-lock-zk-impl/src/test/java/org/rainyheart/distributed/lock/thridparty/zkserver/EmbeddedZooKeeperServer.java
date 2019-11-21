package org.rainyheart.distributed.lock.thridparty.zkserver;

import java.io.IOException;

import org.apache.curator.test.TestingServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * All ZK test case should extends this class to start up a ZK server
 * 
 * @author Ken Ye
 *
 */
public class EmbeddedZooKeeperServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddedZooKeeperServer.class);

    private static TestingServer server;

    @BeforeClass
    public static void startZK() throws Exception {
        LOGGER.info("Starting EmbeddedZooKeeperServer");
        server = new TestingServer(2181, true);
        LOGGER.info("Stared EmbeddedZooKeeperServer");
    }

    @AfterClass
    public static void stopZK() throws IOException {
        if (server != null) {
            LOGGER.info("Stopping EmbeddedZooKeeperServer");
            try {
                server.stop();
                server.close();
                LOGGER.info("Stopped EmbeddedZooKeeperServer");
            } finally {
                LOGGER.info("Deleting temp directoty");
                server.getTempDirectory().deleteOnExit();
            }
        }
    }
}
