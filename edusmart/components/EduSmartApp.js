'use client';
import { useState, useEffect, useMemo } from 'react';
import { useRouter } from 'next/navigation';
import Sidebar from '@/components/Sidebar';
import Header from '@/components/Header';
import Dashboard from '@/components/tabs/Dashboard';
import Schedule from '@/components/tabs/Schedule';
import Exams from '@/components/tabs/Exams';
import Homework from '@/components/tabs/Homework';
import Leaderboard from '@/components/tabs/Leaderboard';
import AlertModal from '@/components/modals/AlertModal';
import { tokenHelper } from '@/lib/api';
import { todayEnum } from '@/lib/constants';
import {
  scheduleService,
  homeworkService,
  userService,
  leaderboardService,
} from '@/lib/services/apiServices';

export default function EduSmartApp() {
  const router = useRouter();

  // ── Auth guard: chưa đăng nhập → redirect về /login ─────────────────────
  const [currentUser, setCurrentUser] = useState(null);
  useEffect(() => {
    const user = tokenHelper.getUser();
    if (!user) {
      router.push('/login');
    } else {
      setCurrentUser(user);
    }
  }, [router]);

  // ── Global State ─────────────────────────────────────────────────────────
  const [activeTab,    setActiveTab]    = useState('dashboard');
  const [sidebarOpen,  setSidebarOpen]  = useState(false);
  const [alertModal,   setAlertModal]   = useState({ isOpen: false, title: '', message: '' });

  // Data state — sẽ được load từ API
  const [userStats,      setUserStats]      = useState({ gpa: 0, coins: 0, completedExams: 0, totalExams: 0, pendingHomework: 0, totalHomework: 0 });
  const [scheduleData,   setScheduleData]   = useState([]);
  const [homeworkData,   setHomeworkData]   = useState([]);
  const [leaderboardData,setLeaderboardData]= useState([]);
  const [loadingData,    setLoadingData]    = useState(true);

  // ── Load data từ API khi user đã xác thực ────────────────────────────────
  const loadAll = async (userId) => {
    setLoadingData(true);
    try {
      // Gọi song song để nhanh hơn (Promise.allSettled không bị abort nếu 1 cái lỗi)
      const [statsRes, scheduleRes, homeworkRes, leaderRes] = await Promise.allSettled([
        userService.getStats(userId),
        scheduleService.getAll(userId),
        homeworkService.getAll(userId),
        leaderboardService.getTop(10),
      ]);

      if (statsRes.status === 'fulfilled' && statsRes.value)
        setUserStats(statsRes.value);

      if (scheduleRes.status === 'fulfilled')
        setScheduleData(scheduleRes.value || []);

      if (homeworkRes.status === 'fulfilled')
        setHomeworkData(homeworkRes.value || []);

      if (leaderRes.status === 'fulfilled')
        setLeaderboardData(leaderRes.value || []);

    } catch (err) {
      console.error('Load data error:', err);
    } finally {
      setLoadingData(false);
    }
  };

  useEffect(() => {
    if (!currentUser) return;
    loadAll(currentUser.id);
  }, [currentUser]);

  // ── Helpers ──────────────────────────────────────────────────────────────
  const showAlert = (title, message) => setAlertModal({ isOpen: true, title, message });
  const closeAlert = () => setAlertModal({ isOpen: false, title: '', message: '' });

  const handleTabChange = (tab) => {
    setActiveTab(tab);
    setSidebarOpen(false);
  };

  // ── Schedule CRUD (gọi API, rồi cập nhật state local) ───────────────────
  const handleAddSchedule = async (form) => {
    try {
      const created = await scheduleService.create(currentUser.id, form);
      setScheduleData((prev) => [...prev, created]);
      return { success: true };
    } catch (err) {
      return { success: false, error: err.message };
    }
  };

  const handleEditSchedule = async (id, form) => {
    try {
      const updated = await scheduleService.update(currentUser.id, id, form);
      setScheduleData((prev) => prev.map((item) => item.id === id ? updated : item));
      return { success: true };
    } catch (err) {
      return { success: false, error: err.message };
    }
  };

  const handleDeleteSchedule = async (id) => {
    try {
      await scheduleService.delete(currentUser.id, id);
      setScheduleData((prev) => prev.filter((item) => item.id !== id));
      return { success: true };
    } catch (err) {
      return { success: false, error: err.message };
    }
  };

  // ── Homework submit (kèm file tùy chọn) ──────────────────────────────────
  const handleSubmitHomework = async (id, file) => {
    try {
      const updated = await homeworkService.submit(currentUser.id, id, file);
      setHomeworkData((prev) => prev.map((hw) => hw.id === id ? updated : hw));
      // Cộng 20 xu vào state local (đồng bộ với backend)
      setUserStats((prev) => ({ ...prev, coins: prev.coins + 20 }));
      return { success: true, homework: updated };
    } catch (err) {
      return { success: false, error: err.message };
    }
  };

  // ── Coins earned từ bài thi ──────────────────────────────────────────────
  const handleCoinsEarned = (amount) => {
    setUserStats((prev) => ({ ...prev, coins: prev.coins + amount }));
  };

  // ── Notifications (từ bài tập sắp đến hạn / quá hạn) ─────────────────────
  const notifications = useMemo(() => {
    const today = new Date().toISOString().slice(0, 10);
    const in3Days = new Date(Date.now() + 3 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10);
    return homeworkData
      .filter((hw) => hw.status === 'PENDING')
      .map((hw) => {
        if (hw.dueDate < today) {
          return { id: `overdue-${hw.id}`, hwId: hw.id, type: 'overdue',
            title: 'Bài tập đã quá hạn!', message: `"${hw.title}" (${hw.subject}) đã quá hạn chót — hãy nộp ngay!` };
        }
        if (hw.dueDate <= in3Days) {
          return { id: `soon-${hw.id}`, hwId: hw.id, type: 'soon',
            title: 'Sắp đến hạn nộp bài', message: `"${hw.title}" (${hw.subject}) sẽ hết hạn trong 3 ngày tới.` };
        }
        return null;
      })
      .filter(Boolean);
  }, [homeworkData]);

  // ── Bài tập vừa tạo thủ công → thêm vào danh sách + cập nhật thống kê ──
  const handleHomeworkCreated = (homework) => {
    if (!homework) return;
    setHomeworkData((prev) => [...prev, homework]);
    setUserStats((prev) => ({
      ...prev,
      pendingHomework: (prev.pendingHomework || 0) + 1,
      totalHomework: (prev.totalHomework || 0) + 1,
    }));
    showAlert('Đã thêm bài tập 📚', `"${homework.title}" (${homework.subject}) — hạn chót ${homework.dueDate}`);
  };

  // ── Logout ───────────────────────────────────────────────────────────────
  const handleLogout = () => {
    tokenHelper.clear();
    router.push('/login');
  };

  // Chưa xác thực → không render
  if (!currentUser) return null;

  // ── Render ───────────────────────────────────────────────────────────────
  return (
    <div className="flex h-screen overflow-hidden">
      {sidebarOpen && (
        <div
          className="fixed inset-0 bg-slate-900/40 z-40 md:hidden"
          onClick={() => setSidebarOpen(false)}
        />
      )}

      <Sidebar
        activeTab={activeTab}
        onTabChange={handleTabChange}
        sidebarOpen={sidebarOpen}
        user={currentUser}
        onLogout={handleLogout}
      />

      <div className="flex-1 flex flex-col min-w-0 overflow-hidden">
        <Header
          activeTab={activeTab}
          gpa={userStats.gpa}
          coins={userStats.coins}
          userName={currentUser.fullName}
          avatarUrl={currentUser.avatarUrl}
          notifications={notifications}
          onOpenHomework={() => handleTabChange('homework')}
          onToggleSidebar={() => setSidebarOpen((prev) => !prev)}
        />

        <main className="flex-1 overflow-y-auto p-4 sm:p-8 bg-slate-50/50">
          {/* Loading skeleton */}
          {loadingData ? (
            <div className="flex items-center justify-center h-64">
              <div className="text-center space-y-3">
                <i className="fa-solid fa-spinner animate-spin text-3xl text-indigo-500"></i>
                <p className="text-slate-500 text-sm">Đang tải dữ liệu...</p>
              </div>
            </div>
          ) : (
            <>
              {activeTab === 'dashboard' && (
                <Dashboard
                  currentUser={currentUser}
                  userStats={userStats}
                  scheduleData={scheduleData}
                  leaderboardData={leaderboardData}
                  onTabChange={handleTabChange}
                />
              )}
              {activeTab === 'schedule' && (
                <Schedule
                  scheduleData={scheduleData}
                  onAdd={handleAddSchedule}
                  onEdit={handleEditSchedule}
                  onDelete={handleDeleteSchedule}
                  onShowAlert={showAlert}
                />
              )}
              {activeTab === 'exams' && (
                <Exams
                  userId={currentUser.id}
                  onCoinsEarned={handleCoinsEarned}
                />
              )}
              {activeTab === 'homework' && (
                <Homework
                  userId={currentUser.id}
                  homeworkData={homeworkData}
                  onSubmitHomework={handleSubmitHomework}
                  onShowAlert={showAlert}
                  onCreated={handleHomeworkCreated}
                />
              )}
              {activeTab === 'leaderboard' && (
                <Leaderboard userId={currentUser.id} />
              )}
            </>
          )}
        </main>
      </div>

      <AlertModal
        isOpen={alertModal.isOpen}
        title={alertModal.title}
        message={alertModal.message}
        onClose={closeAlert}
      />
    </div>
  );
}
