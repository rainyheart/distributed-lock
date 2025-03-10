# Distributed Lock Troubleshooting Guide

## 1. Connection Issues

### Problem: Cannot Connect to ZooKeeper Server
```log
Exception: Unable to connect to ZooKeeper server
```
**Solutions**:
1. Check if ZooKeeper service is running
2. Verify hostPort configuration
3. Confirm network connectivity
4. Check firewall settings

### Problem: Frequent Disconnections
**Solutions**:
1. Adjust sessionTimeout parameter
```properties
# Recommended configuration
sessionTimeout=60000  # 60 seconds or higher for production
```
2. Check network quality
3. Consider using ZooKeeper cluster

## 2. Lock-Related Issues

### Problem: Deadlocks
Symptom: Multiple services waiting for each other's locks

**Solutions**:
1. Use timeout mechanism:
```java
@DistributedLock(value = "resource", timeout = 10000)
public void process() {
    // Business logic
}
```

2. Acquire locks in a fixed order:
```java
// Correct approach
@DistributedLock("lock1")
@DistributedLock("lock2")
public void correctOrder() {
    // Business logic
}
```

### Problem: Locks Not Released Properly
**Solutions**:
1. Use try-finally to ensure lock release:
```java
Lock lock = new LockImpl("resource");
try {
    distributedLockApi.lock(lock, 5000);
    // Business logic
} finally {
    distributedLockApi.unlock(lock);
}
```

## 3. Performance Issues

### Problem: High Latency
**Solutions**:
1. Optimize lock granularity
2. Use local caching
3. Adjust wait interval:
```properties
DISTRIBUTED_LOCK_THREAD_INTERVAL=100
```

### Problem: High Memory Usage
**Solutions**:
1. Clean up ThreadLocal resources promptly
2. Release unused locks regularly
3. Monitor memory usage

## 4. Configuration Issues

### Problem: Authentication Failure
**Solutions**:
1. Check adminAuth configuration
2. Ensure all clients use the same authentication
3. Verify ACL settings

### Problem: Configuration Not Taking Effect
**Solutions**:
1. Check configuration file location
2. Verify Spring configuration
3. Check configuration priorities

## 5. Debugging Tips

### Enable Debug Mode
```bash
# Using zkCli to connect to ZooKeeper server
$ zkCli.sh -server localhost:2181
# Set debug node
set /config/debug "true"
```

### View Lock Status
```bash
# View all locks
ls /distributed_lock

# View specific lock
get /distributed_lock/your_lock_path
```

### Logging Configuration
```xml
<logger name="org.rainyheart.distributed.lock">
    <level value="DEBUG"/>
    <appender-ref ref="FILE"/>
</logger>
```

## 6. Best Practices

### Exception Handling
```java
try {
    distributedLockApi.lock(lock, timeout);
} catch (DistributedLockException e) {
    // Handle lock acquisition failure
    logger.error("Failed to acquire lock", e);
    // Implement compensation logic
} finally {
    try {
        distributedLockApi.unlock(lock);
    } catch (Exception e) {
        logger.error("Failed to release lock", e);
    }
}
```

### Monitoring Recommendations
1. Monitor Metrics:
   - Lock acquisition time
   - Lock hold time
   - Lock contention count
   - Failure count

2. Alert Settings:
   - Lock wait time exceeds threshold
   - Frequent lock acquisition failures
   - ZooKeeper connection exceptions
