'use client';
import { useRef, useState } from 'react';
import { homeworkStatusLabel } from '@/lib/constants';
import { fileUrl } from '@/lib/api';

export default function Homework({ homeworkData, onSubmitHomework, onShowAlert }) {
  const [filter, setFilter] = useState('all');
  const [submittingId, setSubmittingId] = useState(null);
  const fileInputs = useRef({}); // hwId → input element

  // Sắp xếp: quá hạn/sắp đến hạn trước, đã nộp xuống cuối
  const today = new Date().toISOString().slice(0, 10);
  const sorted = [...homeworkData].sort((a, b) => {
    if (a.status !== b.status) return a.status === 'PENDING' ? -1 : 1;
    return (a.dueDate || '').localeCompare(b.dueDate || '');
  });

  const filtered =
    filter === 'all' ? sorted : sorted.filter((hw) => hw.status === filter);

  const pendingCount = homeworkData.filter((hw) => hw.status === 'PENDING').length;
  const overdueCount = homeworkData.filter((hw) => hw.status === 'PENDING' && hw.dueDate < today).length;

  // Nộp bài: lấy file từ input ẩn (nếu có) rồi gọi API
  const handleSubmit = async (hw) => {
    const file = fileInputs.current[hw.id]?.files?.[0] || null;
    setSubmittingId(hw.id);
    const result = await onSubmitHomework(hw.id, file);
    setSubmittingId(null);
    if (result?.success) {
      onShowAlert('Nộp bài thành công! 🎉', `"${hw.title}" đã được gửi. +20 xu`);
    } else {
      onShowAlert('Không thể nộp bài', result?.error || 'Đã có lỗi xảy ra khi nộp bài.');
    }
  };

  return (
    <section className="space-y-6 fade-in">
      {/* Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-slate-800">
            <i className="fa-solid fa-book-open text-indigo-600 mr-2"></i>Danh sách bài tập về nhà
          </h2>
          <p className="text-sm text-slate-500 mt-1">
            Quản lý và nộp bài tập về nhà theo từng môn học trước hạn chót.
            {pendingCount > 0 && (
              <span className="ml-1 font-semibold text-rose-600">
                ({pendingCount} chưa nộp{overdueCount > 0 ? `, ${overdueCount} quá hạn` : ''})
              </span>
            )}
          </p>
        </div>
        <div className="flex items-center gap-2 bg-white border border-slate-200 px-3 py-2 rounded-xl shadow-xs">
          <i className="fa-solid fa-filter text-slate-400"></i>
          <select
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
            className="bg-transparent text-sm font-medium focus:outline-none text-slate-700"
          >
            <option value="all">Tất cả trạng thái</option>
            <option value="PENDING">Chưa nộp</option>
            <option value="SUBMITTED">Đã nộp</option>
          </select>
        </div>
      </div>

      {/* Homework Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {filtered.length === 0 ? (
          <div className="col-span-full py-12 text-center text-slate-400 text-sm">
            Không có bài tập nào trong danh mục này.
          </div>
        ) : (
          filtered.map((hw) => {
            const isOverdue = hw.status === 'PENDING' && hw.dueDate < today;
            const isSubmitted = hw.status === 'SUBMITTED';
            return (
              <div
                key={hw.id}
                className={`bg-white p-6 rounded-2xl border shadow-xs flex flex-col justify-between space-y-4 ${
                  isOverdue ? 'border-rose-200' : 'border-slate-200'
                }`}
              >
                <div className="space-y-2">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-semibold uppercase px-2.5 py-1 bg-indigo-50 text-indigo-600 rounded-md">
                      {hw.subject}
                    </span>
                    <span
                      className={`text-xs font-semibold px-2.5 py-1 rounded-md ${
                        isSubmitted
                          ? 'bg-emerald-50 text-emerald-600'
                          : isOverdue
                          ? 'bg-rose-100 text-rose-700'
                          : 'bg-amber-50 text-amber-600'
                      }`}
                    >
                      {isOverdue ? 'Quá hạn!' : homeworkStatusLabel(hw.status)}
                    </span>
                  </div>
                  <h3 className="font-bold text-base text-slate-800">{hw.title}</h3>
                  <p className="text-xs text-slate-500">
                    <i className="fa-solid fa-calendar-xmark mr-1 text-slate-400"></i>
                    Hạn chót: <strong className="text-slate-700">{hw.dueDate}</strong>
                    {hw.submittedAt && (
                      <> • Đã nộp: <strong className="text-emerald-600">{String(hw.submittedAt).slice(0, 10)}</strong></>
                    )}
                  </p>
                  {/* File đã nộp — mở file từ server */}
                  {hw.fileUrl && (
                    <a
                      href={fileUrl(hw.fileUrl)}
                      target="_blank"
                      rel="noreferrer"
                      className="inline-flex items-center gap-2 text-xs font-semibold text-indigo-600 hover:text-indigo-800 bg-indigo-50 px-3 py-1.5 rounded-lg transition"
                    >
                      <i className="fa-solid fa-paperclip"></i>
                      Xem file đã nộp: {hw.fileName || 'tải xuống'}
                    </a>
                  )}
                </div>

                <div className="pt-4 border-t border-slate-100 flex items-center justify-between gap-3">
                  {/* Input chọn file (ẩn) + nhãn hiển thị tên file đã chọn */}
                  <div className="flex-1 min-w-0">
                    <input
                      type="file"
                      ref={(el) => (fileInputs.current[hw.id] = el)}
                      className="hidden"
                      onChange={(e) => {
                        // Cập nhật label — force re-render bằng cách đổi value của label
                        const label = e.target.closest('.file-picker')?.querySelector('.file-label');
                        if (label) {
                          const f = e.target.files?.[0];
                          label.textContent = f ? f.name : 'Chưa chọn file (tùy chọn)';
                          label.classList.toggle('text-slate-700', !!f);
                        }
                      }}
                    />
                    <button
                      type="button"
                      onClick={() => !isSubmitted && fileInputs.current[hw.id]?.click()}
                      disabled={isSubmitted}
                      className={`file-picker inline-flex items-center gap-2 text-xs px-3 py-1.5 rounded-lg border transition ${
                        isSubmitted
                          ? 'border-slate-100 text-slate-300 cursor-not-allowed'
                          : 'border-slate-200 text-slate-500 hover:border-indigo-300 hover:text-indigo-600'
                      }`}
                    >
                      <i className="fa-solid fa-paperclip"></i>
                      <span className="file-label truncate max-w-40">
                        {isSubmitted ? 'Bài đã nộp' : 'Chọn file bài làm (tùy chọn)'}
                      </span>
                    </button>
                  </div>

                  <button
                    onClick={() => handleSubmit(hw)}
                    disabled={submittingId === hw.id}
                    className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 disabled:bg-indigo-400 text-white text-xs font-semibold rounded-xl transition shadow-sm flex items-center gap-2"
                  >
                    {submittingId === hw.id ? (
                      <><i className="fa-solid fa-spinner animate-spin"></i> Đang nộp...</>
                    ) : (
                      <>
                        <i className={`fa-solid ${isSubmitted ? 'fa-rotate-right' : 'fa-paper-plane'}`}></i>
                        {isSubmitted ? 'Nộp lại bài' : 'Nộp bài ngay'}
                      </>
                    )}
                  </button>
                </div>
              </div>
            );
          })
        )}
      </div>
    </section>
  );
}
