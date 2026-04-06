package com.example.simpletiktok.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String email;

    @TableField("nick_name")
    private String nickName;

    private String password;

    private String description;

    private Integer sex;

    private String avatar;

    @TableField("default_favorites_id")
    private Long defaultFavoritesId;

    @TableField("is_deleted")
    private Integer isDeleted;

    @TableField("gmt_created")
    private LocalDateTime gmtCreated;

    @TableField("gmt_updated")
    private LocalDateTime gmtUpdated;
}
