package com.atguigu.tingshu.album.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MinioConstantProperties.class)
public class MinioConfig {

    @Autowired
    MinioConstantProperties minioProperties;

    @Bean
    public MinioClient minioClient(){
        return MinioClient.builder().endpoint(minioProperties.getEndpointUrl()).
                credentials(minioProperties.getAccessKey(),minioProperties.getSecretKey())
                .build();
    }
}