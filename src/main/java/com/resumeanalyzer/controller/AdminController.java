package com.resumeanalyzer.controller;

import com.resumeanalyzer.model.Job;
import com.resumeanalyzer.model.Resume;
import com.resumeanalyzer.model.User;
import com.resumeanalyzer.repository.ResumeRepository;
import com.resumeanalyzer.repository.UserRepository;
import com.resumeanalyzer.service.JobService;
import com.resumeanalyzer.service.AtsAnalyzerService;
import com.resumeanalyzer.service.ResumeParserService;
import com.resumeanalyzer.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final JobService jobService;
    private final UserRepository userRepository;
    private final ResumeRepository resumeRepository;
    private final ResumeParserService parserService;
    private final AtsAnalyzerService analyzerService;
    private final UserService userService;

    public AdminController(JobService jobService, 
                           UserRepository userRepository, 
                           ResumeRepository resumeRepository,
                           ResumeParserService parserService,
                           AtsAnalyzerService analyzerService,
                           UserService userService) {
        this.jobService = jobService;
        this.userRepository = userRepository;
        this.resumeRepository = resumeRepository;
        this.parserService = parserService;
        this.analyzerService = analyzerService;
        this.userService = userService;
    }

    @GetMapping
    public String redirectToDashboard() {
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/dashboard")
    public String showAdminConsole(@RequestParam(value = "selectedJobId", required = false) Long selectedJobId,
                                   Model model) {
        long totalUsers = userRepository.count();
        long totalResumes = resumeRepository.count();

        Double avg = resumeRepository.getAverageAtsScore();
        int avgAtsScore = avg != null ? (int) Math.round(avg) : 0;

        List<User> usersList = userRepository.findAll();
        List<Resume> recentResumes = resumeRepository.findTop10ByOrderByCreatedAtDesc();
        List<Job> jobsList = jobService.getAllJobs();

        List<Resume> rankedResumes = null;
        Job selectedJob = null;
        if (selectedJobId != null) {
            selectedJob = jobService.getJobById(selectedJobId);
            final String matchedTitle = selectedJob.getTitle();
            rankedResumes = resumeRepository.findAll().stream()
                    .filter(r -> r.getJobTitleMatched() != null && r.getJobTitleMatched().equalsIgnoreCase(matchedTitle))
                    .sorted((r1, r2) -> r2.getAtsScore().compareTo(r1.getAtsScore()))
                    .collect(Collectors.toList());
        }

        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalResumes", totalResumes);
        model.addAttribute("avgAtsScore", avgAtsScore);
        model.addAttribute("users", usersList);
        model.addAttribute("resumes", recentResumes);
        model.addAttribute("jobs", jobsList);
        model.addAttribute("newJob", new Job());
        model.addAttribute("rankedResumes", rankedResumes);
        model.addAttribute("selectedJob", selectedJob);
        model.addAttribute("role", "ROLE_ADMIN");

        return "admin";
    }

    @PostMapping("/jobs/add")
    public String addJobPost(@ModelAttribute("newJob") Job job) {
        jobService.createJob(job);
        return "redirect:/admin/dashboard?jobAdded=true";
    }

    @PostMapping("/jobs/delete/{id}")
    public String deleteJobPost(@PathVariable("id") Long id) {
        jobService.deleteJob(id);
        return "redirect:/admin/dashboard?jobDeleted=true";
    }

    @PostMapping("/recruiter/upload")
    public String uploadRecruiterResumes(@RequestParam("files") MultipartFile[] files,
                                         @RequestParam("jobId") Long jobId) {
        if (files == null || files.length == 0) {
            return "redirect:/admin/dashboard?error=NoFiles";
        }
        
        try {
            User currentUser = userService.getCurrentUser();
            Job targetJob = jobService.getJobById(jobId);
            
            for (MultipartFile file : files) {
                if (file.isEmpty()) continue;
                String parsedText = parserService.parseFile(file);
                
                Resume resume = new Resume();
                resume.setFileName(file.getOriginalFilename());
                resume.setFileType(file.getContentType());
                resume.setParsedText(parsedText);
                resume.setUser(currentUser);
                resume.setResumeName("Recruiter Scan: " + file.getOriginalFilename());
                resume.setFileSize(file.getSize());
                resume.setVersionNumber(0);
                resume.setStatus("Archived");
                resume.setFileData(file.getBytes());
                
                analyzerService.analyze(resume, targetJob);
                resumeRepository.save(resume);
            }
            
            return "redirect:/admin/dashboard?rankingUploaded=true&selectedJobId=" + jobId;
        } catch (Exception e) {
            return "redirect:/admin/dashboard?error=" + e.getMessage();
        }
    }
}
