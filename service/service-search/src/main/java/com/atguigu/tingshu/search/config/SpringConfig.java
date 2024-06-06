package com.atguigu.tingshu.search.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class SpringConfig {

    @Bean
    Executor myExecutor(){
        return new ThreadPoolExecutor(28,50,5, TimeUnit.SECONDS,new ArrayBlockingQueue<>(1000));
    }
}
