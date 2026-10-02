package com.resumeanalyzer.controller;

import com.resumeanalyzer.model.Job;
import com.resumeanalyzer.model.Resume;
import com.resumeanalyzer.model.User;
import com.resumeanalyzer.dto.JobMatchDto;
import com.resumeanalyzer.repository.ResumeRepository;
import com.resumeanalyzer.service.JobService;
import com.resumeanalyzer.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/jobs")
public class JobController {

    private final JobService jobService;
    private final UserService userService;
    private final ResumeRepository resumeRepository;

    public JobController(JobService jobService, UserService userService, ResumeRepository resumeRepository) {
        this.jobService = jobService;
        this.userService = userService;
        this.resumeRepository = resumeRepository;
    }

    @GetMapping
    public String viewJobs(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "company", required = false) String company,
            @RequestParam(value = "role", required = false) String role,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "skills", required = false) String skills,
            @RequestParam(value = "experience", required = false) String experience,
            @RequestParam(value = "salary", required = false) String salary,
            @RequestParam(value = "workMode", required = false) String workMode,
            @RequestParam(value = "sortBy", defaultValue = "latest") String sortBy,
            @RequestParam(value = "resumeId", required = false) Long resumeId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            Model model) {

        User user = userService.getCurrentUser();
        List<Resume> history = resumeRepository.findByUserOrderByCreatedAtDesc(user);
        
        Resume selectedResume = null;
        if (resumeId != null) {
            Optional<Resume> opt = resumeRepository.findById(resumeId);
            if (opt.isPresent() && opt.get().getUser().getId().equals(user.getId())) {
                selectedResume = opt.get();
            }
        }
        
        if (selectedResume == null) {
            List<Resume> activeResumes = resumeRepository.findByUserAndStatus(user, "Active");
            selectedResume = activeResumes.isEmpty() ? (history.isEmpty() ? null : history.get(0)) : activeResumes.get(0);
        }

        List<Job> allJobs = jobService.getAllJobs();

        // 1. Calculate ATS Match percentage and metrics for all jobs
        List<JobMatchDto> matchedJobs = new ArrayList<>();
        for (Job job : allJobs) {
            matchedJobs.add(calculateMatch(job, selectedResume));
        }

        // 2. Perform filtering in Java (for flexibility and speed across dynamic fields)
        List<JobMatchDto> filteredJobs = matchedJobs.stream()
            .filter(dto -> {
                Job job = dto.getJob();
                
                // Search filter (searches title, company, skills, description)
                if (search != null && !search.trim().isEmpty()) {
                    String q = search.toLowerCase();
                    boolean match = (job.getTitle() != null && job.getTitle().toLowerCase().contains(q))
                                 || (job.getCompany() != null && job.getCompany().toLowerCase().contains(q))
                                 || (job.getRequiredSkills() != null && job.getRequiredSkills().toLowerCase().contains(q))
                                 || (job.getDescription() != null && job.getDescription().toLowerCase().contains(q));
                    if (!match) return false;
                }
                
                // Company filter
                if (company != null && !company.trim().isEmpty() && !company.equals("All")) {
                    if (job.getCompany() == null || !job.getCompany().equalsIgnoreCase(company.trim())) {
                        return false;
                    }
                }
                
                // Role filter
                if (role != null && !role.trim().isEmpty() && !role.equals("All")) {
                    String r = role.toLowerCase();
                    if (job.getTitle() == null || !job.getTitle().toLowerCase().contains(r)) {
                        return false;
                    }
                }
                
                // Location filter
                if (location != null && !location.trim().isEmpty() && !location.equals("All")) {
                    String loc = location.toLowerCase();
                    if (job.getLocation() == null || !job.getLocation().toLowerCase().contains(loc)) {
                        return false;
                    }
                }
                
                // Skills filter
                if (skills != null && !skills.trim().isEmpty()) {
                    String sk = skills.toLowerCase();
                    boolean skillMatch = (job.getRequiredSkills() != null && job.getRequiredSkills().toLowerCase().contains(sk))
                                      || (job.getPreferredSkills() != null && job.getPreferredSkills().toLowerCase().contains(sk));
                    if (!skillMatch) return false;
                }
                
                // Experience filter
                if (experience != null && !experience.trim().isEmpty() && !experience.equals("All")) {
                    String exp = experience.toLowerCase();
                    if (job.getExperienceRequired() == null || !job.getExperienceRequired().toLowerCase().contains(exp)) {
                        return false;
                    }
                }
                
                // Salary filter
                if (salary != null && !salary.trim().isEmpty() && !salary.equals("All")) {
                    String sal = salary.toLowerCase();
                    if (job.getSalary() == null || !job.getSalary().toLowerCase().contains(sal)) {
                        return false;
                    }
                }
                
                // Remote/Hybrid/On-site workMode filter
                if (workMode != null && !workMode.trim().isEmpty() && !workMode.equals("All")) {
                    if (job.getWorkMode() == null || !job.getWorkMode().equalsIgnoreCase(workMode.trim())) {
                        return false;
                    }
                }
                
                return true;
            })
            .collect(Collectors.toList());

        // 3. Sorting
        if ("highestSalary".equalsIgnoreCase(sortBy)) {
            filteredJobs.sort((a, b) -> Double.compare(parseSalaryToLpa(b.getJob().getSalary()), parseSalaryToLpa(a.getJob().getSalary())));
        } else if ("atsMatch".equalsIgnoreCase(sortBy)) {
            filteredJobs.sort((a, b) -> Integer.compare(b.getMatchPercentage(), a.getMatchPercentage()));
        } else if ("companyName".equalsIgnoreCase(sortBy)) {
            filteredJobs.sort((a, b) -> a.getJob().getCompany().compareToIgnoreCase(b.getJob().getCompany()));
        } else { // default to "latest"
            filteredJobs.sort((a, b) -> {
                LocalDateTime dateA = a.getJob().getCreatedAt() != null ? a.getJob().getCreatedAt() : LocalDateTime.MIN;
                LocalDateTime dateB = b.getJob().getCreatedAt() != null ? b.getJob().getCreatedAt() : LocalDateTime.MIN;
                return dateB.compareTo(dateA); // Newest first
            });
        }

        // 4. Pagination
        int totalElements = filteredJobs.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        if (totalPages == 0) totalPages = 1;
        
        // Ensure page is within valid range
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;
        
        int start = page * size;
        int end = Math.min(start + size, totalElements);
        
        List<JobMatchDto> paginatedList = new ArrayList<>();
        if (start < totalElements) {
            paginatedList = filteredJobs.subList(start, end);
        }

        // 5. Gather filter dropdown options (unique companies, locations, and roles from seeded jobs)
        List<String> allCompanies = allJobs.stream().map(Job::getCompany).distinct().sorted().collect(Collectors.toList());
        List<String> allLocations = allJobs.stream().map(Job::getLocation).filter(l -> l != null && !l.isEmpty()).distinct().sorted().collect(Collectors.toList());
        
        // Extract standard clean roles (titles without "- Intern" or "Associate" prefixes)
        List<String> allRoles = allJobs.stream()
            .map(j -> j.getTitle()
                .replaceAll("(?i)Associate ", "")
                .replaceAll("(?i) - Intern", "")
                .replaceAll("(?i)Graduate ", "")
                .replaceAll("(?i) Trainee", "")
                .trim()
            )
            .distinct()
            .sorted()
            .collect(Collectors.toList());

        model.addAttribute("jobs", paginatedList);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalElements", totalElements);
        model.addAttribute("pageSize", size);

        model.addAttribute("companiesList", allCompanies);
        model.addAttribute("locationsList", allLocations);
        model.addAttribute("rolesList", allRoles);

        // Keep filter state in view
        model.addAttribute("search", search);
        model.addAttribute("selectedCompany", company);
        model.addAttribute("selectedRole", role);
        model.addAttribute("selectedLocation", location);
        model.addAttribute("selectedSkills", skills);
        model.addAttribute("selectedExperience", experience);
        model.addAttribute("selectedSalary", salary);
        model.addAttribute("selectedWorkMode", workMode);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("hasResume", selectedResume != null);
        model.addAttribute("selectedResume", selectedResume);
        model.addAttribute("resumesList", history);
        model.addAttribute("selectedResumeId", selectedResume != null ? selectedResume.getId() : null);

        return "jobs";
    }

    private JobMatchDto calculateMatch(Job job, Resume resume) {
        if (resume == null || resume.getParsedText() == null || resume.getParsedText().isBlank()) {
            return new JobMatchDto(job, 0, List.of(), List.of(), List.of(), "Upload a resume on the dashboard to calculate ATS compatibility.", false);
        }
        
        String resumeText = resume.getParsedText().toLowerCase();
        
        // Skills match
        String[] reqSkillsArray = job.getRequiredSkills() != null ? job.getRequiredSkills().split(",") : new String[0];
        List<String> matchingSkills = new ArrayList<>();
        List<String> missingSkills = new ArrayList<>();
        
        for (String skill : reqSkillsArray) {
            String cleanSkill = skill.trim();
            if (cleanSkill.isEmpty()) continue;
            
            // Match skill case-insensitively
            if (resumeText.contains(cleanSkill.toLowerCase())) {
                matchingSkills.add(cleanSkill);
            } else {
                missingSkills.add(cleanSkill);
            }
        }
        
        // Keywords match
        String keywordsStr = job.getKeywords() != null ? job.getKeywords() : job.getRequiredSkills();
        String[] keywordsArray = keywordsStr != null ? keywordsStr.split(",") : new String[0];
        List<String> missingKeywords = new ArrayList<>();
        for (String keyword : keywordsArray) {
            String cleanKw = keyword.trim();
            if (cleanKw.isEmpty()) continue;
            if (!resumeText.contains(cleanKw.toLowerCase())) {
                missingKeywords.add(cleanKw);
            }
        }
        
        // Match Score
        int matchPercentage = 0;
        int totalSkillsCount = reqSkillsArray.length;
        if (totalSkillsCount > 0) {
            int matchingCount = matchingSkills.size();
            matchPercentage = (int) Math.round(((double) matchingCount / totalSkillsCount) * 100);
        } else {
            matchPercentage = 100;
        }
        
        // Apply Recommendation
        String recommendation;
        if (matchPercentage >= 85) {
            recommendation = "Strong Match! Excellent fit for your profile. Highly recommended to apply.";
        } else if (matchPercentage >= 65) {
            recommendation = "Good Match. Consider adding a few missing skills to your resume before applying.";
        } else if (matchPercentage >= 40) {
            recommendation = "Potential Match. Add missing skills and highlight relevant projects to increase success rate.";
        } else {
            recommendation = "Low Match. We recommend tailoring your resume or acquiring key skills before applying.";
        }
        
        return new JobMatchDto(job, matchPercentage, matchingSkills, missingSkills, missingKeywords, recommendation, true);
    }

    private double parseSalaryToLpa(String salaryStr) {
        if (salaryStr == null || salaryStr.isEmpty()) return 0.0;
        try {
            String clean = salaryStr.toLowerCase().replaceAll("[^0-9.\\-]", "").trim();
            if (clean.isEmpty()) return 0.0;
            
            double value = 0.0;
            if (clean.contains("-")) {
                String[] parts = clean.split("-");
                String maxPart = parts[parts.length - 1].trim();
                value = Double.parseDouble(maxPart);
            } else {
                value = Double.parseDouble(clean);
            }
            
            // Convert monthly stipends to annual LPA
            if (salaryStr.toLowerCase().contains("month") || salaryStr.toLowerCase().contains("pm")) {
                return (value * 12) / 100.0;
            }
            return value;
        } catch (Exception e) {
            return 0.0;
        }
    }
}

