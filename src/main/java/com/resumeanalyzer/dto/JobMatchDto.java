package com.resumeanalyzer.dto;

import com.resumeanalyzer.model.Job;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JobMatchDto {
    private Job job;
    private Integer matchPercentage;
    private List<String> matchingSkills;
    private List<String> missingSkills;
    private List<String> missingKeywords;
    private String applyRecommendation;
    private boolean hasResume;
}
