import { useEffect, useMemo, useState } from 'react'
import { useForm } from 'react-hook-form'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import { addQuiz, addSection, createCourse, deleteQuiz, deleteSection, getInstructorCourses, updateQuiz, uploadCourseThumbnail, uploadCourseNote } from '../../api/instructor'
import LoadingScreen from '../../components/common/LoadingScreen'
import { useAuth } from '../../contexts/AuthContext'
import type { Quiz, QuizQuestion } from '../../types'

interface CourseFormValues {
  title: string
  description: string
  category: string
  level: string
}

const CurriculumBuilder = () => {
  const { user } = useAuth()
  const queryClient = useQueryClient()
  const [selectedCourseId, setSelectedCourseId] = useState<string>()
  const [newSection, setNewSection] = useState('')
  const [expandedSectionId, setExpandedSectionId] = useState<string | null>(null)
  const [quizForm, setQuizForm] = useState<{
    sectionId?: string
    quiz: Partial<Quiz>
  }>({
    quiz: {
      title: '',
      description: '',
      questions: [],
      passingScore: 70,
      isPublished: false,
    },
  })

  const { register, handleSubmit, reset } = useForm<CourseFormValues>({
    defaultValues: {
      title: '',
      description: '',
      category: 'Technology',
      level: 'Beginner',
    },
  })

  const { data: courses, isLoading } = useQuery({
    queryKey: ['instructor-courses'],
    queryFn: getInstructorCourses,
  })

  useEffect(() => {
    if (!selectedCourseId && courses?.length) {
      setSelectedCourseId(courses[0].id)
    }
  }, [courses, selectedCourseId])

  const selectedCourse = useMemo(
    () => courses?.find((course) => course.id === selectedCourseId),
    [courses, selectedCourseId]
  )

  const createCourseMutation = useMutation({
    mutationFn: (values: CourseFormValues) =>
      createCourse({
        ...values,
        instructorEmail: user?.email,
        instructorName: user?.name,
        language: 'English',
        status: 'draft',
      }),
    onSuccess: (created) => {
      toast.success('Course created')
      reset()
      queryClient.invalidateQueries({ queryKey: ['instructor-courses'] })
      setSelectedCourseId(created.id)
    },
    onError: () => toast.error('Unable to create course'),
  })

  const addSectionMutation = useMutation({
    mutationFn: () => addSection(selectedCourseId ?? '', newSection),
    onSuccess: () => {
      toast.success('Section added')
      setNewSection('')
      queryClient.invalidateQueries({ queryKey: ['instructor-courses'] })
    },
    onError: () => toast.error('Unable to add section'),
  })

  const deleteSectionMutation = useMutation({
    mutationFn: (sectionId: string) => deleteSection(selectedCourseId ?? '', sectionId),
    onSuccess: () => {
      toast.success('Section removed')
      queryClient.invalidateQueries({ queryKey: ['instructor-courses'] })
    },
  })

  // Thumbnail upload
  const [thumbFile, setThumbFile] = useState<File | null>(null)
  const uploadThumbMutation = useMutation({
    mutationFn: () => uploadCourseThumbnail(selectedCourseId ?? '', thumbFile as File),
    onSuccess: () => {
      toast.success('Course thumbnail uploaded')
      setThumbFile(null)
      queryClient.invalidateQueries({ queryKey: ['instructor-courses'] })
    },
    onError: () => toast.error('Unable to upload thumbnail'),
  })

  // Notes (PDF) upload
  const [noteFile, setNoteFile] = useState<File | null>(null)
  const [noteTitle, setNoteTitle] = useState<string>('')
  const uploadNoteMutation = useMutation({
    mutationFn: () => uploadCourseNote(selectedCourseId ?? '', noteFile as File, noteTitle),
    onSuccess: () => {
      toast.success('Notes uploaded and attached to course')
      setNoteFile(null)
      setNoteTitle('')
      queryClient.invalidateQueries({ queryKey: ['instructor-courses'] })
    },
    onError: () => toast.error('Unable to upload notes'),
  })

  // Quiz management
  const addQuizMutation = useMutation({
    mutationFn: () =>
      addQuiz(selectedCourseId ?? '', quizForm.sectionId ?? '', {
        ...quizForm.quiz,
        questions: quizForm.quiz.questions || [],
      }),
    onSuccess: () => {
      toast.success('Quiz created')
      setQuizForm({
        sectionId: undefined,
        quiz: {
          title: '',
          description: '',
          questions: [],
          passingScore: 70,
          isPublished: false,
        },
      })
      queryClient.invalidateQueries({ queryKey: ['instructor-courses'] })
    },
    onError: () => toast.error('Unable to create quiz'),
  })

  const deleteQuizMutation = useMutation({
    mutationFn: (quizId: string) =>
      deleteQuiz(selectedCourseId ?? '', quizForm.sectionId ?? '', quizId),
    onSuccess: () => {
      toast.success('Quiz deleted')
      queryClient.invalidateQueries({ queryKey: ['instructor-courses'] })
    },
    onError: () => toast.error('Unable to delete quiz'),
  })

  // Helper functions for quiz management
  const addQuestion = () => {
    setQuizForm((prev) => ({
      ...prev,
      quiz: {
        ...prev.quiz,
        questions: [
          ...(prev.quiz.questions || []),
          {
            question: '',
            options: ['', '', '', ''],
            correctOptionIndex: 0,
            explanation: '',
          } as QuizQuestion,
        ],
      },
    }))
  }

  const removeQuestion = (index: number) => {
    setQuizForm((prev) => ({
      ...prev,
      quiz: {
        ...prev.quiz,
        questions: prev.quiz.questions?.filter((_, i) => i !== index) || [],
      },
    }))
  }

  const updateQuestion = (index: number, field: string, value: any) => {
    setQuizForm((prev) => {
      const questions = [...(prev.quiz.questions || [])]
      questions[index] = { ...questions[index], [field]: value }
      return { ...prev, quiz: { ...prev.quiz, questions } }
    })
  }

  const updateOption = (qIndex: number, oIndex: number, value: string) => {
    setQuizForm((prev) => {
      const questions = [...(prev.quiz.questions || [])]
      const options = [...(questions[qIndex]?.options || [])]
      options[oIndex] = value
      questions[qIndex] = { ...questions[qIndex], options }
      return { ...prev, quiz: { ...prev.quiz, questions } }
    })
  }

  if (isLoading) {
    return <LoadingScreen message="Loading courses..." />
  }

  return (
    <div className="grid lg:grid-cols-2 gap-8">
      <div className="glass-panel p-6 space-y-4">
        <h2 className="text-xl font-semibold text-slate-900">Create a course</h2>
        <form
          className="space-y-4"
          onSubmit={handleSubmit((values) => createCourseMutation.mutate(values))}
        >
          <div>
            <label className="text-sm font-medium text-slate-600">Title</label>
            <input
              {...register('title', { required: true })}
              className="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2"
            />
          </div>
          <div>
            <label className="text-sm font-medium text-slate-600">Description</label>
            <textarea
              {...register('description', { required: true })}
              className="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2"
              rows={3}
            />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-sm font-medium text-slate-600">Category</label>
              <input
                {...register('category')}
                className="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2"
              />
            </div>
            <div>
              <label className="text-sm font-medium text-slate-600">Level</label>
              <select
                {...register('level')}
                className="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2"
              >
                <option>Beginner</option>
                <option>Intermediate</option>
                <option>Advanced</option>
              </select>
            </div>
          </div>
          <button
            type="submit"
            disabled={createCourseMutation.isPending}
            className="w-full brand-gradient text-white font-semibold py-2 rounded-lg disabled:opacity-70"
          >
            {createCourseMutation.isPending ? 'Creating...' : 'Save draft'}
          </button>
        </form>
      </div>
      <div className="glass-panel p-6 space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-xl font-semibold text-slate-900">Curriculum</h2>
          <select
            value={selectedCourseId}
            onChange={(event) => setSelectedCourseId(event.target.value)}
            className="border border-slate-200 rounded-lg px-3 py-2 text-sm"
          >
            {courses?.map((course) => (
              <option key={course.id} value={course.id}>
                {course.title}
              </option>
            ))}
          </select>
        </div>
        {selectedCourse ? (
          <div className="space-y-4">
            <div>
              <label className="text-sm font-medium text-slate-600">Upload course notes (PDF)</label>
              <div className="grid md:grid-cols-3 gap-2 mt-2">
                <input
                  type="text"
                  placeholder="Title (optional)"
                  value={noteTitle}
                  onChange={(e) => setNoteTitle(e.target.value)}
                  className="rounded-lg border border-slate-200 px-3 py-2"
                />
                <input
                  type="file"
                  accept="application/pdf"
                  onChange={(e) => setNoteFile(e.target.files?.[0] ?? null)}
                  className="rounded-lg border border-slate-200 px-3 py-2 bg-white"
                />
                <button
                  type="button"
                  disabled={!noteFile || !selectedCourseId || uploadNoteMutation.isPending}
                  onClick={() => uploadNoteMutation.mutate()}
                  className="px-4 py-2 rounded-lg bg-brand-500 text-white text-sm font-semibold disabled:opacity-70"
                >
                  {uploadNoteMutation.isPending ? 'Uploading...' : 'Upload notes'}
                </button>
              </div>
            </div>
            <div>
              <label className="text-sm font-medium text-slate-600">Course thumbnail</label>
              <div className="flex gap-2 items-center mt-2">
                <input
                  type="file"
                  accept="image/*"
                  onChange={(e) => setThumbFile(e.target.files?.[0] ?? null)}
                  className="rounded-lg border border-slate-200 px-3 py-2 bg-white"
                />
                <button
                  type="button"
                  disabled={!thumbFile || !selectedCourseId || uploadThumbMutation.isPending}
                  onClick={() => uploadThumbMutation.mutate()}
                  className="px-4 py-2 rounded-lg bg-brand-500 text-white text-sm font-semibold disabled:opacity-70"
                >
                  {uploadThumbMutation.isPending ? 'Uploading...' : 'Upload thumbnail'}
                </button>
              </div>
            </div>
            <div className="flex gap-2">
              <input
                value={newSection}
                onChange={(event) => setNewSection(event.target.value)}
                placeholder="Section title"
                className="flex-1 rounded-lg border border-slate-200 px-3 py-2"
              />
              <button
                type="button"
                disabled={!newSection || !selectedCourseId}
                onClick={() => addSectionMutation.mutate()}
                className="px-4 py-2 rounded-lg bg-brand-500 text-white text-sm font-semibold disabled:opacity-70"
              >
                Add
              </button>
            </div>
            <div className="space-y-3 max-h-[360px] overflow-y-auto pr-2">
              {selectedCourse.sections?.map((section) => (
                <div key={section.id} className="border border-slate-200 rounded-lg p-3">
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-sm font-semibold text-slate-900">{section.title}</p>
                      <p className="text-xs text-slate-500">
                        {section.lectures?.length ?? 0} lectures • {section.quizzes?.length ?? 0} quizzes
                      </p>
                    </div>
                    <div className="flex gap-1">
                      <button
                        type="button"
                        onClick={() =>
                          setExpandedSectionId(
                            expandedSectionId === section.id ? null : section.id
                          )
                        }
                        className="text-xs text-brand-500 font-semibold"
                      >
                        {expandedSectionId === section.id ? '▼ Collapse' : '▶ Expand'}
                      </button>
                      <button
                        type="button"
                        onClick={() => deleteSectionMutation.mutate(section.id)}
                        className="text-xs text-red-500"
                      >
                        Delete
                      </button>
                    </div>
                  </div>

                  {expandedSectionId === section.id && (
                    <div className="mt-4 pt-4 border-t border-slate-200 space-y-3">
                      {/* Quiz list */}
                      {section.quizzes && section.quizzes.length > 0 && (
                        <div className="space-y-2">
                          <p className="text-xs font-semibold text-slate-600">Quizzes:</p>
                          {section.quizzes.map((quiz) => (
                            <div
                              key={quiz.id}
                              className="text-xs bg-slate-50 p-2 rounded flex justify-between items-center"
                            >
                              <span className="text-slate-700">
                                {quiz.title} ({quiz.questions?.length ?? 0} q)
                              </span>
                              <button
                                type="button"
                                onClick={() => deleteQuizMutation.mutate(quiz.id || '')}
                                className="text-red-500 hover:text-red-700"
                              >
                                Delete
                              </button>
                            </div>
                          ))}
                        </div>
                      )}

                      {/* Quiz form (when creating new quiz for this section) */}
                      {quizForm.sectionId === section.id && (
                        <div className="bg-blue-50 p-3 rounded space-y-2">
                          <p className="text-xs font-semibold text-blue-900">Add new quiz</p>
                          <input
                            type="text"
                            placeholder="Quiz title"
                            value={quizForm.quiz.title || ''}
                            onChange={(e) =>
                              setQuizForm((prev) => ({
                                ...prev,
                                quiz: { ...prev.quiz, title: e.target.value },
                              }))
                            }
                            className="w-full text-xs rounded border border-blue-200 px-2 py-1"
                          />
                          <textarea
                            placeholder="Quiz description (optional)"
                            value={quizForm.quiz.description || ''}
                            onChange={(e) =>
                              setQuizForm((prev) => ({
                                ...prev,
                                quiz: { ...prev.quiz, description: e.target.value },
                              }))
                            }
                            className="w-full text-xs rounded border border-blue-200 px-2 py-1"
                            rows={2}
                          />
                          <div className="grid grid-cols-2 gap-2">
                            <input
                              type="number"
                              placeholder="Passing score (%)"
                              value={quizForm.quiz.passingScore || 70}
                              onChange={(e) =>
                                setQuizForm((prev) => ({
                                  ...prev,
                                  quiz: {
                                    ...prev.quiz,
                                    passingScore: parseInt(e.target.value),
                                  },
                                }))
                              }
                              className="text-xs rounded border border-blue-200 px-2 py-1"
                              min="0"
                              max="100"
                            />
                            <label className="text-xs flex items-center gap-1">
                              <input
                                type="checkbox"
                                checked={quizForm.quiz.isPublished || false}
                                onChange={(e) =>
                                  setQuizForm((prev) => ({
                                    ...prev,
                                    quiz: { ...prev.quiz, isPublished: e.target.checked },
                                  }))
                                }
                              />
                              Publish now
                            </label>
                          </div>

                          {/* Questions */}
                          <div className="space-y-2 max-h-[200px] overflow-y-auto">
                            {quizForm.quiz.questions?.map((q, qIdx) => (
                              <div
                                key={qIdx}
                                className="bg-white border border-blue-200 rounded p-2 space-y-1"
                              >
                                <div className="flex justify-between items-start">
                                  <input
                                    type="text"
                                    placeholder="Question"
                                    value={q.question}
                                    onChange={(e) =>
                                      updateQuestion(qIdx, 'question', e.target.value)
                                    }
                                    className="flex-1 text-xs rounded border border-blue-100 px-2 py-1"
                                  />
                                  <button
                                    type="button"
                                    onClick={() => removeQuestion(qIdx)}
                                    className="text-xs text-red-500 ml-1"
                                  >
                                    ✕
                                  </button>
                                </div>

                                {/* Options */}
                                <div className="space-y-1">
                                  {q.options?.map((opt, oIdx) => (
                                    <div key={oIdx} className="flex gap-1 items-center">
                                      <input
                                        type="radio"
                                        name={`correct-${qIdx}`}
                                        checked={q.correctOptionIndex === oIdx}
                                        onChange={() =>
                                          updateQuestion(qIdx, 'correctOptionIndex', oIdx)
                                        }
                                        className="w-3 h-3"
                                      />
                                      <input
                                        type="text"
                                        placeholder={`Option ${oIdx + 1}`}
                                        value={opt}
                                        onChange={(e) =>
                                          updateOption(qIdx, oIdx, e.target.value)
                                        }
                                        className="flex-1 text-xs rounded border border-blue-100 px-2 py-1"
                                      />
                                    </div>
                                  ))}
                                </div>

                                <textarea
                                  placeholder="Explanation (optional)"
                                  value={q.explanation || ''}
                                  onChange={(e) =>
                                    updateQuestion(qIdx, 'explanation', e.target.value)
                                  }
                                  className="w-full text-xs rounded border border-blue-100 px-2 py-1"
                                  rows={1}
                                />
                              </div>
                            ))}
                          </div>

                          {/* Add question button */}
                          <button
                            type="button"
                            onClick={addQuestion}
                            className="w-full text-xs py-1 rounded bg-blue-100 text-blue-700 font-semibold hover:bg-blue-200"
                          >
                            + Add question
                          </button>

                          {/* Save/Cancel */}
                          <div className="flex gap-2">
                            <button
                              type="button"
                              onClick={() =>
                                addQuizMutation.mutate()
                              }
                              disabled={
                                !quizForm.quiz.title ||
                                !quizForm.quiz.questions?.length ||
                                addQuizMutation.isPending
                              }
                              className="flex-1 text-xs rounded bg-blue-500 text-white font-semibold py-1 disabled:opacity-70"
                            >
                              {addQuizMutation.isPending ? 'Saving...' : 'Save quiz'}
                            </button>
                            <button
                              type="button"
                              onClick={() =>
                                setQuizForm({
                                  sectionId: undefined,
                                  quiz: {
                                    title: '',
                                    description: '',
                                    questions: [],
                                    passingScore: 70,
                                    isPublished: false,
                                  },
                                })
                              }
                              className="flex-1 text-xs rounded bg-slate-200 text-slate-700 font-semibold py-1"
                            >
                              Cancel
                            </button>
                          </div>
                        </div>
                      )}

                      {/* Add quiz button (when not editing) */}
                      {quizForm.sectionId !== section.id && (
                        <button
                          type="button"
                          onClick={() =>
                            setQuizForm({
                              sectionId: section.id,
                              quiz: {
                                title: '',
                                description: '',
                                questions: [],
                                passingScore: 70,
                                isPublished: false,
                              },
                            })
                          }
                          className="w-full text-xs py-1 rounded bg-brand-100 text-brand-700 font-semibold hover:bg-brand-200"
                        >
                          + Add quiz to section
                        </button>
                      )}
                    </div>
                  )}
                </div>
              )) ?? <p className="text-sm text-slate-500">No sections yet.</p>}
            </div>
          </div>
        ) : (
          <p className="text-sm text-slate-500">Select or create a course to manage curriculum.</p>
        )}
      </div>
    </div>
  )
}

export default CurriculumBuilder

