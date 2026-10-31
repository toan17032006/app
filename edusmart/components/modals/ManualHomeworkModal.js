'use client';
/**
 * components/modals/ManualHomeworkModal.js — Thêm bài tập về nhà THỦ CÔNG
 *
 * Nhập môn học, tiêu đề và hạn chót → POST /api/homework → bài tập xuất hiện
 * trong danh sách, học sinh nộp bài được +20 xu như bình thường.
 */
import { useState } from 'react';
import { homeworkService } from '@/lib/services/apiServices';

const SUBJECT_SUGGESTIONS = ['Toán học', 'Vật lý', 'Hóa học', 'Tiếng Anh', 'Văn học', 'Sinh học', 'Lịch sử', 'Địa lý', 'Tin học'];

const defaultDueDate = () => {
  const d = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000);
  return d.toISOString().slice(0, 10);
};

export default function ManualHomeworkModal({ isOpen, onClose, onCreated, userId }) {
  const [form, setForm] = useState({ subject: '', title: '', dueDate: defaultDueDate() });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  if (!isOpen) return null;

  const handleClose = () => {
    if (saving) return;
    setError('');
    onClose();
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.subject.trim()) return setError('Vui lòng nhập môn học');
    if (!form.title.trim()) return setError('Vui lòng nhập tiêu đề bài tập');
    const today = new Date().toISOString().slice(0, 10);
    if (form.dueDate <= today) return setError('Hạn chót phải là ngày trong tương lai');

    setError('');
    setSaving(true);
    try {
      const homework = await homeworkService.create(userId, form.subject.trim(), form.title.trim(), form.dueDate);
      setForm({ subject: '', title: '', dueDate: defaultDueDate() });
      onCreated(homework);
    } catch (err) {
      setError(err.message || 'Không thể thêm bài tập');
      setSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-sm p-4">
      <div className="bg-white p-6 sm:p-8 rounded-2xl shadow-xl max-w-lg w-full mx-4 space-y-5">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-slate-100 pb-3">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-rose-500 flex items-center justify-center text-white text-lg shadow-lg shadow-rose-500/30">
              <i className="fa-solid fa-book-open"></i>
            </div>
            <div>
              <h3 className="font-bold text-lg text-slate-800">Thêm bài tập về nhà</h3>
              <p className="text-xs text-slate-400">Tạo thủ công — học sinh nộp bài được +20 xu</p>
            </div>
          </div>
          <button onClick={handleClose} className="text-slate-400 hover:text-slate-600" disabled={saving}>
            <i className="fa-solid fa-xmark text-lg"></i>
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Môn học */}
          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-1.5">Môn học *</label>
            <input
              type="text" required maxLength={50} list="subject-suggestions"
              placeholder="Ví dụ: Toán học"
              value={form.subject}
              onChange={(e) => setForm({ ...form, subject: e.target.value })}
              className="w-full bg-slate-50 border border-slate-300 rounded-xl px-4 py-2.5 text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none"
            />
            <datalist id="subject-suggestions">
              {SUBJECT_SUGGESTIONS.map((s) => <option key={s} value={s} />)}
            </datalist>
          </div>

          {/* Tiêu đề */}
          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-1.5">Tiêu đề bài tập *</label>
            <textarea
              rows={2} required maxLength={200}
              placeholder="Ví dụ: Giải 15 bài tập phương trình bậc hai — trang 72"
              value={form.title}
              onChange={(e) => setForm({ ...form, title: e.target.value })}
              className="w-full bg-slate-50 border border-slate-300 rounded-xl px-4 py-2.5 text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none resize-y"
            />
          </div>

          {/* Hạn chót */}
          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-1.5">Hạn chót *</label>
            <input
              type="date" required min={defaultDueDate()}
              value={form.dueDate}
              onChange={(e) => setForm({ ...form, dueDate: e.target.value })}
              className="w-full bg-slate-50 border border-slate-300 rounded-xl px-4 py-2.5 text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none"
            />
            <p className="text-[11px] text-slate-400 mt-1">
              <i className="fa-solid fa-circle-info mr-1"></i>
              Hạn chót phải là ngày trong tương lai
            </p>
          </div>

          {error && (
            <div className="flex items-start gap-3 p-3.5 bg-red-50 border border-red-200 rounded-xl text-red-700 text-sm">
              <i className="fa-solid fa-circle-exclamation mt-0.5 shrink-0"></i>
              <span>{error}</span>
            </div>
          )}

          {/* Actions */}
          <div className="flex justify-end gap-3 pt-1">
            <button
              type="button" onClick={handleClose} disabled={saving}
              className="px-5 py-2.5 bg-slate-100 hover:bg-slate-200 text-slate-600 font-semibold text-sm rounded-xl transition"
            >
              Hủy
            </button>
            <button
              type="submit" disabled={saving}
              className="px-6 py-2.5 bg-rose-500 hover:bg-rose-600 disabled:bg-rose-300 text-white font-semibold text-sm rounded-xl transition shadow-md flex items-center gap-2"
            >
              {saving ? (
                <><i className="fa-solid fa-spinner animate-spin"></i> Đang lưu...</>
              ) : (
                <><i className="fa-solid fa-plus"></i> Thêm bài tập</>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
