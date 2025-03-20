package org.rainyheart.distributed.lock.thridparty.zk.utils;

import java.security.NoSuchAlgorithmException;

import org.apache.zookeeper.server.auth.DigestAuthenticationProvider;

public class ZkPasswordUtils {

    private ZkPasswordUtils() {
        super();
        throw new AssertionError("Utility class - do not instantiate");
    }

    public static String getDigestUserPwd(String id) throws NoSuchAlgorithmException {
        return DigestAuthenticationProvider.generateDigest(id);
    }
}
