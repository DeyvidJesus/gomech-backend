package com.gomech.dto.Crm;

public record AnalyzeMessageRequest(
        String message,
        String clientName,
        String action // analyze, respond, review_reminder, satisfaction_survey
) {}

