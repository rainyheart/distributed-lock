package org.rainyheart.distributed.lock.thridparty.zk.utils;

import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;

@Ignore
public class TestExceptionUtils {

    private TestExceptionUtils() {
        super();
    }

    private static final List<Throwable> exceptions = new ArrayList<>();

    public static final synchronized List<Throwable> addException(Throwable e) {
        exceptions.add(e);
        return exceptions;
    }

    public static final synchronized int exceptionCount() {
        return exceptions.size();
    }

    public static synchronized boolean hasException() {
        return exceptions.size() > 0;
    }

    public static synchronized void clearException() {
        exceptions.clear();
    }
}
