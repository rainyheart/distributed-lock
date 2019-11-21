package org.rainyheart.distributed.lock.thridparty.zk;

import java.util.List;

import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.ACL;

public class ZkLockConstant {
    private ZkLockConstant() {
        super();
    }
    public static final String SLASH = "/";
    public static final String SERVER_REGISTER_ROOT = "/server";
    public static final String CONFIGURATION_ROOT = "/config";
    public static final String APP_REGISTER_ROOT = "/application";
    public static final String DISTRIBUTED_LOCK_ROOT = "/distributed_lock";
    
    protected static final List<ACL> GLOBAL_ACL = Ids.OPEN_ACL_UNSAFE;

    public static final String DEBUG = "debug";
    public static final String ZK_SCHEME = "digest";
    public static final String ENV_DISTRIBUTED_LOCK_THREAD_INTERVAL = "distributed_lock_thread_interval";
}
