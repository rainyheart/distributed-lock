package org.rainyheart.distributed.lock.thridparty.zk;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({ DistributedLockApiAppLevelTest.class, DistributedLockApiGlobalLevelTest.class,
        DistributedLockApiServerLevelTest.class })
public class DistributedLockApiTestSuite {

}
