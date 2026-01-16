package com.cvibe.biz.config;

import com.cvibe.biz.interview.entity.QuestionTemplate;
import com.cvibe.biz.interview.repository.QuestionTemplateRepository;
import com.cvibe.biz.user.entity.User;
import com.cvibe.biz.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.UUID;

/**
 * Development data initializer - creates admin user and sample data on startup
 */
@Configuration
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DevDataInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final QuestionTemplateRepository questionTemplateRepository;

    @Bean
    public CommandLineRunner initDevData() {
        return args -> {
            // Create admin user if not exists
            if (userRepository.findByEmail("admin@cvibe.com").isEmpty()) {
                User admin = User.builder()
                        .id(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                        .email("admin@cvibe.com")
                        .passwordHash(passwordEncoder.encode("Admin123456!"))
                        .fullName("System Admin")
                        .role(User.UserRole.ROLE_ADMIN)
                        .enabled(true)
                        .build();
                userRepository.save(admin);
                log.info("Created default admin user: admin@cvibe.com");
            }

            // Initialize question templates if empty
            if (questionTemplateRepository.count() == 0) {
                initQuestionTemplates();
                log.info("Initialized interview question templates");
            }
        };
    }

    private void initQuestionTemplates() {
        List<QuestionTemplate> templates = List.of(
                // Personal Info Questions
                QuestionTemplate.builder()
                        .questionText("Let's start with your background. Could you tell me about yourself and your professional journey so far?")
                        .category(QuestionTemplate.QuestionCategory.PERSONAL_INFO)
                        .questionType(QuestionTemplate.QuestionType.OPEN_ENDED)
                        .difficultyLevel(QuestionTemplate.DifficultyLevel.BASIC)
                        .orderWeight(10)
                        .isRequired(true)
                        .isActive(true)
                        .language("en")
                        .build(),
                
                // Education Questions
                QuestionTemplate.builder()
                        .questionText("What is your educational background? Please tell me about your degree(s), university, and major field of study.")
                        .category(QuestionTemplate.QuestionCategory.EDUCATION)
                        .questionType(QuestionTemplate.QuestionType.STRUCTURED)
                        .difficultyLevel(QuestionTemplate.DifficultyLevel.BASIC)
                        .expectedResponseType("education_info")
                        .orderWeight(20)
                        .isRequired(true)
                        .isActive(true)
                        .language("en")
                        .build(),
                
                QuestionTemplate.builder()
                        .questionText("When did you graduate? And what was your GPA or academic standing?")
                        .category(QuestionTemplate.QuestionCategory.EDUCATION)
                        .questionType(QuestionTemplate.QuestionType.STRUCTURED)
                        .difficultyLevel(QuestionTemplate.DifficultyLevel.BASIC)
                        .expectedResponseType("date_gpa")
                        .orderWeight(25)
                        .isRequired(false)
                        .isActive(true)
                        .language("en")
                        .build(),

                // Work Experience Questions
                QuestionTemplate.builder()
                        .questionText("Can you walk me through your most recent work experience? What company did you work for and what was your role?")
                        .category(QuestionTemplate.QuestionCategory.WORK_EXPERIENCE)
                        .questionType(QuestionTemplate.QuestionType.STRUCTURED)
                        .difficultyLevel(QuestionTemplate.DifficultyLevel.BASIC)
                        .expectedResponseType("work_experience")
                        .orderWeight(30)
                        .isRequired(true)
                        .isActive(true)
                        .language("en")
                        .build(),
                
                QuestionTemplate.builder()
                        .questionText("What were your main responsibilities in that role? What technologies or tools did you use?")
                        .category(QuestionTemplate.QuestionCategory.WORK_EXPERIENCE)
                        .questionType(QuestionTemplate.QuestionType.OPEN_ENDED)
                        .difficultyLevel(QuestionTemplate.DifficultyLevel.STANDARD)
                        .expectedResponseType("responsibilities_tech")
                        .orderWeight(35)
                        .isRequired(true)
                        .isActive(true)
                        .language("en")
                        .build(),

                QuestionTemplate.builder()
                        .questionText("Can you describe a significant project or achievement from this role?")
                        .category(QuestionTemplate.QuestionCategory.ACHIEVEMENTS)
                        .questionType(QuestionTemplate.QuestionType.OPEN_ENDED)
                        .difficultyLevel(QuestionTemplate.DifficultyLevel.STANDARD)
                        .orderWeight(40)
                        .isRequired(false)
                        .isActive(true)
                        .language("en")
                        .build(),

                // Skills Questions
                QuestionTemplate.builder()
                        .questionText("What are your core technical skills? Please list your strongest programming languages, frameworks, or tools.")
                        .category(QuestionTemplate.QuestionCategory.SKILLS)
                        .questionType(QuestionTemplate.QuestionType.STRUCTURED)
                        .difficultyLevel(QuestionTemplate.DifficultyLevel.BASIC)
                        .expectedResponseType("skill_list")
                        .orderWeight(50)
                        .isRequired(true)
                        .isActive(true)
                        .language("en")
                        .build(),
                
                QuestionTemplate.builder()
                        .questionText("How would you rate your proficiency in each of these skills? (Beginner, Intermediate, Advanced, Expert)")
                        .category(QuestionTemplate.QuestionCategory.SKILLS)
                        .questionType(QuestionTemplate.QuestionType.CLARIFICATION)
                        .difficultyLevel(QuestionTemplate.DifficultyLevel.STANDARD)
                        .orderWeight(55)
                        .isRequired(false)
                        .isActive(true)
                        .language("en")
                        .build(),

                // Career Goals
                QuestionTemplate.builder()
                        .questionText("What type of role are you looking for? What are your career goals for the next 2-3 years?")
                        .category(QuestionTemplate.QuestionCategory.CAREER_GOALS)
                        .questionType(QuestionTemplate.QuestionType.OPEN_ENDED)
                        .difficultyLevel(QuestionTemplate.DifficultyLevel.BASIC)
                        .orderWeight(60)
                        .isRequired(true)
                        .isActive(true)
                        .language("en")
                        .build(),

                // Projects
                QuestionTemplate.builder()
                        .questionText("Do you have any personal projects, open source contributions, or side projects you'd like to highlight?")
                        .category(QuestionTemplate.QuestionCategory.PROJECTS)
                        .questionType(QuestionTemplate.QuestionType.OPEN_ENDED)
                        .difficultyLevel(QuestionTemplate.DifficultyLevel.STANDARD)
                        .orderWeight(70)
                        .isRequired(false)
                        .isActive(true)
                        .language("en")
                        .build(),

                // Certifications
                QuestionTemplate.builder()
                        .questionText("Do you have any professional certifications? (e.g., AWS, Google Cloud, Kubernetes, etc.)")
                        .category(QuestionTemplate.QuestionCategory.CERTIFICATIONS)
                        .questionType(QuestionTemplate.QuestionType.STRUCTURED)
                        .difficultyLevel(QuestionTemplate.DifficultyLevel.BASIC)
                        .orderWeight(80)
                        .isRequired(false)
                        .isActive(true)
                        .language("en")
                        .build()
        );

        questionTemplateRepository.saveAll(templates);
    }
}
