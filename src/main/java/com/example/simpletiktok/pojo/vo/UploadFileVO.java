package com.example.simpletiktok.pojo.vo;

import lombok.Data;

/**
 * 文件上传结果返回对象。
 */
@Data
public class UploadFileVO {
    /**
     * 文件 ID。
     */
    private Long fileId;
    /**
     * OSS 对象名。
     */
    private String objectName;
    /**
     * 文件访问 URL。
     */
    private String url;
}
