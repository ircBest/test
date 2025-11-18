package com.mpp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenerateResponse {
    private boolean success;
    private String message;
    private String originalContent;
    private String replacedContent;
    private int articleNumber;
    private String savedFilePath;
}
