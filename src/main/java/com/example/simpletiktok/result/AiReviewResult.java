package com.example.simpletiktok.result;

import lombok.Data;

@Data
public class AiReviewResult {
    private Double pornScore;
    private Double violenceScore;
    private Double politicalScore;
    private Double totalScore;
    private String reason;
}
