package com.example.simpletiktok.service.impl;

import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.aliyun.oss.OSS;
import com.example.simpletiktok.config.OssConfig;
import com.example.simpletiktok.exception.BizException;
import com.example.simpletiktok.service.OssService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

@Service
public class OssServiceImpl implements OssService {

    private final OSS ossClient;
    private final OssConfig ossConfig;

    public OssServiceImpl(OSS ossClient, OssConfig ossConfig) {
        this.ossClient = ossClient;
        this.ossConfig = ossConfig;
    }

    @Override
    public String uploadFile(MultipartFile file, String objectName) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file must not be empty");
        }

        String finalObjectName = StrUtil.isBlank(objectName)
                ? buildObjectName(file.getOriginalFilename())
                : objectName;

        try (InputStream inputStream = file.getInputStream()) {
            ossClient.putObject(ossConfig.getBucketName(), finalObjectName, inputStream);
        } catch (Exception e) {
            throw new BizException("文件上传失败");
        }
        return finalObjectName;
    }

    @Override
    public String getPublicUrl(String objectName) {
        if (StrUtil.isBlank(objectName)) {
            throw new IllegalArgumentException("objectName must not be blank");
        }
        if (StrUtil.isBlank(ossConfig.getBucketName())) {
            throw new IllegalArgumentException("bucketName must not be blank");
        }
        if (StrUtil.isBlank(ossConfig.getEndpoint())) {
            throw new IllegalArgumentException("endpoint must not be blank");
        }

        String cleanEndpoint = StrUtil.removePrefixIgnoreCase(
                StrUtil.removePrefixIgnoreCase(StrUtil.trim(ossConfig.getEndpoint()), "https://"),
                "http://"
        );
        String encodedObjectName = encodeObjectName(objectName);
        return "https://" + ossConfig.getBucketName() + "." + cleanEndpoint + "/" + encodedObjectName;
    }

    private String buildObjectName(String originalFilename) {
        String ext = FileNameUtil.extName(originalFilename);
        String suffix = StrUtil.isBlank(ext) ? "" : "." + ext;
        return System.currentTimeMillis() + "_" + IdUtil.fastSimpleUUID() + suffix;
    }

    private String encodeObjectName(String objectName) {
        return Arrays.stream(StrUtil.trim(objectName).split("/"))
                .map(segment -> URLEncoder.encode(segment, StandardCharsets.UTF_8).replace("+", "%20"))
                .collect(Collectors.joining("/"));
    }
}
