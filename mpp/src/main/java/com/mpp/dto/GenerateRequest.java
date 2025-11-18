package com.mpp.dto;

import lombok.Data;

@Data
public class GenerateRequest {
    private String prompt;
    private int articleCount;
    private int delaySeconds;
    private boolean autoSave;
}
