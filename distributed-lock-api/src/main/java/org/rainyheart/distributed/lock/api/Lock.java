package org.rainyheart.distributed.lock.api;

public interface Lock {
    /**
     * 
     * @return byte array of this lock instance
     */
    public byte[] value();

    /**
     * 
     * @return String value as the id of this lock instance
     */
    public String id();

    /**
     * 
     * @return the lock level of this lock instance
     */
    public LockLevel level();

    /**
     * 
     * @return the lock mode if this lock instance
     */
    public Integer mode();
}
