import { useQuery } from '@tanstack/react-query'
import { getMyCourses, browseCourses } from '../../api/student'
import { useAuth } from '../../contexts/AuthContext'
import LoadingScreen from '../../components/common/LoadingScreen'
import StatCard from '../../components/common/StatCard'
import CourseCard from '../../components/common/CourseCard'

const StudentDashboard = () => {
  const { user } = useAuth()

  const {
    data: enrollments,
    isLoading: loadingEnrollments,
  } = useQuery({
    queryKey: ['my-courses', user?.email],
    queryFn: () => getMyCourses(user?.email ?? ''),
    enabled: !!user?.email,
  })

  const { data: recommendations, isLoading: loadingCatalog } = useQuery({
    queryKey: ['catalog'],
    queryFn: browseCourses,
  })

  if (loadingEnrollments || !enrollments) {
    return <LoadingScreen message="Loading your learning data..." />
  }

  const avgProgress =
    enrollments.length > 0
      ? Math.round(
          enrollments.reduce((sum, enrollment) => sum + (enrollment.progress ?? 0), 0) /
            enrollments.length
        )
      : 0

  return (
    <div className="space-y-8">
      <div className="space-y-2">
        <h1 className="text-3xl font-bold text-slate-900">Hey {user?.name?.split(' ')[0]},</h1>
        <p className="text-slate-600">
          Track your active courses, resume where you left off, and discover new skills to forge.
        </p>
      </div>
      <div className="grid md:grid-cols-3 gap-4">
        <StatCard label="Active courses" value={enrollments.length} />
        <StatCard label="Avg. progress" value={`${avgProgress}%`} helper="Based on enrolled courses" />
        
     
     
     
     
      </div>
      <section className="space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-xl font-semibold text-slate-900">Continue learning</h2>
        </div>
        {enrollments.length === 0 ? (
          <div className="glass-panel p-6 text-slate-500 text-sm">
            You are not enrolled in any courses yet. Browse the catalog to get started.
          </div>
        ) : (
          <div className="grid md:grid-cols-2 gap-4">
            {enrollments.map((enrollment) => (
              <div key={enrollment.id} className="glass-panel p-5 space-y-2">
                <p className="text-sm font-semibold text-slate-500">{enrollment.courseTitle}</p>
                <p className="text-xs text-slate-500">Instructor: {enrollment.instructorName}</p>
                <div className="w-full bg-slate-100 rounded-full h-2">
                  <div
                    className="h-2 rounded-full bg-brand-500"
                    style={{ width: `${enrollment.progress ?? 0}%` }}
                  />
                </div>
                <p className="text-xs text-slate-500">
                  {Math.round(enrollment.progress ?? 0)}% complete
                </p>
              </div>
            ))}
          </div>
        )}
      </section>
      <section className="space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-xl font-semibold text-slate-900">Recommended for you</h2>
        </div>
        {loadingCatalog ? (
          <LoadingScreen />
        ) : (
          <div className="grid md:grid-cols-3 gap-4">
            {recommendations?.slice(0, 3).map((course) => (
              <CourseCard key={course.id} course={course} highlight />
            ))}
          </div>
        )}
      </section>
    </div>
  )
}

export default StudentDashboard

