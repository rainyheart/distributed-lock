package org.rainyheart.distributed.lock.thridparty.zk;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooKeeper.States;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.apache.zookeeper.data.Stat;
import org.rainyheart.distributed.lock.api.DistributedLockManager;
import org.rainyheart.distributed.lock.api.Lock;
import org.rainyheart.distributed.lock.thridparty.zk.utils.ZkPasswordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * The ZooKeeper Manager class.
 * 
 * To turn on the debug mode, you need to do so: 1. Use a zkCli to connect to ZK
 * server 2. set /config/debug "true"
 * 
 * @author Ken Ye
 *
 */
@Component
public class ZooKeeperManager implements DistributedLockManager {

    private static final String TRUE = "true";
    private static final String MSG_NON_ZERO_COUNT_DOWN_FOR_ZOOKEEPER_CONNECTION_THREAD = "Non-zero count down for zookeeper connection thread";
    private static final String MSG_FAIL_TO_GENERATE_USER_PASSWORD = "Fail to generate User Password!";
    private static final String MSG_FAIL_TO_REBUILD_ZK_CLIENT_FOR_EVENT = "Fail to rebuild Zk Client for event: ";
    private static final String MSG_ZK_REBUILD_SUCCESSFULLY = "Zk rebuild successfully ...";
    private static final String MSG_ZK_SESSION_EXPIRED_NOW_REBUILDING = "Zk session expired. now rebuilding...";
    private static final String COMMER = ", ";
    private static final String MSG_UNEXPECTED_EXCEPTION = "Unexpected Exception!";
    private static final String MSG_UNHANDLED_KEEPER_EXCEPTION_IS_FOUND = "Unhandled KeeperException is found!";
    private static final String MSG_NODE_EXISTS = "NodeExists!";
    private static final String MSG_UNEXPECTED_NO_AUTH_EXCEPTION_IS_FOUND_FOR_PATH = "Unexpected NoAuth exception is found for path: ";
    private static final String MSG_RECREATED_THE_ROOT_ZNODE_FOR_PATH = "Recreated the root znode for path: ";
    private static final String MSG_ZK_MAY_BE_HACKED = "A root znode was deleted unexpected!! ZK may be hacked!!";
    private static final String MSG_TRYING_TO_RECONNECT_TO_ZOO_KEEPER_SERVER = "Trying to reconnect to ZooKeeper Server";
    private static final String MSG_ZOO_KEEPER_CONNECTION_SESSION_IS_BROKEN = "ZooKeeper Connection/Session is broken!!!";
    private static final String MSG_UNABLE_TO_DELETE_ZNODE = "Unable to delete Znode: ";
    private static final String HOSTNAME_PATTERN = "^([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])(\\.([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9]))*$";

    private static final Logger LOGGER = LoggerFactory.getLogger(ZooKeeperManager.class);

    @Value("#{zkProperties['hostPort']}")
    private String hostPort;
    @Value("#{zkProperties['sessionTimeout']}")
    private int sessionTimeout;
    @Value("#{zkProperties['appName']}")
    private String appName;
    @Value("#{zkProperties['clientConnectCount']}")
    private int clientConnectCount;
    @Value("#{zkProperties['adminAuth']}")
    private String adminAuth;

    private long interval;

    private ZooKeeper zk;
    private List<ACL> acls;

    private String ipAddress;
    /**
     * To turn on the debug mode, you need to do so: 1. Use a zkCli to connect to ZK
     * server 2. set /config/debug "true"
     */
    private boolean debug = false;

    /**
     * A unique uuid for current thread to protect lock() and unlock()
     */
    private ThreadLocal<byte[]> threadUuidBytesHolder = new ThreadLocal<>();
    private ThreadLocal<Integer> lockMode = new ThreadLocal<>();
    private ThreadLocal<Long> startTime = new ThreadLocal<>();
    private ThreadLocal<AtomicLong> timeToWait = new ThreadLocal<>();

    public ZooKeeperManager() {
        super();
        String envInteval = System.getenv(ZkLockConstant.ENV_DISTRIBUTED_LOCK_THREAD_INTERVAL);
        if (StringUtils.isEmpty(envInteval)) {
            this.interval = 500l;
        } else {
            try {
                this.interval = Long.parseLong(envInteval);
            } catch (Exception e) {
                sysoutOrLog("Invalid environment variable " + ZkLockConstant.ENV_DISTRIBUTED_LOCK_THREAD_INTERVAL + ": "
                        + envInteval);
                this.interval = 500l;
            }
        }
    }

    private String doCreateZnode(Lock lock, String path, CreateMode mode) throws KeeperException, InterruptedException {
        this.initThreadUuidBytes();

        synchronized (lock) {
            return zk.create(path, threadUuidBytesHolder.get(), ZkLockConstant.GLOBAL_ACL, mode);
        }
    }

    private boolean doDeleteZnode(String path) throws InterruptedException, KeeperException {
        boolean success = false;
        byte[] lockUuidBytes = null;
        try {
            lockUuidBytes = this.getData(path);
        } catch (ZkServerConnectionException e) {
            printOrLogError(MSG_UNABLE_TO_DELETE_ZNODE + path, e);
        }
        if (isPersistentLockMode() || (isNotPersistentLockMode() && isLockOwnedByThisThread(lockUuidBytes))) {
            zk.delete(path, -1);
            success = true;
        }
        return success;
    }

    private boolean isPersistentLockMode() {
        return !isNotPersistentLockMode();
    }

    private boolean isNotPersistentLockMode() {
        return this.lockMode.get() != null && this.lockMode.get().intValue() != 1;
    }

    private boolean existZnodeOrLeaveWatcher(Lock lock, String path) throws ZkServerConnectionException {
        Stat stat = null;
        try {
            stat = zk.exists(path, new Watcher() {
                @Override
                public void process(WatchedEvent event) {
                    if (event.getType() == EventType.NodeDeleted) {
                        synchronized (lock) {
                            lock.notifyAll();
                        }
                    } else {
                        try {
                            zk.exists(path, true);
                        } catch (KeeperException | InterruptedException e) {
                            try {
                                handleException(path, e);
                            } catch (ZkServerConnectionException e1) {
                                printOrLogError(path + COMMER + e1.getLocalizedMessage(), e1);
                            }
                        }
                    }
                }
            });
        } catch (KeeperException | InterruptedException e) {
            handleException(path, e);
        }
        return stat != null;
    }

    private void handleException(String path, Exception e) throws ZkServerConnectionException {
        if (e instanceof KeeperException) {
            handleKeeperException(path, e);
        } else if (e instanceof InterruptedException) {
            printOrLogError(MSG_UNEXPECTED_EXCEPTION, e);
            Thread.currentThread().interrupt();
        }

        if (debug) {
            if (e instanceof KeeperException) {
                printOrLogError(path + COMMER + e.getLocalizedMessage() + "," + ((KeeperException) e).code(), e);
            } else {
                printOrLogError(MSG_UNEXPECTED_EXCEPTION, e);
            }
        } else {
            if (e instanceof KeeperException) {
                Code code = ((KeeperException) e).code();
                // if it is node exists exception, no need to print logs because it is a very
                // normal case to fail to get lock
                if (!code.equals(Code.NODEEXISTS)) {
                    printOrLogError(path + COMMER + e.getLocalizedMessage(), e);
                }
            } else {
                printOrLogError(MSG_UNEXPECTED_EXCEPTION, e);
            }
        }
    }

    private void handleKeeperException(String path, Exception e) throws ZkServerConnectionException {
        KeeperException ke = (KeeperException) e;
        if (isConnectionOrSessionIssue(ke)) {
            printOrLog(MSG_ZOO_KEEPER_CONNECTION_SESSION_IS_BROKEN, MSG_TRYING_TO_RECONNECT_TO_ZOO_KEEPER_SERVER, ke);
            int retry = 0;
            synchronized (this) {
                while (retry < this.clientConnectCount && this.zk.getState() != States.CONNECTED) {
                    this.init();
                    retry++;
                }
            }
        } else if (isNoNodeIssue(ke)) {
            try {
                syserrOrLog(MSG_ZK_MAY_BE_HACKED);
                createZnodeAndRegister(path);
                sysoutOrLog(MSG_RECREATED_THE_ROOT_ZNODE_FOR_PATH + path);
            } catch (InterruptedException | KeeperException e1) {
                handleException(path, e1);
            }
        } else if (isNoAuthIssue(ke)) {
            printOrLogError(MSG_UNEXPECTED_NO_AUTH_EXCEPTION_IS_FOUND_FOR_PATH + path, ke);
            initZkAuth();
        } else if (isNodeExistsIssue(ke)) {
            printOrLogWarn(MSG_NODE_EXISTS + path, ke);
        } else {
            printOrLogError(MSG_UNHANDLED_KEEPER_EXCEPTION_IS_FOUND, e);
        }
    }

    private void createZnodeAndRegister(String path)
            throws ZkServerConnectionException, KeeperException, InterruptedException {
        if (path.startsWith(getServerLockRootPath())) {
            createNodeIfNotExist(ZkLockConstant.SERVER_REGISTER_ROOT, getClassNameBytes());
            registerServerAddress();
        } else if (path.startsWith(getAppLockRootPath())) {
            createNodeIfNotExist(ZkLockConstant.APP_REGISTER_ROOT, getClassNameBytes());
            registerApp();
        } else if (path.startsWith(getGlobalLockRootPath())) {
            createNodeIfNotExist(ZkLockConstant.DISTRIBUTED_LOCK_ROOT, getClassNameBytes());
        }
    }

    private void initThreadUuidBytes() {
        if (this.threadUuidBytesHolder.get() == null) {
            UUID uuid = UUID.randomUUID();
            this.threadUuidBytesHolder.set(uuid.toString().getBytes());
        }
    }

    /**
     * This is the core lock method
     * 
     * @param lock
     * @param path
     * @param timeout
     * @return boolean
     * @throws ZkServerConnectionException
     */
    private boolean lock(Lock lock, String path, long timeout) throws ZkServerConnectionException {
        String result = null;
        this.startTime.set(Long.valueOf(System.currentTimeMillis()));
        this.timeToWait.set(new AtomicLong(timeout));
        try {
            result = doCreateZnode(lock, path, determinMode(lock.mode()));
        } catch (KeeperException | InterruptedException e) {
            handleException(path, e);

            if (timeout == 0) { // no timeout then return immediately
                return false;
            } else {
                result = waitAndGetZnode(lock, path, timeout);
            }
        } finally {
            this.timeToWait.remove();
            this.startTime.remove();
        }
        return result != null;
    }

    private boolean unlockByPath(String path) throws ZkServerConnectionException {
        return deleteNodeIfExist(path);
    }

    private void wait(Lock lock) throws ZkServerConnectionException {
        synchronized (lock) {
            try {
                lock.wait(interval);
            } catch (InterruptedException e) {
                handleException("_wait", e);
            } finally {
                long timePassed = System.currentTimeMillis() - this.startTime.get();
                this.timeToWait.get().addAndGet(Math.negateExact(timePassed)); // timeToWait = timeToWait - timePassed
            }
        }
    }

    private String waitAndGetZnode(Lock lock, String path, long timeout) throws ZkServerConnectionException {
        boolean existZnode = existZnodeOrLeaveWatcher(lock, path);
        String znodePath = null;
        if (timeout > 0) {
            if (!existZnode) {
                znodePath = createZnode(lock, path); // try to create it since it does not exist
            }
            if (znodePath == null) { // maybe another guy get the lock by this gap...
                while (this.timeToWait.get().longValue() >= 0 && znodePath == null) {
                    wait(lock);
                    znodePath = createZnode(lock, path); // give the girl up if you still can't create it
                }
            }
        } else { // timeout < 0, wait for the girl forever until you get her
            while (znodePath == null) {
                znodePath = createZnode(lock, path);
                if (znodePath == null) {
                    wait(lock);
                }
            }
        }
        return znodePath;
    }

    @Override
    public boolean appLock(Lock lock, long timeout) throws ZkServerConnectionException {
        return lock(lock, getAppLockPath(lock.id()), timeout);
    }

    @Override
    public boolean appUnlock(String id) throws ZkServerConnectionException {
        return unlockByPath(getAppLockPath(id));
    }

    protected boolean createNodeIfNotExist(String path, byte[] data)
            throws InterruptedException, ZkServerConnectionException {
        String createdValue = null;
        Stat stat = null;
        try {
            stat = zk.exists(path, false);
        } catch (KeeperException e) {
            handleException(path, e);
        }
        if (stat == null) {
            try {
                createdValue = zk.create(path, data, acls, CreateMode.PERSISTENT);
            } catch (KeeperException e) {
                handleException(path, e);
            }
        }
        return createdValue != null;
    }

    private String createZnode(Lock lock, String path) throws ZkServerConnectionException {
        String znodePath = null;
        try {
            znodePath = doCreateZnode(lock, path, determinMode(lock.mode()));
        } catch (KeeperException | InterruptedException e1) {
            handleException(path, e1);
        }
        return znodePath;
    }

    /**
     * If mode == 1, then return CreateMode.PERSISTENT, else return
     * CreateMode.EPHEMERAL
     * 
     * @param mode
     * @return CreateMode
     */
    private CreateMode determinMode(Integer mode) {
        if (mode != null && mode.intValue() == 1) {
            return CreateMode.PERSISTENT;
        } else {
            return CreateMode.EPHEMERAL;
        }
    }

    protected boolean deleteNodeIfExist(String path) throws ZkServerConnectionException {
        boolean success = false;
        try {
            success = doDeleteZnode(path);
        } catch (KeeperException | InterruptedException e) {
            handleException(path, e);
        }
        return success;
    }

    @PreDestroy
    public void destroy() throws ZkServerConnectionException {
        if (this.zk != null && this.zk.getState() == States.CONNECTED) {
            try {
                this.zk.close();
            } catch (InterruptedException e) {
                handleException("ZooKeeperManager.destry()", e);
            } finally {
                this.threadUuidBytesHolder.remove();
                this.lockMode.remove();
                this.startTime.remove();
                this.timeToWait.remove();
            }
        }
    }

    protected String getAppLockRootPath() {
        return ZkLockConstant.APP_REGISTER_ROOT + ZkLockConstant.SLASH + appName;
    }

    public String getAppName() {
        return this.appName;
    }

    private String getAppLockPath(String id) {
        return getAppLockRootPath() + ZkLockConstant.SLASH + id;
    }

    private byte[] getClassNameBytes() {
        return this.getClass().getSimpleName().getBytes();
    }

    private String getConfigPath(String id) {
        return ZkLockConstant.CONFIGURATION_ROOT + ZkLockConstant.SLASH + id;
    }

    public byte[] getData(String path) throws ZkServerConnectionException {
        byte[] result = null;
        try {
            result = this.zk.getData(path, false, null);
        } catch (KeeperException | InterruptedException e) {
            handleException(path, e);
        }

        return result;
    }

    private String getGlobalLockPath(String id) {
        return getGlobalLockRootPath() + ZkLockConstant.SLASH + id;
    }

    protected String getGlobalLockRootPath() {
        return ZkLockConstant.DISTRIBUTED_LOCK_ROOT;
    }

    public String getHostPort() {
        return this.hostPort;
    }

    protected String getServerLockRootPath() {
        return ZkLockConstant.SERVER_REGISTER_ROOT + ZkLockConstant.SLASH + ipAddress;
    }

    protected String getServerLockPath(String id) {
        return getServerLockRootPath() + ZkLockConstant.SLASH + id;
    }

    public int getSessionTimeout() {
        return this.sessionTimeout;
    }

    public Stat getStat(String path) throws ZkServerConnectionException {
        Stat stat = null;
        try {
            stat = this.zk.exists(path, false);
        } catch (KeeperException | InterruptedException e) {
            handleException(path, e);
        }

        return stat;
    }

    protected ZooKeeper getZk() {
        return this.zk;
    }

    @Override
    public boolean globalLock(Lock lock, long timeout) throws ZkServerConnectionException {
        return lock(lock, getGlobalLockPath(lock.id()), timeout);
    }

    @Override
    public boolean globalUnlock(String id) throws ZkServerConnectionException {
        return unlockByPath(getGlobalLockPath(id));
    }

    @PostConstruct
    public void init() throws ZkServerConnectionException {
        try {
            if (this.zk != null) {
                this.zk.close();
                this.zk = null;
            }
            CountDownLatch connectedLatch = new CountDownLatch(1);
            this.zk = new ZooKeeper(this.hostPort, this.sessionTimeout, createZkSessionWatcher(connectedLatch));
            if (States.CONNECTING == this.zk.getState()) {
                boolean zeroCount = connectedLatch.await(10, TimeUnit.SECONDS);
                if (!zeroCount && LOGGER != null) {
                    LOGGER.error(MSG_NON_ZERO_COUNT_DOWN_FOR_ZOOKEEPER_CONNECTION_THREAD);
                } else {
                    syserr(MSG_NON_ZERO_COUNT_DOWN_FOR_ZOOKEEPER_CONNECTION_THREAD);
                }

                if (connectedLatch.getCount() != 0) {
                    throw new ZkServerConnectionException();
                }
            }

            initZkAuth();
            initACLs();
            initRootNodes();
            initConfiguration();

            registerServerAddress();
            registerApp();
        } catch (IOException | InterruptedException e) {
            handleException("ZooKeeperManager.init()", e);
        }
    }

    private void initZkAuth() {
        this.zk.addAuthInfo(ZkLockConstant.ZK_SCHEME, this.adminAuth.getBytes());
    }

    private Watcher createZkSessionWatcher(CountDownLatch connectedLatch) {
        return new Watcher() {
            @Override
            public void process(WatchedEvent event) {
                if (event.getState() == KeeperState.SyncConnected) {
                    connectedLatch.countDown();
                } else if (event.getState() == KeeperState.Expired) {
                    syserrOrLog(MSG_ZK_SESSION_EXPIRED_NOW_REBUILDING);
                    try {
                        init();
                        connectedLatch.countDown();
                        sysoutOrLog(MSG_ZK_REBUILD_SUCCESSFULLY);
                    } catch (ZkServerConnectionException e) {
                        printOrLogError(MSG_FAIL_TO_REBUILD_ZK_CLIENT_FOR_EVENT + event.toString(), e);
                    }
                }
            }
        };
    }

    private void initACLs() throws ZkServerConnectionException {
        this.acls = new ArrayList<>(1);

        Id superUser = null;
        try {
            superUser = new Id(ZkLockConstant.ZK_SCHEME, ZkPasswordUtils.getDigestUserPwd(adminAuth));
        } catch (NoSuchAlgorithmException e) {
            throw new ZkServerConnectionException(MSG_FAIL_TO_GENERATE_USER_PASSWORD, e);
        }
        ACL acl = new ACL(ZooDefs.Perms.ALL, superUser);
        this.acls.add(acl);
    }

    private void initConfiguration() throws ZkServerConnectionException {
        Stat stat = null;
        try {
            stat = registerDebugWatcher();
        } catch (KeeperException | InterruptedException e) {
            handleException(ZkLockConstant.DEBUG, e);
        }

        if (stat == null) {
            try {
                this.zk.create(getConfigPath(ZkLockConstant.DEBUG), "false".getBytes(), ZkLockConstant.GLOBAL_ACL,
                        CreateMode.PERSISTENT);
            } catch (KeeperException | InterruptedException e) {
                handleException(ZkLockConstant.DEBUG, e);
            }
        } else {
            byte[] data = null;
            try {
                data = this.zk.getData(getConfigPath(ZkLockConstant.DEBUG), null, null);
            } catch (KeeperException | InterruptedException e) {
                handleException(ZkLockConstant.DEBUG, e);
            }
            this.debug = TRUE.equalsIgnoreCase(new String(data));
        }
    }

    private void initRootNodes() throws InterruptedException, ZkServerConnectionException {
        createNodeIfNotExist(ZkLockConstant.CONFIGURATION_ROOT, getClassNameBytes());
        createNodeIfNotExist(ZkLockConstant.SERVER_REGISTER_ROOT, getClassNameBytes());
        createNodeIfNotExist(ZkLockConstant.APP_REGISTER_ROOT, getClassNameBytes());
        createNodeIfNotExist(ZkLockConstant.DISTRIBUTED_LOCK_ROOT, getClassNameBytes());
    }

    private boolean isLockOwnedByThisThread(byte[] lockUuidBytes) {
        return lockUuidBytes != null && Arrays.equals(lockUuidBytes, this.threadUuidBytesHolder.get());
    }

    @Override
    public boolean lock(Lock lock, long timeout) throws ZkServerConnectionException {
        boolean success = false;
        switch (lock.level()) {
        case GLOBAL:
            success = this.globalLock(lock, timeout);
            break;
        case SERVER:
            success = this.serverLock(lock, timeout);
            break;
        case APPLICATION:
            success = this.appLock(lock, timeout);
            break;
        default:
            break;
        }

        return success;
    }

    private boolean registerApp() throws ZkServerConnectionException, InterruptedException {
        return createNodeIfNotExist(getAppLockRootPath(), appName.getBytes());
    }

    private Stat registerDebugWatcher() throws KeeperException, InterruptedException {
        Stat stat;
        stat = this.zk.exists(getConfigPath(ZkLockConstant.DEBUG), new Watcher() {
            @Override
            public void process(WatchedEvent event) {
                if (EventType.NodeDataChanged == event.getType()) {
                    byte[] data = getZkDebugNodeData();
                    updateLocalDebugFlat(data);
                    doRegisterThisDebugWatcher();
                }
            }

            private void doRegisterThisDebugWatcher() {
                try {
                    zk.exists(getConfigPath(ZkLockConstant.DEBUG), this);
                } catch (KeeperException | InterruptedException e) {
                    try {
                        handleException(ZkLockConstant.DEBUG, e);
                    } catch (ZkServerConnectionException zkServerConnExeption) {
                        printOrLogError(getConfigPath(ZkLockConstant.DEBUG) + COMMER
                                + zkServerConnExeption.getLocalizedMessage(), zkServerConnExeption);
                    }
                }
            }

            private byte[] getZkDebugNodeData() {
                byte[] data = null;
                try {
                    data = zk.getData(getConfigPath(ZkLockConstant.DEBUG), null, null);
                } catch (KeeperException | InterruptedException e) {
                    try {
                        handleException(ZkLockConstant.DEBUG, e);
                    } catch (ZkServerConnectionException zkServerConnExeption) {
                        printOrLogError(getConfigPath(ZkLockConstant.DEBUG) + COMMER
                                + zkServerConnExeption.getLocalizedMessage(), zkServerConnExeption);
                    }
                }
                return data;
            }

            private void updateLocalDebugFlat(byte[] data) {
                if (data != null) {
                    debug = TRUE.equalsIgnoreCase(new String(data));
                } else {
                    debug = false;
                }
            }
        });
        return stat;
    }

    private boolean registerServerAddress() throws ZkServerConnectionException, InterruptedException {
        String hostName = "";
        try {
            this.ipAddress = InetAddress.getLocalHost().getHostAddress();
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            handleException("ZooKeeperManager.registerServerAddress()", e);
            this.ipAddress = "127.0.0.1";
            hostName = "Unknown-Host";
        }

        return createNodeIfNotExist(getServerLockRootPath(), hostName.getBytes());
    }

    @Override
    public boolean serverLock(Lock lock, long timeout) throws ZkServerConnectionException {
        return lock(lock, getServerLockPath(lock.id()), timeout);
    }

    @Override
    public boolean serverUnlock(String id) throws ZkServerConnectionException {
        return unlockByPath(getServerLockPath(id));
    }

    public void setHostPort(String hostPort) {
        if (hostPort == null || hostPort.trim().isEmpty()) {
            throw new IllegalArgumentException("hostPort cannot be null or empty");
        }

        String[] parts = hostPort.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("hostPort must be in format 'host:port'");
        }

        try {
            int port = Integer.parseInt(parts[1]);
            if (port <= 0 || port > 65535) {
                throw new IllegalArgumentException("Port must be between 1 and 65535");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid port number", e);
        }

        String host = parts[0].trim();
        if (!isValidHostname(host)) {
            throw new IllegalArgumentException("Invalid hostname format");
        }

        this.hostPort = hostPort.trim();
    }

    public void setSessionTimeout(String sessionTimeout) {
        this.sessionTimeout = Integer.valueOf(sessionTimeout);
    }

    @Override
    public boolean tryLock(Lock lock) throws ZkServerConnectionException {
        boolean success = false;
        switch (lock.level()) {
        case GLOBAL:
            success = this.globalLock(lock, 0);
            break;
        case SERVER:
            success = this.serverLock(lock, 0);
            break;
        case APPLICATION:
            success = this.appLock(lock, 0);
            break;
        default:
            break;
        }

        return success;
    }

    @Override
    public boolean unlock(Lock lock) throws ZkServerConnectionException {
        boolean success = false;
        this.lockMode.set(lock.mode());
        try {
            switch (lock.level()) {
            case GLOBAL:
                success = this.globalUnlock(lock.id());
                break;
            case SERVER:
                success = this.serverUnlock(lock.id());
                break;
            case APPLICATION:
                success = this.appUnlock(lock.id());
                break;
            default:
                break;
            }
        } finally {
            this.lockMode.remove();
        }
        return success;
    }

    public void setAdminAuth(String adminAuth) {
        this.adminAuth = adminAuth;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    private void syserr(String message) {
        System.err.println(message);
    }

    private void sysout(String message) {
        System.out.println(message);
    }

    private void syserrOrLog(String errMsg) {
        if (LOGGER != null) {
            LOGGER.error(errMsg);
        } else {
            syserr(errMsg);
        }
    }

    private void sysoutOrLog(String infoMsg) {
        if (LOGGER != null) {
            LOGGER.info(infoMsg);
        } else {
            sysout(infoMsg);
        }
    }

    private void printOrLog(String errMsg, String infoMsg, Exception e) {
        if (LOGGER != null) {
            LOGGER.error(errMsg, e);
            LOGGER.info(infoMsg);
        } else {
            syserr(errMsg);
            sysout(infoMsg);
            e.printStackTrace();
        }
    }

    private void printOrLogError(String errMsg, Exception e) {
        if (LOGGER != null) {
            LOGGER.error(errMsg, e);
        } else {
            syserr(errMsg);
            e.printStackTrace();
        }
    }

    private void printOrLogWarn(String errMsg, Exception e) {
        if (LOGGER != null) {
            LOGGER.warn(errMsg, e);
        } else {
            sysout(errMsg);
            e.printStackTrace();
        }
    }

    private boolean isNoAuthIssue(KeeperException ke) {
        return Code.NOAUTH == ke.code();
    }

    private boolean isNoNodeIssue(KeeperException ke) {
        return Code.NONODE == ke.code();
    }

    private boolean isConnectionOrSessionIssue(KeeperException ke) {
        return Code.CONNECTIONLOSS == ke.code() || Code.SESSIONEXPIRED == ke.code() || Code.SESSIONMOVED == ke.code();
    }

    private boolean isNodeExistsIssue(KeeperException ke) {
        return Code.NODEEXISTS == ke.code();
    }

    public boolean isValidHostname(String hostname) {
        if (hostname == null || hostname.length() > 255) {
            return false;
        }

        try {
            if (isIpAddressFormat(hostname)) {
                return isValidIpAddress(hostname);
            }
        } catch (NumberFormatException e) {
            // DO NOTHING
            ;
        }

        return hostname.matches(HOSTNAME_PATTERN);
    }

    private boolean isValidIpAddress(String ip) {
        String[] parts = ip.split("\\.");

        if (parts.length != 4) {
            return false;
        }

        try {
            for (String part : parts) {
                int value = Integer.parseInt(part);
                if (value < 0 || value > 255) {
                    return false;
                }
            }
        } catch (NumberFormatException e) {
            return false;
        }

        return true;
    }

    private boolean isIpAddressFormat(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }

        String[] parts = ip.split("\\.");

        try {
            for (String part : parts) {
                Integer.parseInt(part);
            }
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }
}
