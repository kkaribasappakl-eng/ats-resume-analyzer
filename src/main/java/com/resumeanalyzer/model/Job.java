package com.resumeanalyzer.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String company;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Lob
    @Column(name = "required_skills", nullable = false, columnDefinition = "TEXT")
    private String requiredSkills; // Comma separated list of skills

    private String location;

    @Column(name = "experience_required")
    private String experienceRequired;
    
    private String salary;
    
    @Lob
    @Column(columnDefinition = "TEXT")
    private String keywords;
    
    @Lob
    @Column(columnDefinition = "TEXT")
    private String requirements;

    @Column(name = "company_logo")
    private String companyLogo;

    @Lob
    @Column(name = "company_description", columnDefinition = "TEXT")
    private String companyDescription;

    @Column(name = "work_mode")
    private String workMode; // Remote, Hybrid, On-site

    @Lob
    @Column(name = "preferred_skills", columnDefinition = "TEXT")
    private String preferredSkills;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String responsibilities;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String qualifications;

    @Column(name = "employment_type")
    private String employmentType; // Full-time, Internship

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    // Constructor matching original fields for backward compatibility
    public Job(Long id, String title, String company, String description, String requiredSkills, String location, String experienceRequired, String salary, String keywords, String requirements, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.company = company;
        this.description = description;
        this.requiredSkills = requiredSkills;
        this.location = location;
        this.experienceRequired = experienceRequired;
        this.salary = salary;
        this.keywords = keywords;
        this.requirements = requirements;
        this.createdAt = createdAt;
        // Default values for new fields
        this.companyLogo = "https://ui-avatars.com/api/?name=" + company + "&background=random";
        this.companyDescription = "A leading enterprise in the technology sector.";
        this.workMode = "Remote";
        this.preferredSkills = "Problem Solving, Teamwork, Communication";
        this.responsibilities = "Collaborate with developers and product managers to build innovative solutions.";
        this.qualifications = "B.E. / B.Tech / BCA / MCA in Computer Science or IT.";
        this.employmentType = "Full-time";
    }
}

