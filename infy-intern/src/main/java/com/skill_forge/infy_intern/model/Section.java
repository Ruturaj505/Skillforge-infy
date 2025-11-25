package com.skill_forge.infy_intern.model;

import java.util.ArrayList;
import java.util.List;

public class Section {
    private String id;
    private String title;
    private List<VideoContent> videos = new ArrayList<>();
    private List<Object> lectures = new ArrayList<>(); // For frontend compatibility
    private List<Quiz> quizzes = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<VideoContent> getVideos() {
        return videos;
    }

    public void setVideos(List<VideoContent> videos) {
        this.videos = videos != null ? videos : new ArrayList<>();
    }
    
    public List<Object> getLectures() {
        return lectures;
    }
    
    public void setLectures(List<Object> lectures) {
        this.lectures = lectures != null ? lectures : new ArrayList<>();
    }

    public List<Quiz> getQuizzes() {
        return quizzes;
    }

    public void setQuizzes(List<Quiz> quizzes) {
        this.quizzes = quizzes != null ? quizzes : new ArrayList<>();
    }
}
