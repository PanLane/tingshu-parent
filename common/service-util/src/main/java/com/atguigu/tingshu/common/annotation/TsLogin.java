package com.atguigu.tingshu.common.annotation;

import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 登录注解，用在方法上，
 * 拦截未登录的请求，或者将登录信息放入到当前线程中
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TsLogin {
    boolean required() default true;
}
