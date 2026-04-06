package com.example.simpletiktok.pojo.vo;

import lombok.Data;

import java.util.List;

/**
 * 关注/粉丝分页结果。
 */
@Data
public class CustomerRelationPageVO {
    /**
     * 当前页。
     */
    private Long page;
    /**
     * 每页条数。
     */
    private Long limit;
    /**
     * 总记录数。
     */
    private Long total;
    /**
     * 当前页用户列表。
     */
    private List<CustomerRelationVO> records;
}
