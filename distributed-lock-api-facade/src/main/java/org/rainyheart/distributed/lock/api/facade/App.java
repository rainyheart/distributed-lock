package org.rainyheart.distributed.lock.api.facade;

import org.springframework.util.StringUtils;

import org.rainyheart.distributed.lock.api.DistributedLockApi;
import org.rainyheart.distributed.lock.api.Lock;
import org.rainyheart.distributed.lock.api.LockLevel;
import org.rainyheart.distributed.lock.api.exception.DistributedLockException;
import org.rainyheart.distributed.lock.api.impl.DistributedLockApiImpl;
import org.rainyheart.distributed.lock.api.impl.LockImpl;
import org.rainyheart.distributed.lock.thridparty.zk.ZkServerConnectionException;
import org.rainyheart.distributed.lock.thridparty.zk.ZooKeeperManager;

/**
 * @author Ken Ye
 */
public class App {
    private static final String UNLOCK_CMD = "unlock";
    private static final String LOCK_CMD = "lock";
    private static DistributedLockApi api = null;

    public static void main(String[] args) throws DistributedLockException, NumberFormatException, InterruptedException {
        registerShutdownHook();

        initializeDistributedLockApi();

        Lock lock = newLockImpl(args);
        String cmd = args[0];
        switch (cmd) {
        case LOCK_CMD:
            String timeout = args[2];
            boolean lockSuccess = api.lock(lock, Long.valueOf(timeout) * 1000);
            if (lockSuccess) {
                System.out.println("Lock: " + lock.id() + " is obtained!!");
                exit0();
            } else {
                System.out.println("Fail to Lock: " + lock.id() + " in " + timeout + " seconds!!");
                exit1();
            }
            break;
        case UNLOCK_CMD:
            boolean unlockSuccess = api.unlock(lock);
            if (unlockSuccess) {
                System.out.println("Lock: " + lock.id() + " is released!!");
            } else {
                System.out.println("Fail to release Lock: " + lock.id());
            }
            break;
        default:
            System.out.println("Unknown command : " + cmd);
            break;
        }
    }

    private static void exit0() {
        System.exit(0);
    }

    private static void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                if (App.api == null) {
                    return;
                }
                ZooKeeperManager zkMgr = (ZooKeeperManager) ((DistributedLockApiImpl) App.api).getManager();
                try {
                    if (zkMgr != null) {
                        zkMgr.destroy();
                        System.out.println("Zookeeper Manager is destryed!");
                    }

                } catch (ZkServerConnectionException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private static Lock newLockImpl(String[] args) {
        checkArgs(args);
        String lockKey = args[1];
        Lock lock = new LockImpl(lockKey, null, LockLevel.APPLICATION, 1);
        return lock;
    }

    private static void checkArgs(String[] args) {
        if (args == null) {
            System.err.println("Invalid arguments : " + args);
            exit1();
        } else {
            String cmd = args[0];
            switch (cmd) {
            case LOCK_CMD:
                if (args.length != 3) {
                    System.err.println("Invalid lock arguments : " + args);
                    printArgs(args);
                    exit1();
                }
                break;
            case UNLOCK_CMD:
                if (args.length != 2) {
                    System.err.println("Invalid unlock arguments : ");
                    printArgs(args);
                    exit1();
                }
                break;
            default:
                System.err.println("Invalid arguments : " + args);
                exit1();
            }
        }
    }

    private static void printArgs(String[] args) {
        for (String arg : args) {
            System.out.println(arg);
        }
    }

    private static void exit1() {
        System.exit(1);
    }

    private static void initializeDistributedLockApi() throws ZkServerConnectionException {
        checkEnvVars("appName", "hostPort", "sessionTimeout", "adminAuth");

        String appName = System.getenv("appName");
        String hostPort = System.getenv("hostPort");
        String sessionTimeout = System.getenv("sessionTimeout");
        String adminAuth = System.getenv("adminAuth");

        ZooKeeperManager zkMgr = new ZooKeeperManager();
        zkMgr.setAdminAuth(adminAuth);
        zkMgr.setAppName(appName);
        zkMgr.setHostPort(hostPort);
        zkMgr.setSessionTimeout(sessionTimeout);
        zkMgr.init();
        DistributedLockApiImpl api = new DistributedLockApiImpl();
        api.setManager(zkMgr);
        App.api = api;
        System.out.println("DistributedLockApi initialized completed!");
    }

    private static void checkEnvVars(String... envVars) {
        for (String envVarKey : envVars) {
            String envVar = System.getenv(envVarKey);
            if (StringUtils.isEmpty(envVar)) {
                System.err.println("Empty environment variable " + envVarKey);
                exit1();
            }
        }
    }
}
