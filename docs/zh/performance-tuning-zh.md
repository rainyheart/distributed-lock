# Distributed Lock 性能调优指南

## 性能优化关键点

### 1. ZooKeeper配置优化
```properties
# 建议配置
sessionTimeout=60000           # 适当增加会话超时时间，减少不必要的重连
clientConnectCount=5          # 合理的重试次数
DISTRIBUTED_LOCK_THREAD_INTERVAL=100  # 根据业务需求调整等待间隔
```

### 2. JVM参数优化
```bash
# 建议的JVM参数
-Xms2g -Xmx2g                 # 保持堆大小一致，避免动态调整
-XX:+UseG1GC                 # 使用G1垃圾收集器
-XX:MaxGCPauseMillis=200     # 控制GC停顿时间
```

### 3. 网络优化
- 确保ZooKeeper服务器和客户端在同一网络区域
- 使用内网IP地址而不是域名
- 配置合适的网络超时参数

### 4. 最佳实践

#### 锁粒度优化
```java
// 不推荐：粒度太粗
@DistributedLock("order")
public void processOrder() { ... }

// 推荐：细化锁粒度
@DistributedLock("order:#{orderId}")
public void processOrder(String orderId) { ... }
```

#### 锁持有时间优化
```java
// 不推荐：长时间持有锁
@DistributedLock("resource")
public void longProcess() {
    // 耗时操作...
}

// 推荐：最小化锁持有时间
public void optimizedProcess() {
    // 预处理
    @DistributedLock("resource")
    public void criticalSection() {
        // 只锁定关键部分
    }
    // 后续处理
}
```

### 5. 监控指标

#### 关键指标
- 锁获取等待时间
- 锁持有时间
- 锁争用次数
- ZooKeeper会话状态
- 重试次数

#### 监控阈值建议
| 指标 | 警告阈值 | 严重阈值 |
|------|----------|----------|
| 锁等待时间 | >500ms | >2s |
| 锁持有时间 | >1s | >5s |
| 重试次数 | >3次/分钟 | >10次/分钟 |

### 6. 性能测试建议

#### 测试场景
1. 正常负载测试
   - 模拟日常业务负载
   - 观察平均响应时间

2. 高并发测试
   - 模拟峰值流量
   - 关注锁争用情况

3. 故障恢复测试
   - 模拟网络抖动
   - 验证重试机制

#### 测试指标
- 吞吐量（TPS）
- 响应时间（RT）
- 错误率
- 资源使用率

### 7. 常见性能问题及解决方案

#### 问题1：锁等待时间过长
- 原因：锁争用严重
- 解决：
  1. 优化锁粒度
  2. 减少锁持有时间
  3. 使用锁分段技术

#### 问题2：频繁的会话超时
- 原因：网络不稳定或负载过高
- 解决：
  1. 优化网络配置
  2. 调整会话超时时间
  3. 增加ZooKeeper资源

#### 问题3：内存使用过高
- 原因：锁对象过多或内存泄漏
- 解决：
  1. 及时释放锁资源
  2. 优化ThreadLocal使用
  3. 定期清理过期锁
