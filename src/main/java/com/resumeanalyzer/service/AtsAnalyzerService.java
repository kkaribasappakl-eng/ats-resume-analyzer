package com.resumeanalyzer.service;

import com.resumeanalyzer.model.Job;
import com.resumeanalyzer.model.Resume;
import com.resumeanalyzer.repository.ResumeRepository;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AtsAnalyzerService {

    private final ResumeRepository resumeRepository;
    private final AiAssistantService aiAssistantService;

    public AtsAnalyzerService(ResumeRepository resumeRepository, AiAssistantService aiAssistantService) {
        this.resumeRepository = resumeRepository;
        this.aiAssistantService = aiAssistantService;
    }

    public void analyze(Resume resume, Job job) {
        String resumeText = resume.getParsedText();
        if (resumeText == null || resumeText.isBlank()) {
            resume.setAtsScore(10);
            resume.setKeywordMatchScore(10);
            resume.setSkillsScore(10);
            resume.setProjectScore(10);
            resume.setEducationScore(10);
            resume.setExperienceScore(10);
            resume.setInternshipScore(10);
            resume.setCertificationsScore(10);
            resume.setStructureScore(10);
            resume.setFormattingScore(10);
            resume.setGrammarScore(10);
            resume.setProjectedAtsScore(10);
            resume.setCareerReadinessScore(10);
            resume.setSkillsFound("");
            resume.setSkillsMissing(job.getRequiredSkills());
            resume.setMissingInformation("Entire resume text is missing or unreadable.");
            resume.setPresentSections("");
            resume.setMissingSections("Name, Email, Phone, LinkedIn, GitHub, Objective, Education, Skills, Projects, Experience, Certifications, Achievements, Languages");
            resume.setSuggestions("The uploaded resume appears to be empty. Please ensure your file contains extractable text.");
            resume.setImprovementRoadmap("- Upload a valid PDF or Word document.");
            resume.setJobTitleMatched(job.getTitle());
            resume.setMatchPercentage(10);
            return;
        }

        // --- 1. DYNAMIC SECTION PARSING & SPLITTING ---
        Map<String, String> extractedSections = extractSections(resumeText);
        
        resume.setSectionSummary(extractedSections.getOrDefault("summary", ""));
        resume.setSectionEducation(extractedSections.getOrDefault("education", ""));
        resume.setSectionSkills(extractedSections.getOrDefault("skills", ""));
        resume.setSectionProjects(extractedSections.getOrDefault("projects", ""));
        resume.setSectionExperience(extractedSections.getOrDefault("experience", ""));
        resume.setSectionCertifications(extractedSections.getOrDefault("certifications", ""));
        resume.setSectionAchievements(extractedSections.getOrDefault("achievements", ""));
        resume.setSectionLanguages(extractedSections.getOrDefault("languages", ""));

        // --- 2. CONTACT DETAILS EXTRACTION ---
        extractContactDetails(resumeText, resume);

        // --- 3. DYNAMIC JOB DESCRIPTION MATCHING ---
        String[] requiredSkillsArray = job.getRequiredSkills().split(",");
        Set<String> requiredSkills = Arrays.stream(requiredSkillsArray)
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        Set<String> foundSkills = new HashSet<>();
        Set<String> missingSkills = new HashSet<>();
        String lowerCaseText = resumeText.toLowerCase();

        for (String skill : requiredSkills) {
            String escapedSkill = Pattern.quote(skill);
            Pattern pattern;
            if (skill.length() <= 2) {
                pattern = Pattern.compile("\\b" + escapedSkill + "\\b");
            } else {
                pattern = Pattern.compile("(?i)\\b" + escapedSkill + "\\b|\\b" + escapedSkill + "s?\\b");
            }

            Matcher matcher = pattern.matcher(lowerCaseText);
            if (matcher.find()) {
                foundSkills.add(skill);
            } else {
                missingSkills.add(skill);
            }
        }

        double skillMatchRatio = requiredSkills.isEmpty() ? 1.0 : (double) foundSkills.size() / requiredSkills.size();
        int skillsScore = (int) (skillMatchRatio * 100);
        int keywordMatchScore = Math.min(100, skillsScore);

        resume.setSkillsFound(foundSkills.stream().map(this::capitalizeWord).collect(Collectors.joining(", ")));
        resume.setSkillsMissing(missingSkills.stream().map(this::capitalizeWord).collect(Collectors.joining(", ")));

        // --- 4. QUALITY ANALYSIS & SECTION SCORING ---
        List<String> presentList = new ArrayList<>();
        List<String> missingList = new ArrayList<>();

        if (resume.getExtractedName() != null) presentList.add("Name"); else missingList.add("Name");
        if (resume.getExtractedEmail() != null) presentList.add("Email"); else missingList.add("Email");
        if (resume.getExtractedPhone() != null) presentList.add("Phone"); else missingList.add("Phone");
        if (resume.getExtractedLinkedin() != null) presentList.add("LinkedIn"); else missingList.add("LinkedIn");
        if (resume.getExtractedGithub() != null) presentList.add("GitHub"); else missingList.add("GitHub");

        for (String key : extractedSections.keySet()) {
            if (!extractedSections.get(key).isEmpty()) {
                presentList.add(capitalizeWord(key));
            } else {
                missingList.add(capitalizeWord(key));
            }
        }

        resume.setPresentSections(String.join(", ", presentList));
        resume.setMissingSections(String.join(", ", missingList));

        // 4.1 Contact Info Score (10 pts)
        int contactScore = 0;
        if (resume.getExtractedEmail() != null) contactScore += 30;
        if (resume.getExtractedPhone() != null) contactScore += 30;
        if (resume.getExtractedLinkedin() != null) contactScore += 20;
        if (resume.getExtractedGithub() != null) contactScore += 20;

        // 4.2 Summary Score (10 pts)
        int summaryScore = 0;
        String summaryText = resume.getSectionSummary();
        if (!summaryText.isEmpty()) {
            summaryScore = Math.min(100, 40 + (summaryText.length() / 5));
            if (summaryText.toLowerCase().contains("spearheaded") || summaryText.toLowerCase().contains("led") || summaryText.toLowerCase().contains("engineered")) {
                summaryScore = Math.min(100, summaryScore + 15);
            }
        }

        // 4.3 Projects Quality (20 pts)
        String projectsText = resume.getSectionProjects();
        if (projectsText == null || projectsText.isBlank()) {
            projectsText = resumeText;
        }
        String projectAnalysis = aiAssistantService.getProjectsStarAnalysis(projectsText);
        resume.setProjectAnalysis(projectAnalysis);
        int projectScore = parseScoreFromHtml(projectAnalysis, 80);

        // 4.4 Experience Detail (10 pts)
        int experienceScore = 0;
        String expText = resume.getSectionExperience();
        if (!expText.isEmpty()) {
            experienceScore = Math.min(100, 50 + (expText.split("\\r?\\n").length * 5));
        }

        // 4.5 Education detail (10 pts)
        int educationScore = 0;
        String eduText = resume.getSectionEducation();
        if (!eduText.isEmpty()) {
            educationScore = 70;
            if (eduText.toLowerCase().contains("gpa") || eduText.toLowerCase().contains("cgpa") || eduText.toLowerCase().contains("%")) {
                educationScore = 95;
            }
        }

        // 4.6 Certifications (Dynamic calculation)
        int certificationsScore = 0;
        String certsText = resume.getSectionCertifications();
        if (certsText != null && !certsText.trim().isEmpty()) {
            certificationsScore = 60;
            for (String skill : foundSkills) {
                if (certsText.toLowerCase().contains(skill.toLowerCase())) {
                    certificationsScore = 90;
                    break;
                }
            }
        }

        // 4.7 Soft Skills (Dynamic calculation)
        String softDetails = aiAssistantService.getSoftSkillsAnalysis(resumeText);
        resume.setSoftSkillsAnalysis(softDetails);
        int softSkillsScore = parseScoreFromHtml(softDetails, 75);

        // 4.8 Grammar
        String grammarDetails = aiAssistantService.getGrammarAndReadabilityAnalysis(resumeText);
        resume.setGrammarDetails(grammarDetails);
        int grammarScore = parseScoreFromHtml(grammarDetails, 85);

        // 4.9 Formatting
        List<String> formattingIssues = inspectFormatting(resume);
        String formatCtx = formattingIssues.isEmpty() ? "No layout anomalies" : String.join(", ", formattingIssues);
        String formattingDetails = aiAssistantService.getAtsFormattingAnalysis(resumeText, formatCtx);
        resume.setFormattingDetails(formattingDetails);
        int formattingScore = Math.max(40, 100 - (formattingIssues.size() * 8));

        // 4.11 Weighted Placement Readiness Index (Total Score calculation)
        // Technical Skills: 30%
        // Projects: 25%
        // Resume Quality (Formatting + Grammar average): 10%
        // ATS Compatibility (Structure / Contact Score): 15%
        // Soft Skills: 10%
        // Certifications: 5%
        // Internship (Experience proxy): 5%
        int totalScore = (int) (
            (skillsScore * 0.30) +
            (projectScore * 0.25) +
            (((formattingScore + grammarScore) / 2) * 0.10) +
            (contactScore * 0.15) +
            (softSkillsScore * 0.10) +
            (certificationsScore * 0.05) +
            (experienceScore * 0.05)
        );

        totalScore = Math.max(10, Math.min(100, totalScore));
        int projectedAtsScore = Math.min(100, totalScore + (missingSkills.size() * 3) + (resume.getExtractedGithub() == null ? 3 : 0) + (resume.getSectionCertifications().isEmpty() ? 5 : 0));
        int careerReadinessScore = totalScore; // maps directly to the weighted placement score

        resume.setAtsScore(totalScore);
        resume.setKeywordMatchScore(keywordMatchScore);
        resume.setSkillsScore(skillsScore);
        resume.setEducationScore(educationScore);
        resume.setProjectScore(projectScore);
        resume.setExperienceScore(experienceScore);
        resume.setInternshipScore(experienceScore);
        resume.setCertificationsScore(certificationsScore);
        resume.setStructureScore(contactScore);
        resume.setFormattingScore(formattingScore);
        resume.setGrammarScore(grammarScore);
        resume.setSoftSkillsScore(softSkillsScore);
        resume.setProjectedAtsScore(projectedAtsScore);
        resume.setCareerReadinessScore(careerReadinessScore);
        resume.setJobTitleMatched(job.getTitle());
        resume.setMatchPercentage(totalScore);

        // --- 5. DYNAMIC RECOMMENDATION ENGINE ---
        StringBuilder suggestions = new StringBuilder();
        suggestions.append("<ul>");
        
        if (resume.getExtractedGithub() == null) {
            suggestions.append("<li><b>Missing GitHub link:</b> Add your GitHub profile to highlight your code repository contributions. (Expected Increase +2%)</li>");
        }
        if (resume.getExtractedLinkedin() == null) {
            suggestions.append("<li><b>Missing LinkedIn Profile:</b> Integrate your profile link to showcase professional networking. (Expected Increase +2%)</li>");
        }
        if (resume.getSectionCertifications().isEmpty()) {
            suggestions.append("<li><b>Missing Certifications:</b> Include relevant industry certifications (e.g. AWS, Azure, Java) to validate your capabilities. (Expected Increase +5%)</li>");
        }
        if (resume.getSectionSummary().length() < 30) {
            suggestions.append("<li><b>Improve Professional Summary:</b> Expand your summary to define your value proposition and core engineering skillset. (Expected Increase +5%)</li>");
        }
        if (!missingSkills.isEmpty()) {
            suggestions.append("<li><b>Missing Keywords:</b> Seamlessly integrate the missing technical keywords (").append(String.join(", ", missingSkills)).append(") to improve requirements match. (Expected Increase +10%)</li>");
        } else {
            suggestions.append("<li><b>Excellent Match:</b> All required technical keywords are already present. No missing keywords identified.</li>");
        }
        if (formattingScore < 80) {
            suggestions.append("<li><b>Layout issues:</b> ").append(String.join("; ", formattingIssues)).append(". Use standard single column clean typography. (Expected Increase +3%)</li>");
        }
        SkillsAnalysis skillsAnalysis = analyzeSkills(resume.getSectionSkills(), job.getRequiredSkills());
        if (!skillsAnalysis.stuffingWarnings.isEmpty()) {
            suggestions.append("<li><b>Warning:</b> ").append(String.join("<br/>", skillsAnalysis.stuffingWarnings)).append("</li>");
        }
        suggestions.append("</ul>");
        resume.setSuggestions(suggestions.toString());

        // Roadmap
        StringBuilder roadmap = new StringBuilder();
        int roadmapCount = 1;
        if (!missingSkills.isEmpty()) {
            roadmap.append(roadmapCount++).append(". **Keyword Injection**: Add missing required keywords: ").append(String.join(", ", missingSkills)).append(".\n");
        }
        if (resume.getExtractedGithub() == null || resume.getExtractedLinkedin() == null) {
            roadmap.append(roadmapCount++).append(". **Contact Links**: Add your GitHub and LinkedIn professional references at the header.\n");
        }
        if (resume.getSectionCertifications().isEmpty()) {
            roadmap.append(roadmapCount++).append(". **Credentials**: Add industry certifications to stand out to recruiters.\n");
        }
        if (formattingScore < 90) {
            roadmap.append(roadmapCount++).append(". **Format Polish**: Clean up tables, columns, or layout warnings from document structure.\n");
        }
        if (roadmap.length() == 0) {
            roadmap.append("1. **Profile Polish**: Your resume has perfect alignment with the Job Profile!");
        }
        resume.setImprovementRoadmap(roadmap.toString());

        // Strengths & Weaknesses
        List<String> strengthsList = new ArrayList<>();
        if (skillsScore >= 70) strengthsList.add("Strong Technical Skillset");
        if (projectScore >= 80) strengthsList.add("Good Project Quality");
        if (formattingScore >= 80) strengthsList.add("Clean Formatting Structure");
        if (grammarScore >= 80) strengthsList.add("Professional Action Verbs");
        if (contactScore >= 80) strengthsList.add("Complete Contact Info");
        if (strengthsList.isEmpty()) strengthsList.add("Standard Font & Layout");
        resume.setStrengths(String.join(", ", strengthsList));

        List<String> weaknessesList = new ArrayList<>();
        if (resume.getSectionCertifications().isEmpty()) weaknessesList.add("No Certifications");
        if (resume.getSectionExperience().isEmpty()) weaknessesList.add("No Internships/Experience");
        if (projectScore < 80) weaknessesList.add("Weak Project Details");
        if (resume.getExtractedGithub() == null) weaknessesList.add("Missing GitHub Profile");
        if (resume.getExtractedLinkedin() == null) weaknessesList.add("Missing LinkedIn Profile");
        if (!missingSkills.isEmpty()) {
            missingSkills.stream().limit(2).forEach(ms -> weaknessesList.add("Missing " + capitalizeWord(ms)));
        }
        if (weaknessesList.isEmpty()) weaknessesList.add("None identified");
        resume.setWeaknesses(String.join(", ", weaknessesList));

        // --- 5.5 DETAILED AI AUDIT MODULE GENERATION ---
        String recruiterSummary = aiAssistantService.getRecruiterSummary(resumeText, job.getDescription());
        resume.setRecruiterSummary(recruiterSummary);

        String keywordDetails = aiAssistantService.getDetailedKeywordAnalysis(resumeText, job.getRequiredSkills());
        resume.setKeywordAnalysisDetails(keywordDetails);

        String explainScoring = aiAssistantService.getDetailedExplainableScoring(resume, job);
        resume.setExplainableScoringDetails(explainScoring);

        String roadmapDetails = aiAssistantService.getDetailedImprovementRoadmap(suggestions.toString());
        resume.setImprovementRoadmapDetails(roadmapDetails);

        // --- 6. SCORE DIFFERENCE ENGINE ---
        List<Resume> history = resumeRepository.findByUserOrderByCreatedAtDesc(resume.getUser());
        Resume prevResume = history.isEmpty() ? null : history.get(0);
        
        List<String> differenceReasons = new ArrayList<>();
        int prevScore = prevResume != null ? prevResume.getAtsScore() : 0;
        int diff = totalScore - prevScore;
        
        if (prevResume != null) {
            if (prevResume.getExtractedGithub() == null && resume.getExtractedGithub() != null) {
                differenceReasons.add("+2 Added GitHub Link");
            }
            if (prevResume.getExtractedLinkedin() == null && resume.getExtractedLinkedin() != null) {
                differenceReasons.add("+2 Added LinkedIn Link");
            }
            if (resume.getProjectScore() > prevResume.getProjectScore()) {
                differenceReasons.add("+4 Improved Project Descriptions");
            }
            if (resume.getCertificationsScore() > prevResume.getCertificationsScore()) {
                differenceReasons.add("+10 Added Credentials/Certifications");
            }
            
            // Compare parsed skills
            Set<String> prevFound = Arrays.stream(prevResume.getSkillsFound().split(",\\s*"))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());
            for (String fs : foundSkills) {
                if (!prevFound.contains(fs.toLowerCase())) {
                    differenceReasons.add("+3 Added Skill: " + capitalizeWord(fs));
                }
            }
        }
        
        if (differenceReasons.isEmpty() && prevResume != null) {
            if (diff > 0) {
                differenceReasons.add("+" + diff + " Enhanced keyword density and layout readability");
            } else if (diff < 0) {
                differenceReasons.add(diff + " Removed technical keywords or critical sections");
            } else {
                differenceReasons.add("0 No changes detected");
            }
        }
        resume.setScoreDifferenceReason(String.join(", ", differenceReasons));

        // --- 7. DYNAMIC INTERVIEW QUESTIONS GENERATOR ---
        String interviewQuestions = aiAssistantService.getInterviewQuestions(resumeText, job.getDescription() + " Required Skills: " + job.getRequiredSkills());
        resume.setInterviewQuestions(interviewQuestions);

        resume.setAiSummary("AI Analysis Complete: Your resume scored " + totalScore + "% matching the " + job.getTitle() + " profile. " +
                            "By addressing the recommended items you can scale your score up to " + projectedAtsScore + "%.");
    }

    // --- UTILITIES --- //

    private Map<String, String> extractSections(String text) {
        Map<String, String> sections = new HashMap<>();
        String lower = text.toLowerCase();
        
        Map<String, List<String>> headings = new LinkedHashMap<>();
        headings.put("summary", List.of("summary", "objective", "profile", "about me", "professional summary"));
        headings.put("skills", List.of("technical skills", "skills", "technologies", "key skills", "core competencies"));
        headings.put("projects", List.of("projects", "academic projects", "key projects", "personal projects"));
        headings.put("experience", List.of("experience", "work history", "employment", "professional experience", "internships"));
        headings.put("education", List.of("education", "academic profile", "academic qualification", "qualifications"));
        headings.put("certifications", List.of("certifications", "certificates", "courses"));
        headings.put("achievements", List.of("achievements", "awards", "honors"));
        headings.put("languages", List.of("languages", "languages known"));

        List<SectionIndex> indices = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : headings.entrySet()) {
            String sectionKey = entry.getKey();
            for (String heading : entry.getValue()) {
                Pattern pattern = Pattern.compile("(?i)(?:^|\\r?\\n)\\s*(" + Pattern.quote(heading) + ")\\s*(?:\\r?\\n|$)");
                Matcher matcher = pattern.matcher(text);
                if (matcher.find()) {
                    indices.add(new SectionIndex(sectionKey, matcher.start(1), matcher.end(1)));
                    break;
                }
            }
        }

        indices.sort(Comparator.comparingInt(a -> a.start));

        for (int i = 0; i < indices.size(); i++) {
            SectionIndex current = indices.get(i);
            int start = current.end;
            int end = (i + 1 < indices.size()) ? indices.get(i + 1).start : text.length();
            String sectionText = text.substring(start, end).trim();
            sections.put(current.key, sectionText);
        }

        return sections;
    }

    private String findNameHeuristic(String text) {
        String[] lines = text.split("\\r?\\n");
        int count = 0;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            count++;
            if (count > 15) break;

            if (trimmed.length() < 3 || trimmed.length() > 40) continue;
            String lower = trimmed.toLowerCase();
            if (lower.contains("@") || lower.contains(".") || lower.contains("/") || 
                lower.contains("resume") || lower.contains("curriculum") || 
                lower.contains("page") || lower.contains("email") || lower.contains("phone") ||
                lower.contains("github") || lower.contains("linkedin") || lower.contains("portfolio") ||
                lower.contains("skills") || lower.contains("experience") || lower.contains("education") ||
                trimmed.matches(".*\\d+.*")) {
                continue;
            }

            String[] words = trimmed.split("\\s+");
            if (words.length >= 1 && words.length <= 4) {
                boolean looksLikeName = true;
                for (String w : words) {
                    if (w.isEmpty()) continue;
                    if (!Character.isUpperCase(w.charAt(0)) && !trimmed.equals(trimmed.toUpperCase())) {
                        looksLikeName = false;
                        break;
                    }
                }
                if (looksLikeName) {
                    return trimmed;
                }
            }
        }
        return null;
    }

    private String findEmailHeuristic(String text) {
        Pattern pattern = Pattern.compile("(?i)[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group().trim();
        }
        return null;
    }

    private String findPhoneHeuristic(String text) {
        Pattern pattern = Pattern.compile("(?:\\+?\\d{1,3}[\\s.-]?)?\\(?\\d{3}\\)?[\\s.-]?\\d{3}[\\s.-]?\\d{4,5}");
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String val = matcher.group().trim();
            String digitsOnly = val.replaceAll("\\D", "");
            if (digitsOnly.length() >= 10 && digitsOnly.length() <= 15) {
                return val;
            }
        }
        return null;
    }

    private String findLinkedinHeuristic(String text) {
        Pattern pattern = Pattern.compile("(?i)(?:https?://)?(?:www\\.)?linkedin\\.com/(?:in|company)/[a-zA-Z0-9-_/]+");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String url = matcher.group().trim();
            if (!url.startsWith("http")) {
                url = "https://" + url;
            }
            return url;
        }
        return null;
    }

    private String findGithubHeuristic(String text) {
        Pattern pattern = Pattern.compile("(?i)(?:https?://)?(?:www\\.)?github\\.com/[a-zA-Z0-9-_]+");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String url = matcher.group().trim();
            if (!url.startsWith("http")) {
                url = "https://" + url;
            }
            return url;
        }
        return null;
    }

    private String findPortfolioHeuristic(String text) {
        Pattern pattern = Pattern.compile("(?i)(?:https?://)?(?:www\\.)?[a-zA-Z0-9-]+\\.[a-zA-Z0-9.-]+(?:/[^\\s]*)?");
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String url = matcher.group().trim();
            String lower = url.toLowerCase();
            if (!lower.contains("linkedin.com") && !lower.contains("github.com") && !lower.contains("email") && !lower.contains("png") && !lower.contains("jpg")) {
                if (!url.startsWith("http")) {
                    url = "https://" + url;
                }
                return url;
            }
        }
        return null;
    }

    private void extractContactDetails(String text, Resume resume) {
        String name = findNameHeuristic(text);
        String email = findEmailHeuristic(text);
        String phone = findPhoneHeuristic(text);
        String linkedin = findLinkedinHeuristic(text);
        String github = findGithubHeuristic(text);
        String portfolio = findPortfolioHeuristic(text);

        int nameConf = (name != null) ? 85 : 0;
        int emailConf = (email != null) ? 90 : 0;
        int phoneConf = (phone != null) ? 90 : 0;
        int linkedinConf = (linkedin != null) ? 80 : 0;
        int githubConf = (github != null) ? 80 : 0;
        int portfolioConf = (portfolio != null) ? 75 : 0;

        try {
            String aiResponse = aiAssistantService.extractContactEntities(text);
            if (aiResponse != null && !aiResponse.trim().isEmpty()) {
                String[] lines = aiResponse.split("\n");
                for (String line : lines) {
                    String upperLine = line.toUpperCase().trim();
                    if (upperLine.startsWith("NAME:") && !upperLine.contains("NOT FOUND") && !upperLine.contains("NOT PARSED")) {
                        String val = line.substring(line.indexOf(":") + 1).trim();
                        if (!val.isEmpty()) {
                            name = val;
                            nameConf = 100;
                        }
                    } else if (upperLine.startsWith("EMAIL:") && !upperLine.contains("NOT FOUND")) {
                        String val = line.substring(line.indexOf(":") + 1).trim();
                        if (!val.isEmpty()) {
                            email = val;
                            emailConf = 100;
                        }
                    } else if (upperLine.startsWith("PHONE:") && !upperLine.contains("NOT FOUND")) {
                        String val = line.substring(line.indexOf(":") + 1).trim();
                        if (!val.isEmpty()) {
                            phone = val;
                            phoneConf = 100;
                        }
                    } else if (upperLine.startsWith("LINKEDIN:") && !upperLine.contains("NOT FOUND")) {
                        String val = line.substring(line.indexOf(":") + 1).trim();
                        if (!val.isEmpty()) {
                            linkedin = val;
                            linkedinConf = 100;
                        }
                    } else if (upperLine.startsWith("GITHUB:") && !upperLine.contains("NOT FOUND")) {
                        String val = line.substring(line.indexOf(":") + 1).trim();
                        if (!val.isEmpty()) {
                            github = val;
                            githubConf = 100;
                        }
                    } else if (upperLine.startsWith("PORTFOLIO:") && !upperLine.contains("NOT FOUND")) {
                        String val = line.substring(line.indexOf(":") + 1).trim();
                        if (!val.isEmpty()) {
                            portfolio = val;
                            portfolioConf = 100;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Fallback gracefully to Regex
        }

        resume.setExtractedName(name != null ? name : "Not Parsed");
        resume.setExtractedEmail(email != null ? email : "Not Found");
        resume.setExtractedPhone(phone != null ? phone : "Not Found");
        resume.setExtractedLinkedin(linkedin != null ? linkedin : "Not Found");
        resume.setExtractedGithub(github != null ? github : "Not Found");
        resume.setExtractedPortfolio(portfolio != null ? portfolio : "Not Found");

        resume.setConfidenceName(nameConf);
        resume.setConfidenceEmail(emailConf);
        resume.setConfidencePhone(phoneConf);
        resume.setConfidenceLinkedin(linkedinConf);
        resume.setConfidenceGithub(githubConf);
        resume.setConfidencePortfolio(portfolioConf);
    }

    private List<ProjectCheck> checkProjects(String projectsText) {
        List<ProjectCheck> checks = new ArrayList<>();
        if (projectsText == null || projectsText.isBlank()) {
            return checks;
        }

        String[] lines = projectsText.split("\\r?\\n");
        ProjectCheck currentProject = null;
        
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            
            if (!trimmed.startsWith("-") && !trimmed.startsWith("*") && trimmed.length() < 60) {
                if (currentProject != null) {
                    checks.add(currentProject);
                }
                currentProject = new ProjectCheck(trimmed);
            } else if (currentProject != null) {
                currentProject.bullets.add(trimmed);
            } else {
                currentProject = new ProjectCheck("Project Details");
                currentProject.bullets.add(trimmed);
            }
        }
        if (currentProject != null) {
            checks.add(currentProject);
        }

        for (ProjectCheck proj : checks) {
            boolean hasMetrics = false;
            boolean hasApi = false;
            boolean hasDb = false;
            
            for (String bullet : proj.bullets) {
                String lower = bullet.toLowerCase();
                if (lower.matches(".*\\d+%.*") || lower.matches(".*\\d+\\s*(users|sec|ms|gb|tb|lpa|x|percent|active).*")) {
                    hasMetrics = true;
                }
                if (lower.contains("api") || lower.contains("rest") || lower.contains("graphql") || lower.contains("endpoint") || lower.contains("controller")) {
                    hasApi = true;
                }
                if (lower.contains("sql") || lower.contains("database") || lower.contains("db") || lower.contains("mongodb") || lower.contains("query") || lower.contains("hibernate") || lower.contains("jpa")) {
                    hasDb = true;
                }
            }

            if (!hasMetrics) {
                proj.warnings.add("Missing business impact metrics (e.g. scale, performance increase %).");
            }
            if (!hasApi) {
                proj.warnings.add("Missing API architecture descriptions.");
            }
            if (!hasDb) {
                proj.warnings.add("Missing database indexing or query details.");
            }
            
            proj.suggestions.add("Spearheaded " + proj.title + " implementing REST APIs and optimized SQL database structures, enhancing load times by 25%.");
        }
        return checks;
    }

    private List<GrammarIssue> checkGrammar(String text) {
        List<GrammarIssue> issues = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return issues;
        }

        String[] lines = text.split("\\r?\\n");
        Pattern repeatedWordsPat = Pattern.compile("(?i)\\b(\\w+)\\s+\\1\\b");
        
        List<String> weakVerbs = List.of("worked", "did", "helped", "handled", "made");
        Map<String, String> verbSuggestions = Map.of(
            "worked", "spearheaded",
            "did", "engineered",
            "helped", "facilitated",
            "handled", "orchestrated",
            "made", "architected"
        );

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.length() < 10) continue;

            Matcher repMatcher = repeatedWordsPat.matcher(trimmed);
            if (repMatcher.find()) {
                issues.add(new GrammarIssue("Repeated word: '" + repMatcher.group() + "'", trimmed, trimmed.replace(repMatcher.group(), repMatcher.group(1))));
            }

            for (String weak : weakVerbs) {
                Pattern weakPat = Pattern.compile("(?i)\\b" + Pattern.quote(weak) + "\\b");
                if (weakPat.matcher(trimmed).find()) {
                    issues.add(new GrammarIssue("Weak verb: '" + weak + "'", trimmed, trimmed.replaceAll("(?i)\\b" + Pattern.quote(weak) + "\\b", verbSuggestions.get(weak.toLowerCase()))));
                }
            }

            if (trimmed.toLowerCase().contains("was developed by")) {
                issues.add(new GrammarIssue("Passive voice detected", trimmed, "Rewrite using active voice starting with action verb"));
            }

            String[] words = trimmed.split("\\s+");
            if (words.length > 25) {
                issues.add(new GrammarIssue("Sentence too long (" + words.length + " words)", trimmed, "Break down into bullet points"));
            }

            if (trimmed.contains(" ,") || trimmed.contains(" .")) {
                issues.add(new GrammarIssue("Wrong punctuation spacing", trimmed, trimmed.replaceAll("\\s+,", ",").replaceAll("\\s+\\.", ".")));
            }
        }
        return issues;
    }

    private List<String> inspectFormatting(Resume resume) {
        List<String> issues = new ArrayList<>();
        byte[] bytes = resume.getFileData();
        if (bytes == null || bytes.length == 0) {
            return issues;
        }
        
        String fileName = resume.getFileName().toLowerCase();
        if (fileName.endsWith(".pdf")) {
            try (org.apache.pdfbox.pdmodel.PDDocument document = org.apache.pdfbox.pdmodel.PDDocument.load(bytes)) {
                boolean hasImages = false;
                for (org.apache.pdfbox.pdmodel.PDPage page : document.getPages()) {
                    org.apache.pdfbox.pdmodel.PDResources res = page.getResources();
                    for (org.apache.pdfbox.cos.COSName name : res.getXObjectNames()) {
                        if (res.isImageXObject(name)) {
                            hasImages = true;
                            break;
                        }
                    }
                    if (hasImages) break;
                }
                if (hasImages) {
                    issues.add("Contains embedded images (ATS parsers cannot read image contents)");
                }
                if (resume.getParsedText() != null && resume.getParsedText().contains("  ")) {
                    issues.add("Detected double-spacing or complex horizontal layouts");
                }
            } catch (Exception e) {}
        } else if (fileName.endsWith(".docx")) {
            try (org.apache.poi.xwpf.usermodel.XWPFDocument doc = new org.apache.poi.xwpf.usermodel.XWPFDocument(new java.io.ByteArrayInputStream(bytes))) {
                if (!doc.getTables().isEmpty()) {
                    issues.add("Contains tables (Tables break formatting reads in standard ATS parsers)");
                }
                if (!doc.getHeaderList().isEmpty() || !doc.getFooterList().isEmpty()) {
                    issues.add("Contains headers/footers (ATS systems skip header details)");
                }
                if (doc.getDocument().getBody().toString().contains("w:txbxContent")) {
                    issues.add("Contains float text boxes (floating text blocks are unreadable by parser)");
                }
            } catch (Exception e) {}
        }
        return issues;
    }

    private SkillsAnalysis analyzeSkills(String skillsText, String jdSkills) {
        SkillsAnalysis analysis = new SkillsAnalysis();
        if (skillsText == null) skillsText = "";
        
        Set<String> uniqueSkills = new LinkedHashSet<>();
        String[] parts = skillsText.split("[,;|\\r?\\n]");
        for (String p : parts) {
            String trimmed = p.trim().replaceAll("[\\*\\-•]", "").trim();
            if (!trimmed.isEmpty() && trimmed.length() < 30) {
                uniqueSkills.add(trimmed);
            }
        }
        analysis.uniqueSkillsList = new ArrayList<>(uniqueSkills);

        String lowerText = skillsText.toLowerCase();
        for (String skill : uniqueSkills) {
            String lowerSkill = skill.toLowerCase();
            int count = 0;
            int idx = 0;
            while ((idx = lowerText.indexOf(lowerSkill, idx)) != -1) {
                count++;
                idx += lowerSkill.length();
            }
            if (count > 5) {
                analysis.stuffingWarnings.add("Skill stuffing warning: '" + skill + "' repeated excessively.");
            }
        }
        return analysis;
    }

    private String capitalizeWord(String str) {
        if (str == null || str.isEmpty()) return "";
        return Arrays.stream(str.split("\\s+"))
                .map(w -> w.isEmpty() ? "" : Character.toUpperCase(w.charAt(0)) + w.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    private static class SectionIndex {
        String key;
        int start;
        int end;
        SectionIndex(String key, int start, int end) {
            this.key = key;
            this.start = start;
            this.end = end;
        }
    }

    public static class ProjectCheck {
        public String title;
        public List<String> bullets = new ArrayList<>();
        public List<String> warnings = new ArrayList<>();
        public List<String> suggestions = new ArrayList<>();
        public ProjectCheck(String title) {
            this.title = title;
        }
    }

    public static class GrammarIssue {
        public String message;
        public String original;
        public String suggestion;
        public GrammarIssue(String message, String original, String suggestion) {
            this.message = message;
            this.original = original;
            this.suggestion = suggestion;
        }
    }

    public static class SkillsAnalysis {
        public List<String> uniqueSkillsList = new ArrayList<>();
        public List<String> stuffingWarnings = new ArrayList<>();
    }

    private int parseScoreFromHtml(String html, int defaultVal) {
        if (html == null || html.isBlank()) return defaultVal;
        Pattern pattern = Pattern.compile("(?i)(?:Formatting|Readability|STAR|Soft\\s+Skills|Grammar)\\s+Score[^0-9]*(\\d+)");
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (Exception e) {
                // ignore
            }
        }
        return defaultVal;
    }
}

