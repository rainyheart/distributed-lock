# Distributed Lock Usage Examples

## 1. Basic Usage Examples

### Annotation-based Approach
```java
@Service
public class OrderService {
    
    // Simple lock usage
    @DistributedLock("order")
    public void processOrder(String orderId) {
        // Business logic
    }
    
    // Lock with timeout
    @DistributedLock(value = "order:#{orderId}", timeout = 5000)
    public void processOrderWithTimeout(String orderId) {
        // Business logic
    }
    
    // Persistent lock mode
    @DistributedLock(value = "order:#{orderId}", mode = 1)
    public void processOrderWithPersistentLock(String orderId) {
        // Business logic
    }
}
```

### Programmatic Approach
```java
@Service
public class OrderService {
    
    @Autowired
    private DistributedLockApi distributedLockApi;
    
    public void processOrder(String orderId) {
        Lock lock = new LockImpl("order:" + orderId);
        try {
            if (distributedLockApi.tryLock(lock)) {
                // Business logic
            } else {
                // Handle lock acquisition failure
            }
        } finally {
            distributedLockApi.unlock(lock);
        }
    }
    
    public void processOrderWithTimeout(String orderId) {
        Lock lock = new LockImpl("order:" + orderId);
        try {
            if (distributedLockApi.lock(lock, 5000)) {
                // Business logic
            } else {
                // Handle timeout
            }
        } finally {
            distributedLockApi.unlock(lock);
        }
    }
}
```

## 2. Advanced Usage Examples

### Multi-granularity Locks
```java
@Service
public class OrderService {
    
    // Order-level lock
    @DistributedLock("order:#{orderId}")
    public void processOrder(String orderId) {
        // Process single order
    }
    
    // User-level lock
    @DistributedLock("user:#{userId}")
    public void processUserOrders(String userId) {
        // Process all orders for a user
    }
    
    // Global lock
    @DistributedLock("global:orders")
    public void processAllOrders() {
        // Process all orders
    }
}
```

### Composite Lock Usage
```java
@Service
public class TransferService {
    
    @Autowired
    private DistributedLockApi lockApi;
    
    public void transfer(String fromAccount, String toAccount, BigDecimal amount) {
        Lock fromLock = new LockImpl("account:" + fromAccount);
        Lock toLock = new LockImpl("account:" + toAccount);
        
        try {
            // Acquire locks in fixed order to prevent deadlocks
            if (fromAccount.compareTo(toAccount) < 0) {
                lockApi.lock(fromLock, 5000);
                lockApi.lock(toLock, 5000);
            } else {
                lockApi.lock(toLock, 5000);
                lockApi.lock(fromLock, 5000);
            }
            
            // Perform transfer
            doTransfer(fromAccount, toAccount, amount);
            
        } finally {
            // Release locks
            lockApi.unlock(fromLock);
            lockApi.unlock(toLock);
        }
    }
}
```

### Retry Mechanism Example
```java
@Service
public class RetryableService {
    
    @Autowired
    private DistributedLockApi lockApi;
    
    public void processWithRetry(String resourceId) {
        Lock lock = new LockImpl("resource:" + resourceId);
        int retryCount = 3;
        long retryInterval = 1000; // 1 second
        
        while (retryCount > 0) {
            try {
                if (lockApi.tryLock(lock)) {
                    try {
                        // Business logic
                        return;
                    } finally {
                        lockApi.unlock(lock);
                    }
                }
                Thread.sleep(retryInterval);
                retryCount--;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Processing interrupted", e);
            }
        }
        throw new RuntimeException("Failed to acquire lock, retry count exhausted");
    }
}
```

## 3. Real Business Scenario Examples

### Inventory Deduction
```java
@Service
public class InventoryService {
    
    @Autowired
    private DistributedLockApi lockApi;
    
    public boolean deductStock(String productId, int quantity) {
        Lock lock = new LockImpl("inventory:" + productId);
        try {
            if (lockApi.lock(lock, 5000)) {
                // Check stock
                int currentStock = getStock(productId);
                if (currentStock >= quantity) {
                    // Deduct stock
                    updateStock(productId, currentStock - quantity);
                    return true;
                }
                return false;
            }
            throw new RuntimeException("Inventory lock acquisition timeout");
        } finally {
            lockApi.unlock(lock);
        }
    }
}
```

### Scheduled Task Coordination
```java
@Service
public class ScheduledTaskService {
    
    @Scheduled(cron = "0 0 * * * ?")
    @DistributedLock(value = "scheduled:hourly-task", timeout = 3600000) // 1 hour timeout
    public void hourlyTask() {
        // Ensure only one node in the cluster executes the scheduled task
        // Business logic
    }
}
```

### Cache Update
```java
@Service
public class CacheService {
    
    @Autowired
    private DistributedLockApi lockApi;
    
    public void updateCache(String key) {
        Lock lock = new LockImpl("cache:" + key);
        try {
            // Short timeout to avoid blocking other cache updates
            if (lockApi.lock(lock, 1000)) {
                // Get latest data from database
                Object latestData = fetchFromDB(key);
                // Update cache
                updateCache(key, latestData);
            }
        } finally {
            lockApi.unlock(lock);
        }
    }
}
```

## 4. Testing Examples

### Unit Tests
```java
@SpringBootTest
public class DistributedLockTest {
    
    @Autowired
    private DistributedLockApi lockApi;
    
    @Test
    public void testLockAndUnlock() {
        Lock lock = new LockImpl("test:lock");
        assertTrue(lockApi.tryLock(lock));
        // Verify lock is acquired
        assertFalse(lockApi.tryLock(lock));
        // Release lock
        assertTrue(lockApi.unlock(lock));
        // Verify lock is released
        assertTrue(lockApi.tryLock(lock));
    }
    
    @Test
    public void testTimeout() {
        Lock lock = new LockImpl("test:timeout");
        assertTrue(lockApi.tryLock(lock));
        
        // Try to acquire same lock in another thread
        CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
            try {
                return lockApi.lock(lock, 1000); // 1 second timeout
            } catch (Exception e) {
                return false;
            }
        });
        
        // Verify lock acquisition timeout
        assertFalse(future.join());
    }
}
```
