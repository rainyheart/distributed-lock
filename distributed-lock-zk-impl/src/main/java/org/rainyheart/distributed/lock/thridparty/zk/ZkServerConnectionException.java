package org.rainyheart.distributed.lock.thridparty.zk;

import java.security.NoSuchAlgorithmException;

import org.rainyheart.distributed.lock.api.exception.DistributedLockException;

public class ZkServerConnectionException extends DistributedLockException {

    private static final long serialVersionUID = -2527690750213548401L;

    public ZkServerConnectionException() {
        super();
    }
    
    public ZkServerConnectionException(String msg) {
        super(msg);
    }

    public ZkServerConnectionException(String msg, NoSuchAlgorithmException e) {
        super(msg, e);
    }

}
