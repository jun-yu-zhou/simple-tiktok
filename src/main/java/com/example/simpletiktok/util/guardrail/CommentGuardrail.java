package com.example.simpletiktok.util.guardrail;

import jakarta.annotation.PostConstruct;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 评论内容护栏（DFA/Trie 实现）。
 * 在评论入库前执行本地敏感词检测，命中则拒绝提交。
 */
@Component
@NoArgsConstructor
public class CommentGuardrail {

    /**
     * 最小可用敏感词词典。
     * 后续可按业务需要替换为 DB/配置中心动态加载。
     */
    private static final Set<String> SENSITIVE_WORDS = Set.of(
            "杀人", "暴力", "色情", "赌博", "毒品", "违法", "恐怖", "自杀", "炸弹", "抢劫",
            "杀生", "洗钱", "卖淫", "吸毒", "枪支", "砍人", "造谣", "绑架", "纵火", "邪教"
    );

    /**
     * DFA 根节点
     */
    private final DfaNode rootNode = new DfaNode();

    /**
     * Spring 初始化完成后构建 DFA 树。
     */
    @PostConstruct
    private void init() {
        initDfa();
    }

    /**
     * 判断评论文本是否安全。
     *
     * @param text 评论文本
     * @return true=安全，false=命中敏感词
     */
    public boolean isSafe(String text) {
        return findFirstHitWord(text) == null;
    }

    /**
     * 返回首个命中的敏感词；未命中返回 null。
     * 该方法便于后续在接口层返回更细粒度提示/审计日志。
     *
     * @param text 评论文本
     * @return 命中的敏感词（首个）或 null
     */
    public String findFirstHitWord(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        // 从每个起点尝试向后匹配，命中结束节点即返回命中词
        for (int start = 0; start < text.length(); start++) {
            DfaNode current = rootNode;
            for (int i = start; i < text.length(); i++) {
                char key = Character.toLowerCase(text.charAt(i));
                DfaNode next = current.getChildren().get(key);
                if (next == null) {
                    break;
                }
                if (next.isEnd()) {
                    return text.substring(start, i + 1);
                }
                current = next;
            }
        }
        return null;
    }

    /**
     * 初始化 DFA 树。
     */
    private void initDfa() {
        for (String word : SENSITIVE_WORDS) {
            if (word == null || word.isBlank()) {
                continue;
            }
            DfaNode current = rootNode;
            for (int i = 0; i < word.length(); i++) {
                char key = Character.toLowerCase(word.charAt(i));
                DfaNode child = current.getChildren().get(key);
                if (child == null) {
                    child = new DfaNode();
                    current.getChildren().put(key, child);
                }
                current = child;
            }
            current.setEnd(true);
        }
    }
}

