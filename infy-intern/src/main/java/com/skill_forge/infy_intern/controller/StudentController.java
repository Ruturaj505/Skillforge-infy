package com.skill_forge.infy_intern.controller;

import com.skill_forge.infy_intern.model.Course;
import com.skill_forge.infy_intern.model.Enrollment;
import com.skill_forge.infy_intern.model.VideoEntity;
import com.skill_forge.infy_intern.repository.VideoRepository;
import com.skill_forge.infy_intern.service.StudentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import com.skill_forge.infy_intern.model.QuizResponse;

@RestController
@RequestMapping("/api/student")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class StudentController {

    private final StudentService studentService;
    private final VideoRepository videoRepository;

    public StudentController(StudentService studentService, VideoRepository videoRepository) {
        this.studentService = studentService;
        this.videoRepository = videoRepository;
    }

    @GetMapping("/browse")
    public ResponseEntity<List<Course>> browseCourses() {
        return ResponseEntity.ok(studentService.browseCourses());
    }

    @PostMapping("/enroll/{courseId}")
    public ResponseEntity<String> enroll(@RequestParam String email, @PathVariable String courseId) {
        return ResponseEntity.ok(studentService.enrollInCourse(email, courseId));
    }

    @GetMapping("/my-courses")
    public ResponseEntity<List<Enrollment>> getMyCourses(@RequestParam String email) {
        return ResponseEntity.ok(studentService.getMyCourses(email));
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<?> getCourseById(@PathVariable String courseId) {
        return studentService.getCourseById(courseId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.badRequest().body("Course not found"));
    }

    @GetMapping("/course/{courseId}/videos")
    public ResponseEntity<List<VideoEntity>> getCourseVideos(@PathVariable String courseId) {
        return ResponseEntity.ok(videoRepository.findByCourseId(courseId));
    }

    @PostMapping("/progress")
    public ResponseEntity<String> updateProgress(@RequestParam String email,
                                                 @RequestParam String courseId,
                                                 @RequestParam double progress) {
        return ResponseEntity.ok(studentService.updateProgress(email, courseId, progress));
    }

    // 🟢 Submit quiz answers for grading
    @PostMapping("/course/{courseId}/sections/{sectionId}/quizzes/{quizId}/submit")
    public ResponseEntity<?> submitQuiz(@PathVariable String courseId,
                                        @PathVariable String sectionId,
                                        @PathVariable String quizId,
                                        @RequestBody Map<String, Object> payload) {
        try {
            String studentEmail = (String) payload.get("studentEmail");
            // payload.answers expected to be a map of stringified indices -> integer selected index
            Object rawAnswers = payload.get("answers");
            java.util.Map<Integer, Integer> answers = new java.util.HashMap<>();
            if (rawAnswers instanceof java.util.Map) {
                java.util.Map<?,?> raw = (java.util.Map<?,?>) rawAnswers;
                for (java.util.Map.Entry<?,?> e : raw.entrySet()) {
                    try {
                        Integer key = Integer.parseInt(e.getKey().toString());
                        Integer val = Integer.parseInt(e.getValue().toString());
                        answers.put(key, val);
                    } catch (Exception ex) {
                        // skip bad entries
                    }
                }
            }
            Integer durationSeconds = payload.get("durationSeconds") == null ? null : Integer.parseInt(payload.get("durationSeconds").toString());

            com.skill_forge.infy_intern.model.QuizResponse resp = studentService.gradeQuiz(courseId, sectionId, quizId, studentEmail, answers, durationSeconds);
            return ResponseEntity.ok(resp);
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body(java.util.Map.of("error", e.getMessage()));
        }
    }
}
