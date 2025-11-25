import { useMemo, useState, useRef, useEffect } from 'react'
import { useParams } from 'react-router-dom'
import { useMutation, useQueries } from '@tanstack/react-query'
import { getCourseById, getCourseVideos, updateProgress, getMyCourses, submitQuiz as submitQuizApi } from '../../api/student'
import LoadingScreen from '../../components/common/LoadingScreen'
import VideoPlayer from '../../components/common/VideoPlayer'
import { useAuth } from '../../contexts/AuthContext'
import toast from 'react-hot-toast'
import type { Quiz } from '../../types'

const CoursePlayer = () => {
  const { courseId } = useParams<{ courseId: string }>()
  const { user } = useAuth()

  const [courseQuery, videoQuery] = useQueries({
    queries: [
      {
        queryKey: ['course', courseId],
        queryFn: () => getCourseById(courseId ?? ''),
        enabled: !!courseId,
      },
      {
        queryKey: ['videos', courseId],
        queryFn: () => getCourseVideos(courseId ?? ''),
        enabled: !!courseId,
      },
    ],
  })

  const [activeVideo, setActiveVideo] = useState<string | null>(null)

  const [selectedQuiz, setSelectedQuiz] = useState<Quiz | null>(null)
  const [quizAnswers, setQuizAnswers] = useState<{ [questionId: number]: number }>({})
  const [quizSubmitted, setQuizSubmitted] = useState(false)
  const [quizScore, setQuizScore] = useState(0)

  const videos = videoQuery.data ?? []
  const currentVideo = useMemo(
    () => videos.find((video) => video.id === (activeVideo ?? videos[0]?.id)),
    [videos, activeVideo]
  )

  const progressMutation = useMutation({
    mutationFn: (progress: number) =>
      updateProgress(user?.email ?? '', courseId ?? '', progress),
    onSuccess: () => toast.success('Progress updated'),
    onError: () => toast.error('Unable to update progress'),
  })

  const [currentProgress, setCurrentProgress] = useState<number>(0)
  const debounceRef = useRef<any>(null)

  // Fetch existing enrollment progress for this course
  useEffect(() => {
    let mounted = true
    if (!user?.email || !courseId) return
    getMyCourses(user.email).then((enrollments) => {
      if (!mounted) return
      const enrollment = enrollments.find((e) => e.courseId === courseId)
      if (enrollment) setCurrentProgress(Math.round(enrollment.progress ?? 0))
    })
    return () => {
      mounted = false
    }
  }, [user?.email, courseId])

  const onVideoProgress = (percent: number) => {
    // update UI immediately
    setCurrentProgress(percent)

    // debounce server updates to once per 2s
    if (debounceRef.current) clearTimeout(debounceRef.current)
    debounceRef.current = setTimeout(() => {
      progressMutation.mutate(percent)
    }, 2000)
  }

  const submitQuiz = async () => {
    if (!selectedQuiz || !user?.email) return

    try {
      const sectionId = courseQuery.data?.sections?.find((s) => s.quizzes?.some((q) => q.id === selectedQuiz.id))?.id ?? ''
      const answersPayload: Record<number, number> = {}
      Object.keys(quizAnswers).forEach((k) => {
        answersPayload[Number(k)] = quizAnswers[Number(k)]
      })

      const resp: any = await submitQuizApi(
        courseId ?? '',
        sectionId,
        selectedQuiz.id ?? '',
        user.email,
        answersPayload,
        undefined
      )

      setQuizScore(resp.score ?? 0)
      setQuizSubmitted(true)

      if (resp.passed) toast.success(`Quiz completed! Score: ${resp.score}% (Passed)`)
      else toast.error(`Quiz completed! Score: ${resp.score}% (Failed)`)
    } catch (err) {
      console.error(err)
      toast.error('Failed to submit quiz')
    }
  }

  const resetQuiz = () => {
    setSelectedQuiz(null)
    setQuizAnswers({})
    setQuizSubmitted(false)
    setQuizScore(0)
  }

  // Get all quizzes from current course sections
  const allQuizzes = useMemo(() => {
    const quizzes: (Quiz & { sectionTitle: string })[] = []
    courseQuery.data?.sections?.forEach((section) => {
      section.quizzes?.forEach((quiz) => {
        quizzes.push({ ...quiz, sectionTitle: section.title })
      })
    })
    return quizzes
  }, [courseQuery.data])

  if (courseQuery.isLoading || videoQuery.isLoading || !courseQuery.data) {
    return <LoadingScreen message="Preparing player..." />
  }

  return (
    <div className="space-y-6">
      <div className="glass-panel p-5">
        <p className="text-xs uppercase text-slate-500">Now learning</p>
        <h1 className="text-2xl font-semibold text-slate-900">{courseQuery.data.title}</h1>
      </div>

      {currentVideo ? (
        <VideoPlayer
          key={currentVideo.id}
          src={currentVideo.videoUrl}
          title={currentVideo.title}
          poster={currentVideo.thumbnail}
          onProgress={onVideoProgress}
        />
      ) : (
        <div className="glass-panel p-10 text-center text-slate-500">
          No videos have been uploaded yet.
        </div>
      )}

      <div className="grid lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 space-y-3">
          <h2 className="text-lg font-semibold text-slate-900">Lecture playlist</h2>
          <div className="space-y-2 max-h-[420px] overflow-y-auto pr-2">
            {videos.map((video) => (
              <button
                key={video.id}
                type="button"
                onClick={() => setActiveVideo(video.id)}
                className={`w-full text-left p-3 rounded-xl border ${
                  video.id === currentVideo?.id
                    ? 'border-brand-200 bg-brand-50'
                    : 'border-transparent bg-white'
                }`}
              >
                <p className="text-sm font-semibold text-slate-900">{video.title}</p>
                <p className="text-xs text-slate-500">{video.sectionTitle}</p>
              </button>
            ))}
          </div>

          {/* Quiz Section */}
          {allQuizzes.length > 0 && (
            <div className="pt-4 border-t">
              <h2 className="text-lg font-semibold text-slate-900 mb-3">Quizzes</h2>
              {!selectedQuiz ? (
                <div className="space-y-2 max-h-[420px] overflow-y-auto pr-2">
                  {allQuizzes.map((quiz) => (
                    <button
                      key={quiz.id}
                      type="button"
                      onClick={() => {
                        setSelectedQuiz(quiz)
                        setQuizAnswers({})
                        setQuizSubmitted(false)
                      }}
                      className="w-full text-left p-3 rounded-xl border border-amber-200 bg-amber-50 hover:bg-amber-100 transition"
                    >
                      <p className="text-sm font-semibold text-slate-900">{quiz.title}</p>
                      <p className="text-xs text-slate-500">
                        {quiz.sectionTitle} • {quiz.questions?.length ?? 0} questions
                      </p>
                    </button>
                  ))}
                </div>
              ) : (
                // Quiz taking interface
                <div className="border border-amber-200 rounded-xl p-4 bg-amber-50 space-y-4">
                  <div className="flex items-center justify-between">
                    <div>
                      <h3 className="text-lg font-semibold text-slate-900">{selectedQuiz.title}</h3>
                      <p className="text-xs text-slate-500">
                        Pass with {selectedQuiz.passingScore ?? 70}%
                      </p>
                    </div>
                    {!quizSubmitted && (
                      <button
                        type="button"
                        onClick={resetQuiz}
                        className="text-xs text-slate-600 hover:text-slate-900"
                      >
                        ✕
                      </button>
                    )}
                  </div>

                  {!quizSubmitted ? (
                    <div className="space-y-4 max-h-[400px] overflow-y-auto">
                      {selectedQuiz.questions?.map((question, qIdx) => (
                        <div
                          key={qIdx}
                          className="bg-white rounded-lg p-3 border border-amber-100 space-y-2"
                        >
                          <p className="text-sm font-semibold text-slate-900">
                            {qIdx + 1}. {question.question}
                          </p>
                          <div className="space-y-2">
                            {question.options?.map((option, oIdx) => (
                              <label
                                key={oIdx}
                                className="flex items-center gap-2 cursor-pointer hover:bg-amber-50 p-1 rounded"
                              >
                                <input
                                  type="radio"
                                  name={`question-${qIdx}`}
                                  checked={quizAnswers[qIdx] === oIdx}
                                  onChange={() =>
                                    setQuizAnswers((prev) => ({ ...prev, [qIdx]: oIdx }))
                                  }
                                  className="w-4 h-4"
                                />
                                <span className="text-sm text-slate-700">{option}</span>
                              </label>
                            ))}
                          </div>
                        </div>
                      ))}
                    </div>
                  ) : (
                    // Results display
                    <div className="space-y-4">
                      <div className="bg-white rounded-lg p-4 text-center space-y-2">
                        <p className="text-3xl font-bold text-slate-900">{quizScore}%</p>
                        <p className={`text-lg font-semibold ${
                          quizScore >= (selectedQuiz.passingScore ?? 70)
                            ? 'text-green-600'
                            : 'text-red-600'
                        }`}>
                          {quizScore >= (selectedQuiz.passingScore ?? 70) ? '✓ Passed' : '✗ Failed'}
                        </p>
                      </div>

                      {/* Show answers review */}
                      <div className="space-y-3 max-h-[300px] overflow-y-auto">
                        {selectedQuiz.questions?.map((question, qIdx) => {
                          const isCorrect = quizAnswers[qIdx] === question.correctOptionIndex
                          return (
                            <div
                              key={qIdx}
                              className={`rounded-lg p-3 border ${
                                isCorrect
                                  ? 'border-green-200 bg-green-50'
                                  : 'border-red-200 bg-red-50'
                              }`}
                            >
                              <p className="text-sm font-semibold text-slate-900">
                                {qIdx + 1}. {question.question}
                              </p>
                              <p className="text-xs text-slate-600 mt-1">
                                <span className={isCorrect ? 'text-green-700' : 'text-red-700'}>
                                  Your answer:{' '}
                                </span>
                                {question.options?.[quizAnswers[qIdx]]}
                              </p>
                              {!isCorrect && (
                                <p className="text-xs text-green-700 mt-1">
                                  Correct answer: {question.options?.[question.correctOptionIndex]}
                                </p>
                              )}
                              {question.explanation && (
                                <p className="text-xs text-slate-600 mt-2 italic">
                                  {question.explanation}
                                </p>
                              )}
                            </div>
                          )
                        })}
                      </div>
                    </div>
                  )}

                  {!quizSubmitted && (
                    <button
                      type="button"
                      onClick={submitQuiz}
                      disabled={
                        Object.keys(quizAnswers).length !== (selectedQuiz.questions?.length ?? 0)
                      }
                      className="w-full py-2 rounded-lg bg-amber-600 text-white font-semibold hover:bg-amber-700 disabled:opacity-70 transition"
                    >
                      Submit quiz
                    </button>
                  )}

                  {quizSubmitted && (
                    <button
                      type="button"
                      onClick={resetQuiz}
                      className="w-full py-2 rounded-lg bg-slate-600 text-white font-semibold hover:bg-slate-700 transition"
                    >
                      Back to quizzes
                    </button>
                  )}
                </div>
              )}
            </div>
          )}
        </div>
        <div className="space-y-3 glass-panel p-5">
          <h3 className="text-lg font-semibold text-slate-900">Progress tracking</h3>
          <p className="text-sm text-slate-500">
            SkillForge stores granular percentage values in MongoDB enrollment documents.
          </p>
          <input
            type="range"
            min={0}
            max={100}
            value={currentProgress}
            onChange={(event) => {
              const v = Number(event.target.value)
              setCurrentProgress(v)
              if (debounceRef.current) clearTimeout(debounceRef.current)
              debounceRef.current = setTimeout(() => progressMutation.mutate(v), 800)
            }}
            className="w-full accent-brand-500"
          />
          <p className="text-xs text-slate-500">
            Tracking progress automatically ({progressMutation.isPending ? 'Saving...' : 'Auto-saved'}).
          </p>
        </div>
      </div>
    </div>
  )
}

export default CoursePlayer

