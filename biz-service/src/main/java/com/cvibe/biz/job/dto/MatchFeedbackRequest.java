package com.cvibe.biz.job.dto;

import lombok.Data;

@Data
public class MatchFeedbackRequest {
    private Integer rating;  // 1-5 stars
    private String feedback;
}
