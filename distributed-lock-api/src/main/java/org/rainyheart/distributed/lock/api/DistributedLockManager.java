package org.rainyheart.distributed.lock.api;

import org.rainyheart.distributed.lock.api.exception.DistributedLockException;

public interface DistributedLockManager {
    /**
     * 
     * @param lock:
     *            lock instance to try to obtain
     * @return success or not to obtain this lock
     * @throws DistributedLockException:
     *             throw this exception when anything wrong to obtain this lock
     */
    public boolean tryLock(Lock lock) throws DistributedLockException;

    /**
     * 
     * @param lock:
     *            lock instance to try to obtain
     * @param timeout:
     *            timeout value to obtain the given lock
     * @return success or not to obtain this lock
     * @throws DistributedLockException:
     *             throw this exception when anything wrong to obtain this lock
     */
    public boolean lock(Lock lock, long timeout) throws DistributedLockException;

    /**
     * AppLevel lock
     * 
     * @param lock:
     *            lock instance to try to obtain
     * @param timeout:
     *            timeout value to obtain the given lock
     * @return success or not to obtain this lock
     * @throws DistributedLockException:
     *             throw this exception when anything wrong to obtain this lock
     */
    public boolean appLock(Lock lock, long timeout) throws DistributedLockException;

    /**
     * ServerLevel lock
     * 
     * @param lock:
     *            lock instance to try to obtain
     * @param timeout:
     *            timeout value to obtain the given lock
     * @return success or not to obtain this lock
     * @throws DistributedLockException:
     *             throw this exception when anything wrong to obtain this lock
     */
    public boolean serverLock(Lock lock, long timeout) throws DistributedLockException;

    /**
     * GlobalLevel lock
     * 
     * @param lock:
     *            lock instance to try to obtain
     * @param timeout:
     *            timeout value to obtain the given lock
     * @return success or not to obtain this lock
     * @throws DistributedLockException:
     *             throw this exception when anything wrong to obtain this lock
     */
    public boolean globalLock(Lock lock, long timeout) throws DistributedLockException;

    /**
     * 
     * @param lock:
     *            lock instance to try to release
     * @return success or not to release this lock
     * @throws DistributedLockException:
     *             throw this exception when anything wrong to release this lock
     */
    public boolean unlock(Lock lock) throws DistributedLockException;

    /**
     * AppLevel unlock by the given lock id
     * 
     * @param id:
     *            lock id to try to release the lock
     * @return success or not to release this lock
     * @throws DistributedLockException:
     *             throw this exception when anything wrong to release this lock
     */
    public boolean appUnlock(String id) throws DistributedLockException;

    /**
     * ServerLevel unlock by the given lock id
     * 
     * @param id:
     *            lock id to try to release the lock
     * @return success or not to release this lock
     * @throws DistributedLockException:
     *             throw this exception when anything wrong to release this lock
     */
    public boolean serverUnlock(String id) throws DistributedLockException;

    /**
     * GlobalLevel unlock by the given lock id
     * 
     * @param id:
     *            lock id to try to release the lock
     * @return success or not to release this lock
     * @throws DistributedLockException:
     *             throw this exception when anything wrong to release this lock
     */
    public boolean globalUnlock(String id) throws DistributedLockException;
}
