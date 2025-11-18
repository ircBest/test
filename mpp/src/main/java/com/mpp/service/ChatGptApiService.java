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
public class ChatGptApiService {

    @Value("${chatgpt.api.key}")
    private String apiKey;

    @Value("${chatgpt.api.url}")
    private String apiUrl;

    @Value("${chatgpt.api.model}")
    private String model;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ChatGptApiService.class);

    public String replaceContent(String originalContent) throws Exception {
        if (originalContent == null || originalContent.trim().isEmpty()) {
            throw new Exception("원본 콘텐츠가 비어있습니다");
        }
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        try {
            log.debug("ChatGPT API 요청 준비 - URL: {}, Model: {}", apiUrl, model);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String systemMessage = "가독성 좋게 줄바꿈 처리해서 출력하고, 마침표 기준으로 줄바꿈 해줘.";
            String userMessage = "다음 텍스트를 자연스럽게 재작성해줘: " + originalContent;

            String requestBody = String.format(
                "{\"model\": \"%s\", \"messages\": [{\"role\": \"system\", \"content\": \"%s\"}, {\"role\": \"user\", \"content\": \"%s\"}], \"temperature\": 0.4, \"max_tokens\": 2000}",
                model,
                escapeJson(systemMessage),
                escapeJson(userMessage)
            );

            log.debug("ChatGPT API 요청 전송 중...");

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int status = conn.getResponseCode();
            log.debug("ChatGPT API 응답 상태 코드: {}", status);

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
                log.error("ChatGPT API 오류 응답: {}", response.toString());
                throw new Exception("ChatGPT API Error - HTTP " + status + ": " + response.toString());
            }

            JsonNode jsonNode = objectMapper.readTree(response.toString());

            if (jsonNode.has("choices") && jsonNode.get("choices").isArray()) {
                JsonNode choices = jsonNode.get("choices");
                log.debug("응답 choices 개수: {}", choices.size());
                if (choices.size() > 0) {
                    JsonNode firstChoice = choices.get(0);
                    if (firstChoice.has("message") && firstChoice.get("message").has("content")) {
                        String result = firstChoice.get("message").get("content").asText().trim();
                        log.info("ChatGPT API 응답 성공 (길이: {}자)", result.length());
                        return result;
                    }
                }
            }

            log.warn("ChatGPT API 응답에 콘텐츠가 없습니다");
            throw new Exception("ChatGPT API 응답에 콘텐츠가 없습니다");

        } finally {
            conn.disconnect();
        }
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}
