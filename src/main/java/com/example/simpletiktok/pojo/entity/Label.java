package com.example.simpletiktok.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("label")
public class Label implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String name;

    @TableField("name_norm")
    private String nameNorm;

    @TableField("vector_status")
    private Integer vectorStatus;

    @TableField("qdrant_point_id")
    private String qdrantPointId;

    @TableField("gmt_created")
    private LocalDateTime gmtCreated;

    @TableField("gmt_updated")
    private LocalDateTime gmtUpdated;
}
