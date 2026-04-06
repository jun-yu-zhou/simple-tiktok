package com.example.simpletiktok.util.guardrail;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * DFA 树节点。
 * 用于维护敏感词路径状态。
 */
@Getter
@Setter
@NoArgsConstructor
public class DfaNode {

    /**
     * 子节点映射：字符 -> 下一个节点
     */
    private final Map<Character, DfaNode> children = new HashMap<>();

    /**
     * 是否为敏感词结束节点
     */
    private boolean end;
}

