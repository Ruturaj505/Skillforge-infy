package com.skill_forge.infy_intern.model;

import java.util.ArrayList;
import java.util.List;

public class Quiz {
    private String id;
    private String title;
    private String description;
    private List<QuizQuestion> questions = new ArrayList<>();
    private Integer passingScore; // e.g., 70 for 70%
    private Boolean isPublished;
    private Integer timeLimitSeconds; // optional time limit in seconds
    private Boolean generatedByAI = false;

    public Quiz() {}

    public Quiz(String id, String title) {
        this.id = id;
        this.title = title;
        this.passingScore = 70;
        this.isPublished = false;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<QuizQuestion> getQuestions() { return questions; }
    public void setQuestions(List<QuizQuestion> questions) { this.questions = questions != null ? questions : new ArrayList<>(); }

    public Integer getPassingScore() { return passingScore; }
    public void setPassingScore(Integer passingScore) { this.passingScore = passingScore; }

    public Boolean getIsPublished() { return isPublished; }
    public void setIsPublished(Boolean isPublished) { this.isPublished = isPublished; }

    public Integer getTimeLimitSeconds() { return timeLimitSeconds; }
    public void setTimeLimitSeconds(Integer timeLimitSeconds) { this.timeLimitSeconds = timeLimitSeconds; }

    public Boolean getGeneratedByAI() { return generatedByAI; }
    public void setGeneratedByAI(Boolean generatedByAI) { this.generatedByAI = generatedByAI; }
}
