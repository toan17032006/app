'use client';
import { dayLabel, todayEnum, avatarFor, currentSchoolYear, gradeDisplay } from '@/lib/constants';

export default function Dashboard({ currentUser, userStats, scheduleData, leaderboardData, onTabChange }) {
  // Lịch HÔM NAY: lọc theo ngày hiện tại (MON..SUN), sắp theo giờ bắt đầu
  const today = todayEnum();
  const todaySchedule = scheduleData
    .filter((item) => item.dayOfWeek === today)
    .sort((a, b) => (a.startTime || '').localeCompare(b.startTime || ''));
  const topLeaderboard = leaderboardData.slice(0, 3);

  const completedExams = userStats.completedExams ?? 0;
  const totalExams = userStats.totalExams ?? 0;
  const pendingHomework = userStats.pendingHomework ?? 0;
  const examProgress = totalExams > 0 ? Math.round((completedExams / totalExams) * 100) : 0;

  return (
    <section className="space-y-6 fade-in">
      {/* Welcome Banner */}
      <div className="bg-gradient-to-r from-indigo-600 to-violet-600 rounded-3xl p-6 sm:p-8 text-white shadow-xl shadow-indigo-500/10 flex flex-col md:flex-row items-center justify-between gap-6">
        <div className="space-y-3">
          <div className="inline-block px-3 py-1 rounded-full bg-white/20 text-xs font-medium backdrop-blur-sm">
            Năm học {currentSchoolYear()}
          </div>
          <h2 className="text-2xl sm:text-3xl font-bold">
            Chào mừng trở lại, {currentUser?.fullName || 'bạn'}! 👋
          </h2>
          <p className="text-indigo-100 text-sm max-w-xl">
            Hôm nay là một ngày tuyệt vời để học tập. Hãy kiểm tra thời gian biểu và hoàn thành các mục tiêu của bạn nhé!
          </p>
        </div>
        <div className="flex flex-col sm:flex-row gap-3 w-full md:w-auto">
          <button
            onClick={() => onTabChange('schedule')}
            className="px-5 py-3 rounded-xl bg-white text-indigo-600 font-semibold text-sm hover:bg-indigo-50 transition shadow-md text-center"
          >
            <i className="fa-solid fa-calendar-days mr-2"></i> Xem thời gian biểu
          </button>
          <button
            onClick={() => onTabChange('exams')}
            className="px-5 py-3 rounded-xl bg-indigo-700/50 border border-white/30 text-white font-semibold text-sm hover:bg-indigo-700 transition text-center"
          >
            <i className="fa-solid fa-file-pen mr-2"></i> Làm bài kiểm tra
          </button>
        </div>
      </div>

      {/* Statistics Cards — toàn bộ số liệu từ API /users/{id}/stats */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 sm:gap-6">
        {/* GPA */}
        <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-xs flex items-center justify-between">
          <div>
            <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">Điểm trung bình (GPA)</p>
            <h3 className="text-3xl font-bold text-slate-800 mt-1">{userStats.gpa ?? 0}</h3>
            <span className="text-xs font-medium text-slate-500 bg-slate-50 px-2 py-0.5 rounded-md mt-2 inline-block">
              <i className="fa-solid fa-school mr-1"></i> {gradeDisplay(currentUser?.grade)}
            </span>
          </div>
          <div className="w-14 h-14 rounded-2xl bg-emerald-50 text-emerald-600 flex items-center justify-center text-2xl">
            <i className="fa-solid fa-award"></i>
          </div>
        </div>

        {/* Coins */}
        <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-xs flex items-center justify-between">
          <div>
            <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">Điểm tích lũy / Xu</p>
            <h3 className="text-3xl font-bold text-slate-800 mt-1">{(userStats.coins ?? 0).toLocaleString()}</h3>
            <span className="text-xs font-medium text-indigo-600 bg-indigo-50 px-2 py-0.5 rounded-md mt-2 inline-block">
              <i className="fa-solid fa-coins"></i> Hoạt động tốt
            </span>
          </div>
          <div className="w-14 h-14 rounded-2xl bg-indigo-50 text-indigo-600 flex items-center justify-center text-2xl">
            <i className="fa-solid fa-piggy-bank"></i>
          </div>
        </div>

        {/* Tests */}
        <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-xs flex items-center justify-between">
          <div>
            <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">Bài kiểm tra đã làm</p>
            <h3 className="text-3xl font-bold text-slate-800 mt-1">{completedExams}/{totalExams}</h3>
            <span className="text-xs font-medium text-amber-600 bg-amber-50 px-2 py-0.5 rounded-md mt-2 inline-block">
              {examProgress}% hoàn thành
            </span>
          </div>
          <div className="w-14 h-14 rounded-2xl bg-amber-50 text-amber-600 flex items-center justify-center text-2xl">
            <i className="fa-solid fa-list-check"></i>
          </div>
        </div>

        {/* Homework */}
        <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-xs flex items-center justify-between">
          <div>
            <p className="text-xs font-semibold uppercase tracking-wider text-slate-400">Bài tập về nhà</p>
            <h3 className="text-3xl font-bold text-slate-800 mt-1">
              {pendingHomework} <span className="text-sm font-normal text-slate-400">chưa nộp</span>
            </h3>
            <span className={`text-xs font-medium px-2 py-0.5 rounded-md mt-2 inline-block ${pendingHomework > 0 ? 'text-rose-600 bg-rose-50' : 'text-emerald-600 bg-emerald-50'}`}>
              {pendingHomework > 0 ? 'Cần hoàn thành sớm' : 'Đã hoàn thành hết 🎉'}
            </span>
          </div>
          <div className="w-14 h-14 rounded-2xl bg-rose-50 text-rose-600 flex items-center justify-center text-2xl">
            <i className="fa-solid fa-triangle-exclamation"></i>
          </div>
        </div>
      </div>

      {/* Today's Schedule + Mini Leaderboard */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Today's Schedule */}
        <div className="lg:col-span-2 bg-white p-6 rounded-2xl border border-slate-200 shadow-xs space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="font-bold text-lg text-slate-800">
              Thời gian biểu hôm nay <span className="text-sm font-normal text-slate-400">({dayLabel(today)})</span>
            </h3>
            <button
              onClick={() => onTabChange('schedule')}
              className="text-sm font-semibold text-indigo-600 hover:text-indigo-700"
            >
              Xem cả tuần
            </button>
          </div>
          <div className="space-y-3">
            {todaySchedule.length === 0 ? (
              <div className="py-8 text-center text-slate-400 text-sm">
                <i className="fa-regular fa-calendar-check text-xl block mb-2"></i>
                Hôm nay không có tiết học nào — thời gian để ôn bài! 📚
              </div>
            ) : (
              todaySchedule.map((item) => (
                <div
                  key={item.id}
                  className="flex items-center justify-between p-4 rounded-xl bg-slate-50 border border-slate-100 hover:border-indigo-200 transition"
                >
                  <div className="flex items-center gap-3.5">
                    <div className="w-10 h-10 rounded-xl bg-indigo-100 text-indigo-600 flex items-center justify-center font-bold text-sm">
                      <i className="fa-solid fa-clock"></i>
                    </div>
                    <div>
                      <div className="flex items-center gap-2">
                        <h4 className="font-bold text-sm text-slate-800">{item.subject}</h4>
                        <span className="text-[10px] bg-indigo-50 text-indigo-600 font-semibold px-2 py-0.5 rounded">
                          {dayLabel(item.dayOfWeek)}
                        </span>
                      </div>
                      <p className="text-xs text-slate-500">
                        {item.teacher} •{' '}
                        <span className="text-indigo-600 font-medium">
                          {item.startTime} - {item.endTime}
                        </span>
                      </p>
                    </div>
                  </div>
                  <span className="text-xs font-semibold px-2.5 py-1 bg-emerald-50 text-emerald-600 rounded-lg">
                    Sẵn sàng
                  </span>
                </div>
              ))
            )}
          </div>
        </div>

        {/* Mini Leaderboard */}
        <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-xs space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="font-bold text-lg text-slate-800">Bảng vàng</h3>
            <button
              onClick={() => onTabChange('leaderboard')}
              className="text-sm font-semibold text-indigo-600 hover:text-indigo-700"
            >
              Chi tiết
            </button>
          </div>
          <div className="space-y-3">
            {topLeaderboard.length === 0 ? (
              <div className="py-8 text-center text-slate-400 text-sm">Chưa có dữ liệu xếp hạng.</div>
            ) : (
              topLeaderboard.map((user) => {
                const rankClass =
                  user.rank === 1
                    ? 'bg-amber-500 text-white'
                    : user.rank === 2
                    ? 'bg-slate-300 text-slate-800'
                    : 'bg-amber-700/80 text-white';
                return (
                  <div
                    key={user.rank}
                    className={`flex items-center justify-between p-3 rounded-xl bg-slate-50 border border-slate-100 ${user.isCurrentUser ? 'ring-1 ring-indigo-300' : ''}`}
                  >
                    <div className="flex items-center gap-3">
                      <div
                        className={`w-7 h-7 rounded-full ${rankClass} font-bold text-xs flex items-center justify-center`}
                      >
                        {user.rank}
                      </div>
                      <img
                        src={user.avatarUrl || avatarFor(user.fullName)}
                        alt={user.fullName}
                        className="w-7 h-7 rounded-full object-cover"
                      />
                      <div>
                        <h5 className="font-bold text-xs text-slate-800">
                          {user.fullName}
                          {user.isCurrentUser && (
                            <span className="ml-1 text-[10px] bg-indigo-50 text-indigo-600 px-1.5 py-0.5 rounded font-medium">Bạn</span>
                          )}
                        </h5>
                        <span className="text-[10px] text-slate-400">{gradeDisplay(user.grade)}</span>
                      </div>
                    </div>
                    <span className="text-xs font-bold text-indigo-600">{user.coins?.toLocaleString()} xu</span>
                  </div>
                );
              })
            )}
          </div>
        </div>
      </div>
    </section>
  );
}
