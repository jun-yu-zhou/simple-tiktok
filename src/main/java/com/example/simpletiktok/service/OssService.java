package com.example.simpletiktok.service;

import org.springframework.web.multipart.MultipartFile;

public interface OssService {

    String uploadFile(MultipartFile file, String objectName);

    String getPublicUrl(String objectName);
}
