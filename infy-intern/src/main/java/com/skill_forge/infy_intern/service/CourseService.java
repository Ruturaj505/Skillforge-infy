package com.skill_forge.infy_intern.service;

import com.skill_forge.infy_intern.model.Course;
import com.skill_forge.infy_intern.model.Note;
import com.skill_forge.infy_intern.model.Quiz;
import com.skill_forge.infy_intern.model.Section;
import com.skill_forge.infy_intern.model.VideoEntity;
import com.skill_forge.infy_intern.repository.CourseRepository;
import com.skill_forge.infy_intern.repository.VideoRepository;
import com.skill_forge.infy_intern.model.QuizResponse;
import com.skill_forge.infy_intern.repository.QuizResponseRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final VideoRepository videoRepository;
    private final CloudinaryService cloudinaryService;
    private final QuizResponseRepository quizResponseRepository;
    private final String perplexityApiKey;
    private final String perplexityApiUrl;


    // Additional constructor used by Spring to inject QuizResponseRepository and API config
    public CourseService(CourseRepository courseRepository,
                         VideoRepository videoRepository,
                         CloudinaryService cloudinaryService,
                         QuizResponseRepository quizResponseRepository,
                         @Value("${perplexity.api.key:}") String perplexityApiKey,
                         @Value("${perplexity.api.url:https://api.perplexity.ai/v1/generate}") String perplexityApiUrl) {
        this.courseRepository = courseRepository;
        this.videoRepository = videoRepository;
        this.cloudinaryService = cloudinaryService;
        this.quizResponseRepository = quizResponseRepository;
        this.perplexityApiKey = perplexityApiKey;
        this.perplexityApiUrl = perplexityApiUrl;
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

    // 🟢 Generate a quiz using external AI (Perplexity) based on a topic provided by instructor
    public Course generateQuizFromTopic(String courseId, String sectionId, String topic, int numQuestions, Integer timeLimitSeconds) {
        // Check if API key is configured; if not, fall back to mock generator
        if (perplexityApiKey == null || perplexityApiKey.isEmpty() || perplexityApiKey.equals("")) {
            System.out.println("⚠️  Perplexity API key not configured. Using mock quiz generator for testing.");
            return generateMockQuiz(courseId, sectionId, topic, numQuestions, timeLimitSeconds);
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        Section section = course.getSections().stream()
                .filter(s -> sectionId.equals(s.getId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Section not found"));

        try {
            // Build prompt for AI
            String prompt = String.format(
                    "Generate %d multiple-choice questions (4 options each) about the topic: %s. " +
                    "Return a JSON object with key 'questions' which is an array of objects {question, options, correctOptionIndex, explanation}. " +
                    "Do not include any extra text outside JSON.",
                    numQuestions, topic
            );

            ObjectMapper mapper = new ObjectMapper();
            RestTemplate rest = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + perplexityApiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            // request body - many external APIs accept prompt, adapt as necessary
            java.util.Map<String,Object> payload = new java.util.HashMap<>();
            payload.put("prompt", prompt);
            payload.put("max_questions", numQuestions);

            HttpEntity<java.util.Map<String,Object>> entity = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response = rest.postForEntity(perplexityApiUrl, entity, String.class);

            String body = response.getBody();
            JsonNode root = mapper.readTree(body);

            JsonNode questionsNode = root.path("questions");
            if (!questionsNode.isArray()) {
                // try if root is array
                if (root.isArray()) {
                    questionsNode = root;
                } else {
                    throw new RuntimeException("AI response did not include a 'questions' array. Response: " + body);
                }
            }

            Quiz quiz = new Quiz();
            quiz.setId(java.util.UUID.randomUUID().toString());
            quiz.setTitle("Quiz: " + topic);
            quiz.setDescription("Auto-generated quiz on: " + topic);
            quiz.setPassingScore(70);
            quiz.setIsPublished(true);
            quiz.setGeneratedByAI(true);
            quiz.setTimeLimitSeconds(timeLimitSeconds);

            java.util.List<com.skill_forge.infy_intern.model.QuizQuestion> qlist = new java.util.ArrayList<>();
            for (JsonNode qn : questionsNode) {
                com.skill_forge.infy_intern.model.QuizQuestion qq = new com.skill_forge.infy_intern.model.QuizQuestion();
                qq.setQuestion(qn.path("question").asText(""));
                java.util.List<String> opts = new java.util.ArrayList<>();
                if (qn.has("options") && qn.get("options").isArray()) {
                    for (JsonNode on : qn.get("options")) {
                        opts.add(on.asText());
                    }
                }
                qq.setOptions(opts);
                qq.setCorrectOptionIndex(qn.path("correctOptionIndex").asInt(0));
                qq.setExplanation(qn.path("explanation").asText(null));
                qq.setId(java.util.UUID.randomUUID().toString());
                qlist.add(qq);
            }

            quiz.setQuestions(qlist);

            // Attach quiz to section and save
            return addQuizToSection(courseId, sectionId, quiz);

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate quiz from AI: " + e.getMessage(), e);
        }
    }

    // 🟢 Generate a mock quiz for testing when API is not configured
    private Course generateMockQuiz(String courseId, String sectionId, String topic, int numQuestions, Integer timeLimitSeconds) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        Section section = course.getSections().stream()
                .filter(s -> sectionId.equals(s.getId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Section not found"));

        Quiz quiz = new Quiz();
        quiz.setId(java.util.UUID.randomUUID().toString());
        quiz.setTitle("Quiz: " + topic);
        quiz.setDescription("Auto-generated quiz on: " + topic + " (Mock - API not configured)");
        quiz.setPassingScore(70);
        quiz.setIsPublished(true);
        quiz.setGeneratedByAI(true);
        quiz.setTimeLimitSeconds(timeLimitSeconds);

        java.util.List<com.skill_forge.infy_intern.model.QuizQuestion> qlist = new java.util.ArrayList<>();

        // Generate mock questions with unique options per question
        String[][] questionTemplates = {
            {
                "What is the primary definition of " + topic + "?",
                "A fundamental concept in " + topic,
                "An advanced technique in " + topic,
                "A historical reference to " + topic,
                "A modern interpretation of " + topic
            },
            {
                "Which of the following is a key characteristic of " + topic + "?",
                "It focuses on efficiency and speed",
                "It emphasizes scalability and flexibility",
                "It prioritizes security and reliability",
                "It combines all of the above"
            },
            {
                "How is " + topic + " typically implemented?",
                "Through a top-down approach",
                "Using a bottom-up methodology",
                "By iterative development cycles",
                "By waterfall project management"
            },
            {
                "What is one major advantage of " + topic + "?",
                "Reduces complexity significantly",
                "Improves performance and throughput",
                "Enhances user experience",
                "Lowers overall costs"
            },
            {
                "Which field or industry benefits most from " + topic + "?",
                "Software development",
                "Data science and analytics",
                "Cloud computing",
                "All fields that require structured solutions"
            }
        };

        for (int i = 0; i < Math.min(numQuestions, questionTemplates.length); i++) {
            String[] template = questionTemplates[i];
            
            com.skill_forge.infy_intern.model.QuizQuestion qq = new com.skill_forge.infy_intern.model.QuizQuestion();
            qq.setQuestion(template[0]);
            
            // Create options from array indices 1-4
            java.util.List<String> options = new java.util.ArrayList<>();
            options.add(template[1]);
            options.add(template[2]);
            options.add(template[3]);
            options.add(template[4]);
            
            qq.setOptions(options);
            qq.setCorrectOptionIndex((i + 1) % 4); // Rotate correct answer position
            qq.setExplanation("This is a sample question generated without API access. To enable real AI-generated questions with " + topic + ", configure your Perplexity API key in the server environment.");
            qq.setId(java.util.UUID.randomUUID().toString());
            qlist.add(qq);
        }

        quiz.setQuestions(qlist);

        // Attach quiz to section and save
        return addQuizToSection(courseId, sectionId, quiz);
    }

    // 🟢 Grade a student quiz submission, store QuizResponse
    public com.skill_forge.infy_intern.model.QuizResponse gradeQuizSubmission(String courseId, String sectionId, String quizId, String studentEmail, java.util.Map<Integer,Integer> answers, Integer durationSeconds) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        Section section = course.getSections().stream()
                .filter(s -> sectionId.equals(s.getId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Section not found"));

        Quiz quiz = section.getQuizzes().stream()
                .filter(q -> quizId.equals(q.getId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Quiz not found"));

        int total = quiz.getQuestions() == null ? 0 : quiz.getQuestions().size();
        int correct = 0;
        for (int i = 0; i < total; i++) {
            Integer selected = answers.get(i);
            Integer correctIdx = quiz.getQuestions().get(i).getCorrectOptionIndex();
            if (selected != null && selected.equals(correctIdx)) correct++;
        }

        int score = total == 0 ? 0 : (int)Math.round((correct / (double) total) * 100);
        boolean passed = score >= (quiz.getPassingScore() == null ? 70 : quiz.getPassingScore());

        com.skill_forge.infy_intern.model.QuizResponse resp = new com.skill_forge.infy_intern.model.QuizResponse();
        resp.setId(java.util.UUID.randomUUID().toString());
        resp.setStudentEmail(studentEmail);
        resp.setCourseId(courseId);
        resp.setSectionId(sectionId);
        resp.setQuizId(quizId);
        resp.setAnswers(answers);
        resp.setScore(score);
        resp.setPassed(passed);
        resp.setDurationSeconds(durationSeconds);
        resp.setSubmittedAt(java.time.Instant.now());

        if (quizResponseRepository != null) {
            quizResponseRepository.save(resp);
        }

        return resp;
    }
}
