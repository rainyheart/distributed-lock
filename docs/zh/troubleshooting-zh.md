# Distributed Lock 常见问题解决方案

## 1. 连接问题

### 问题：无法连接到ZooKeeper服务器
```log
Exception: Unable to connect to ZooKeeper server
```
**解决方案**：
1. 检查ZooKeeper服务是否正常运行
2. 验证hostPort配置是否正确
3. 确认网络连接是否正常
4. 检查防火墙设置

### 问题：频繁断开连接
**解决方案**：
1. 调整sessionTimeout参数
```properties
# 建议配置
sessionTimeout=60000  # 生产环境建议60秒或更高
```
2. 检查网络质量
3. 考虑使用ZooKeeper集群

## 2. 锁相关问题

### 问题：死锁
症状：多个服务互相等待对方释放锁

**解决方案**：
1. 使用超时机制：
```java
@DistributedLock(value = "resource", timeout = 10000)
public void process() {
    // 业务逻辑
}
```

2. 按固定顺序获取多个锁：
```java
// 正确方式
@DistributedLock("lock1")
@DistributedLock("lock2")
public void correctOrder() {
    // 业务逻辑
}
```

### 问题：锁未正确释放
**解决方案**：
1. 使用try-finally确保锁释放：
```java
Lock lock = new LockImpl("resource");
try {
    distributedLockApi.lock(lock, 5000);
    // 业务逻辑
} finally {
    distributedLockApi.unlock(lock);
}
```

## 3. 性能问题

### 问题：高延迟
**解决方案**：
1. 优化锁粒度
2. 使用本地缓存
3. 调整等待间隔：
```properties
DISTRIBUTED_LOCK_THREAD_INTERVAL=100
```

### 问题：内存使用过高
**解决方案**：
1. 及时清理ThreadLocal资源
2. 定期释放不使用的锁
3. 监控内存使用情况

## 4. 配置问题

### 问题：权限认证失败
**解决方案**：
1. 检查adminAuth配置
2. 确保所有客户端使用相同的认证信息
3. 验证ACL设置

### 问题：配置不生效
**解决方案**：
1. 检查配置文件位置
2. 验证Spring配置是否正确
3. 检查配置优先级

## 5. 调试技巧

### 开启调试模式
```bash
# 使用zkCli连接到ZooKeeper服务器
$ zkCli.sh -server localhost:2181
# 设置debug节点
set /config/debug "true"
```

### 查看锁状态
```bash
# 查看所有锁
ls /distributed_lock

# 查看特定锁
get /distributed_lock/your_lock_path
```

### 日志配置
```xml
<logger name="org.rainyheart.distributed.lock">
    <level value="DEBUG"/>
    <appender-ref ref="FILE"/>
</logger>
```

## 6. 最佳实践

### 异常处理
```java
try {
    distributedLockApi.lock(lock, timeout);
} catch (DistributedLockException e) {
    // 处理锁获取失败
    logger.error("获取锁失败", e);
    // 实现补偿逻辑
} finally {
    try {
        distributedLockApi.unlock(lock);
    } catch (Exception e) {
        logger.error("释放锁失败", e);
    }
}
```

### 监控建议
1. 监控指标：
   - 锁获取时间
   - 锁持有时间
   - 锁争用次数
   - 失败次数

2. 告警设置：
   - 锁等待时间超过阈值
   - 频繁的锁获取失败
   - ZooKeeper连接异常
