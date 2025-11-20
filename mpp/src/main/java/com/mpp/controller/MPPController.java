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
public class MPPController {

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

                    // Claude API로 원본 원고 생성
                    log.debug("Claude API 호출 시작");
                    String originalContent = claudeApiService.generateArticle(request.getPrompt());
                    if (originalContent == null || originalContent.trim().isEmpty()) {
                        throw new Exception("Claude API에서 빈 응답을 반환했습니다");
                    }
                    response.setOriginalContent(originalContent);
                    log.info("원본 원고 생성 완료 (길이: {}자)", originalContent.length());

                    // ChatGPT API로 원고 치환
                    log.debug("ChatGPT API 호출 시작");
                    String replacedContent = chatGptApiService.replaceContent(originalContent);
                    if (replacedContent == null || replacedContent.trim().isEmpty()) {
                        throw new Exception("ChatGPT API에서 빈 응답을 반환했습니다");
                    }
                    response.setReplacedContent(replacedContent);
                    log.info("치환 원고 생성 완료 (길이: {}자)", replacedContent.length());

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
                    log.error("원고 {} 생성 중 오류 발생: {}", i, e.getMessage(), e);
                    response.setSuccess(false);
                    response.setMessage("오류 발생: " + e.getMessage());
                    // 에러가 발생해도 이미 생성된 내용이 있으면 표시
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
