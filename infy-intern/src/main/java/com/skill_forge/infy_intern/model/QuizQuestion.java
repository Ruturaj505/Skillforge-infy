package com.skill_forge.infy_intern.model;

import java.util.ArrayList;
import java.util.List;

public class QuizQuestion {
    private String id;
    private String question;
    private List<String> options = new ArrayList<>();
    private Integer correctOptionIndex; // 0-based index
    private String explanation;

    public QuizQuestion() {}

    public QuizQuestion(String id, String question) {
        this.id = id;
        this.question = question;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public List<String> getOptions() { return options; }
    public void setOptions(List<String> options) { this.options = options != null ? options : new ArrayList<>(); }

    public Integer getCorrectOptionIndex() { return correctOptionIndex; }
    public void setCorrectOptionIndex(Integer correctOptionIndex) { this.correctOptionIndex = correctOptionIndex; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
}
