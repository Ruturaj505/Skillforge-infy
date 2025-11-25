export type UserRole = 'STUDENT' | 'INSTRUCTOR' | 'ADMIN'

export interface UserProfile {
  id?: string
  name: string
  email: string
  role: UserRole
}

export interface Lecture {
  id: string
  title: string
  videoId?: string
  videoUrl?: string
  url?: string
  thumbnail?: string
}

export interface QuizQuestion {
  id?: string
  question: string
  options: string[]
  correctOptionIndex: number
  explanation?: string
}

export interface Quiz {
  id?: string
  title: string
  description?: string
  questions: QuizQuestion[]
  passingScore?: number
  isPublished?: boolean
}

export interface Section {
  id: string
  title: string
  lectures?: Lecture[]
  quizzes?: Quiz[]
}

export interface Course {
  id: string
  title: string
  description?: string
  instructorName?: string
  instructorEmail?: string
  category?: string
  level?: string
  language?: string
  price?: string
  thumbnail?: string
  status?: string
  studentsCount?: number
  videoCount?: number
  sections?: Section[]
  notes?: Note[]
}

export interface Enrollment {
  id: string
  studentEmail: string
  courseId: string
  courseTitle: string
  instructorName?: string
  thumbnail?: string
  progress: number
  status?: string
}

export interface VideoEntity {
  id: string
  title: string
  videoUrl: string
  courseId: string
  uploadedBy: string
  thumbnail?: string
  sectionTitle?: string
  duration?: number
}

export interface Note {
  id: string
  title: string
  url: string
}

export interface ApiError {
  message: string
}

export interface InstructorProfile {
  id?: string
  email: string
  name?: string
  bio?: string
  profileImageUrl?: string
  linkedin?: string
  github?: string
  specialization?: string
  experience?: string
  education?: string
  website?: string
  expertise?: string
  profileImage?: string
}

