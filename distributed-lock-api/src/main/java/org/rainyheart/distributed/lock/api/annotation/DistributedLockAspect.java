package org.rainyheart.distributed.lock.api.annotation;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PreDestroy;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import org.rainyheart.distributed.lock.api.DistributedLockApi;
import org.rainyheart.distributed.lock.api.Lock;
import org.rainyheart.distributed.lock.api.exception.DistributedLockException;
import org.rainyheart.distributed.lock.api.impl.LockImpl;

@Component
@Aspect
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class DistributedLockAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistributedLockAspect.class);

    private ThreadLocal<Map<JoinPoint, Lock>> lockHolder = new ThreadLocal<>();

    @Autowired
    private DistributedLockApi api;

    @Pointcut("@annotation(org.rainyheart.distributed.lock.api.annotation.DistributedLock)")
    public void pointcut() {
        // No need to do anything
    }

    @Before(value = "pointcut() && @annotation(distributedLock)")
    public void before(JoinPoint point, DistributedLock distributedLock) throws DistributedLockException {
        if (this.lockHolder.get() != null && this.lockHolder.get().size() > 0) {
            throw new DistributedLockException("Cannot obtain another lock:" + distributedLock.id()
                    + " because an existing lock " + this.lockHolder.get() + " had existed!");
        }

        String methodFullName = point.getTarget().getClass().getName() + point.getSignature().getName();

        String id = distributedLock.id();
        if (id.contains("#")) {
            MethodSignature methodSignature = (MethodSignature) point.getSignature();
            Method method = methodSignature.getMethod();
            DistributedLock definedLock = method.getAnnotation(DistributedLock.class);

            Object idInParm = AnnotationResolver.getInstance().resolve(point, definedLock.id());
            if (idInParm instanceof String) {
                id = (String) idInParm;
            } else {
                // this should never happen unless the id inside @DistributedLock is changed
                throw new DistributedLockException(
                        "Fail to handle the parameter in " + methodFullName + ", idInParm: " + idInParm);
            }
        }

        long timeout = distributedLock.timeout();
        Lock lock = new LockImpl(id, methodFullName.getBytes(), distributedLock.level());
        if (lock(lock, timeout)) {
            Map<JoinPoint, Lock> map = new HashMap<>();
            map.put(point, lock);
            lockHolder.set(map);
        } else {
            throw new DistributedLockException("Failed to get lock for " + lock + " in " + methodFullName);
        }
    }

    @After(value = "pointcut() && @annotation(distributedLock)")
    public void after(JoinPoint point, DistributedLock distributedLock) throws DistributedLockException {
        Map<JoinPoint, Lock> map = lockHolder.get();
        if (map != null && map.get(point) != null) {
            try {
                api.unlock(map.get(point));
            } finally {
                lockHolder.remove();
            }
        }
    }

    @AfterThrowing(value = "pointcut()", throwing = "e")
    public void afterThrow(JoinPoint point, Exception e) throws DistributedLockException {
        try {
        LOGGER.error("Exception occurred in distributed lock operation", e);
        Map<JoinPoint, Lock> lockMap = lockHolder.get();
        if (lockMap != null) {
            Lock lock = lockMap.get(point);
            if (lock != null) {
                try {
                    api.unlock(lock);
                } catch (Exception unlockEx) {
                    LOGGER.error("Failed to unlock during exception handling", unlockEx);
                }
            }
            lockMap.remove(point);
        }
    } finally {
        // Ensure ThreadLocal is always cleaned up
        lockHolder.remove();
    }
    }

    private boolean lock(Lock lock, long timeout) throws DistributedLockException {
        if (timeout != 0) {
            return api.lock(lock, timeout);
        } else {
            return api.tryLock(lock);
        }
    }
    
    @PreDestroy
    public void cleanup() {
        lockHolder.remove();
    }
}
