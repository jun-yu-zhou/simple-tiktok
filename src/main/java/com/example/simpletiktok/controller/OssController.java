package com.example.simpletiktok.controller;

import cn.hutool.core.util.StrUtil;
import com.example.simpletiktok.config.SecurityProperties;
import com.example.simpletiktok.pojo.entity.File;
import com.example.simpletiktok.pojo.vo.UploadFileVO;
import com.example.simpletiktok.service.IFileService;
import com.example.simpletiktok.service.OssService;
import com.example.simpletiktok.util.R;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/oss")
@RequiredArgsConstructor
public class OssController {

    private final OssService ossService;
    private final IFileService fileService;
    private final SecurityProperties securityProperties;

    @PostMapping("/upload")
    public R<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "objectName", required = false) String objectName) {
        String finalObjectName = ossService.uploadFile(file, objectName);

        File record = new File();
        record.setObjectName(finalObjectName);
        record.setOriginalName(file.getOriginalFilename());
        record.setContentType(file.getContentType());
        record.setSize(file.getSize());
        fileService.save(record);

        // 上传接口继续返回 url，用于前端即时回显
        UploadFileVO payload = new UploadFileVO();
        payload.setFileId(record.getId());
        payload.setObjectName(finalObjectName);
        payload.setUrl(ossService.getPublicUrl(finalObjectName));
        return R.ok().data(payload);
    }

    @GetMapping("/url/{objectName}")
    public R<?> getFileUrl(@PathVariable String objectName, HttpServletRequest request) {
        String referer = request.getHeader(HttpHeaders.REFERER);
        if (!isRefererAllowed(referer)) {
            return R.error().code(403).message("非法来源，禁止访问资源");
        }
        return R.ok().data(ossService.getPublicUrl(objectName));
    }

    private boolean isRefererAllowed(String referer) {
        if (StrUtil.isBlank(referer)) {
            return securityProperties.isAllowEmptyReferer();
        }
        if (securityProperties.getRefererWhitelist() == null || securityProperties.getRefererWhitelist().isEmpty()) {
            return false;
        }
        String ref = normalizePrefix(referer);
        for (String allow : securityProperties.getRefererWhitelist()) {
            String prefix = normalizePrefix(allow);
            if (StrUtil.isBlank(prefix)) {
                continue;
            }
            // 使用前缀匹配，避免 contains 导致伪造域名绕过
            if (ref.equals(prefix) || ref.startsWith(prefix + "/")) {
                return true;
            }
        }
        return false;
    }

    private String normalizePrefix(String value) {
        if (StrUtil.isBlank(value)) {
            return "";
        }
        String text = value.trim();
        while (text.endsWith("/")) {
            text = text.substring(0, text.length() - 1);
        }
        return text;
    }
}
