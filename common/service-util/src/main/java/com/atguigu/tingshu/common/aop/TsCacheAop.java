package com.atguigu.tingshu.common.aop;

import com.atguigu.tingshu.common.annotation.TsCache;
import com.atguigu.tingshu.common.constant.RedisConstant;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Component
@Aspect
@Slf4j
public class TsCacheAop {

    @Autowired
    RedisTemplate redisTemplate;
    @Autowired
    RedissonClient redissonClient;

    @Around("@annotation(tsCache)")
    public Object tsCacheAopMethod(ProceedingJoinPoint joinPoint, TsCache tsCache){

        String key = tsCache.cachePrefix() + Arrays.asList(joinPoint.getArgs());

        //从redis中获取数据
        Object obj = redisTemplate.opsForValue().get(key);
        if(obj !=null) {
            log.info("=====================从缓存中获取数据=====================");
            return obj;
        }

        //获取分布式锁
        String lockKey = tsCache.lockPrefix() + Arrays.asList(joinPoint.getArgs());
        RLock lock = redissonClient.getLock(lockKey);

        boolean tryLock = lock.tryLock();
        if(tryLock){
            try {
                //获取到锁
                //执行目标方法，从数据库中获取数据
                log.info("=====================从数据库中获取数据=====================");
                obj = joinPoint.proceed();
                if(obj == null){
                    //将数据存储到缓存中，设置较短过期时间
                    redisTemplate.opsForValue().set(key,new Object(), RedisConstant.CACHE_TEMPORARY_TIMEOUT, TimeUnit.SECONDS);
                }else {
                    //将数据存储到缓存中，设置较长过期时间
                    redisTemplate.opsForValue().set(key,obj,RedisConstant.CACHE_TIMEOUT,TimeUnit.SECONDS);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            } finally {
                //释放锁
                lock.unlock();
            }
        }else {
            //没获取到锁，自旋
            tsCacheAopMethod(joinPoint,tsCache);
        }

        return obj;
    }
}
