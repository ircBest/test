package com.mpp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Service
public class ClaudeApiService {

    @Value("${claude.api.key}")
    private String apiKey;

    @Value("${claude.api.url}")
    private String apiUrl;

    @Value("${claude.api.model}")
    private String model;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ClaudeApiService.class);

    /**
     * Claude AI를 사용하여 블로그 원고 생성
     * @param prompt 기본 프롬프트
     * @param keyword 중요 키워드 (예: "콜레스테롤수치")
     * @param topic 주제 (예: "총콜레스테롤 정상수치")
     * @return 생성된 원고
     */
    public String generateArticle(String prompt, String keyword, String topic) throws Exception {
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new Exception("프롬프트가 비어있습니다");
        }
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        try {
            log.debug("Claude API 요청 준비 - URL: {}, Model: {}", apiUrl, model);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("x-api-key", apiKey);
            conn.setRequestProperty("anthropic-version", "2023-06-01");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            // 키워드와 주제가 있으면 프롬프트에 추가
            String enhancedPrompt = prompt;
            if (keyword != null && !keyword.isEmpty() && topic != null && !topic.isEmpty()) {
                log.debug("키워드와 주제 포함하여 프롬프트 강화 - 키워드: {}, 주제: {}", keyword, topic);
                enhancedPrompt = String.format(
                    "%s\n\n" +
                    "【작성 조건】\n" +
                    "- 주제: %s\n" +
                    "- 중요 키워드: %s (자연스럽게 포함)\n" +
                    "- 공백 제외 2500~3000자 분량\n" +
                    "- 목차 5개 구성\n" +
                    "- 각 문장은 20~30자 기준으로 줄바꿈하여 가독성 향상\n" +
                    "- 전문적이면서도 이해하기 쉬운 문체",
                    prompt,
                    topic,
                    keyword
                );
            }

            String requestBody = String.format(
                "{\"model\": \"%s\", \"max_tokens\": 4096, \"messages\": [{\"role\": \"user\", \"content\": \"%s\"}]}",
                model,
                escapeJson(enhancedPrompt)
            );

            log.debug("Claude API 요청 전송 중...");

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int status = conn.getResponseCode();
            log.debug("Claude API 응답 상태 코드: {}", status);

            BufferedReader reader;
            if (status >= 200 && status < 300) {
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            } else {
                reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
            }

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            if (status < 200 || status >= 300) {
                log.error("Claude API 오류 응답: {}", response.toString());
                throw new Exception("Claude API Error - HTTP " + status + ": " + response.toString());
            }

            JsonNode jsonNode = objectMapper.readTree(response.toString());
            JsonNode contentArray = jsonNode.get("content");

            if (contentArray != null && contentArray.isArray() && contentArray.size() > 0) {
                JsonNode firstContent = contentArray.get(0);
                if (firstContent.has("text")) {
                    String result = firstContent.get("text").asText().trim();
                    log.info("Claude API 응답 성공 (길이: {}자)", result.length());
                    return result;
                }
            }

            log.warn("Claude API 응답에 콘텐츠가 없습니다");
            throw new Exception("Claude API 응답에 콘텐츠가 없습니다");

        } finally {
            conn.disconnect();
        }
    }

    /**
     * 기존 메서드 호환성 유지 (키워드/주제 없이 사용)
     */
    public String generateArticle(String prompt) throws Exception {
        return generateArticle(prompt, "", "");
    }

    /**
     * JSON 문자열 이스케이프 처리
     */
    private String escapeJson(String text) {
        if (text == null) return "";

        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t")
                   .replace("\b", "\\b")
                   .replace("\f", "\\f");
    }
}
