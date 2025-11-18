package com.mpp.controller;

import com.mpp.dto.GenerateRequest;
import com.mpp.dto.GenerateResponse;
import com.mpp.service.ChatGptApiService;
import com.mpp.service.ClaudeApiService;
import com.mpp.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BlogController {

    private final ClaudeApiService claudeApiService;
    private final ChatGptApiService chatGptApiService;
    private final FileStorageService fileStorageService;

    @PostMapping("/generate")
    public ResponseEntity<List<GenerateResponse>> generateArticles(@RequestBody GenerateRequest request) {
        List<GenerateResponse> responses = new ArrayList<>();

        try {
            for (int i = 1; i <= request.getArticleCount(); i++) {
                GenerateResponse response = new GenerateResponse();
                response.setArticleNumber(i);

                try {
                    log.info("원고 {}/{} 생성 시작", i, request.getArticleCount());

                    String originalContent = claudeApiService.generateArticle(request.getPrompt());
                    response.setOriginalContent(originalContent);

                    String replacedContent = chatGptApiService.replaceContent(originalContent);
                    response.setReplacedContent(replacedContent);

                    if (request.isAutoSave()) {
                        String filePath = fileStorageService.saveArticleWithBothVersions(
                            originalContent,
                            replacedContent,
                            i
                        );
                        response.setSavedFilePath(filePath);
                        log.info("원고 {} 저장 완료: {}", i, filePath);
                    }

                    response.setSuccess(true);
                    response.setMessage("원고 생성 성공");

                    responses.add(response);

                    if (i < request.getArticleCount()) {
                        log.info("{}초 대기 중...", request.getDelaySeconds());
                        Thread.sleep(request.getDelaySeconds() * 1000L);
                    }

                } catch (Exception e) {
                    log.error("원고 {} 생성 중 오류 발생", i, e);
                    response.setSuccess(false);
                    response.setMessage("오류 발생: " + e.getMessage());
                    responses.add(response);
                }
            }

            return ResponseEntity.ok(responses);

        } catch (Exception e) {
            log.error("전체 작업 중 오류 발생", e);
            GenerateResponse errorResponse = new GenerateResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("전체 작업 오류: " + e.getMessage());
            responses.add(errorResponse);
            return ResponseEntity.internalServerError().body(responses);
        }
    }

    @PostMapping("/save")
    public ResponseEntity<GenerateResponse> saveArticle(
        @RequestParam String original,
        @RequestParam String replaced,
        @RequestParam int number
    ) {
        GenerateResponse response = new GenerateResponse();

        try {
            String filePath = fileStorageService.saveArticleWithBothVersions(original, replaced, number);
            response.setSuccess(true);
            response.setMessage("저장 성공");
            response.setSavedFilePath(filePath);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("파일 저장 중 오류 발생", e);
            response.setSuccess(false);
            response.setMessage("저장 오류: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("AI Blog Generator is running");
    }
}
