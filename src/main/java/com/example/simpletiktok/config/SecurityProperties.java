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

    private List<String> refererWhitelist = new ArrayList<>();

    private boolean allowEmptyReferer = false;
}
