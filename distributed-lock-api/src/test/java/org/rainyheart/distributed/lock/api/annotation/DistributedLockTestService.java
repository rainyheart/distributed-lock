package org.rainyheart.distributed.lock.api.annotation;

import org.springframework.stereotype.Component;

import org.rainyheart.distributed.lock.api.LockLevel;

@Component
public class DistributedLockTestService {
    @DistributedLock(id = "#{id}", level = LockLevel.GLOBAL)
    public void test(String id) {
        System.out.println("test: " + id);
    }
}
