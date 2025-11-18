# AI 블로그 원고 생성기

Claude AI와 ChatGPT를 활용한 블로그 원고 자동 생성 및 치환 시스템

## 주요 기능

- Claude API를 사용한 원고 생성
- ChatGPT API를 사용한 원고 치환
- 프롬프트 입력 UI
- 생성할 원고 수 설정
- 작업 딜레이 설정 (API 오류 방지)
- 자동/수동 저장 기능
- 텍스트 파일로 저장

## 기술 스택

- **Backend**: Spring Boot 3.2.0
- **Frontend**: HTML, CSS, JavaScript
- **API**: Claude API, OpenAI ChatGPT API
- **Java**: 17
- **Build Tool**: Gradle

## 프로젝트 구조

```
ai-blog-generator/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/blog/generator/
│   │   │       ├── AiBlogGeneratorApplication.java
│   │   │       ├── controller/
│   │   │       │   └── BlogController.java
│   │   │       ├── service/
│   │   │       │   ├── ClaudeApiService.java
│   │   │       │   ├── ChatGptApiService.java
│   │   │       │   └── FileStorageService.java
│   │   │       └── dto/
│   │   │           ├── GenerateRequest.java
│   │   │           └── GenerateResponse.java
│   │   └── resources/
│   │       ├── application.properties
│   │       └── static/
│   │           ├── index.html
│   │           ├── style.css
│   │           └── app.js
├── generated-articles/    (생성된 원고 저장 폴더)
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── gradlew                (Unix용 Gradle Wrapper)
├── gradlew.bat            (Windows용 Gradle Wrapper)
├── build.gradle
├── settings.gradle
└── README.md
```

## 설치 및 실행

### 1. 사전 요구사항

- Java 17 이상
- Gradle 8.0 이상 (또는 포함된 Gradle Wrapper 사용)
- Claude API Key
- OpenAI API Key

### 2. API 키 설정

`src/main/resources/application.properties` 파일에서 API 키를 설정합니다:

```properties
# Claude API Configuration
claude.api.key=YOUR_CLAUDE_API_KEY_HERE

# ChatGPT API Configuration
chatgpt.api.key=YOUR_CHATGPT_API_KEY_HERE
```

### 3. 프로젝트 빌드

**Gradle Wrapper 사용 (권장):**
```bash
cd ai-blog-generator
./gradlew clean build
```

**또는 시스템 Gradle 사용:**
```bash
cd ai-blog-generator
gradle clean build
```

### 4. 애플리케이션 실행

**Gradle Wrapper 사용 (권장):**
```bash
./gradlew bootRun
```

**또는 시스템 Gradle 사용:**
```bash
gradle bootRun
```

**또는 빌드된 JAR 파일 실행:**
```bash
java -jar build/libs/ai-blog-generator-1.0.0.jar
```

### 5. 웹 브라우저에서 접속

```
http://localhost:8080
```

## 사용 방법

1. **프롬프트 입력**: 생성할 블로그 원고의 주제나 내용을 입력합니다
2. **원고 수 설정**: 생성할 원고의 개수를 설정합니다 (1-10개)
3. **딜레이 설정**: API 호출 간 대기 시간을 설정합니다 (기본 10초)
4. **자동 저장**: 체크박스를 선택하면 생성된 원고가 자동으로 저장됩니다
5. **원고 생성 시작**: 버튼을 클릭하여 원고 생성을 시작합니다
6. **결과 확인**: 생성된 원본 원고와 치환된 원고를 확인합니다
7. **복사 또는 저장**: 필요한 원고를 클립보드에 복사하거나 파일로 저장합니다

## API 엔드포인트

### POST /api/generate
원고 생성 요청

**Request Body:**
```json
{
  "prompt": "블로그 주제",
  "articleCount": 3,
  "delaySeconds": 10,
  "autoSave": true
}
```

**Response:**
```json
[
  {
    "success": true,
    "message": "원고 생성 성공",
    "originalContent": "Claude가 생성한 원고...",
    "replacedContent": "ChatGPT가 치환한 원고...",
    "articleNumber": 1,
    "savedFilePath": "generated-articles/article_1_20241118_123456.txt"
  }
]
```

### POST /api/save
원고 저장 요청

**Parameters:**
- `original`: 원본 원고
- `replaced`: 치환된 원고
- `number`: 원고 번호

### GET /api/health
서버 상태 확인

## 설정 옵션

`application.properties`에서 다음 옵션을 설정할 수 있습니다:

```properties
# 서버 포트
server.port=8080

# Claude API 모델
claude.api.model=claude-3-5-sonnet-20241022

# ChatGPT API 모델
chatgpt.api.model=gpt-4

# 파일 저장 경로
file.storage.path=generated-articles

# 로그 레벨
logging.level.com.blog.generator=DEBUG
```

## 생성된 파일 형식

생성된 원고는 다음 형식으로 저장됩니다:

```
==================== 원본 원고 ====================

[Claude AI가 생성한 원고 내용]


==================== 치환된 원고 ====================

[ChatGPT가 치환한 원고 내용]
```

파일명 형식: `article_[번호]_[날짜시간].txt`
예: `article_1_20241118_143025.txt`

## 주의사항

1. API 키는 절대 공개 저장소에 업로드하지 마세요
2. API 사용량에 따라 요금이 부과될 수 있습니다
3. 딜레이 시간을 너무 짧게 설정하면 API 오류가 발생할 수 있습니다
4. 대량의 원고 생성 시 시간이 오래 걸릴 수 있습니다

## 트러블슈팅

### API 오류 발생 시
- API 키가 올바르게 설정되었는지 확인
- 딜레이 시간을 늘려보세요 (15-20초)
- API 사용 한도를 확인하세요

### 파일 저장 실패 시
- `generated-articles` 폴더에 쓰기 권한이 있는지 확인
- 디스크 공간이 충분한지 확인

### 포트 충돌 시
- `application.properties`에서 `server.port`를 변경하세요

## 라이센스

MIT License

## 문의

문제가 발생하거나 개선 사항이 있으면 이슈를 등록해주세요.
