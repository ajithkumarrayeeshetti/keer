package com.outreach.dto.response;

import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class DashboardStatsResponse {
    private int    totalJobsUploaded;
    private int    emailsGenerated;
    private int    emailsSent;
    private int    replies;
    private int    interviews;
    private int    rejections;
    private int    followUpsPending;
    private double responseRate;
}
