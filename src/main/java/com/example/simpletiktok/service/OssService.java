package com.example.simpletiktok.service;

import org.springframework.web.multipart.MultipartFile;

public interface OssService {

    String uploadFile(MultipartFile file, String objectName);

    /**
     * 获取 OSS 公网访问链接（用于服务内部链路）。
     */
    String getPublicUrl(String objectName);

    /**
     * 获取对前端下发的访问链接：优先 CDN，未配置时回退 OSS。
     */
    String getAccessUrl(String objectName);
}
