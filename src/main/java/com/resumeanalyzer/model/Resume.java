package com.resumeanalyzer.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "resumes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_type", nullable = false)
    private String fileType;

    @Lob
    @Column(name = "parsed_text", columnDefinition = "LONGTEXT")
    private String parsedText;

    @Column(name = "resume_name")
    private String resumeName;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "version_number")
    private Integer versionNumber;

    @Column(name = "status")
    private String status; // "Active" or "Archived"

    @Lob
    @Column(name = "file_data", columnDefinition = "LONGBLOB")
    private byte[] fileData;


    // Advanced Scoring Metrics
    @Column(name = "ats_score")
    private Integer atsScore;
    
    @Column(name = "keyword_match_score")
    private Integer keywordMatchScore;
    
    @Column(name = "skills_score")
    private Integer skillsScore;
    
    @Column(name = "education_score")
    private Integer educationScore;
    
    @Column(name = "project_score")
    private Integer projectScore;
    
    @Column(name = "experience_score")
    private Integer experienceScore;
    
    @Column(name = "internship_score")
    private Integer internshipScore;
    
    @Column(name = "certifications_score")
    private Integer certificationsScore;
    
    @Column(name = "structure_score")
    private Integer structureScore;
    
    @Column(name = "formatting_score")
    private Integer formattingScore;
    
    @Column(name = "grammar_score")
    private Integer grammarScore;

    @Column(name = "soft_skills_score")
    private Integer softSkillsScore;

    @Column(name = "projected_ats_score")
    private Integer projectedAtsScore;
    
    @Column(name = "career_readiness_score")
    private Integer careerReadinessScore;

    // Extracted contact details
    @Column(name = "extracted_name")
    private String extractedName;

    @Column(name = "extracted_email")
    private String extractedEmail;

    @Column(name = "extracted_phone")
    private String extractedPhone;

    @Column(name = "extracted_linkedin")
    private String extractedLinkedin;

    @Column(name = "extracted_github")
    private String extractedGithub;

    @Column(name = "extracted_portfolio")
    private String extractedPortfolio;

    @Column(name = "confidence_name")
    private Integer confidenceName;

    @Column(name = "confidence_email")
    private Integer confidenceEmail;

    @Column(name = "confidence_phone")
    private Integer confidencePhone;

    @Column(name = "confidence_linkedin")
    private Integer confidenceLinkedin;

    @Column(name = "confidence_github")
    private Integer confidenceGithub;

    @Column(name = "confidence_portfolio")
    private Integer confidencePortfolio;

    // Parsed Sections Text
    @Lob
    @Column(name = "section_summary", columnDefinition = "LONGTEXT")
    private String sectionSummary;

    @Lob
    @Column(name = "section_education", columnDefinition = "LONGTEXT")
    private String sectionEducation;

    @Lob
    @Column(name = "section_skills", columnDefinition = "LONGTEXT")
    private String sectionSkills;

    @Lob
    @Column(name = "section_projects", columnDefinition = "LONGTEXT")
    private String sectionProjects;

    @Lob
    @Column(name = "section_experience", columnDefinition = "LONGTEXT")
    private String sectionExperience;

    @Lob
    @Column(name = "section_certifications", columnDefinition = "LONGTEXT")
    private String sectionCertifications;

    @Lob
    @Column(name = "section_achievements", columnDefinition = "LONGTEXT")
    private String sectionAchievements;

    @Lob
    @Column(name = "section_languages", columnDefinition = "LONGTEXT")
    private String sectionLanguages;

    // Deep Analysis Findings
    @Lob
    @Column(name = "grammar_details", columnDefinition = "TEXT")
    private String grammarDetails;

    @Lob
    @Column(name = "formatting_details", columnDefinition = "TEXT")
    private String formattingDetails;

    @Lob
    @Column(name = "project_analysis", columnDefinition = "TEXT")
    private String projectAnalysis;

    @Lob
    @Column(name = "interview_questions", columnDefinition = "TEXT")
    private String interviewQuestions;

    @Lob
    @Column(name = "soft_skills_analysis", columnDefinition = "LONGTEXT")
    private String softSkillsAnalysis;

    @Lob
    @Column(name = "keyword_analysis_details", columnDefinition = "LONGTEXT")
    private String keywordAnalysisDetails;

    @Lob
    @Column(name = "recruiter_summary", columnDefinition = "LONGTEXT")
    private String recruiterSummary;

    @Lob
    @Column(name = "improvement_roadmap_details", columnDefinition = "LONGTEXT")
    private String improvementRoadmapDetails;

    @Lob
    @Column(name = "explainable_scoring_details", columnDefinition = "LONGTEXT")
    private String explainableScoringDetails;

    @Lob
    @Column(name = "score_difference_reason", columnDefinition = "TEXT")
    private String scoreDifferenceReason;

    // Analysis Details
    @Column(name = "skills_found", columnDefinition = "TEXT")
    private String skillsFound;

    @Column(name = "skills_missing", columnDefinition = "TEXT")
    private String skillsMissing;

    @Column(name = "missing_information", columnDefinition = "TEXT")
    private String missingInformation;

    @Column(name = "present_sections", columnDefinition = "TEXT")
    private String presentSections;

    @Column(name = "missing_sections", columnDefinition = "TEXT")
    private String missingSections;

    @Column(name = "suggestions", columnDefinition = "TEXT")
    private String suggestions;

    @Column(name = "improvement_roadmap", columnDefinition = "TEXT")
    private String improvementRoadmap;

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    @Column(name = "strengths", columnDefinition = "TEXT")
    private String strengths;

    @Column(name = "weaknesses", columnDefinition = "TEXT")
    private String weaknesses;

    @Column(name = "hr_recommendations", columnDefinition = "TEXT")
    private String hrRecommendations;

    @Column(name = "interview_prep_tips", columnDefinition = "TEXT")
    private String interviewPrepTips;

    @Column(name = "job_title_matched")
    private String jobTitleMatched;

    @Column(name = "match_percentage")
    private Integer matchPercentage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
