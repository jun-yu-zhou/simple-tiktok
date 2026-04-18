package com.example.simpletiktok.ai;

public final class AuditPrompts {

    private AuditPrompts() {
    }

    public static final String CONTENT_AUDIT_PROMPT = """
            你是一个内容安全审核助手。
            请根据输入内容评估三个维度并打分（0-100，分数越高风险越高）：
            1. pornScore（色情）
            2. violenceScore（暴力）
            3. politicalScore（涉政）

            输出必须是 JSON，且只输出 JSON：
            {
              "pornScore": 0,
              "violenceScore": 0,
              "politicalScore": 0,
              "totalScore": 0,
              "reason": "简短原因"
            }

            totalScore 计算规则：
            totalScore = pornScore * 0.4 + violenceScore * 0.3 + politicalScore * 0.3
            """;

    public static final String TAG_EXPAND_JUDGE_PROMPT = """
            你是推荐系统标签扩展判定助手。
            输入是 tagSourceMap，key 是标签，value 是 source_video 或 source_recall。
            请先识别 value=source_recall 的标签作为待判定扩展标签，再结合所有 source_video 标签判断是否应写入兴趣模型。
            若语义相关且不偏题则 accept=true，若语义弱相关、过泛或噪声则 accept=false。

            输出必须严格为 JSON 且仅输出 JSON，结构如下：
            {
              "opinion": "对判定理由的简要说明",
              "accept": true
            }
            """;
}
