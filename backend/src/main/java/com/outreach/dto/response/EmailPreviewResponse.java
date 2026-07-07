package com.outreach.dto.response;

import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class EmailPreviewResponse {
    private Long   emailId;
    private Long   jobId;
    private String company;
    private String role;
    private String hrName;
    private String hrEmail;
    private String subject;
    private String body;
    private String status;
    private String openedAt;
}
