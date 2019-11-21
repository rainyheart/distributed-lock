package org.rainyheart.distributed.lock.api.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.rainyheart.distributed.lock.api.DistributedLockApi;
import org.rainyheart.distributed.lock.api.DistributedLockManager;
import org.rainyheart.distributed.lock.api.Lock;
import org.rainyheart.distributed.lock.api.exception.DistributedLockException;

@Component
public class DistributedLockApiImpl implements DistributedLockApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistributedLockApiImpl.class);

    @Autowired
    private DistributedLockManager manager;

    @Override
    public boolean tryLock(Lock lock) throws DistributedLockException {
        LOGGER.debug("tryLock: " + lock);
        return manager.tryLock(lock);
    }

    @Override
    public boolean lock(Lock lock, long timeout) throws DistributedLockException {
        LOGGER.debug("lock: " + lock + ", timeout: " + timeout);
        return manager.lock(lock, timeout);
    }

    @Override
    public boolean unlock(Lock lock) throws DistributedLockException {
        LOGGER.debug("unlock: " + lock);
        return manager.unlock(lock);
    }

    public DistributedLockManager getManager() {
        return manager;
    }

    public void setManager(DistributedLockManager manager) {
        this.manager = manager;
    }

}
