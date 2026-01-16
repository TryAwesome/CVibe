# Auth 模块详细设计

> 认证模块是整个系统的入口，必须确保安全性和可靠性。

---

## 1. 模块结构

```
biz/user/
├── controller/
│   └── AuthController.java
├── service/
│   └── AuthService.java
├── repository/
│   └── UserRepository.java
├── entity/
│   └── User.java
└── dto/
    ├── LoginRequest.java
    ├── RegisterRequest.java
    ├── GoogleLoginRequest.java
    ├── RefreshTokenRequest.java
    ├── AuthResponse.java
    └── UserDto.java
```

---

## 2. API 端点

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/api/auth/register` | 注册 |
| POST | `/api/auth/login` | 登录 |
| POST | `/api/auth/google` | Google 登录 |
| POST | `/api/auth/refresh` | 刷新 Token |
| GET | `/api/auth/me` | 获取当前用户 |
| POST | `/api/auth/logout` | 登出 |

---

## 3. 详细实现

### 3.1 Controller

```java
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 注册
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ApiResponse.success(response);
    }

    /**
     * 登录
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ApiResponse.success(response);
    }

    /**
     * Google 登录
     * POST /api/auth/google
     */
    @PostMapping("/google")
    public ApiResponse<AuthResponse> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        AuthResponse response = authService.googleLogin(request);
        return ApiResponse.success(response);
    }

    /**
     * 刷新 Token
     * POST /api/auth/refresh
     */
    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request.getRefreshToken());
        return ApiResponse.success(response);
    }

    /**
     * 获取当前用户
     * GET /api/auth/me
     */
    @GetMapping("/me")
    public ApiResponse<UserDto> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        UserDto user = authService.getCurrentUser(principal.getId());
        return ApiResponse.success(user);
    }

    /**
     * 登出
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        // 前端清除 Token 即可，无需后端处理
        // 如需黑名单机制，可在此实现
        return ApiResponse.success();
    }
}
```

### 3.2 DTOs

```java
// RegisterRequest.java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
             message = "Password must contain uppercase, lowercase and number")
    private String password;

    @Size(max = 100, message = "Nickname too long")
    private String nickname;
}

// LoginRequest.java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}

// GoogleLoginRequest.java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoogleLoginRequest {
    @NotBlank(message = "ID token is required")
    private String idToken;
}

// RefreshTokenRequest.java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequest {
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}

// AuthResponse.java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;
    private UserDto user;

    public static AuthResponse of(String accessToken, String refreshToken, 
                                   long expiresIn, UserDto user) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .user(user)
                .build();
    }
}

// UserDto.java - 前端期望的 User 结构
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private String id;
    private String email;
    private String nickname;      // ⚠️ 注意：前端用 nickname，不是 fullName
    private String role;
    private boolean hasPassword;  // passwordHash != null
    private String createdAt;
    private boolean googleUser;   // googleSub != null

    public static UserDto fromEntity(User user) {
        return UserDto.builder()
                .id(user.getId().toString())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .role(user.getRole().name())
                .hasPassword(user.getPasswordHash() != null)
                .createdAt(user.getCreatedAt().toString())
                .googleUser(user.getGoogleSub() != null)
                .build();
    }
}
```

### 3.3 Service

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 注册新用户
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // 1. 检查邮箱是否已存在
        if (userRepository.existsByEmail(request.getEmail().toLowerCase())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // 2. 创建用户
        User user = User.builder()
                .email(request.getEmail().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .role(UserRole.ROLE_USER)
                .enabled(true)
                .build();

        user = userRepository.save(user);
        log.info("New user registered: {}", user.getEmail());

        // 3. 生成 Token 并返回
        return generateAuthResponse(user);
    }

    /**
     * 邮箱密码登录
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        // 1. 查找用户
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        // 2. 检查账户状态
        if (!user.getEnabled()) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }

        // 3. 检查是否有密码（Google 用户可能没有）
        if (user.getPasswordHash() == null) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS,
                    "This account was created via Google. Please use Google to sign in.");
        }

        // 4. 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 5. 更新最后登录时间
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);
        log.info("User logged in: {}", user.getEmail());

        return generateAuthResponse(user);
    }

    /**
     * Google 登录
     */
    @Transactional
    public AuthResponse googleLogin(GoogleLoginRequest request) {
        // 1. 验证 Google ID Token
        GoogleUserInfo googleUser = verifyGoogleToken(request.getIdToken());

        // 2. 查找或创建用户
        User user = userRepository.findByGoogleSub(googleUser.getSub())
                .orElseGet(() -> userRepository.findByEmail(googleUser.getEmail())
                        .map(existingUser -> {
                            // 关联 Google 账号到已有用户
                            existingUser.setGoogleSub(googleUser.getSub());
                            if (existingUser.getAvatarUrl() == null) {
                                existingUser.setAvatarUrl(googleUser.getPicture());
                            }
                            return userRepository.save(existingUser);
                        })
                        .orElseGet(() -> createGoogleUser(googleUser)));

        // 3. 检查账户状态
        if (!user.getEnabled()) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }

        // 4. 更新最后登录时间
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);
        log.info("User logged in via Google: {}", user.getEmail());

        return generateAuthResponse(user);
    }

    /**
     * 刷新 Token
     */
    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        // 1. 验证 Token
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        // 2. 检查是否是 Refresh Token
        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "Not a refresh token");
        }

        // 3. 获取用户
        UUID userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 4. 检查账户状态
        if (!user.getEnabled()) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }

        return generateAuthResponse(user);
    }

    /**
     * 获取当前用户
     */
    @Transactional(readOnly = true)
    public UserDto getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return UserDto.fromEntity(user);
    }

    /**
     * 生成认证响应
     */
    private AuthResponse generateAuthResponse(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);
        long expiresIn = jwtTokenProvider.getAccessTokenExpiration() / 1000;

        return AuthResponse.of(accessToken, refreshToken, expiresIn, UserDto.fromEntity(user));
    }

    /**
     * 验证 Google ID Token
     */
    private GoogleUserInfo verifyGoogleToken(String idToken) {
        // TODO: 实现 Google Token 验证
        // 可以使用 Google API Client Library 或直接调用 tokeninfo endpoint
        throw new UnsupportedOperationException("Google login not implemented");
    }

    /**
     * 创建 Google 用户
     */
    private User createGoogleUser(GoogleUserInfo googleUser) {
        User user = User.builder()
                .email(googleUser.getEmail())
                .googleSub(googleUser.getSub())
                .nickname(googleUser.getName())
                .avatarUrl(googleUser.getPicture())
                .role(UserRole.ROLE_USER)
                .enabled(true)
                .build();
        return userRepository.save(user);
    }
}
```

### 3.4 JWT Token Provider

```java
@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration) {
        
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    /**
     * 生成 Access Token
     */
    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put("email", user.getEmail());
        claims.put("role", user.getRole().name());
        claims.put("type", "access");

        return buildToken(claims, user.getId().toString(), accessTokenExpiration);
    }

    /**
     * 生成 Refresh Token
     */
    public String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put("type", "refresh");

        return buildToken(claims, user.getId().toString(), refreshTokenExpiration);
    }

    private String buildToken(Map<String, Object> claims, String subject, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 从 Token 获取用户 ID
     */
    public UUID getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        String userId = claims.get("userId", String.class);
        return UUID.fromString(userId);
    }

    /**
     * 验证 Token
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 判断是否是 Refresh Token
     */
    public boolean isRefreshToken(String token) {
        Claims claims = parseToken(token);
        return "refresh".equals(claims.get("type", String.class));
    }

    /**
     * 判断是否是 Access Token
     */
    public boolean isAccessToken(String token) {
        Claims claims = parseToken(token);
        return "access".equals(claims.get("type", String.class));
    }

    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
```

---

## 4. 错误码

| 错误码 | HTTP 状态 | 描述 |
|--------|----------|------|
| 20001 | 401 | Invalid credentials |
| 20002 | 400 | Email already exists |
| 20003 | 403 | Account disabled |
| 20004 | 401 | Invalid token |
| 20005 | 404 | User not found |
| 20006 | 400 | Password required |
| 20007 | 401 | Google auth failed |

---

## 5. 配置

```yaml
# application.yml
jwt:
  secret: ${JWT_SECRET:your-256-bit-secret-key-here-at-least-32-chars}
  access-token-expiration: 3600000   # 1 hour in milliseconds
  refresh-token-expiration: 604800000 # 7 days in milliseconds

google:
  oauth2:
    client-id: ${GOOGLE_CLIENT_ID:}
```

---

## 6. 安全配置

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final String[] PUBLIC_ENDPOINTS = {
            "/auth/**",
            "/health",
            "/actuator/**",
            "/swagger-ui/**",
            "/v3/api-docs/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> 
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, 
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

---

## 7. 测试用例

```java
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void register_Success() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("Password123");
        request.setNickname("Test User");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.user.email").value("test@example.com"));
    }

    @Test
    void login_InvalidCredentials() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("wrong@example.com");
        request.setPassword("wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value(20001));
    }
}
```

---

## 8. 常见 Bug 及修复

### Bug 1: 前端用 `nickname` 但后端返回 `fullName`
**修复**: 确保 `UserDto` 使用 `nickname` 字段名

### Bug 2: 密码验证不区分大小写
**修复**: 邮箱转小写，密码保持原样

### Bug 3: Google 用户尝试密码登录
**修复**: 检查 `passwordHash` 是否存在，给出友好提示

### Bug 4: Token 过期后前端无法刷新
**修复**: 确保 Refresh Token 有效期足够长，正确返回 `expiresIn`
