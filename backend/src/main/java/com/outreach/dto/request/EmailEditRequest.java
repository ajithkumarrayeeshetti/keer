package com.outreach.dto.request;

import lombok.*;

@Getter @Setter
public class EmailEditRequest {
    private String subject;
    private String body;
}
