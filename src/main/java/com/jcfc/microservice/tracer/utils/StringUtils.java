package com.jcfc.microservice.tracer.utils;


import com.alibaba.fastjson.JSON;

import java.util.Date;

/**
 * Created by zhangjinpeng on 2018/1/16.
 */

public class StringUtils {

    private static final String COMMA_SEPARATOR = ",";

    public static String toArgumentString(Object[] args) {
        StringBuilder buf = new StringBuilder();
        for (Object arg : args) {
            if (buf.length() > 0) {
                buf.append(COMMA_SEPARATOR);
            }
            if (arg == null || isPrimitives(arg.getClass())) {
                buf.append(arg);
            } else {
                try {
                    buf.append(JSON.toJSONString(arg));
                } catch (Exception e) {
                    buf.append(arg);
                }
            }
        }
        return buf.toString();
    }

    public static boolean isPrimitives(Class<?> cls) {
        if (cls.isArray()) {
            return isPrimitive(cls.getComponentType());
        }
        return isPrimitive(cls);
    }

    public static boolean isPrimitive(Class<?> cls) {
        return cls.isPrimitive() || cls == String.class || cls == Boolean.class || cls == Character.class
                || Number.class.isAssignableFrom(cls) || Date.class.isAssignableFrom(cls);
    }
}
