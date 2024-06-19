package com.atguigu.tingshu.common.config.redis;

import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties("spring.data.redis")
public class RedissonConfig {

    private String host;
    private String port;
    private String password;
    private int timeout = 3000;

    @Bean
    RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer().setPassword(password).setAddress("redis://"+host+":"+port).setTimeout(timeout);
        return Redisson.create(config);
    }
}
