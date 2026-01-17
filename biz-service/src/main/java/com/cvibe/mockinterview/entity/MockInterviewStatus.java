package com.cvibe.mockinterview.entity;

/**
 * Mock interview session status
 */
public enum MockInterviewStatus {
    SETUP,        // Initial setup phase
    IN_PROGRESS,  // Interview in progress
    PAUSED,       // Interview paused
    ANALYZING,    // AI analyzing responses
    COMPLETED     // Interview completed with feedback
}
