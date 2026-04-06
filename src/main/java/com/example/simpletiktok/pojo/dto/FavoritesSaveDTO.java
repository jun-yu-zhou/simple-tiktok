package com.example.simpletiktok.pojo.dto;

import lombok.Data;

/**
 * 收藏夹新增/编辑参数。
 */
@Data
public class FavoritesSaveDTO {
    /**
     * 收藏夹 ID，新增可为空。
     */
    private Long id;
    /**
     * 收藏夹名称。
     */
    private String name;
    /**
     * 收藏夹描述。
     */
    private String description;
}
