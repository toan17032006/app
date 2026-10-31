'use client';
import { useState, useEffect, useRef } from 'react';
import { avatarFor } from '@/lib/constants';

const PAGE_TITLES = {
  dashboard: 'Tổng quan hệ thống',
  schedule: 'Thời gian biểu cá nhân',
  exams: 'Kho bài kiểm tra trực tuyến',
  homework: 'Danh sách bài tập về nhà',
  leaderboard: 'Bảng xếp hạng học sinh xuất sắc',
};

export default function Header({ activeTab, gpa, coins, userName, avatarUrl, notifications = [], onOpenHomework, onToggleSidebar }) {
  const [bellOpen, setBellOpen] = useState(false);
  const bellRef = useRef(null);

  // Đóng dropdown khi bấm ra ngoài
  useEffect(() => {
    const handler = (e) => {
      if (bellRef.current && !bellRef.current.contains(e.target)) setBellOpen(false);
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  return (
    <header className="bg-white border-b border-slate-200 h-16 flex items-center justify-between px-6 z-30 shadow-xs flex-shrink-0">
      <div className="flex items-center gap-4">
        <button
          onClick={onToggleSidebar}
          className="text-slate-500 hover:text-slate-700 md:hidden p-2 rounded-lg hover:bg-slate-100"
        >
          <i className="fa-solid fa-bars text-lg"></i>
        </button>
        <h2 className="text-xl font-bold text-slate-800">{PAGE_TITLES[activeTab]}</h2>
      </div>

      <div className="flex items-center gap-3 sm:gap-6">
        {/* GPA Badge */}
        <div className="hidden sm:flex items-center gap-2 bg-emerald-50 text-emerald-700 px-3 py-1.5 rounded-full text-xs sm:text-sm font-semibold border border-emerald-200">
          <i className="fa-solid fa-star text-amber-500"></i>
          <span>
            GPA: <strong>{gpa}</strong>/4.0
          </span>
        </div>

        {/* Coins Badge */}
        <div className="flex items-center gap-2 bg-indigo-50 text-indigo-700 px-3 py-1.5 rounded-full text-xs sm:text-sm font-semibold border border-indigo-200">
          <i className="fa-solid fa-coins text-amber-500"></i>
          <span>
            Tích lũy: <strong>{(coins || 0).toLocaleString()}</strong> xu
          </span>
        </div>

        {/* Notification Bell — thông báo thật: bài tập sắp đến hạn / quá hạn */}
        <div className="relative" ref={bellRef}>
          <button
            onClick={() => setBellOpen((v) => !v)}
            className="w-10 h-10 rounded-full bg-slate-100 flex items-center justify-center text-slate-600 hover:bg-slate-200 transition relative"
            aria-label="Thông báo"
          >
            <i className="fa-regular fa-bell"></i>
            {notifications.length > 0 && (
              <span className="absolute top-2 right-2 w-2 h-2 bg-rose-500 rounded-full ring-2 ring-white"></span>
            )}
          </button>

          {bellOpen && (
            <div className="absolute right-0 top-12 w-80 bg-white rounded-2xl border border-slate-200 shadow-xl overflow-hidden z-40">
              <div className="px-4 py-3 border-b border-slate-100 flex items-center justify-between">
                <h4 className="font-bold text-sm text-slate-800">Thông báo</h4>
                <span className="text-xs text-slate-400">{notifications.length} mục</span>
              </div>
              <div className="max-h-80 overflow-y-auto divide-y divide-slate-50">
                {notifications.length === 0 ? (
                  <div className="py-8 text-center text-slate-400 text-sm">
                    <i className="fa-regular fa-bell-slash text-xl block mb-2"></i>
                    Không có thông báo mới. Bạn đang rất ổn! 🎉
                  </div>
                ) : (
                  notifications.map((n) => (
                    <button
                      key={n.id}
                      onClick={() => { setBellOpen(false); onOpenHomework?.(); }}
                      className="w-full text-left px-4 py-3 hover:bg-slate-50 transition flex gap-3"
                    >
                      <i className={`fa-solid ${n.type === 'overdue' ? 'fa-triangle-exclamation text-rose-500' : 'fa-clock text-amber-500'} mt-0.5`}></i>
                      <span>
                        <span className="block text-xs font-bold text-slate-800">{n.title}</span>
                        <span className="block text-xs text-slate-500 mt-0.5">{n.message}</span>
                      </span>
                    </button>
                  ))
                )}
              </div>
            </div>
          )}
        </div>

        {/* User avatar + tên (từ tài khoản đang đăng nhập) */}
        <div className="hidden md:flex items-center gap-2.5">
          <img
            src={avatarUrl || avatarFor(userName)}
            alt={userName || 'User'}
            className="w-9 h-9 rounded-full object-cover ring-2 ring-indigo-100"
          />
          <span className="text-sm font-semibold text-slate-700 max-w-28 truncate">{userName}</span>
        </div>
      </div>
    </header>
  );
}
