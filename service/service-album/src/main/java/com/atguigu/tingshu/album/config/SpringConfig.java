package com.atguigu.tingshu.album.config;

import com.qcloud.vod.VodUploadClient;
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MinioConstantProperties.class)
public class SpringConfig {

    @Autowired
    MinioConstantProperties minioProperties;
    @Autowired
    VodConstantProperties vodConstantProperties;

    @Bean
    public MinioClient minioClient(){
        return MinioClient.builder().endpoint(minioProperties.getEndpointUrl()).
                credentials(minioProperties.getAccessKey(),minioProperties.getSecretKey())
                .build();
    }

    @Bean
    public VodUploadClient vodUploadClient(){
        return new VodUploadClient(vodConstantProperties.getSecretId(),vodConstantProperties.getSecretKey());
    }
}