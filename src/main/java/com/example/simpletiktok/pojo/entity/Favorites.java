package com.example.simpletiktok.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("favorites")
public class Favorites implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    @TableField("user_id")
    private Long userId;

    @TableField("is_deleted")
    private Integer isDeleted;

    @TableField("gmt_created")
    private LocalDateTime gmtCreated;

    @TableField("gmt_updated")
    private LocalDateTime gmtUpdated;

    @TableField(exist = false)
    private Long videoCount;
}
