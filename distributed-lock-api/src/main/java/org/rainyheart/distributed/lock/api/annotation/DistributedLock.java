/**
 * 
 */
package org.rainyheart.distributed.lock.api.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.rainyheart.distributed.lock.api.LockLevel;

@Retention(RUNTIME)
@Target(METHOD)
/**
 * @author Ken Ye
 *
 */
public @interface DistributedLock {
    String id();
    LockLevel level();
    long timeout() default 0;
}
