package org.rainyheart.distributed.lock.api.annotation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AnnotationResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationResolver.class);

    private static final String SPACE = "";

    private static volatile AnnotationResolver resolver;

    private static final Pattern REGEX = Pattern.compile("#\\{\\D*\\}");

    private AnnotationResolver() {
        super();
    }

    public static final AnnotationResolver getInstance() {
        if (resolver == null) {
            synchronized (AnnotationResolver.class) {
                if (resolver == null) {
                    resolver = new AnnotationResolver();
                }
            }
        }
        return resolver;
    }

    public Object resolve(JoinPoint joinPoint, String str) {
        if (str == null) {
            return null;
        }
        Object value = null;
        Matcher matcher = REGEX.matcher(str);
        if (matcher.matches()) {
            String newStr = str.replaceAll("#\\{", SPACE).replaceAll("\\}", SPACE);
            if (newStr.contains(".")) {
                try {
                    value = complexResolver(joinPoint, newStr);
                } catch (Exception e) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.error(e.getMessage(), e);
                    }
                }
            } else {
                value = simpleResolver(joinPoint, newStr);
            }
        } else {
            value = str;
        }
        return value;
    }

    private Object complexResolver(JoinPoint joinPoint, String str) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();

        String[] names = methodSignature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        String[] strs = str.split("\\.");

        for (int i = 0; i < names.length; i++) {
            if (strs[0].equals(names[i])) {
                Object obj = args[i];
                Method dmethod = obj.getClass().getDeclaredMethod(getMethodName(strs[1]), null);
                Object value = dmethod.invoke(args[i]);
                return getValue(value, 1, strs);
            }
        }

        return null;
    }

    private Object getValue(Object obj, int index, String[] strs) {
        try {
            if (obj != null && index < strs.length - 1) {
                Method method = obj.getClass().getDeclaredMethod(getMethodName(strs[index + 1]), null);
                obj = method.invoke(obj);
                getValue(obj, index + 1, strs);
            }

            return obj;

        } catch (Exception e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.error(e.getMessage(), e);
            }
            return null;
        }
    }

    private String getMethodName(String name) {
        return "get" + name.replaceFirst(name.substring(0, 1), name.substring(0, 1).toUpperCase());
    }

    private Object simpleResolver(JoinPoint joinPoint, String str) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        String[] names = methodSignature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < names.length; i++) {
            if (str.equals(names[i])) {
                return args[i];
            }
        }
        return null;
    }

}
