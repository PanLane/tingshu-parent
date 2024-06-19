package com.atguigu.tingshu.common.annotation;

import com.atguigu.tingshu.common.constant.RedisConstant;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TsCache {
    String cachePrefix() default RedisConstant.CACHE_INFO_PREFIX;
    String lockPrefix() default RedisConstant.CACHE_LOCK_PREFIX;
}
