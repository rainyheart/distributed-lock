package org.rainyheart.distributed.lock.thridparty.zk.utils;

import java.security.NoSuchAlgorithmException;

import org.junit.Test;

public class ZkPasswordUtilsTest {
    @Test
    public void superUser() throws NoSuchAlgorithmException {
        String pwd = ZkPasswordUtils.getDigestUserPwd("super:xuebang1306zoo!");
        System.out.println(pwd);
    }

    @Test
    public void adminUser() throws NoSuchAlgorithmException {
        String pwd = ZkPasswordUtils.getDigestUserPwd("admin:xuebang1306adm!n");
        System.out.println(pwd);
    }
}
