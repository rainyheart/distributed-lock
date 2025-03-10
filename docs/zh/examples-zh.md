# Distributed Lock 使用示例

## 1. 基础使用示例

### 注解方式
```java
@Service
public class OrderService {
    
    // 简单的锁使用
    @DistributedLock("order")
    public void processOrder(String orderId) {
        // 业务逻辑
    }
    
    // 带超时的锁
    @DistributedLock(value = "order:#{orderId}", timeout = 5000)
    public void processOrderWithTimeout(String orderId) {
        // 业务逻辑
    }
    
    // 持久锁模式
    @DistributedLock(value = "order:#{orderId}", mode = 1)
    public void processOrderWithPersistentLock(String orderId) {
        // 业务逻辑
    }
}
```

### 编程方式
```java
@Service
public class OrderService {
    
    @Autowired
    private DistributedLockApi distributedLockApi;
    
    public void processOrder(String orderId) {
        Lock lock = new LockImpl("order:" + orderId);
        try {
            if (distributedLockApi.tryLock(lock)) {
                // 业务逻辑
            } else {
                // 获取锁失败的处理
            }
        } finally {
            distributedLockApi.unlock(lock);
        }
    }
    
    public void processOrderWithTimeout(String orderId) {
        Lock lock = new LockImpl("order:" + orderId);
        try {
            if (distributedLockApi.lock(lock, 5000)) {
                // 业务逻辑
            } else {
                // 超时处理
            }
        } finally {
            distributedLockApi.unlock(lock);
        }
    }
}
```

## 2. 高级使用示例

### 多粒度锁
```java
@Service
public class OrderService {
    
    // 订单级别锁
    @DistributedLock("order:#{orderId}")
    public void processOrder(String orderId) {
        // 处理单个订单
    }
    
    // 用户级别锁
    @DistributedLock("user:#{userId}")
    public void processUserOrders(String userId) {
        // 处理用户的所有订单
    }
    
    // 全局锁
    @DistributedLock("global:orders")
    public void processAllOrders() {
        // 处理所有订单
    }
}
```

### 组合锁使用
```java
@Service
public class TransferService {
    
    @Autowired
    private DistributedLockApi lockApi;
    
    public void transfer(String fromAccount, String toAccount, BigDecimal amount) {
        Lock fromLock = new LockImpl("account:" + fromAccount);
        Lock toLock = new LockImpl("account:" + toAccount);
        
        try {
            // 按固定顺序获取锁，避免死锁
            if (fromAccount.compareTo(toAccount) < 0) {
                lockApi.lock(fromLock, 5000);
                lockApi.lock(toLock, 5000);
            } else {
                lockApi.lock(toLock, 5000);
                lockApi.lock(fromLock, 5000);
            }
            
            // 执行转账
            doTransfer(fromAccount, toAccount, amount);
            
        } finally {
            // 释放锁
            lockApi.unlock(fromLock);
            lockApi.unlock(toLock);
        }
    }
}
```

### 重试机制示例
```java
@Service
public class RetryableService {
    
    @Autowired
    private DistributedLockApi lockApi;
    
    public void processWithRetry(String resourceId) {
        Lock lock = new LockImpl("resource:" + resourceId);
        int retryCount = 3;
        long retryInterval = 1000; // 1秒
        
        while (retryCount > 0) {
            try {
                if (lockApi.tryLock(lock)) {
                    try {
                        // 业务逻辑
                        return;
                    } finally {
                        lockApi.unlock(lock);
                    }
                }
                Thread.sleep(retryInterval);
                retryCount--;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("处理被中断", e);
            }
        }
        throw new RuntimeException("无法获取锁，重试次数耗尽");
    }
}
```

## 3. 实际业务场景示例

### 库存扣减
```java
@Service
public class InventoryService {
    
    @Autowired
    private DistributedLockApi lockApi;
    
    public boolean deductStock(String productId, int quantity) {
        Lock lock = new LockImpl("inventory:" + productId);
        try {
            if (lockApi.lock(lock, 5000)) {
                // 检查库存
                int currentStock = getStock(productId);
                if (currentStock >= quantity) {
                    // 扣减库存
                    updateStock(productId, currentStock - quantity);
                    return true;
                }
                return false;
            }
            throw new RuntimeException("获取库存锁超时");
        } finally {
            lockApi.unlock(lock);
        }
    }
}
```

### 定时任务调度
```java
@Service
public class ScheduledTaskService {
    
    @Scheduled(cron = "0 0 * * * ?")
    @DistributedLock(value = "scheduled:hourly-task", timeout = 3600000) // 1小时超时
    public void hourlyTask() {
        // 确保集群中只有一个节点执行定时任务
        // 业务逻辑
    }
}
```

### 缓存更新
```java
@Service
public class CacheService {
    
    @Autowired
    private DistributedLockApi lockApi;
    
    public void updateCache(String key) {
        Lock lock = new LockImpl("cache:" + key);
        try {
            // 短超时时间，避免长时间阻塞其他缓存更新
            if (lockApi.lock(lock, 1000)) {
                // 从数据库获取最新数据
                Object latestData = fetchFromDB(key);
                // 更新缓存
                updateCache(key, latestData);
            }
        } finally {
            lockApi.unlock(lock);
        }
    }
}
```

## 4. 测试示例

### 单元测试
```java
@SpringBootTest
public class DistributedLockTest {
    
    @Autowired
    private DistributedLockApi lockApi;
    
    @Test
    public void testLockAndUnlock() {
        Lock lock = new LockImpl("test:lock");
        assertTrue(lockApi.tryLock(lock));
        // 验证锁已被获取
        assertFalse(lockApi.tryLock(lock));
        // 释放锁
        assertTrue(lockApi.unlock(lock));
        // 验证锁已被释放
        assertTrue(lockApi.tryLock(lock));
    }
    
    @Test
    public void testTimeout() {
        Lock lock = new LockImpl("test:timeout");
        assertTrue(lockApi.tryLock(lock));
        
        // 在另一个线程中尝试获取同一个锁
        CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
            try {
                return lockApi.lock(lock, 1000); // 1秒超时
            } catch (Exception e) {
                return false;
            }
        });
        
        // 验证获取锁超时
        assertFalse(future.join());
    }
}
```
