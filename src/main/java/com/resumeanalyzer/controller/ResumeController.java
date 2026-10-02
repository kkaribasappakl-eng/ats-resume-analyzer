package com.resumeanalyzer.controller;

import com.resumeanalyzer.model.Job;
import com.resumeanalyzer.model.Resume;
import com.resumeanalyzer.model.User;
import com.resumeanalyzer.repository.ResumeRepository;
import com.resumeanalyzer.service.AtsAnalyzerService;
import com.resumeanalyzer.service.AiAssistantService;
import com.resumeanalyzer.service.JobService;
import com.resumeanalyzer.service.ResumeParserService;
import com.resumeanalyzer.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/resume")
public class ResumeController {

    private final ResumeParserService parserService;
    private final AtsAnalyzerService analyzerService;
    private final AiAssistantService aiAssistantService;
    private final JobService jobService;
    private final UserService userService;
    private final ResumeRepository resumeRepository;

    public ResumeController(ResumeParserService parserService,
                            AtsAnalyzerService analyzerService,
                            AiAssistantService aiAssistantService,
                            JobService jobService,
                            UserService userService,
                            ResumeRepository resumeRepository) {
        this.parserService = parserService;
        this.analyzerService = analyzerService;
        this.aiAssistantService = aiAssistantService;
        this.jobService = jobService;
        this.userService = userService;
        this.resumeRepository = resumeRepository;
    }

    @GetMapping("/analyze")
    public String showAnalyzePage(Model model) {
        List<Job> jobs = jobService.getAllJobs();
        model.addAttribute("jobs", jobs);
        return "analyze";
    }

    @PostMapping("/analyze")
    public String handleFileUpload(@RequestParam("file") MultipartFile file,
                                   @RequestParam("jobId") Long jobId,
                                   @RequestParam(value = "makeActive", defaultValue = "false") boolean makeActive,
                                   Model model) {
        if (file.isEmpty()) {
            model.addAttribute("error", "Please select a valid PDF or DOCX file to upload.");
            model.addAttribute("jobs", jobService.getAllJobs());
            return "analyze";
        }

        try {
            User currentUser = userService.getCurrentUser();
            Job targetJob = jobService.getJobById(jobId);

            String parsedText = parserService.parseFile(file);

            long count = resumeRepository.countByUser(currentUser);
            int version = (int) count + 1;

            Resume resume = new Resume();
            resume.setFileName(file.getOriginalFilename());
            resume.setFileType(file.getContentType());
            resume.setParsedText(parsedText);
            resume.setUser(currentUser);
            resume.setResumeName("Resume V" + version);
            resume.setFileSize(file.getSize());
            resume.setVersionNumber(version);
            resume.setFileData(file.getBytes());

            boolean shouldBeActive = makeActive || count == 0;
            resume.setStatus(shouldBeActive ? "Active" : "Archived");

            if (shouldBeActive) {
                List<Resume> activeResumes = resumeRepository.findByUserAndStatus(currentUser, "Active");
                for (Resume active : activeResumes) {
                    active.setStatus("Archived");
                    resumeRepository.save(active);
                }
            }

            analyzerService.analyze(resume, targetJob);
            Resume saved = resumeRepository.save(resume);

            return "redirect:/resume/report/" + saved.getId();

        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("jobs", jobService.getAllJobs());
            return "analyze";
        } catch (IOException e) {
            model.addAttribute("error", "Error reading file content: " + e.getMessage());
            model.addAttribute("jobs", jobService.getAllJobs());
            return "analyze";
        }
    }

    @GetMapping("/report/{id}")
    public String showReport(@PathVariable("id") Long id, Model model) {
        Resume resume = resumeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Resume analysis report not found."));

        User currentUser = userService.getCurrentUser();
        if ((currentUser.getRole() == null || currentUser.getRole().name().equals("ROLE_USER")) && !resume.getUser().getId().equals(currentUser.getId())) {
            return "redirect:/dashboard?error=Unauthorized";
        }

        List<Resume> history = resumeRepository.findByUserOrderByCreatedAtDesc(currentUser);
        int bestScore = 0;
        Integer previousScore = null;
        boolean foundCurrent = false;
        for (Resume r : history) {
            if (r.getAtsScore() != null && r.getAtsScore() > bestScore) {
                bestScore = r.getAtsScore();
            }
            if (foundCurrent && previousScore == null) {
                previousScore = r.getAtsScore();
            }
            if (r.getId().equals(resume.getId())) {
                foundCurrent = true;
            }
        }

        model.addAttribute("resume", resume);
        model.addAttribute("bestScore", bestScore);
        model.addAttribute("previousScore", previousScore);
        return "report";
    }

    @PostMapping("/report/{id}/chat")
    @ResponseBody
    public ResponseEntity<Map<String, String>> chatWithResume(@PathVariable("id") Long id, @RequestBody Map<String, String> payload) {
        Resume resume = resumeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Resume not found."));
        
        String question = payload.get("message");
        if (question == null || question.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("response", "Question cannot be empty."));
        }

        String answer = aiAssistantService.getResumeChatAnswer(resume, question);
        return ResponseEntity.ok(Map.of("response", answer));
    }

    @PostMapping("/delete/{id}")
    public String deleteReport(@PathVariable("id") Long id) {
        Resume resume = resumeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Resume not found."));

        User currentUser = userService.getCurrentUser();
        if ((currentUser.getRole() == null || currentUser.getRole().name().equals("ROLE_USER")) && !resume.getUser().getId().equals(currentUser.getId())) {
            return "redirect:/dashboard?error=Unauthorized";
        }

        boolean wasActive = "Active".equals(resume.getStatus());
        resumeRepository.delete(resume);

        if (wasActive) {
            List<Resume> remaining = resumeRepository.findByUserOrderByCreatedAtDesc(currentUser);
            if (!remaining.isEmpty()) {
                Resume latest = remaining.get(0);
                latest.setStatus("Active");
                resumeRepository.save(latest);
            }
        }
        return "redirect:/resume/my-resumes?deleted=true";
    }

    // --- Resume Management Endpoints --- //

    @GetMapping("/my-resumes")
    public String myResumes(Model model) {
        User currentUser = userService.getCurrentUser();
        List<Resume> history = resumeRepository.findByUserOrderByCreatedAtDesc(currentUser);
        List<Resume> versionHistory = resumeRepository.findByUserOrderByVersionNumberAsc(currentUser);
        
        Resume activeResume = null;
        for (Resume r : history) {
            if ("Active".equals(r.getStatus())) {
                activeResume = r;
                break;
            }
        }

        // Prep data for history line chart
        List<String> labels = new ArrayList<>();
        List<Integer> scores = new ArrayList<>();
        for (Resume r : versionHistory) {
            labels.add("V" + r.getVersionNumber() + " (" + r.getResumeName() + ")");
            scores.add(r.getAtsScore() != null ? r.getAtsScore() : 0);
        }

        model.addAttribute("history", history);
        model.addAttribute("activeResume", activeResume);
        model.addAttribute("role", currentUser.getRole() != null ? currentUser.getRole().name() : "ROLE_USER");
        model.addAttribute("chartLabels", labels);
        model.addAttribute("chartScores", scores);

        return "my-resumes";
    }

    @GetMapping("/download/{id}")
    @ResponseBody
    public ResponseEntity<byte[]> downloadResume(@PathVariable("id") Long id) {
        Resume resume = resumeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Resume not found."));

        User currentUser = userService.getCurrentUser();
        if ((currentUser.getRole() == null || currentUser.getRole().name().equals("ROLE_USER")) && !resume.getUser().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(403).body(null);
        }

        byte[] fileBytes = resume.getFileData();
        if (fileBytes == null || fileBytes.length == 0) {
            fileBytes = (resume.getParsedText() != null ? resume.getParsedText() : "Empty Resume Text").getBytes();
        }

        String safeFileName = resume.getFileName().replaceAll("[^a-zA-Z0-9.\\-_]", "_");

        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + safeFileName + "\"")
                .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                .body(fileBytes);
    }

    @PostMapping("/rename/{id}")
    public String renameResume(@PathVariable("id") Long id, @RequestParam("resumeName") String resumeName) {
        Resume resume = resumeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Resume not found."));

        User currentUser = userService.getCurrentUser();
        if ((currentUser.getRole() == null || currentUser.getRole().name().equals("ROLE_USER")) && !resume.getUser().getId().equals(currentUser.getId())) {
            return "redirect:/resume/my-resumes?error=Unauthorized";
        }

        if (resumeName != null && !resumeName.trim().isEmpty()) {
            resume.setResumeName(resumeName.trim());
            resumeRepository.save(resume);
        }

        return "redirect:/resume/my-resumes?renamed=true";
    }

    @PostMapping("/activate/{id}")
    public String activateResume(@PathVariable("id") Long id) {
        Resume resume = resumeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Resume not found."));

        User currentUser = userService.getCurrentUser();
        if ((currentUser.getRole() == null || currentUser.getRole().name().equals("ROLE_USER")) && !resume.getUser().getId().equals(currentUser.getId())) {
            return "redirect:/resume/my-resumes?error=Unauthorized";
        }

        // Archive other resumes
        List<Resume> activeResumes = resumeRepository.findByUserAndStatus(currentUser, "Active");
        for (Resume active : activeResumes) {
            active.setStatus("Archived");
            resumeRepository.save(active);
        }

        resume.setStatus("Active");
        resumeRepository.save(resume);

        return "redirect:/resume/my-resumes?activated=true";
    }

    @GetMapping("/compare")
    public String compareResumes(@RequestParam(value = "id1", required = false) Long id1,
                                 @RequestParam(value = "id2", required = false) Long id2,
                                 Model model) {
        User currentUser = userService.getCurrentUser();
        List<Resume> history = resumeRepository.findByUserOrderByCreatedAtDesc(currentUser);

        if (id1 != null && id2 != null) {
            Resume resume1 = resumeRepository.findById(id1)
                    .orElseThrow(() -> new IllegalArgumentException("Resume 1 not found."));
            Resume resume2 = resumeRepository.findById(id2)
                    .orElseThrow(() -> new IllegalArgumentException("Resume 2 not found."));

            // Verify ownership
            if (!resume1.getUser().getId().equals(currentUser.getId()) || !resume2.getUser().getId().equals(currentUser.getId())) {
                return "redirect:/resume/my-resumes?error=Unauthorized";
            }

            model.addAttribute("resume1", resume1);
            model.addAttribute("resume2", resume2);
        }

        model.addAttribute("resumes", history);
        model.addAttribute("role", currentUser.getRole() != null ? currentUser.getRole().name() : "ROLE_USER");
        return "compare";
    }

    // --- AI Assistant API Endpoints --- //

    @PostMapping("/api/ai/rewrite")
    @ResponseBody
    public ResponseEntity<Map<String, String>> rewriteDescription(@RequestBody Map<String, String> payload) {
        String original = payload.getOrDefault("text", "");
        String rewritten = aiAssistantService.rewriteProjectDescription(original);
        return ResponseEntity.ok(Map.of("result", rewritten));
    }

    @PostMapping("/api/ai/summary")
    @ResponseBody
    public ResponseEntity<Map<String, String>> generateSummary(@RequestBody Map<String, String> payload) {
        String jobTitle = payload.getOrDefault("jobTitle", "Professional");
        String exp = payload.getOrDefault("experience", "several years");
        String skills = payload.getOrDefault("skills", "industry tools");
        
        String summary = aiAssistantService.generateResumeSummary(jobTitle, exp, skills);
        return ResponseEntity.ok(Map.of("result", summary));
    }
}
