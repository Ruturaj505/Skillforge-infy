package com.skill_forge.infy_intern.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.Map;

@Document(collection = "quiz_responses")
public class QuizResponse {
    @Id
    private String id;
    private String studentEmail;
    private String courseId;
    private String sectionId;
    private String quizId;
    private Map<Integer, Integer> answers; // questionIndex -> selectedOptionIndex
    private Integer score;
    private Boolean passed;
    private Integer durationSeconds;
    private Instant submittedAt;

    public QuizResponse() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStudentEmail() { return studentEmail; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }

    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }

    public String getSectionId() { return sectionId; }
    public void setSectionId(String sectionId) { this.sectionId = sectionId; }

    public String getQuizId() { return quizId; }
    public void setQuizId(String quizId) { this.quizId = quizId; }

    public Map<Integer, Integer> getAnswers() { return answers; }
    public void setAnswers(Map<Integer, Integer> answers) { this.answers = answers; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public Boolean getPassed() { return passed; }
    public void setPassed(Boolean passed) { this.passed = passed; }

    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }

    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
}
