package com.skill_forge.infy_intern.service;

import com.skill_forge.infy_intern.model.Course;
import com.skill_forge.infy_intern.model.Note;
import com.skill_forge.infy_intern.model.Quiz;
import com.skill_forge.infy_intern.model.Section;
import com.skill_forge.infy_intern.model.VideoEntity;
import com.skill_forge.infy_intern.repository.CourseRepository;
import com.skill_forge.infy_intern.repository.VideoRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final VideoRepository videoRepository;
    private final CloudinaryService cloudinaryService;

    public CourseService(CourseRepository courseRepository,
                         VideoRepository videoRepository,
                         CloudinaryService cloudinaryService) {
        this.courseRepository = courseRepository;
        this.videoRepository = videoRepository;
        this.cloudinaryService = cloudinaryService;
    }

    // 🟢 Create a new course
    public Course createCourse(Course course) {
        // Set default values if not provided
        if (course.getStatus() == null || course.getStatus().isEmpty()) {
            course.setStatus("draft");
        }
        if (course.getStudentsCount() == null) {
            course.setStudentsCount(0);
        }
        if (course.getVideoCount() == null) {
            course.setVideoCount(0);
        }
        if (course.getLanguage() == null || course.getLanguage().isEmpty()) {
            course.setLanguage("English");
        }
        return courseRepository.save(course);
    }

    // 🟢 Fetch all courses
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    // 🟢 Get course by ID
    public Optional<Course> getById(String id) {
        return courseRepository.findById(id);
    }

    // 🟢 Add section to course
    public Course addSection(String courseId, String sectionTitle) {
        Optional<Course> courseOpt = courseRepository.findById(courseId);
        if (courseOpt.isEmpty()) {
            throw new RuntimeException("Course not found");
        }
        
        Course course = courseOpt.get();
        Section newSection = new Section();
        newSection.setTitle(sectionTitle);
        newSection.setId(java.util.UUID.randomUUID().toString());
        
        if (course.getSections() == null) {
            course.setSections(new java.util.ArrayList<>());
        }
        course.getSections().add(newSection);
        
        return courseRepository.save(course);
    }
    
    // 🟢 Delete section from course
    public Course deleteSection(String courseId, String sectionId) {
        Optional<Course> courseOpt = courseRepository.findById(courseId);
        if (courseOpt.isEmpty()) {
            throw new RuntimeException("Course not found");
        }
        
        Course course = courseOpt.get();
        if (course.getSections() != null) {
            course.getSections().removeIf(section -> sectionId.equals(section.getId()));
            return courseRepository.save(course);
        }
        
        return course;
    }
    
    // 🟢 Delete course
    public void deleteCourse(String courseId) {
        courseRepository.deleteById(courseId);
    }

    // 🟢 Upload video and attach to a course
    public VideoEntity uploadVideoAndAttach(String courseId,
                                            String sectionTitle,
                                            MultipartFile file,
                                            String title,
                                            String uploadedBy) {

        Course course;

        // 🟦 Case 1: Course exists
        Optional<Course> courseOpt = courseRepository.findById(courseId);

        if (courseOpt.isPresent()) {
            course = courseOpt.get();
        }
        else {
            // 🟥 Case 2: Auto-create new course
            course = new Course();
            course.setTitle("Untitled Course");
            course.setDescription("Auto-created course during video upload");
            course.setInstructorEmail(uploadedBy);
            course.setInstructorName(uploadedBy);

            course = courseRepository.save(course); // save new course
            courseId = course.getId();

            System.out.println("🆕 Auto-created course with ID: " + courseId);
        }

        try {
            // Validate inputs
            if (file == null || file.isEmpty()) {
                throw new RuntimeException("Video file is required");
            }
            if (title == null || title.trim().isEmpty()) {
                throw new RuntimeException("Video title is required");
            }
            
            // Use final variable for section title (required for lambda)
            final String finalSectionTitle = (sectionTitle == null || sectionTitle.trim().isEmpty()) 
                ? "Default Section" 
                : sectionTitle;
            
            System.out.println("📹 Uploading video: " + title);
            System.out.println("   Course ID: " + courseId);
            System.out.println("   Section: " + finalSectionTitle);
            System.out.println("   File: " + file.getOriginalFilename() + " (" + (file.getSize() / 1024 / 1024) + " MB)");
            
            String folder = "skillforge/videos/" + courseId + "/" + finalSectionTitle;
            String videoUrl = cloudinaryService.uploadVideo(file, folder);
            
            System.out.println("✅ Video uploaded to Cloudinary: " + videoUrl);
            
            // Generate thumbnail from video (Cloudinary auto-generates thumbnails)
            String thumbnailUrl = cloudinaryService.generateVideoThumbnail(videoUrl);
            System.out.println("🖼️ Thumbnail generated: " + thumbnailUrl);

            VideoEntity video = new VideoEntity(title, videoUrl, courseId, uploadedBy);
            video.setSectionTitle(finalSectionTitle);
            video.setThumbnail(thumbnailUrl);
            video = videoRepository.save(video);
            
            System.out.println("💾 Video saved to database with ID: " + video.getId());

            // Add video to the section's lectures list
            if (course.getSections() != null) {
                Section targetSection = course.getSections().stream()
                    .filter(s -> finalSectionTitle.equals(s.getTitle()))
                    .findFirst()
                    .orElse(null);
                
                if (targetSection == null) {
                    // Create section if it doesn't exist
                    targetSection = new Section();
                    targetSection.setId(java.util.UUID.randomUUID().toString());
                    targetSection.setTitle(finalSectionTitle);
                    if (course.getSections() == null) {
                        course.setSections(new java.util.ArrayList<>());
                    }
                    course.getSections().add(targetSection);
                }
                
                // Add video to section's lectures
                if (targetSection.getLectures() == null) {
                    targetSection.setLectures(new java.util.ArrayList<>());
                }
                
                // Create lecture object from video
                java.util.Map<String, Object> lecture = new java.util.HashMap<>();
                lecture.put("id", video.getId());
                lecture.put("title", video.getTitle());
                lecture.put("videoId", video.getId());
                lecture.put("videoUrl", video.getVideoUrl());
                lecture.put("url", video.getVideoUrl());
                lecture.put("thumbnail", video.getThumbnail()); // Include thumbnail
                
                targetSection.getLectures().add(lecture);
                
                // Update video count
                if (course.getVideoCount() == null) {
                    course.setVideoCount(0);
                }
                course.setVideoCount(course.getVideoCount() + 1);
                
                courseRepository.save(course);
            }

            return video;

        } catch (RuntimeException e) {
            // Re-throw RuntimeException with original message
            System.err.println("❌ Video upload failed: " + e.getMessage());
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            // Wrap other exceptions
            System.err.println("❌ Unexpected error during video upload: " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Video upload failed: " + e.getMessage(), e);
        }
    }

    // 🟢 Get all videos for a course
    public List<VideoEntity> getCourseVideos(String courseId) {
        return videoRepository.findByCourseId(courseId);
    }
    
    // 🟢 Add lecture to a specific section
    public Course addLectureToSection(String courseId, String sectionId, 
                                      MultipartFile file, String title, String uploadedBy) {
        Optional<Course> courseOpt = courseRepository.findById(courseId);
        if (courseOpt.isEmpty()) {
            throw new RuntimeException("Course not found");
        }
        
        Course course = courseOpt.get();
        Section targetSection = null;
        
        if (course.getSections() != null) {
            targetSection = course.getSections().stream()
                .filter(s -> sectionId.equals(s.getId()))
                .findFirst()
                .orElse(null);
        }
        
        if (targetSection == null) {
            throw new RuntimeException("Section not found");
        }
        
        try {
            String folder = "skillforge/videos/" + courseId + "/" + targetSection.getTitle();
            String videoUrl = cloudinaryService.uploadVideo(file, folder);
            
            // Generate thumbnail from video
            String thumbnailUrl = cloudinaryService.generateVideoThumbnail(videoUrl);
            
            VideoEntity video = new VideoEntity(title, videoUrl, courseId, uploadedBy);
            video.setSectionTitle(targetSection.getTitle());
            video.setThumbnail(thumbnailUrl);
            video = videoRepository.save(video);
            
            // Add to section's lectures
            if (targetSection.getLectures() == null) {
                targetSection.setLectures(new java.util.ArrayList<>());
            }
            
            java.util.Map<String, Object> lecture = new java.util.HashMap<>();
            lecture.put("id", video.getId());
            lecture.put("title", video.getTitle());
            lecture.put("videoId", video.getId());
            lecture.put("videoUrl", video.getVideoUrl());
            lecture.put("url", video.getVideoUrl());
            
            targetSection.getLectures().add(lecture);
            
            // Update video count
            if (course.getVideoCount() == null) {
                course.setVideoCount(0);
            }
            course.setVideoCount(course.getVideoCount() + 1);
            
            return courseRepository.save(course);
            
        } catch (Exception e) {
            throw new RuntimeException("Lecture upload failed: " + e.getMessage());
        }
    }

    // 🟢 Update course thumbnail
    public Course updateCourseThumbnail(String courseId, MultipartFile file) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Thumbnail file is required");
        }

        String folder = "skillforge/courses/" + courseId + "/thumbnail";
        String thumbnailUrl = cloudinaryService.uploadImage(file, folder);
        course.setThumbnail(thumbnailUrl);
        return courseRepository.save(course);
    }

    // 🟢 Upload notes (PDF) and attach to course
    public Course uploadCourseNote(String courseId, MultipartFile file, String title) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Notes file is required");
        }

        String folder = "skillforge/courses/" + courseId + "/notes";
        String fileUrl = cloudinaryService.uploadFile(file, folder);

        // Create Note object and attach
        Note note = new Note();
        note.setId(java.util.UUID.randomUUID().toString());
        note.setTitle(title == null || title.trim().isEmpty() ? file.getOriginalFilename() : title);
        note.setUrl(fileUrl);

        if (course.getNotes() == null) {
            course.setNotes(new java.util.ArrayList<>());
        }
        course.getNotes().add(note);
        return courseRepository.save(course);
    }

    // 🟢 Add quiz to section
    public Course addQuizToSection(String courseId, String sectionId, Quiz quiz) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        Section section = course.getSections().stream()
                .filter(s -> sectionId.equals(s.getId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Section not found"));

        // Generate ID if not present
        if (quiz.getId() == null || quiz.getId().isEmpty()) {
            quiz.setId(java.util.UUID.randomUUID().toString());
        }

        // Initialize quizzes list if null
        if (section.getQuizzes() == null) {
            section.setQuizzes(new java.util.ArrayList<>());
        }

        section.getQuizzes().add(quiz);
        return courseRepository.save(course);
    }

    // 🟢 Update quiz in section
    public Course updateQuiz(String courseId, String sectionId, String quizId, Quiz updatedQuiz) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        Section section = course.getSections().stream()
                .filter(s -> sectionId.equals(s.getId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Section not found"));

        Quiz existingQuiz = section.getQuizzes().stream()
                .filter(q -> quizId.equals(q.getId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Quiz not found"));

        // Update quiz fields
        existingQuiz.setTitle(updatedQuiz.getTitle());
        existingQuiz.setDescription(updatedQuiz.getDescription());
        existingQuiz.setQuestions(updatedQuiz.getQuestions());
        existingQuiz.setPassingScore(updatedQuiz.getPassingScore());
        existingQuiz.setIsPublished(updatedQuiz.getIsPublished());

        return courseRepository.save(course);
    }

    // 🟢 Delete quiz from section
    public Course deleteQuizFromSection(String courseId, String sectionId, String quizId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        Section section = course.getSections().stream()
                .filter(s -> sectionId.equals(s.getId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Section not found"));

        if (section.getQuizzes() != null) {
            section.getQuizzes().removeIf(q -> quizId.equals(q.getId()));
        }

        return courseRepository.save(course);
    }
}
