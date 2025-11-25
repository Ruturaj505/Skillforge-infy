import apiClient from './http'
import type { Course, VideoEntity, Quiz } from '../types'

export const getInstructorCourses = async (): Promise<Course[]> => {
  const { data } = await apiClient.get<Course[]>('/api/instructor/courses')
  return data
}

export const createCourse = async (course: Partial<Course>) => {
  const { data } = await apiClient.post<Course>('/api/instructor/courses/create', course)
  return data
}

export const addSection = async (courseId: string, sectionTitle: string) => {
  const { data } = await apiClient.post<Course>(
    `/api/instructor/courses/${courseId}/add-section`,
    null,
    {
      params: { sectionTitle },
    }
  )
  return data
}

export const deleteSection = async (courseId: string, sectionId: string) => {
  const { data } = await apiClient.delete<Course>(
    `/api/instructor/courses/${courseId}/sections/${sectionId}`
  )
  return data
}

interface UploadVideoParams {
  courseId: string
  sectionTitle?: string
  file: File
  title: string
  uploadedBy: string
}

export const uploadVideo = async ({
  courseId,
  sectionTitle,
  file,
  title,
  uploadedBy,
}: UploadVideoParams): Promise<VideoEntity> => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('title', title)
  formData.append('uploadedBy', uploadedBy)
  if (sectionTitle) {
    formData.append('sectionTitle', sectionTitle)
  }

  const { data } = await apiClient.post<VideoEntity>(
    `/api/instructor/courses/${courseId}/upload-video`,
    formData,
    {
      headers: { 'Content-Type': 'multipart/form-data' },
    }
  )
  return data
}

export const uploadCourseThumbnail = async (courseId: string, file: File) => {
  const formData = new FormData()
  formData.append('file', file)

  const { data } = await apiClient.post<Course>(
    `/api/instructor/courses/${courseId}/thumbnail`,
    formData,
    { headers: { 'Content-Type': 'multipart/form-data' } }
  )
  return data
}

export const uploadCourseNote = async (courseId: string, file: File, title?: string) => {
  const formData = new FormData()
  formData.append('file', file)
  if (title) formData.append('title', title)

  const { data } = await apiClient.post<Course>(
    `/api/instructor/courses/${courseId}/notes`,
    formData,
    { headers: { 'Content-Type': 'multipart/form-data' } }
  )
  return data
}

// 🟢 Quiz API helpers
export const addQuiz = async (courseId: string, sectionId: string, quiz: Partial<Quiz>) => {
  const { data } = await apiClient.post<Course>(
    `/api/instructor/courses/${courseId}/sections/${sectionId}/quizzes`,
    quiz
  )
  return data
}

export const updateQuiz = async (
  courseId: string,
  sectionId: string,
  quizId: string,
  quiz: Partial<Quiz>
) => {
  const { data } = await apiClient.put<Course>(
    `/api/instructor/courses/${courseId}/sections/${sectionId}/quizzes/${quizId}`,
    quiz
  )
  return data
}

export const deleteQuiz = async (courseId: string, sectionId: string, quizId: string) => {
  const { data } = await apiClient.delete<Course>(
    `/api/instructor/courses/${courseId}/sections/${sectionId}/quizzes/${quizId}`
  )
  return data
}