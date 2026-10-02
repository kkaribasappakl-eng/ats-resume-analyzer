package com.resumeanalyzer.controller;

import com.resumeanalyzer.model.Resume;
import com.resumeanalyzer.model.User;
import com.resumeanalyzer.repository.ResumeRepository;
import com.resumeanalyzer.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Controller
public class DashboardController {

    private final UserService userService;
    private final ResumeRepository resumeRepository;

    public DashboardController(UserService userService, ResumeRepository resumeRepository) {
        this.userService = userService;
        this.resumeRepository = resumeRepository;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        User user = userService.getCurrentUser();
        List<Resume> history = resumeRepository.findByUserOrderByCreatedAtDesc(user);

        long count = resumeRepository.countByUser(user);
        Double avg = resumeRepository.getAverageScoreByUser(user);
        int avgScore = avg != null ? (int) Math.round(avg) : 0;

        Resume latest = history.isEmpty() ? null : history.get(0);

        // Build simple lists for the trend chart (oldest to newest)
        List<String> trendDates = new ArrayList<>();
        List<Integer> trendScores = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        List<Resume> reversed = new ArrayList<>(history);
        Collections.reverse(reversed);
        for (Resume r : reversed) {
            trendDates.add(r.getCreatedAt().format(fmt));
            trendScores.add(r.getAtsScore() != null ? r.getAtsScore() : 0);
        }

        int bestScore = 0;
        for (Resume r : history) {
            if (r.getAtsScore() != null && r.getAtsScore() > bestScore) {
                bestScore = r.getAtsScore();
            }
        }

        model.addAttribute("username", user.getUsername());
        model.addAttribute("role", user.getRole() != null ? user.getRole().name() : "ROLE_USER");
        model.addAttribute("history", history);
        model.addAttribute("uploadCount", count);
        model.addAttribute("avgScore", avgScore);
        model.addAttribute("bestScore", bestScore);
        model.addAttribute("latest", latest);
        model.addAttribute("trendDates", trendDates);
        model.addAttribute("trendScores", trendScores);

        return "dashboard";
    }
}
