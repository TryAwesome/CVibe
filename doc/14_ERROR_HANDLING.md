# 错误处理规范

> 统一的错误处理和响应格式

---

## 1. 统一响应格式

### 1.1 成功响应

```json
{
  "success": true,
  "data": { ... },
  "message": null,
  "errorCode": null,
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### 1.2 错误响应

```json
{
  "success": false,
  "data": null,
  "message": "用户名或密码错误",
  "errorCode": 10001,
  "timestamp": "2024-01-15T10:30:00Z"
}
```

---

## 2. 响应包装类

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    
    private boolean success;
    private T data;
    private String message;
    private Integer errorCode;
    private Instant timestamp;

    /**
     * 成功响应（有数据）
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * 成功响应（无数据）
     */
    public static <T> ApiResponse<T> success() {
        return ApiResponse.<T>builder()
                .success(true)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * 错误响应
     */
    public static <T> ApiResponse<T> error(int errorCode, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .errorCode(errorCode)
                .message(message)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * 从错误码创建
     */
    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return ApiResponse.<T>builder()
                .success(false)
                .errorCode(errorCode.getCode())
                .message(errorCode.getMessage())
                .timestamp(Instant.now())
                .build();
    }
}
```

---

## 3. 错误码定义

### 3.1 错误码枚举

```java
@Getter
@AllArgsConstructor
public enum ErrorCode {
    
    // ==================== 系统错误 (1000-1999) ====================
    INTERNAL_ERROR(1000, HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"),
    SERVICE_UNAVAILABLE(1001, HttpStatus.SERVICE_UNAVAILABLE, "Service unavailable"),
    INVALID_REQUEST(1002, HttpStatus.BAD_REQUEST, "Invalid request"),
    RATE_LIMITED(1003, HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded"),

    // ==================== 认证错误 (10000-10999) ====================
    UNAUTHORIZED(10000, HttpStatus.UNAUTHORIZED, "Unauthorized"),
    INVALID_CREDENTIALS(10001, HttpStatus.UNAUTHORIZED, "Invalid email or password"),
    TOKEN_EXPIRED(10002, HttpStatus.UNAUTHORIZED, "Token expired"),
    TOKEN_INVALID(10003, HttpStatus.UNAUTHORIZED, "Invalid token"),
    EMAIL_ALREADY_EXISTS(10004, HttpStatus.BAD_REQUEST, "Email already exists"),
    USER_NOT_FOUND(10005, HttpStatus.NOT_FOUND, "User not found"),
    REFRESH_TOKEN_INVALID(10006, HttpStatus.UNAUTHORIZED, "Invalid refresh token"),
    ACCOUNT_DISABLED(10007, HttpStatus.FORBIDDEN, "Account is disabled"),

    // ==================== 权限错误 (20000-20999) ====================
    FORBIDDEN(20000, HttpStatus.FORBIDDEN, "Access denied"),
    INSUFFICIENT_PERMISSIONS(20001, HttpStatus.FORBIDDEN, "Insufficient permissions"),

    // ==================== Profile 错误 (30000-30999) ====================
    PROFILE_NOT_FOUND(30001, HttpStatus.NOT_FOUND, "Profile not found"),
    EXPERIENCE_NOT_FOUND(30002, HttpStatus.NOT_FOUND, "Experience not found"),
    SKILL_NOT_FOUND(30003, HttpStatus.NOT_FOUND, "Skill not found"),
    SKILL_ALREADY_EXISTS(30004, HttpStatus.BAD_REQUEST, "Skill already exists"),

    // ==================== Resume 错误 (40000-40999) ====================
    RESUME_NOT_FOUND(40001, HttpStatus.NOT_FOUND, "Resume not found"),
    FILE_EMPTY(40002, HttpStatus.BAD_REQUEST, "File is empty"),
    FILE_TOO_LARGE(40003, HttpStatus.BAD_REQUEST, "File too large (max 5MB)"),
    FILE_TYPE_NOT_ALLOWED(40004, HttpStatus.BAD_REQUEST, "File type not allowed"),
    FILE_UPLOAD_FAILED(40005, HttpStatus.INTERNAL_SERVER_ERROR, "File upload failed"),
    RESUME_PARSE_FAILED(40006, HttpStatus.INTERNAL_SERVER_ERROR, "Resume parsing failed"),

    // ==================== Resume Builder 错误 (50000-50999) ====================
    TEMPLATE_NOT_FOUND(50001, HttpStatus.NOT_FOUND, "Template not found"),
    PROFILE_EMPTY(50002, HttpStatus.BAD_REQUEST, "Profile is empty"),
    RESUME_GENERATION_FAILED(50003, HttpStatus.INTERNAL_SERVER_ERROR, "Resume generation failed"),

    // ==================== Interview 错误 (60000-60999) ====================
    SESSION_NOT_FOUND(60001, HttpStatus.NOT_FOUND, "Session not found"),
    INVALID_INTERVIEW_TYPE(60002, HttpStatus.BAD_REQUEST, "Invalid interview type"),
    SESSION_NOT_ACTIVE(60003, HttpStatus.BAD_REQUEST, "Session is not active"),
    SESSION_ALREADY_ENDED(60004, HttpStatus.BAD_REQUEST, "Session already ended"),

    // ==================== Mock Interview 错误 (70000-70999) ====================
    MOCK_SESSION_NOT_FOUND(70001, HttpStatus.NOT_FOUND, "Mock interview session not found"),
    QUESTION_INDEX_OUT_OF_RANGE(70002, HttpStatus.BAD_REQUEST, "Question index out of range"),
    FEEDBACK_NOT_READY(70003, HttpStatus.BAD_REQUEST, "Feedback not ready yet"),
    INVALID_MEDIA_FILE(70004, HttpStatus.BAD_REQUEST, "Invalid media file"),

    // ==================== Growth 错误 (80000-80999) ====================
    GOAL_NOT_FOUND(80001, HttpStatus.NOT_FOUND, "Goal not found"),
    INVALID_GOAL_STATUS(80002, HttpStatus.BAD_REQUEST, "Invalid goal status"),
    INVALID_DATE_FORMAT(80003, HttpStatus.BAD_REQUEST, "Invalid date format"),

    // ==================== Job 错误 (90000-90999) ====================
    JOB_NOT_FOUND(90001, HttpStatus.NOT_FOUND, "Job not found"),
    INVALID_SEARCH_PARAMS(90002, HttpStatus.BAD_REQUEST, "Invalid search parameters"),
    SEARCH_SERVICE_UNAVAILABLE(90003, HttpStatus.SERVICE_UNAVAILABLE, "Search service unavailable"),

    // ==================== Community 错误 (100000-100999) ====================
    POST_NOT_FOUND(100001, HttpStatus.NOT_FOUND, "Post not found"),
    COMMENT_NOT_FOUND(100002, HttpStatus.NOT_FOUND, "Comment not found"),
    INVALID_CATEGORY(100003, HttpStatus.BAD_REQUEST, "Invalid category"),
    CONTENT_TOO_LONG(100004, HttpStatus.BAD_REQUEST, "Content too long"),

    // ==================== Notification 错误 (110000-110999) ====================
    NOTIFICATION_NOT_FOUND(110001, HttpStatus.NOT_FOUND, "Notification not found"),
    INVALID_NOTIFICATION_TYPE(110002, HttpStatus.BAD_REQUEST, "Invalid notification type"),

    // ==================== Settings 错误 (120000-120999) ====================
    INVALID_PASSWORD(120001, HttpStatus.BAD_REQUEST, "Invalid current password"),
    PASSWORD_SAME_AS_OLD(120002, HttpStatus.BAD_REQUEST, "New password same as old"),
    INVALID_LANGUAGE(120003, HttpStatus.BAD_REQUEST, "Invalid language"),
    INVALID_RESPONSE_STYLE(120004, HttpStatus.BAD_REQUEST, "Invalid response style"),
    INVALID_DIFFICULTY(120005, HttpStatus.BAD_REQUEST, "Invalid difficulty"),

    // ==================== AI Service 错误 (130000-130999) ====================
    AI_SERVICE_UNAVAILABLE(130001, HttpStatus.SERVICE_UNAVAILABLE, "AI service unavailable"),
    AI_REQUEST_TIMEOUT(130002, HttpStatus.GATEWAY_TIMEOUT, "AI request timeout"),
    AI_RESPONSE_INVALID(130003, HttpStatus.INTERNAL_SERVER_ERROR, "Invalid AI response");

    private final int code;
    private final HttpStatus httpStatus;
    private final String message;
}
```

### 3.2 业务异常类

```java
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Object[] args;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.args = null;
    }

    public BusinessException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
        this.args = null;
    }

    public BusinessException(ErrorCode errorCode, Object... args) {
        super(String.format(errorCode.getMessage(), args));
        this.errorCode = errorCode;
        this.args = args;
    }
}
```

---

## 4. 全局异常处理器

```java
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        log.warn("Business exception: {}", e.getMessage());
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(ApiResponse.error(e.getErrorCode().getCode(), e.getMessage()));
    }

    /**
     * 参数校验异常 (JSR-303)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        
        log.warn("Validation failed: {}", message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCode.INVALID_REQUEST.getCode(), message));
    }

    /**
     * 约束违反异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
            ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining(", "));
        
        log.warn("Constraint violation: {}", message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCode.INVALID_REQUEST.getCode(), message));
    }

    /**
     * 请求体缺失
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMessageNotReadable(
            HttpMessageNotReadableException e) {
        log.warn("Message not readable: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCode.INVALID_REQUEST.getCode(), 
                        "Request body is missing or malformed"));
    }

    /**
     * 请求方法不支持
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException e) {
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error(ErrorCode.INVALID_REQUEST.getCode(), 
                        "Method " + e.getMethod() + " not supported"));
    }

    /**
     * 资源未找到
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoHandlerFound(
            NoHandlerFoundException e) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ErrorCode.INVALID_REQUEST.getCode(), 
                        "Endpoint not found: " + e.getRequestURL()));
    }

    /**
     * 认证异常
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(
            AuthenticationException e) {
        log.warn("Authentication failed: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ErrorCode.UNAUTHORIZED));
    }

    /**
     * 访问拒绝异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(
            AccessDeniedException e) {
        log.warn("Access denied: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ErrorCode.FORBIDDEN));
    }

    /**
     * 文件上传大小超限
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSize(
            MaxUploadSizeExceededException e) {
        log.warn("File too large: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCode.FILE_TOO_LARGE));
    }

    /**
     * 数据访问异常
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataAccessException(
            DataAccessException e) {
        log.error("Database error: {}", e.getMessage(), e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR));
    }

    /**
     * 其他未捕获异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAllExceptions(Exception e) {
        log.error("Unexpected error: {}", e.getMessage(), e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR.getCode(), 
                        "An unexpected error occurred"));
    }
}
```

---

## 5. 国际化支持

### 5.1 消息文件

`messages_zh.properties`:
```properties
error.10001=邮箱或密码错误
error.10002=登录已过期，请重新登录
error.10004=该邮箱已被注册
error.10005=用户不存在
error.30002=工作经历不存在
error.30004=该技能已添加
error.40002=请选择要上传的文件
error.40003=文件大小不能超过5MB
error.40004=不支持的文件格式，请上传 PDF 或 Word 文档
```

`messages_en.properties`:
```properties
error.10001=Invalid email or password
error.10002=Session expired, please login again
error.10004=Email already registered
error.10005=User not found
error.30002=Experience not found
error.30004=Skill already exists
error.40002=Please select a file to upload
error.40003=File size cannot exceed 5MB
error.40004=Unsupported file format, please upload PDF or Word document
```

### 5.2 消息源配置

```java
@Configuration
public class MessageConfig {

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = 
                new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:messages");
        messageSource.setDefaultEncoding("UTF-8");
        return messageSource;
    }
}
```

### 5.3 使用国际化消息

```java
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageSource messageSource;

    public String getMessage(ErrorCode errorCode, Locale locale) {
        try {
            return messageSource.getMessage(
                    "error." + errorCode.getCode(), 
                    null, 
                    errorCode.getMessage(),  // 默认消息
                    locale);
        } catch (Exception e) {
            return errorCode.getMessage();
        }
    }
}
```

---

## 6. 日志规范

### 6.1 日志格式

```java
// Controller 层 - 记录请求
log.info("Request: {} {} from user: {}", request.getMethod(), request.getRequestURI(), userId);

// Service 层 - 记录业务操作
log.info("Created goal {} for user: {}", goal.getId(), userId);

// 警告 - 业务异常
log.warn("Login failed for email: {}", email);

// 错误 - 系统异常
log.error("Failed to parse resume for user: {}", userId, e);
```

### 6.2 请求日志拦截器

```java
@Slf4j
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
            HttpServletResponse response, FilterChain filterChain) 
            throws ServletException, IOException {
        
        long startTime = System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        
        MDC.put("requestId", requestId);
        
        try {
            log.info(">>> {} {} from {}", 
                    request.getMethod(), 
                    request.getRequestURI(), 
                    request.getRemoteAddr());
            
            filterChain.doFilter(request, response);
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("<<< {} {} - {} in {}ms", 
                    request.getMethod(), 
                    request.getRequestURI(), 
                    response.getStatus(), 
                    duration);
        } finally {
            MDC.clear();
        }
    }
}
```

---

## 7. 前端错误处理

```typescript
// api.ts

interface ApiError {
  success: false;
  errorCode: number;
  message: string;
  timestamp: string;
}

const handleApiError = (error: ApiError): never => {
  switch (error.errorCode) {
    case 10002: // Token expired
    case 10003: // Token invalid
      // 清除 token 并跳转登录页
      localStorage.removeItem('token');
      window.location.href = '/login';
      break;
    case 10001: // Invalid credentials
      toast.error('邮箱或密码错误');
      break;
    case 40003: // File too large
      toast.error('文件太大，请选择小于5MB的文件');
      break;
    default:
      toast.error(error.message || '操作失败，请稍后重试');
  }
  throw error;
};
```
