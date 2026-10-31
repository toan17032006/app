'use client';
import { avatarFor, gradeDisplay } from '@/lib/constants';

const NAV_ITEMS = [
  { id: 'dashboard', label: 'Tổng quan', icon: 'fa-chart-pie' },
  { id: 'schedule', label: 'Thời gian biểu', icon: 'fa-calendar-days' },
  { id: 'exams', label: 'Bài kiểm tra', icon: 'fa-file-pen' },
  { id: 'homework', label: 'Bài tập về nhà', icon: 'fa-book-open' },
  { id: 'leaderboard', label: 'Bảng xếp hạng', icon: 'fa-trophy' },
];

export default function Sidebar({ activeTab, onTabChange, sidebarOpen, user, onLogout }) {
  const fullName = user?.fullName || 'Học sinh EduSmart';
  const grade = user?.grade || 'Chưa cập nhật';
  // Đăng nhập Google → chuỗi chữ cái đầu từ avatar URL (nếu có), ngược lại lấy từ tên
  const initials = fullName
    .trim()
    .split(/\s+/)
    .map((w) => w[0])
    .slice(0, 2)
    .join('')
    .toUpperCase();

  return (
    <aside
      className={`fixed inset-y-0 left-0 z-50 w-64 bg-slate-900 text-slate-300 transform transition-transform duration-300 ease-in-out md:translate-x-0 md:static md:inset-auto flex flex-col justify-between shadow-xl ${
        sidebarOpen ? 'translate-x-0' : '-translate-x-full'
      }`}
    >
      <div>
        {/* Logo */}
        <div className="flex items-center gap-3 px-6 py-6 border-b border-slate-800">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-indigo-500 to-violet-500 flex items-center justify-center text-white font-bold text-xl shadow-lg shadow-indigo-500/30">
            <i className="fa-solid fa-graduation-cap"></i>
          </div>
          <div>
            <h1 className="font-bold text-lg text-white tracking-wide">EduSmart</h1>
            <p className="text-xs text-slate-400">Học tập thông minh</p>
          </div>
        </div>

        {/* Navigation */}
        <nav className="px-4 py-6 space-y-1">
          {NAV_ITEMS.map((item) => {
            const isActive = activeTab === item.id;
            return (
              <button
                key={item.id}
                onClick={() => onTabChange(item.id)}
                className={`w-full flex items-center gap-3.5 px-4 py-3 rounded-xl text-sm font-medium transition-all ${
                  isActive
                    ? 'bg-indigo-600 text-white shadow-md shadow-indigo-600/20'
                    : 'text-slate-400 hover:bg-slate-800 hover:text-white'
                }`}
              >
                <i className={`fa-solid ${item.icon} w-5 text-center`}></i>
                {item.label}
              </button>
            );
          })}
        </nav>
      </div>

      {/* User profile summary + Logout */}
      <div className="p-4 space-y-2">
        <div className="rounded-xl bg-slate-800/60 border border-slate-700/50 flex items-center gap-3 p-3">
          <img
            src={user?.avatarUrl || avatarFor(fullName)}
            alt={fullName}
            className="w-10 h-10 rounded-full object-cover ring-2 ring-indigo-500"
          />
          <div className="overflow-hidden flex-1">
            <h4 className="text-sm font-semibold text-white truncate">{fullName}</h4>
            <p className="text-xs text-indigo-400 truncate">{gradeDisplay(grade)} • Thành viên</p>
          </div>
          {!user?.avatarUrl && (
            <span className="sr-only">{initials}</span>
          )}
        </div>
        {/* Logout: gọi onLogout để xóa token trong localStorage trước khi rời trang */}
        <button
          onClick={onLogout}
          className="w-full flex items-center gap-3 px-4 py-2.5 rounded-xl text-sm font-medium text-slate-400 hover:bg-slate-800 hover:text-rose-400 transition-all text-left"
        >
          <i className="fa-solid fa-right-from-bracket w-5 text-center"></i>
          Đăng xuất
        </button>
      </div>
    </aside>
  );
}
