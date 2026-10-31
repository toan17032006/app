import { apiFetch } from '@/lib/api';

export const scheduleService = {
  getAll:   (userId)         => apiFetch(`/api/schedules?userId=${userId}`),
  getByDay: (userId, day)    => apiFetch(`/api/schedules?userId=${userId}&day=${day}`),
  create:   (userId, data)   => apiFetch(`/api/schedules?userId=${userId}`,   { method: 'POST',   body: JSON.stringify(data) }),
  update:   (userId, id, data) => apiFetch(`/api/schedules/${id}?userId=${userId}`, { method: 'PUT',    body: JSON.stringify(data) }),
  delete:   (userId, id)     => apiFetch(`/api/schedules/${id}?userId=${userId}`, { method: 'DELETE' }),
};

export const homeworkService = {
  getAll:    (userId)        => apiFetch(`/api/homework?userId=${userId}`),
  getUpcoming:(userId, days) => apiFetch(`/api/homework/upcoming?userId=${userId}&days=${days}`),
  getOverdue: (userId)       => apiFetch(`/api/homework/overdue?userId=${userId}`),
  /** Tạo bài tập thủ công: subject, title, dueDate (YYYY-MM-DD, phải tương lai) */
  create:    (userId, subject, title, dueDate) =>
    apiFetch(`/api/homework?userId=${userId}`, {
      method: 'POST',
      body: JSON.stringify({ subject, title, dueDate }),
    }),
  /**
   * Nộp bài tập, kèm file đính kèm (tùy chọn).
   * Backend nhận multipart/form-data: file + fileName + fileUrl.
   */
  submit: (userId, id, file) => {
    const formData = new FormData();
    if (file) formData.append('file', file);
    return apiFetch(`/api/homework/${id}/submit?userId=${userId}`, {
      method: 'POST',
      body: formData,
      headers: {}, // để browser tự set Content-Type: multipart/form-data; boundary=...
    });
  },
};

export const examService = {
  getAll:    ()              => apiFetch('/api/exams'),
  /** Tạo đề thi thủ công: { title, category?, durationMinutes, status:'PUBLISHED', questions:[{content, type, options:[{content, isCorrect}], explanation?}] } */
  create:    (data)          => apiFetch('/api/exams', { method: 'POST', body: JSON.stringify(data) }),
  start:     (userId, examId) => apiFetch(`/api/exams/${examId}/start?userId=${userId}`, { method: 'POST' }),
  submit:    (userId, data, timedOut)  => apiFetch(`/api/exams/submit?userId=${userId}&timedOut=${timedOut}`,  { method: 'POST', body: JSON.stringify(data) }),
  getHistory:(userId)        => apiFetch(`/api/exams/history?userId=${userId}`),
};

export const leaderboardService = {
  /** period: 'all' | 'week' | 'month' */
  getTop:  (limit = 10, period = 'all') => apiFetch(`/api/leaderboard/top?limit=${limit}&period=${period}`),
  getAll:  (currentUserId, period = 'all') => apiFetch(`/api/leaderboard?currentUserId=${currentUserId}&period=${period}`),
  getRank: (userId)             => apiFetch(`/api/leaderboard/rank/${userId}`),
};

export const userService = {
  getProfile: (id)        => apiFetch(`/api/users/${id}`),
  getStats:   (id)        => apiFetch(`/api/users/${id}/stats`),
  update:     (id, data)  => apiFetch(`/api/users/${id}`,          { method: 'PUT',  body: JSON.stringify(data) }),
  changePass: (id, data)  => apiFetch(`/api/users/${id}/password`, { method: 'PUT',  body: JSON.stringify(data) }),
};

export const aiService = {
  /** Gửi file tài liệu cho AI Agent tạo đề thi/bài tập (FormData: file, mode, ...) */
  generateExam: (formData) =>
    apiFetch('/api/ai/generate-exam', { method: 'POST', body: formData }),
};
