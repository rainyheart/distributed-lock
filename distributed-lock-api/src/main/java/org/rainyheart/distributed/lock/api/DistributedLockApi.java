package org.rainyheart.distributed.lock.api;

import org.rainyheart.distributed.lock.api.exception.DistributedLockException;

public interface DistributedLockApi {

    /**
     * 
     * This API will try to get a lock for the specific lock object, if obtained,
     * then return true, else false.
     * 
     * @param lock:
     *            the lock instance to obtain
     * @return true: acquire lock successfully, false: fail to acquire lock
     * @throws DistributedLockException:
     *             throws an DistributedLockException if failed to unlock the given
     *             lock
     */
    public boolean tryLock(Lock lock) throws DistributedLockException;

    /**
     * 
     * This API will try to get a lock for the specific lock object, if obtained,
     * then return true. If not, then block current thread until the lock is
     * obtained unless timeout. If timeout < 0, then the current thread will be
     * always blocked. If timeout = 0, it equals to {@link tryLock}
     * 
     * @param lock:
     *            the lock instance to obtain
     * @return true: acquire lock successfully, false: fail to acquire lock
     * @throws DistributedLockException:
     *             throws an DistributedLockException if failed to unlock the given
     *             lock
     */
    public boolean lock(Lock lock, long timeout) throws DistributedLockException;

    /**
     * This API will try to unlock the specific lock object.
     * 
     * @param lock:
     *            the lock instance to obtain
     * @return true: unlock successfully, false: unlock failed
     * @throws DistributedLockException:
     *             throws an DistributedLockException if failed to unlock the given
     *             lock
     */
    public boolean unlock(Lock lock) throws DistributedLockException;
}
