package com.skill_forge.infy_intern.controller;

import com.skill_forge.infy_intern.model.Course;
import com.skill_forge.infy_intern.model.Quiz;
import com.skill_forge.infy_intern.model.VideoEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.Map;
import com.skill_forge.infy_intern.service.CourseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/instructor/courses")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    // 🟢 Create a course
    @PostMapping("/create")
    public ResponseEntity<Course> createCourse(@RequestBody Course course) {
        return ResponseEntity.ok(courseService.createCourse(course));
    }

    // 🟢 Get all courses
    @GetMapping
    public ResponseEntity<List<Course>> getAllCourses() {
        return ResponseEntity.ok(courseService.getAllCourses());
    }

    // 🟢 Get course by ID
    @GetMapping("/{id}")
    public ResponseEntity<Object> getCourseById(@PathVariable String id) {
        return courseService.getById(id)
                .<ResponseEntity<Object>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.badRequest().body("Course not found"));
    }



    // 🟢 Add a section to course
    @PostMapping("/{courseId}/add-section")
    public ResponseEntity<Course> addSection(@PathVariable String courseId,
                                             @RequestParam String sectionTitle) {
        return ResponseEntity.ok(courseService.addSection(courseId, sectionTitle));
    }
    
    // 🟢 Delete a section from course
    @DeleteMapping("/{courseId}/sections/{sectionId}")
    public ResponseEntity<Course> deleteSection(@PathVariable String courseId,
                                                @PathVariable String sectionId) {
        return ResponseEntity.ok(courseService.deleteSection(courseId, sectionId));
    }
    
    // 🟢 Delete a course
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteCourse(@PathVariable String id) {
        courseService.deleteCourse(id);
        return ResponseEntity.ok("Course deleted successfully");
    }

    // 🟢 Upload video and attach to course
    @PostMapping("/{courseId}/upload-video")
    public ResponseEntity<?> uploadVideoAndAttach(@PathVariable String courseId,
                                                  @RequestParam(required = false) String sectionTitle,
                                                  @RequestParam("file") MultipartFile file,
                                                  @RequestParam String title,
                                                  @RequestParam String uploadedBy) {
        try {
            // Validate required parameters
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Video file is required"));
            }
            if (title == null || title.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Video title is required"));
            }
            if (uploadedBy == null || uploadedBy.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Uploader email is required"));
            }
            
            // Log request info
            System.out.println("📤 Video upload request received:");
            System.out.println("   Course ID: " + courseId);
            System.out.println("   Section: " + (sectionTitle != null ? sectionTitle : "Default"));
            System.out.println("   Title: " + title);
            System.out.println("   Uploaded by: " + uploadedBy);
            System.out.println("   File: " + file.getOriginalFilename() + 
                             " (" + (file.getSize() / 1024 / 1024) + " MB)");
            
            VideoEntity video = courseService.uploadVideoAndAttach(courseId, sectionTitle, file, title, uploadedBy);
            System.out.println("✅ Video upload successful: " + video.getId());
            return ResponseEntity.ok(video);
        } catch (RuntimeException e) {
            String errorMsg = e.getMessage();
            System.err.println("❌ Video upload error: " + errorMsg);
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", errorMsg != null ? errorMsg : "Video upload failed"));
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            System.err.println("❌ Unexpected error during video upload: " + e.getClass().getName() + ": " + errorMsg);
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Video upload failed: " + (errorMsg != null ? errorMsg : "Unknown error")));
        }
    }

    // 🟢 Get course videos
    @GetMapping("/{courseId}/videos")
    public ResponseEntity<List<VideoEntity>> getCourseVideos(@PathVariable String courseId) {
        return ResponseEntity.ok(courseService.getCourseVideos(courseId));
    }
    
    // 🟢 Add lecture to section (alternative endpoint for direct section/lecture management)
    @PostMapping("/{courseId}/sections/{sectionId}/lectures")
    public ResponseEntity<Course> addLectureToSection(@PathVariable String courseId,
                                                      @PathVariable String sectionId,
                                                      @RequestParam("file") MultipartFile file,
                                                      @RequestParam String title,
                                                      @RequestParam String uploadedBy) {
        return ResponseEntity.ok(courseService.addLectureToSection(courseId, sectionId, file, title, uploadedBy));
    }

    // 🟢 Upload course thumbnail
    @PostMapping("/{courseId}/thumbnail")
    public ResponseEntity<Course> uploadThumbnail(@PathVariable String courseId,
                                                  @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(courseService.updateCourseThumbnail(courseId, file));
    }

    // 🟢 Upload notes (PDF) for a course
    @PostMapping("/{courseId}/notes")
    public ResponseEntity<?> uploadCourseNotes(@PathVariable String courseId,
                                               @RequestParam("file") MultipartFile file,
                                               @RequestParam(required = false) String title) {
        try {
            Course updated = courseService.uploadCourseNote(courseId, file, title);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // 🟢 Add quiz to section
    @PostMapping("/{courseId}/sections/{sectionId}/quizzes")
    public ResponseEntity<?> addQuiz(@PathVariable String courseId,
                                     @PathVariable String sectionId,
                                     @RequestBody Quiz quiz) {
        try {
            Course updated = courseService.addQuizToSection(courseId, sectionId, quiz);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // 🟢 Generate quiz via AI for a given topic
    @PostMapping("/{courseId}/sections/{sectionId}/generate-quiz")
    public ResponseEntity<?> generateQuiz(@PathVariable String courseId,
                                          @PathVariable String sectionId,
                                          @RequestParam String topic,
                                          @RequestParam(required = false, defaultValue = "5") int numQuestions,
                                          @RequestParam(required = false) Integer timeLimitSeconds) {
        try {
            Course updated = courseService.generateQuizFromTopic(courseId, sectionId, topic, numQuestions, timeLimitSeconds);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // 🟢 Update quiz in section
    @PutMapping("/{courseId}/sections/{sectionId}/quizzes/{quizId}")
    public ResponseEntity<?> updateQuiz(@PathVariable String courseId,
                                        @PathVariable String sectionId,
                                        @PathVariable String quizId,
                                        @RequestBody Quiz quiz) {
        try {
            Course updated = courseService.updateQuiz(courseId, sectionId, quizId, quiz);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // 🟢 Delete quiz from section
    @DeleteMapping("/{courseId}/sections/{sectionId}/quizzes/{quizId}")
    public ResponseEntity<?> deleteQuiz(@PathVariable String courseId,
                                        @PathVariable String sectionId,
                                        @PathVariable String quizId) {
        try {
            Course updated = courseService.deleteQuizFromSection(courseId, sectionId, quizId);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
