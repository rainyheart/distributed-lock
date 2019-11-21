package org.rainyheart.distributed.lock.api.exception;

import java.security.NoSuchAlgorithmException;

public class DistributedLockException extends Exception {

    private static final long serialVersionUID = 5815822609555434842L;

    public DistributedLockException() {
        super();
    }

    public DistributedLockException(String msg) {
        super(msg);
    }

    public DistributedLockException(String msg, NoSuchAlgorithmException e) {
        super(msg, e);
    }
}
