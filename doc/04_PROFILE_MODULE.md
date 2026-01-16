# Profile 模块详细设计

> 用户资料模块是个人简历库的核心，所有 AI 功能都依赖此数据。

---

## 1. 模块结构

```
biz/profile/
├── controller/
│   └── ProfileController.java
├── service/
│   └── ProfileService.java
├── repository/
│   ├── UserProfileRepository.java
│   ├── ProfileExperienceRepository.java
│   └── ProfileSkillRepository.java
├── entity/
│   ├── UserProfile.java
│   ├── ProfileExperience.java
│   └── ProfileSkill.java
└── dto/
    ├── ProfileDto.java
    ├── ProfileRequest.java
    ├── ExperienceDto.java
    └── SkillDto.java
```

---

## 2. API 端点

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/profile` | 获取用户资料 |
| PUT | `/api/profile` | 更新用户资料 |
| GET | `/api/profile/experiences` | 获取工作经历列表 |
| POST | `/api/profile/experiences` | 添加工作经历 |
| PUT | `/api/profile/experiences/{id}` | 更新工作经历 |
| DELETE | `/api/profile/experiences/{id}` | 删除工作经历 |
| GET | `/api/profile/skills` | 获取技能列表 |
| POST | `/api/profile/skills` | 添加技能 |
| DELETE | `/api/profile/skills/{id}` | 删除技能 |

---

## 3. 前端期望的数据结构

### 3.1 Profile 响应

```typescript
interface Profile {
  id: string;
  userId: string;
  headline?: string;
  summary?: string;
  location?: string;
  experiences: Experience[];
  skills: Skill[];
  createdAt: string;
  updatedAt: string;
}
```

### 3.2 Experience

```typescript
interface Experience {
  id: string;
  company: string;
  title: string;
  location?: string;
  employmentType?: 'FULL_TIME' | 'PART_TIME' | 'CONTRACT' | 'INTERNSHIP' | 'FREELANCE';
  startDate: string;      // ISO date: "2022-01-01"
  endDate?: string;       // null if isCurrent
  isCurrent?: boolean;
  description?: string;
  achievements?: string[];  // ⚠️ 数组，不是 JSON 字符串
  technologies?: string[];  // ⚠️ 数组，不是 JSON 字符串
}
```

### 3.3 Skill

```typescript
interface Skill {
  id: string;
  name: string;
  level: string;  // "BEGINNER" | "INTERMEDIATE" | "ADVANCED" | "EXPERT"
}
```

---

## 4. 详细实现

### 4.1 Controller

```java
@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    /**
     * 获取用户资料
     * GET /api/profile
     */
    @GetMapping
    public ApiResponse<ProfileDto> getProfile(@AuthenticationPrincipal UserPrincipal principal) {
        ProfileDto profile = profileService.getOrCreateProfile(principal.getId());
        return ApiResponse.success(profile);
    }

    /**
     * 更新用户资料（基本信息）
     * PUT /api/profile
     */
    @PutMapping
    public ApiResponse<ProfileDto> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ProfileRequest request) {
        ProfileDto profile = profileService.updateProfile(principal.getId(), request);
        return ApiResponse.success(profile);
    }

    // ==================== Experience ====================

    /**
     * 获取工作经历列表
     * GET /api/profile/experiences
     */
    @GetMapping("/experiences")
    public ApiResponse<List<ExperienceDto>> getExperiences(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<ExperienceDto> experiences = profileService.getExperiences(principal.getId());
        return ApiResponse.success(experiences);
    }

    /**
     * 添加工作经历
     * POST /api/profile/experiences
     */
    @PostMapping("/experiences")
    public ApiResponse<ExperienceDto> addExperience(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ExperienceDto request) {
        ExperienceDto experience = profileService.addExperience(principal.getId(), request);
        return ApiResponse.success(experience);
    }

    /**
     * 更新工作经历
     * PUT /api/profile/experiences/{experienceId}
     */
    @PutMapping("/experiences/{experienceId}")
    public ApiResponse<ExperienceDto> updateExperience(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID experienceId,
            @Valid @RequestBody ExperienceDto request) {
        ExperienceDto experience = profileService.updateExperience(
                principal.getId(), experienceId, request);
        return ApiResponse.success(experience);
    }

    /**
     * 删除工作经历
     * DELETE /api/profile/experiences/{experienceId}
     */
    @DeleteMapping("/experiences/{experienceId}")
    public ApiResponse<Void> deleteExperience(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID experienceId) {
        profileService.deleteExperience(principal.getId(), experienceId);
        return ApiResponse.success();
    }

    // ==================== Skills ====================

    /**
     * 获取技能列表
     * GET /api/profile/skills
     */
    @GetMapping("/skills")
    public ApiResponse<List<SkillDto>> getSkills(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<SkillDto> skills = profileService.getSkills(principal.getId());
        return ApiResponse.success(skills);
    }

    /**
     * 添加技能
     * POST /api/profile/skills
     */
    @PostMapping("/skills")
    public ApiResponse<SkillDto> addSkill(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SkillDto request) {
        SkillDto skill = profileService.addSkill(principal.getId(), request);
        return ApiResponse.success(skill);
    }

    /**
     * 删除技能
     * DELETE /api/profile/skills/{skillId}
     */
    @DeleteMapping("/skills/{skillId}")
    public ApiResponse<Void> deleteSkill(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID skillId) {
        profileService.deleteSkill(principal.getId(), skillId);
        return ApiResponse.success();
    }
}
```

### 4.2 DTOs

```java
// ProfileDto.java - 返回给前端的完整 Profile
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileDto {
    private String id;
    private String userId;
    private String headline;
    private String summary;
    private String location;
    private List<ExperienceDto> experiences;
    private List<SkillDto> skills;
    private String createdAt;
    private String updatedAt;

    public static ProfileDto fromEntity(UserProfile profile) {
        return ProfileDto.builder()
                .id(profile.getId().toString())
                .userId(profile.getUser().getId().toString())
                .headline(profile.getHeadline())
                .summary(profile.getSummary())
                .location(profile.getLocation())
                .experiences(profile.getExperiences().stream()
                        .map(ExperienceDto::fromEntity)
                        .toList())
                .skills(profile.getSkills().stream()
                        .map(SkillDto::fromEntity)
                        .toList())
                .createdAt(profile.getCreatedAt().toString())
                .updatedAt(profile.getUpdatedAt().toString())
                .build();
    }
}

// ProfileRequest.java - 更新基本信息的请求
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileRequest {
    @Size(max = 200, message = "Headline too long")
    private String headline;
    
    private String summary;
    
    @Size(max = 100, message = "Location too long")
    private String location;
}

// ExperienceDto.java - ⚠️ 关键：数组字段的正确处理
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperienceDto {
    private String id;

    @NotBlank(message = "Company is required")
    @Size(max = 100, message = "Company name too long")
    private String company;

    @NotBlank(message = "Title is required")
    @Size(max = 100, message = "Title too long")
    private String title;

    @Size(max = 100, message = "Location too long")
    private String location;

    private String employmentType;  // FULL_TIME, PART_TIME, etc.

    @NotNull(message = "Start date is required")
    private String startDate;  // ISO date: "2022-01-01"

    private String endDate;

    private Boolean isCurrent;

    private String description;

    private List<String> achievements;   // ⚠️ 数组，不是字符串

    private List<String> technologies;   // ⚠️ 数组，不是字符串

    public static ExperienceDto fromEntity(ProfileExperience exp) {
        ExperienceDto dto = ExperienceDto.builder()
                .id(exp.getId().toString())
                .company(exp.getCompany())
                .title(exp.getTitle())
                .location(exp.getLocation())
                .employmentType(exp.getEmploymentType() != null 
                        ? exp.getEmploymentType().name() : null)
                .startDate(exp.getStartDate().toString())
                .endDate(exp.getEndDate() != null ? exp.getEndDate().toString() : null)
                .isCurrent(exp.getIsCurrent())
                .description(exp.getDescription())
                .build();

        // 解析 JSON 数组
        dto.setAchievements(parseJsonArray(exp.getAchievements()));
        dto.setTechnologies(parseJsonArray(exp.getTechnologies()));

        return dto;
    }

    private static List<String> parseJsonArray(String json) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}

// SkillDto.java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillDto {
    private String id;

    @NotBlank(message = "Skill name is required")
    @Size(max = 50, message = "Skill name too long")
    private String name;

    @NotBlank(message = "Level is required")
    private String level;  // BEGINNER, INTERMEDIATE, ADVANCED, EXPERT

    public static SkillDto fromEntity(ProfileSkill skill) {
        return SkillDto.builder()
                .id(skill.getId().toString())
                .name(skill.getName())
                .level(skill.getLevel())
                .build();
    }
}
```

### 4.3 Service

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserProfileRepository profileRepository;
    private final ProfileExperienceRepository experienceRepository;
    private final ProfileSkillRepository skillRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /**
     * 获取或创建用户资料
     */
    @Transactional
    public ProfileDto getOrCreateProfile(UUID userId) {
        return profileRepository.findByUserId(userId)
                .map(ProfileDto::fromEntity)
                .orElseGet(() -> createEmptyProfile(userId));
    }

    /**
     * 创建空资料
     */
    @Transactional
    public ProfileDto createEmptyProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        UserProfile profile = UserProfile.builder()
                .user(user)
                .build();

        profile = profileRepository.save(profile);
        log.info("Created empty profile for user: {}", userId);
        return ProfileDto.fromEntity(profile);
    }

    /**
     * 更新基本信息
     */
    @Transactional
    public ProfileDto updateProfile(UUID userId, ProfileRequest request) {
        UserProfile profile = getOrCreateProfileEntity(userId);

        profile.setHeadline(request.getHeadline());
        profile.setSummary(request.getSummary());
        profile.setLocation(request.getLocation());

        profile = profileRepository.save(profile);
        log.info("Updated profile for user: {}", userId);
        return ProfileDto.fromEntity(profile);
    }

    // ==================== Experience ====================

    /**
     * 获取工作经历列表
     */
    @Transactional(readOnly = true)
    public List<ExperienceDto> getExperiences(UUID userId) {
        return profileRepository.findByUserId(userId)
                .map(p -> p.getExperiences().stream()
                        .map(ExperienceDto::fromEntity)
                        .toList())
                .orElse(List.of());
    }

    /**
     * 添加工作经历
     */
    @Transactional
    public ExperienceDto addExperience(UUID userId, ExperienceDto dto) {
        UserProfile profile = getOrCreateProfileEntity(userId);

        ProfileExperience experience = ProfileExperience.builder()
                .profile(profile)
                .company(dto.getCompany())
                .title(dto.getTitle())
                .location(dto.getLocation())
                .employmentType(dto.getEmploymentType() != null 
                        ? EmploymentType.valueOf(dto.getEmploymentType()) : null)
                .startDate(LocalDate.parse(dto.getStartDate()))
                .endDate(dto.getEndDate() != null ? LocalDate.parse(dto.getEndDate()) : null)
                .isCurrent(dto.getIsCurrent() != null ? dto.getIsCurrent() : false)
                .description(dto.getDescription())
                .achievements(toJsonArray(dto.getAchievements()))
                .technologies(toJsonArray(dto.getTechnologies()))
                .build();

        experience = experienceRepository.save(experience);
        log.info("Added experience for user: {}", userId);
        return ExperienceDto.fromEntity(experience);
    }

    /**
     * 更新工作经历
     */
    @Transactional
    public ExperienceDto updateExperience(UUID userId, UUID experienceId, ExperienceDto dto) {
        ProfileExperience experience = experienceRepository.findById(experienceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXPERIENCE_NOT_FOUND));

        // 验证归属
        if (!experience.getProfile().getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        experience.setCompany(dto.getCompany());
        experience.setTitle(dto.getTitle());
        experience.setLocation(dto.getLocation());
        experience.setEmploymentType(dto.getEmploymentType() != null 
                ? EmploymentType.valueOf(dto.getEmploymentType()) : null);
        experience.setStartDate(LocalDate.parse(dto.getStartDate()));
        experience.setEndDate(dto.getEndDate() != null ? LocalDate.parse(dto.getEndDate()) : null);
        experience.setIsCurrent(dto.getIsCurrent() != null ? dto.getIsCurrent() : false);
        experience.setDescription(dto.getDescription());
        experience.setAchievements(toJsonArray(dto.getAchievements()));
        experience.setTechnologies(toJsonArray(dto.getTechnologies()));

        experience = experienceRepository.save(experience);
        log.info("Updated experience {} for user: {}", experienceId, userId);
        return ExperienceDto.fromEntity(experience);
    }

    /**
     * 删除工作经历
     */
    @Transactional
    public void deleteExperience(UUID userId, UUID experienceId) {
        ProfileExperience experience = experienceRepository.findById(experienceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXPERIENCE_NOT_FOUND));

        // 验证归属
        if (!experience.getProfile().getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        experienceRepository.delete(experience);
        log.info("Deleted experience {} for user: {}", experienceId, userId);
    }

    // ==================== Skills ====================

    /**
     * 获取技能列表
     */
    @Transactional(readOnly = true)
    public List<SkillDto> getSkills(UUID userId) {
        return profileRepository.findByUserId(userId)
                .map(p -> p.getSkills().stream()
                        .map(SkillDto::fromEntity)
                        .toList())
                .orElse(List.of());
    }

    /**
     * 添加技能
     */
    @Transactional
    public SkillDto addSkill(UUID userId, SkillDto dto) {
        UserProfile profile = getOrCreateProfileEntity(userId);

        // 检查是否已存在同名技能
        boolean exists = profile.getSkills().stream()
                .anyMatch(s -> s.getName().equalsIgnoreCase(dto.getName()));
        if (exists) {
            throw new BusinessException(ErrorCode.SKILL_ALREADY_EXISTS);
        }

        ProfileSkill skill = ProfileSkill.builder()
                .profile(profile)
                .name(dto.getName())
                .level(dto.getLevel())
                .build();

        skill = skillRepository.save(skill);
        log.info("Added skill {} for user: {}", dto.getName(), userId);
        return SkillDto.fromEntity(skill);
    }

    /**
     * 删除技能
     */
    @Transactional
    public void deleteSkill(UUID userId, UUID skillId) {
        ProfileSkill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SKILL_NOT_FOUND));

        // 验证归属
        if (!skill.getProfile().getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        skillRepository.delete(skill);
        log.info("Deleted skill {} for user: {}", skillId, userId);
    }

    // ==================== Helper ====================

    private UserProfile getOrCreateProfileEntity(UUID userId) {
        return profileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
                    UserProfile profile = UserProfile.builder().user(user).build();
                    return profileRepository.save(profile);
                });
    }

    private String toJsonArray(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            return null;
        }
    }
}
```

### 4.4 Repository

```java
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {
    
    Optional<UserProfile> findByUserId(UUID userId);
    
    boolean existsByUserId(UUID userId);
    
    @Query("SELECT p FROM UserProfile p " +
           "LEFT JOIN FETCH p.experiences " +
           "LEFT JOIN FETCH p.skills " +
           "WHERE p.user.id = :userId")
    Optional<UserProfile> findByUserIdWithAllDetails(@Param("userId") UUID userId);
}

public interface ProfileExperienceRepository extends JpaRepository<ProfileExperience, UUID> {
}

public interface ProfileSkillRepository extends JpaRepository<ProfileSkill, UUID> {
}
```

---

## 5. 错误码

| 错误码 | HTTP 状态 | 描述 |
|--------|----------|------|
| 30001 | 404 | Profile not found |
| 30002 | 404 | Experience not found |
| 30003 | 404 | Skill not found |
| 30004 | 400 | Skill already exists |
| 30005 | 403 | Forbidden (not owner) |

---

## 6. 常见 Bug 及修复

### Bug 1: `achievements` 和 `technologies` 返回 JSON 字符串而非数组
**问题**: 前端期望数组 `["Java", "Python"]`，后端返回字符串 `"[\"Java\", \"Python\"]"`

**修复**: 在 DTO 转换时解析 JSON
```java
dto.setAchievements(parseJsonArray(exp.getAchievements()));
```

### Bug 2: 日期格式不匹配
**问题**: 前端发送 `"2022-01-01"`，后端期望 `LocalDate`

**修复**: 使用 `LocalDate.parse(dto.getStartDate())`

### Bug 3: 新用户没有 Profile 导致 NPE
**问题**: 新注册用户访问 Profile 时返回 null

**修复**: 使用 `getOrCreateProfile` 自动创建空 Profile

### Bug 4: employmentType 枚举转换失败
**问题**: 前端发送 `"FULL_TIME"`，但后端无法转换

**修复**: 
```java
employmentType(dto.getEmploymentType() != null 
    ? EmploymentType.valueOf(dto.getEmploymentType()) : null)
```

### Bug 5: 级联删除问题
**问题**: 删除 Profile 时 Experience 没有被删除

**修复**: 使用 `CascadeType.ALL` 和 `orphanRemoval = true`
```java
@OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
private List<ProfileExperience> experiences = new ArrayList<>();
```

---

## 7. 测试用例

```java
@SpringBootTest
@AutoConfigureMockMvc
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(userId = "test-user-id")
    void addExperience_Success() throws Exception {
        ExperienceDto request = new ExperienceDto();
        request.setCompany("Google");
        request.setTitle("Software Engineer");
        request.setStartDate("2022-01-01");
        request.setEmploymentType("FULL_TIME");
        request.setIsCurrent(true);
        request.setAchievements(List.of("Increased performance by 50%"));
        request.setTechnologies(List.of("Java", "Python"));

        mockMvc.perform(post("/api/profile/experiences")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.company").value("Google"))
                .andExpect(jsonPath("$.data.achievements").isArray())
                .andExpect(jsonPath("$.data.achievements[0]").value("Increased performance by 50%"));
    }
}
```
