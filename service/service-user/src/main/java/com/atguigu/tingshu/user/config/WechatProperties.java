package com.atguigu.tingshu.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("wechat.login")
public class WechatProperties {
    private String appId;
    private String appSecret;
}
