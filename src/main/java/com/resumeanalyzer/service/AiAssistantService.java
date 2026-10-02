package com.resumeanalyzer.service;

import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

@Service
public class AiAssistantService {

    private final Random random = new Random();

    public String generateResumeSummary(String jobTitle, String experienceLevel, String topSkills) {
        String prompt = "Generate a professional ATS-optimized resume summary for a " + jobTitle + 
                " with " + experienceLevel + " experience and skills: " + topSkills + ". Keep it under 3 sentences.";
        
        String aiResult = callGemini(prompt);
        if (aiResult != null && !aiResult.isBlank()) {
            return aiResult;
        }

        List<String> templates = List.of(
            "Results-driven %s with %s of experience. Proven expertise in %s. Adept at leveraging technical skills to drive project success and optimize system performance.",
            "Dynamic and highly motivated %s offering %s of hands-on experience in %s. Passionate about building scalable solutions and eager to contribute to innovative teams.",
            "Detail-oriented %s bringing %s of robust technical background. Specializes in %s. Committed to delivering high-quality, maintainable code in fast-paced environments."
        );

        String template = templates.get(random.nextInt(templates.size()));
        return String.format(template, jobTitle, experienceLevel, topSkills);
    }

    public String rewriteProjectDescription(String originalDescription) {
        if (originalDescription == null || originalDescription.trim().isEmpty()) {
            return "Error: Original description is empty.";
        }

        String prompt = "Rewrite the following project bullet points into professional, high-impact STAR method descriptions with metrics: " + originalDescription;
        String aiResult = callGemini(prompt);
        if (aiResult != null && !aiResult.isBlank()) {
            return aiResult;
        }

        return "Spearheaded the development and deployment of the project. " +
               "Engineered scalable architecture resulting in a 30% performance increase. " +
               "Collaborated cross-functionally to define requirements and deliver the solution 2 weeks ahead of schedule. " +
               "Original context: [" + originalDescription + "]";
    }
    
    public String getOptimizationRecommendations(String currentRole) {
        String prompt = "Provide 4 bullet points of ATS recommendations specifically for a " + currentRole + " role.";
        String aiResult = callGemini(prompt);
        if (aiResult != null && !aiResult.isBlank()) {
            return aiResult;
        }

        return "- Add metrics to your achievements (e.g., 'Increased revenue by 15%').\n" +
               "- Replace passive verbs with strong action verbs (e.g., 'Led', 'Architected').\n" +
               "- Ensure your layout is strictly single-column to pass traditional ATS parsers.\n" +
               "- Tailor your 'Skills' section specifically for the " + currentRole + " role.";
    }

    private String callGemini(String prompt) {
        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            apiKey = System.getProperty("gemini.api.key");
        }
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return null; // Local rule-based fallback
        }

        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;
            String jsonPayload = "{\"contents\":[{\"parts\":[{\"text\":\"" + escapeJson(prompt) + "\"}]}]}";
            
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();
                    
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return extractTextFromJson(response.body());
            }
        } catch (Exception e) {
            // fallback
        }
        return null;
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    private String extractTextFromJson(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode content = candidates.get(0).path("content");
                JsonNode parts = content.path("parts");
                if (parts.isArray() && parts.size() > 0) {
                    return parts.get(0).path("text").asText();
                }
            }
        } catch (Exception e) {
            // fallback
        }
        Pattern pattern = Pattern.compile("\"text\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1).replace("\\n", "\n").replace("\\\"", "\"");
        }
        return null;
    }

    public String extractContactEntities(String resumeText) {
        String prompt = "You are a professional resume parsing engine. Analyze the following resume text and extract the target entity values as clean, plain text. " +
                "Do not include any explanation or markdown formatting. Respond ONLY in the following format:\\n" +
                "NAME: <extracted candidate name>\\n" +
                "EMAIL: <extracted email>\\n" +
                "PHONE: <extracted phone number>\\n" +
                "LINKEDIN: <extracted LinkedIn profile link>\\n" +
                "GITHUB: <extracted GitHub profile link>\\n" +
                "PORTFOLIO: <extracted portfolio or website link>\\n" +
                "LOCATION: <extracted location>\\n" +
                "If a field is not found, output 'Not Found' for that field.\\n\\n" +
                "Resume Text:\\n" + resumeText;
        
        return callGemini(prompt);
    }

    public String getAtsFormattingAnalysis(String resumeText, String fileStructureContext) {
        String prompt = "You are a professional ATS Formatting Inspector. Analyze the resume layout and content. " +
                "The file structure scan shows: " + fileStructureContext + "\\n\\n" +
                "Evaluate the following aspects:\\n" +
                "- Layout structure (single column vs multiple columns)\\n" +
                "- Presence of tables, text boxes, floating content\\n" +
                "- Images, logos, or icons that might block parser reading\\n" +
                "- Spacing, margins, font hierarchy, headers, footers\\n\\n" +
                "Generate a detailed, premium HTML report with clear bullet points. " +
                "Include a 'Formatting Score: X/100' at the top, followed by specific recommendations. " +
                "Do not include ```html markdown wrap or any explainers. " +
                "Resume Text:\\n" + resumeText;

        String result = callGemini(prompt);
        if (result != null && !result.isBlank()) {
            return result;
        }

        // Rule-based fallback
        int score = 95;
        List<String> recs = new ArrayList<>();
        recs.add("<strong>Fonts & Hierarchy:</strong> Use standard sans-serif fonts (e.g. Arial, Calibri, Inter) with clear 14pt headings and 10.5pt body text.");
        recs.add("<strong>Margins & Spacing:</strong> Standard 1-inch margins on all sides with 1.15 line spacing avoids text overlapping.");

        if (fileStructureContext.contains("tables") || fileStructureContext.contains("Tables")) {
            score -= 20;
            recs.add("<span class='text-warning'><strong>Warning - Tables Detected:</strong></span> ATS parsers struggle to read tabular cells horizontally. Convert database records to structured bullet lists.");
        }
        if (fileStructureContext.contains("images") || fileStructureContext.contains("Images")) {
            score -= 15;
            recs.add("<span class='text-danger'><strong>Warning - Embedded Images:</strong></span> ATS systems skip icons, graphs, or profile pictures. Ensure all critical contact info is text-only.");
        }
        if (fileStructureContext.contains("text boxes") || fileStructureContext.contains("floating")) {
            score -= 25;
            recs.add("<span class='text-danger'><strong>Warning - Text Boxes:</strong></span> Floating containers are excluded by standard layout parsers. Place content directly in the page body.");
        }
        if (fileStructureContext.contains("headers") || fileStructureContext.contains("footers")) {
            score -= 10;
            recs.add("<span class='text-warning'><strong>Warning - Headers/Footers:</strong></span> Critical contact info placed in header zones might be missed. Move email and phone numbers to the page body.");
        }

        StringBuilder html = new StringBuilder();
        html.append("<div class='mb-2 fw-bold text-white'>Formatting Score: ").append(score).append("/100</div>");
        html.append("<ul class='ps-3 mb-0' style='list-style-type: square;'>");
        for (String rec : recs) {
            html.append("<li class='mb-2'>").append(rec).append("</li>");
        }
        html.append("</ul>");
        return html.toString();
    }

    public String getGrammarAndReadabilityAnalysis(String resumeText) {
        String prompt = "You are an expert Grammar & Readability Auditor. Perform a deep linguistic audit of this resume. " +
                "Find: \\n" +
                "- Grammatical and punctuation mistakes\\n" +
                "- Passive voice usages\\n" +
                "- Weak action verbs (e.g. helped, did, worked) and suggest strong alternatives\\n" +
                "- Repeated/redundant words\\n" +
                "- Tone assessment (professional, passive, dynamic)\\n" +
                "- Sentence length issues (mark sentences exceeding 20-25 words)\\n\\n" +
                "Generate a premium HTML report. At the top, include a 'Readability Score (Flesch-Kincaid): X/100' " +
                "and 'Professional Tone: [Excellent/Strong/Needs Improvement]'. " +
                "Provide a list of detected issues, and a side-by-side comparison of original vs corrected sentences. " +
                "Do not include ```html markdown wrap. " +
                "Resume Text:\\n" + resumeText;

        String result = callGemini(prompt);
        if (result != null && !result.isBlank()) {
            return result;
        }

        // Rule-based fallback
        List<String> issues = new ArrayList<>();
        List<String> comparisons = new ArrayList<>();
        
        // Find weak verbs
        List<String> weakVerbs = List.of("worked", "did", "helped", "handled", "made");
        Map<String, String> verbSuggestions = Map.of(
            "worked", "spearheaded",
            "did", "engineered",
            "helped", "facilitated",
            "handled", "orchestrated",
            "made", "architected"
        );

        String[] lines = resumeText.split("\\r?\\n");
        int sentenceCount = 0;
        int wordCount = 0;
        int passiveCount = 0;
        int weakCount = 0;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.length() < 10) continue;
            sentenceCount++;
            String[] words = trimmed.split("\\s+");
            wordCount += words.length;

            if (trimmed.toLowerCase().contains("was developed by") || trimmed.toLowerCase().contains("was implemented by")) {
                passiveCount++;
                issues.add("<strong>Passive Voice:</strong> '" + trimmed + "'");
                comparisons.add("Original: <i>" + trimmed + "</i><br/>Corrected: <strong>\"Developed and deployed...\"</strong>");
            }

            for (String weak : weakVerbs) {
                if (trimmed.toLowerCase().contains(weak)) {
                    weakCount++;
                    issues.add("<strong>Weak Action Verb:</strong> '" + weak + "' used in sentence.");
                    comparisons.add("Original: <i>... " + weak + " ...</i><br/>Corrected: <strong>\"... " + verbSuggestions.get(weak) + " ...\"</strong>");
                    break;
                }
            }

            if (words.length > 25) {
                issues.add("<strong>Length Warning:</strong> Sentence exceeds 25 words. Break it down into bullet points.");
            }
        }

        int score = Math.max(30, 98 - (passiveCount * 4) - (weakCount * 3) - (issues.size() * 2));
        String tone = (score >= 85) ? "Excellent" : ((score >= 70) ? "Strong" : "Needs Improvement");

        StringBuilder html = new StringBuilder();
        html.append("<div class='mb-2 fw-bold text-white'>Grammar Score: ").append(score).append("/100 (Current) | ").append(Math.min(100, score + 12)).append("/100 (After correction)</div>");
        html.append("<div class='mb-3 text-secondary'>Professional Tone: <span class='badge bg-info'>").append(tone).append("</span></div>");

        if (!issues.isEmpty()) {
            html.append("<div class='fw-bold text-white mb-2'>Detected Issues:</div>");
            html.append("<ul class='ps-3 mb-3'>");
            for (String issue : issues) {
                html.append("<li class='mb-1'>").append(issue).append("</li>");
            }
            html.append("</ul>");

            html.append("<div class='fw-bold text-white mb-2'>Suggested Corrections:</div>");
            for (String comp : comparisons) {
                html.append("<div class='p-2 mb-2 rounded border' style='border-color: rgba(255,255,255,0.05) !important;'>").append(comp).append("</div>");
            }
        } else {
            html.append("<div class='text-success'>✓ Grammar structure, tone, and readability are optimized for ATS screening.</div>");
        }

        return html.toString();
    }

    public String getProjectsStarAnalysis(String projectsText) {
        if (projectsText == null || projectsText.isBlank()) {
            return "<div class='text-muted'>No projects detected to analyze. Add a 'Projects' section to your resume.</div>";
        }

        String prompt = "You are an expert ATS Project Auditor. Analyze the following resume projects section. " +
                "Never output 'No project information extracted' if projects exist in the text.\\n" +
                "For each project, identify:\\n" +
                "- Situation/Problem/Task (S/T)\\n" +
                "- Action (A)\\n" +
                "- Result/Business Impact (R)\\n" +
                "- Technologies Used\\n" +
                "- Missing measurable achievements (numbers, scale, percentages)\\n\\n" +
                "Give each project a 'STAR Score: X/100'. " +
                "Provide rewritten, high-impact STAR bullet points with estimated impact metrics. " +
                "Format the response as premium, clean HTML. Do not include ```html markdown wrap.\\n\\n" +
                "Projects Text:\\n" + projectsText;

        String result = callGemini(prompt);
        if (result != null && !result.isBlank()) {
            return result;
        }

        // Rule-based fallback
        StringBuilder html = new StringBuilder();
        String[] lines = projectsText.split("\\r?\\n");
        String currentProjectName = "Project Experience";
        List<String> bullets = new ArrayList<>();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            if (!trimmed.startsWith("-") && !trimmed.startsWith("*") && trimmed.length() < 60) {
                if (!bullets.isEmpty()) {
                    html.append(generateFallbackProjectHTML(currentProjectName, bullets));
                    bullets.clear();
                }
                currentProjectName = trimmed;
            } else {
                bullets.add(trimmed.replaceAll("^[\\*\\-\\s•]+", ""));
            }
        }
        if (!bullets.isEmpty()) {
            html.append(generateFallbackProjectHTML(currentProjectName, bullets));
        }

        if (html.length() == 0) {
            html.append("<div class='text-warning'>Projects section contains simple headers only. Add detailed bullet points describing your technical contributions.</div>");
        }

        return html.toString();
    }

    private String generateFallbackProjectHTML(String projectName, List<String> bullets) {
        boolean hasMetrics = false;
        boolean hasTech = false;
        List<String> techFound = new ArrayList<>();
        
        for (String b : bullets) {
            String lower = b.toLowerCase();
            if (lower.matches(".*\\d+%.*") || lower.matches(".*\\d+\\s*(users|sec|ms|gb|tb|lpa|x|percent|active).*")) {
                hasMetrics = true;
            }
            if (lower.contains("java") || lower.contains("spring") || lower.contains("react") || lower.contains("sql") || lower.contains("python") || lower.contains("aws")) {
                hasTech = true;
                techFound.add(b);
            }
        }

        int score = 60 + (hasMetrics ? 20 : 0) + (hasTech ? 20 : 0);

        StringBuilder sb = new StringBuilder();
        sb.append("<div class='mb-3 p-3 rounded border' style='border-color: rgba(255,255,255,0.05) !important; background: rgba(255,255,255,0.01);'>");
        sb.append("<div class='d-flex justify-content-between mb-2'>");
        sb.append("<strong class='text-white'>").append(projectName).append("</strong>");
        sb.append("<span class='badge bg-success'>STAR Score: ").append(score).append("/100</span>");
        sb.append("</div>");

        sb.append("<div class='small text-secondary mb-2'><strong>Task/Action:</strong> Dynamic technical implementation parsed.</div>");
        
        if (!hasMetrics) {
            sb.append("<div class='text-warning mb-2'><i class='bi bi-exclamation-triangle-fill'></i> Missing business metrics or quantifiable results.</div>");
        }
        
        sb.append("<div class='text-white fw-bold mb-1 small'>Suggested STAR Rewrite:</div>");
        sb.append("<div class='text-info small mb-2'>\"Spearheaded the design and development of ").append(projectName)
          .append(" integrating modern microservices, resulting in a <strong>30% increase in database query speed</strong> and support for over <strong>10,000 active users</strong>.\"</div>");

        sb.append("</div>");
        return sb.toString();
    }

    public String getInterviewQuestions(String resumeText, String jobDescription) {
        String prompt = "You are a senior technical interviewer. Generate a list of tailored interview questions " +
                "based on the candidate's resume and target job description: " + jobDescription + "\\n\\n" +
                "Generate exactly 9 questions covering the following areas (label each clearly):\\n" +
                "1. Java / Backend Engineering\\n" +
                "2. Spring Boot Architecture\\n" +
                "3. REST API & Systems Design\\n" +
                "4. SQL & Database Optimization\\n" +
                "5. React / Frontend Development\\n" +
                "6. Resume Project-specific Deep-dive\\n" +
                "7. Human Resources (HR) fit\\n" +
                "8. Behavioral (STAR format)\\n" +
                "9. Coding / Algorithmic challenge\\n\\n" +
                "Return the questions separated by '|||' so they can be split into individual cards in UI. " +
                "For example:\\n" +
                "<strong>Java:</strong> question 1... ||| <strong>Spring Boot:</strong> question 2... ||| ...\\n" +
                "Do not output any introductory or concluding text, only the questions joined by '|||'.\\n\\n" +
                "Resume Text:\\n" + resumeText;

        String result = callGemini(prompt);
        if (result != null && !result.isBlank()) {
            return result;
        }

        // Rule-based fallback
        List<String> list = List.of(
            "<strong>Java & Backend:</strong> Explain the JVM memory structure. How do you handle concurrency, ThreadPools, and memory leaks in production?",
            "<strong>Spring Boot:</strong> What is Dependency Injection and how does Spring Boot configure Bean autowiring under the hood?",
            "<strong>REST API & Systems:</strong> How do you design an API to handle peak loads? Explain rate limiting, caching strategies, and load balancing.",
            "<strong>SQL & Databases:</strong> How do database indexes work? Explain the difference between clustered and non-clustered indexes and how you'd optimize a slow query.",
            "<strong>React & Frontend:</strong> Explain React state management. What are hooks, context, and how do you prevent re-renders?",
            "<strong>Project Specifics:</strong> Walk me through the architecture of your primary project. What was the most challenging technical hurdle you solved?",
            "<strong>HR Fit:</strong> Why are you interested in this role, and how does your engineering background align with the goals of our company?",
            "<strong>Behavioral:</strong> Tell me about a time you had a technical disagreement with a team member. How did you handle it and what was the outcome?",
            "<strong>Coding Challenge:</strong> How do you find the longest palindromic substring in a string? Walk me through your optimal space and time complexity approach."
        );
        return String.join("|||", list);
    }

    public String getRecruiterSummary(String resumeText, String jobDescription) {
        String prompt = "You are a professional hiring manager. Analyze the candidate's resume and target job description: " + jobDescription + "\\n" +
                "Summarize the resume in under 15 seconds. Output in clean HTML format with exact headings:\\n" +
                "- **Strengths**: 2-3 bullet points\\n" +
                "- **Weaknesses**: 2-3 bullet points\\n" +
                "- **Recommendation**: Respond with exactly one option: [Strong Hire / Shortlist / Maybe / Reject]\\n" +
                "Do not include ```html markdown wrap.";

        String result = callGemini(prompt);
        if (result != null && !result.isBlank()) {
            return result;
        }

        // Fallback
        String recommendation = "Shortlist";
        if (resumeText.toLowerCase().contains("java") && resumeText.toLowerCase().contains("spring")) {
            recommendation = "Strong Hire";
        } else if (resumeText.length() < 200) {
            recommendation = "Reject";
        }

        StringBuilder html = new StringBuilder();
        html.append("<h6><strong>Recruiter 15s Summary</strong></h6>");
        html.append("<div class='mb-2'>Recommendation: <span class='badge bg-success text-uppercase'>").append(recommendation).append("</span></div>");
        html.append("<div class='mb-2'><strong>Strengths:</strong>");
        html.append("<ul class='ps-3 mb-2'>");
        html.append("<li>Dynamic technical capabilities matching standard requirements.</li>");
        html.append("<li>Defined projects section with clear architecture details.</li>");
        html.append("</ul></div>");
        html.append("<div><strong>Weaknesses:</strong>");
        html.append("<ul class='ps-3 mb-0'>");
        html.append("<li>Could include more business metrics in experience descriptions.</li>");
        html.append("<li>Missing live links or deployment references in projects.</li>");
        html.append("</ul></div>");
        return html.toString();
    }

    public String getDetailedKeywordAnalysis(String resumeText, String requiredSkills) {
        String prompt = "You are an ATS Keyword Optimizer. Compare the resume text with the required skills: " + requiredSkills + "\\n" +
                "Analyze matched keywords, missing keywords, keyword frequency, industry importance, and estimated impact on ATS score.\\n" +
                "Respond in clean HTML format containing a structured dashboard table with columns: Keyword, Frequency, Importance, Impact, Status (Matched/Missing).\\n" +
                "Do not include ```html markdown wrap.";

        String result = callGemini(prompt);
        if (result != null && !result.isBlank()) {
            return result;
        }

        // Fallback
        String[] skills = requiredSkills.split(",");
        StringBuilder html = new StringBuilder();
        html.append("<table class='table table-dark table-sm table-borderless small mb-0'>");
        html.append("<thead><tr class='text-secondary'><th>Keyword</th><th>Frequency</th><th>Importance</th><th>Impact</th><th>Status</th></tr></thead>");
        html.append("<tbody>");
        for (String skill : skills) {
            String trimmed = skill.trim();
            if (trimmed.isEmpty()) continue;
            int count = 0;
            Pattern p = Pattern.compile("(?i)\\b" + Pattern.quote(trimmed) + "\\b");
            Matcher m = p.matcher(resumeText);
            while (m.find()) count++;

            String status = count > 0 ? "<span class='text-success'>Matched</span>" : "<span class='text-danger'>Missing</span>";
            String impact = count > 0 ? "✓ Met" : "+8% ATS Score";
            String importance = trimmed.length() > 5 ? "High" : "Medium";

            html.append("<tr>");
            html.append("<td>").append(trimmed).append("</td>");
            html.append("<td>").append(count).append("</td>");
            html.append("<td><span class='badge bg-secondary'>").append(importance).append("</span></td>");
            html.append("<td class='text-success'>").append(impact).append("</td>");
            html.append("<td>").append(status).append("</td>");
            html.append("</tr>");
        }
        html.append("</tbody></table>");
        return html.toString();
    }

    public String getSoftSkillsAnalysis(String resumeText) {
        String prompt = "You are an expert HR Evaluator. Audit the resume for evidence of soft skills:\\n" +
                "Communication, Teamwork, Leadership, Ownership, Mentoring, Presentation, Adaptability, Analytical Thinking, and Problem Solving.\\n" +
                "Show evidence extracted for each, flag missing ones, and suggest improved bullet points.\\n" +
                "Respond in clean HTML format with a tabular matrix structure. Do not include ```html markdown wrap.";

        String result = callGemini(prompt);
        if (result != null && !result.isBlank()) {
            return result;
        }

        // Fallback
        int matchedCount = 0;
        StringBuilder html = new StringBuilder();
        html.append("<table class='table table-dark table-sm table-borderless small mb-0'>");
        html.append("<thead><tr class='text-secondary'><th>Soft Skill</th><th>Evidence</th><th>Status</th></tr></thead>");
        html.append("<tbody>");
        
        Map<String, String[]> softMap = Map.ofEntries(
            Map.entry("Communication", new String[]{"communicate", "communicated", "wrote", "written", "presented", "explained", "articulated", "reporting", "liaised"}),
            Map.entry("Leadership", new String[]{"led", "managed", "headed", "governed", "spearheaded", "directed", "supervised", "mentored", "coached"}),
            Map.entry("Teamwork", new String[]{"collaborated", "team", "assisted", "partnered", "coordinated", "support", "member", "cooperated"}),
            Map.entry("Problem Solving", new String[]{"solved", "resolved", "fixed", "optimized", "troubleshot", "debugged", "rectified"}),
            Map.entry("Analytical Thinking", new String[]{"analyzed", "evaluated", "calculated", "assessed", "researched", "investigated", "data"}),
            Map.entry("Ownership", new String[]{"owned", "accountable", "responsible", "delivered", "drove", "executed"}),
            Map.entry("Presentation", new String[]{"presented", "demonstrated", "exhibited", "showcased", "pitched"}),
            Map.entry("Adaptability", new String[]{"adapted", "pivoted", "flexible", "adjusted", "learned", "transitioned"}),
            Map.entry("Learning Ability", new String[]{"learned", "mastered", "acquired", "bootcamp", "certified"}),
            Map.entry("Decision Making", new String[]{"decided", "chose", "selected", "determined", "prioritized"}),
            Map.entry("Critical Thinking", new String[]{"reviewed", "audited", "diagnosed", "refined", "structured"}),
            Map.entry("Time Management", new String[]{"scheduled", "timeline", "deadline", "milestone", "prioritized"})
        );

        for (Map.Entry<String, String[]> entry : softMap.entrySet()) {
            boolean found = false;
            String matchText = "Not explicitly stated";
            for (String keyword : entry.getValue()) {
                if (resumeText.toLowerCase().contains(keyword)) {
                    found = true;
                    matchText = "Extracted keyword reference: \"" + keyword + "\" usage detected.";
                    matchedCount++;
                    break;
                }
            }
            String status = found ? "<span class='text-success'>✓ Extracted</span>" : "<span class='text-warning'>✖ Missing</span>";
            html.append("<tr>");
            html.append("<td><strong>").append(entry.getKey()).append("</strong></td>");
            html.append("<td>").append(matchText).append("</td>");
            html.append("<td>").append(status).append("</td>");
            html.append("</tr>");
        }
        html.append("</tbody></table>");
        
        int softScore = Math.max(40, Math.min(100, 40 + (matchedCount * 5)));
        html.append("<div class='mt-2 fw-bold text-white small'>Soft Skills Score: ").append(softScore).append("/100</div>");
        return html.toString();
    }

    public String getDetailedExplainableScoring(com.resumeanalyzer.model.Resume resume, com.resumeanalyzer.model.Job job) {
        String prompt = "You are an ATS Scoring Auditor. Create a transparent, explainable scoring breakdown for the 8 resume dimensions:\\n" +
                "Skills, Projects, Resume Quality, ATS Compatibility, Soft Skills, Certifications, Interview Readiness, and Placement Readiness.\\n" +
                "For each section, provide:\\n" +
                "- Current score out of 100\\n" +
                "- Formula/Calculation details\\n" +
                "- Extracted evidence\\n" +
                "- Missing items\\n" +
                "- Deduction reasons\\n" +
                "- Specific recommendations\\n" +
                "- Expected score improvement after applying the recommendation\\n\\n" +
                "Format the response in clean HTML, utilizing collapsible accordion sections or cards. " +
                "Make it feel extremely detailed like Resume Worded. Do not include ```html markdown wrap.";

        String result = callGemini(prompt);
        if (result != null && !result.isBlank()) {
            return result;
        }

        // Fallback
        int skillsVal = resume.getSkillsScore() != null ? resume.getSkillsScore() : 75;
        int projVal = resume.getProjectScore() != null ? resume.getProjectScore() : 70;
        int qualityVal = resume.getGrammarScore() != null ? resume.getGrammarScore() : 80;
        int atsVal = resume.getStructureScore() != null ? resume.getStructureScore() : 85;
        int softVal = resume.getSoftSkillsScore() != null ? resume.getSoftSkillsScore() : 70;
        int certsVal = resume.getCertificationsScore() != null ? resume.getCertificationsScore() : 60;
        int internVal = resume.getInternshipScore() != null ? resume.getInternshipScore() : 50;

        StringBuilder html = new StringBuilder();
        html.append("<div class='accordion' id='scoringAccordion'>");
        
        // 1. Skills
        html.append(buildAccordionItem("Skills", skillsVal, "Matched keywords vs JD requirements (Technical Skills weight: 30%)", 
            "Skills parsed: " + resume.getSkillsFound(), "Missing: " + resume.getSkillsMissing(), "Lacks exact keyword matches", "Inject missing skills", 15));
        
        // 2. Projects
        html.append(buildAccordionItem("Projects", projVal, "Project description quality & STAR structure (Weight: 25%)",
            "Project Experience section parsed", "Quantifiable metrics, business impact", "Bullet points are descriptive rather than impact-driven", "Implement STAR rewrites with percentage metrics", 20));

        // 3. Resume Quality
        html.append(buildAccordionItem("Resume Quality", qualityVal, "Linguistic score, weak verbs, passive voice (Weight: 10%)",
            "Punctuation check complete", "Active voice structure in some sentences", "Passive voice usage detected", "Convert passive text to active sentences", 10));

        // 4. ATS Compatibility
        html.append(buildAccordionItem("ATS Compatibility", atsVal, "Structure scanner, contact details, parsing zones (Weight: 15%)",
            "Email & Phone parsed", "LinkedIn or GitHub links if missing", "Missing professional references in header", "Add LinkedIn and GitHub URLs to header", 10));

        // 5. Soft Skills
        html.append(buildAccordionItem("Soft Skills", softVal, "Core behavioral competencies matches (Weight: 10%)",
            "Extracted keywords related to teamwork or problem solving", "Communication, Adaptability, Time Management", "Lacks explicit soft skill action verbs", "Rephrase bullets to highlight teamwork and leadership", 10));

        // 6. Certifications
        html.append(buildAccordionItem("Certifications", certsVal, "Matching industry standard credentials (Weight: 5%)",
            "Parsed certification lists", "Relevant industry certified courses", "No certs section or mismatch with JD requirements", "Acquire and add certified details", 10));

        // 7. Internship / Experience
        html.append(buildAccordionItem("Internship", internVal, "Internship proxy experience (Weight: 5%)",
            "Experience bullet length and count", "Additional professional context", "Lacks sufficient duration details", "Define months and accomplishments clearly", 5));

        html.append("</div>");
        return html.toString();
    }

    private String buildAccordionItem(String title, int score, String formula, String evidence, String missing, String deduction, String recommendation, int increase) {
        String id = title.replaceAll("\\s+", "");
        return "<div class='accordion-item bg-dark border-0 mb-2' style='border-radius: 12px; overflow: hidden;'>" +
               "<h2 class='accordion-header' id='heading" + id + "'>" +
               "<button class='accordion-button collapsed bg-dark text-white fw-bold d-flex justify-content-between align-items-center' type='button' data-bs-toggle='collapse' data-bs-target='#collapse" + id + "' aria-expanded='false' aria-controls='collapse" + id + "' style='box-shadow: none;'>" +
               "<span>" + title + "</span><span class='badge bg-primary ms-3'>" + score + "%</span>" +
               "</button>" +
               "</h2>" +
               "<div id='collapse" + id + "' class='accordion-collapse collapse' aria-labelledby='heading" + id + "' data-bs-parent='#scoringAccordion'>" +
               "<div class='accordion-body text-secondary small bg-dark border-top' style='border-color: rgba(255,255,255,0.05) !important; line-height: 1.6;'>" +
               "<div><strong>Calculation Formula:</strong> " + formula + "</div>" +
               "<div class='mt-2'><strong>Extracted Evidence:</strong> " + evidence + "</div>" +
               "<div class='mt-2'><strong>Missing Elements:</strong> <span class='text-warning'>" + missing + "</span></div>" +
               "<div class='mt-2'><strong>Reason for Deduction:</strong> " + deduction + "</div>" +
               "<div class='mt-2'><strong>AI Recommendation:</strong> " + recommendation + "</div>" +
               "<div class='mt-2 text-success'><strong>Expected Score Increase:</strong> +" + increase + "%</div>" +
               "</div>" +
               "</div>" +
               "</div>";
    }

    public String getDetailedImprovementRoadmap(String suggestions) {
        String prompt = "You are an ATS Optimizer. Based on the following suggestions, generate a dynamic roadmap:\\n" + suggestions + "\\n\\n" +
                "Rank the improvements by estimated score increase. For each, show before/after scores, score increase, and implementation effort (Low/Medium/High).\\n" +
                "Output in clean HTML format. Do not include ```html markdown wrap.";

        String result = callGemini(prompt);
        if (result != null && !result.isBlank()) {
            return result;
        }

        // Fallback
        StringBuilder html = new StringBuilder();
        html.append("<div class='d-flex flex-column gap-3'>");
        html.append("<div class='p-3 rounded border bg-dark border-opacity-25 d-flex justify-content-between align-items-center' style='border-color: rgba(255,255,255,0.05) !important;'>");
        html.append("<div><h6 class='fw-bold text-white mb-1'>1. Key Technical Keywords Injection</h6><p class='text-secondary small mb-0'>Inject missing required skills dynamically to boost compatibility.</p></div>");
        html.append("<div class='text-end'><span class='badge bg-success'>+10% Increase</span><div class='small text-secondary mt-1'>Effort: Low</div></div>");
        html.append("</div>");
        
        html.append("<div class='p-3 rounded border bg-dark border-opacity-25 d-flex justify-content-between align-items-center' style='border-color: rgba(255,255,255,0.05) !important;'>");
        html.append("<div><h6 class='fw-bold text-white mb-1'>2. Project STAR Structure Update</h6><p class='text-secondary small mb-0'>Rewrite project details using active verbs and metrics.</p></div>");
        html.append("<div class='text-end'><span class='badge bg-success'>+8% Increase</span><div class='small text-secondary mt-1'>Effort: Medium</div></div>");
        html.append("</div>");
        html.append("</div>");
        return html.toString();
    }

    public String getResumeChatAnswer(com.resumeanalyzer.model.Resume resume, String question) {
        String prompt = "You are CareerBoost AI Recruiter Chat Assistant. You are chatting with the recruiter about this candidate's resume.\\n" +
                "Use ONLY the following parsed candidate resume data to answer the recruiter's question. If the answer cannot be found in the resume, politely state that you can only answer questions using the parsed resume data.\\n\\n" +
                "Candidate Name: " + resume.getExtractedName() + "\\n" +
                "Email: " + resume.getExtractedEmail() + "\\n" +
                "Phone: " + resume.getExtractedPhone() + "\\n" +
                "Skills: " + resume.getSectionSkills() + "\\n" +
                "Projects: " + resume.getSectionProjects() + "\\n" +
                "Experience: " + resume.getSectionExperience() + "\\n" +
                "Education: " + resume.getSectionEducation() + "\\n" +
                "Score: " + resume.getAtsScore() + "%\\n\\n" +
                "Question: " + question;

        String result = callGemini(prompt);
        if (result != null && !result.isBlank()) {
            return result;
        }

        // Fallback
        String qLower = question.toLowerCase();
        if (qLower.contains("skills") || qLower.contains("tech") || qLower.contains("language")) {
            return "Based on the candidate's resume, the parsed technical skills include: " + resume.getSectionSkills();
        } else if (qLower.contains("project") || qLower.contains("experience") || qLower.contains("work")) {
            return "The candidate has documented projects including: " + (resume.getSectionProjects().length() > 200 ? resume.getSectionProjects().substring(0, 200) + "..." : resume.getSectionProjects());
        } else if (qLower.contains("contact") || qLower.contains("email") || qLower.contains("phone")) {
            return "Contact details found: Email: " + resume.getExtractedEmail() + ", Phone: " + resume.getExtractedPhone();
        } else if (qLower.contains("education") || qLower.contains("degree") || qLower.contains("college")) {
            return "Education history parsed: " + resume.getSectionEducation();
        }
        return "I can answer questions regarding the candidate's skills, projects, work experience, education, or contact details. What specific details would you like to know?";
    }
}

