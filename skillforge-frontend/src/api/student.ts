import apiClient from './http'
import type { Course, Enrollment, VideoEntity } from '../types'

export const browseCourses = async (): Promise<Course[]> => {
  const { data } = await apiClient.get<Course[]>('/api/student/browse')
  return data
}

export const enrollInCourse = async (email: string, courseId: string) => {
  const { data } = await apiClient.post(`/api/student/enroll/${courseId}`, null, {
    params: { email },
  })
  return data
}

export const getMyCourses = async (email: string): Promise<Enrollment[]> => {
  const { data } = await apiClient.get<Enrollment[]>('/api/student/my-courses', {
    params: { email },
  })
  return data
}

export const getCourseById = async (courseId: string): Promise<Course> => {
  const { data } = await apiClient.get<Course>(`/api/student/course/${courseId}`)
  return data
}

export const getCourseVideos = async (courseId: string): Promise<VideoEntity[]> => {
  const { data } = await apiClient.get<VideoEntity[]>(
    `/api/student/course/${courseId}/videos`
  )
  return data
}

export const updateProgress = async (email: string, courseId: string, progress: number) => {
  const { data } = await apiClient.post('/api/student/progress', null, {
    params: { email, courseId, progress },
  })
  return data
}

export const submitQuiz = async (
  courseId: string,
  sectionId: string,
  quizId: string,
  studentEmail: string,
  answers: Record<number, number>,
  durationSeconds?: number
) => {
  const payload: any = { studentEmail, answers }
  if (durationSeconds !== undefined) payload.durationSeconds = durationSeconds
  const { data } = await apiClient.post(`/api/student/course/${courseId}/sections/${sectionId}/quizzes/${quizId}/submit`, payload)
  return data
}

