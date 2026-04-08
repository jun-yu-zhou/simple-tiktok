package com.example.simpletiktok.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "aliyun.oss")
public class OssConfig {

    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;
    /**
     * CDN 访问域名（仅域名或 host[:port]）。
     */
    private String cdnDomain;
    /**
     * CDN 链接协议，当前环境无证书时可使用 http。
     */
    private String cdnScheme = "http";

    @Bean(destroyMethod = "shutdown")
    public OSS ossClient() {
        return new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }
}
