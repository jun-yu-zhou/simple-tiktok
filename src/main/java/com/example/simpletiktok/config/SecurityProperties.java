package com.example.simpletiktok.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    /**
     * Referer 白名单前缀列表（例如 http://example.com）。
     */
    private List<String> refererWhitelist = new ArrayList<>();

    /**
     * 是否允许空 Referer 请求访问资源链接接口。
     */
    private boolean allowEmptyReferer = false;
}
