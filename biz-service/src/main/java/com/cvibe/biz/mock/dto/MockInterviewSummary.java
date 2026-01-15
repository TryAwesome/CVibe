package com.cvibe.biz.mock.dto;

import com.cvibe.biz.mock.entity.MockInterview.InterviewType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO for mock interview dashboard summary
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MockInterviewSummary {

    // Counts
    private Long totalInterviews;
    private Long completedInterviews;
    private Long inProgressInterviews;
    
    // Scores
    private Double averageScore;
    private Integer highestScore;
    private Integer lowestScore;
    
    // Time
    private Long totalPracticeMinutes;
    private Integer averageDurationMinutes;
    
    // Performance by type
    private Map<InterviewType, Double> scoreByType;
    
    // Trends
    private List<Integer> recentScores;
    private String performanceTrend;  // "improving", "stable", "declining"
    
    // Recommendations
    private List<String> weakAreas;
    private List<String> strongAreas;
    private String recommendedPractice;
    
    // Recent activity
    private MockInterviewDto latestInterview;
    private MockInterviewDto inProgressInterview;
    private List<MockInterviewDto> recentInterviews;

    /**
     * Calculate performance trend based on recent scores
     */
    public static String calculateTrend(List<Integer> scores) {
        if (scores == null || scores.size() < 3) {
            return "insufficient_data";
        }
        
        // Compare average of first half to second half
        int mid = scores.size() / 2;
        double firstHalf = scores.subList(0, mid).stream()
                .mapToInt(Integer::intValue).average().orElse(0);
        double secondHalf = scores.subList(mid, scores.size()).stream()
                .mapToInt(Integer::intValue).average().orElse(0);
        
        double diff = secondHalf - firstHalf;
        if (diff > 5) return "improving";
        if (diff < -5) return "declining";
        return "stable";
    }
}
