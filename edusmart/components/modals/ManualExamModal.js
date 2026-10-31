'use client';
/**
 * components/modals/ManualExamModal.js — Tạo đề thi THỦ CÔNG (không dùng AI)
 *
 * Người dùng tự nhập: tiêu đề, môn, thời gian, và từng câu hỏi trắc nghiệm
 * (4 lựa chọn + chọn đáp án đúng). Gửi lên POST /api/exams — đề xuất hiện
 * ngay trong kho bài kiểm tra và được chấm tự động như mọi bài thi khác.
 */
import { useRef, useState } from 'react';
import { examService } from '@/lib/services/apiServices';

const emptyQuestion = () => ({ content: '', options: ['', '', '', ''], correctIndex: 0, explanation: '' });

export default function ManualExamModal({ isOpen, onClose, onCreated }) {
  const [form, setForm] = useState({ title: '', category: '', durationMinutes: 15, description: '' });
  const [questions, setQuestions] = useState([emptyQuestion()]);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const scrollRef = useRef(null);

  if (!isOpen) return null;

  const setQuestion = (idx, patch) =>
    setQuestions((prev) => prev.map((q, i) => (i === idx ? { ...q, ...patch } : q)));

  const setOption = (qIdx, oIdx, value) =>
    setQuestions((prev) => prev.map((q, i) => {
      if (i !== qIdx) return q;
      const options = q.options.map((o, j) => (j === oIdx ? value : o));
      return { ...q, options };
    }));

  const addQuestion = () => {
    if (questions.length >= 100) return;
    setQuestions((prev) => [...prev, emptyQuestion()]);
    setTimeout(() => scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: 'smooth' }), 50);
  };

  const removeQuestion = (idx) => {
    if (questions.length <= 1) return;
    setQuestions((prev) => prev.filter((_, i) => i !== idx));
  };

  const handleClose = () => {
    if (saving) return;
    setError('');
    onClose();
  };

  // Kiểm tra dữ liệu trước khi gửi — trả về thông báo lỗi đầu tiên (nếu có)
  const validate = () => {
    if (!form.title.trim()) return 'Vui lòng nhập tiêu đề đề thi';
    const duration = parseInt(form.durationMinutes, 10);
    if (!duration || duration < 1 || duration > 180) return 'Thời gian làm bài phải từ 1 đến 180 phút';
    for (let i = 0; i < questions.length; i++) {
      const q = questions[i];
      if (!q.content.trim()) return `Câu ${i + 1}: chưa nhập nội dung câu hỏi`;
      const filled = q.options.filter((o) => o.trim());
      if (filled.length < 2) return `Câu ${i + 1}: cần ít nhất 2 lựa chọn có nội dung`;
      if (!q.options[q.correctIndex]?.trim()) return `Câu ${i + 1}: đáp án đúng đang trỏ vào lựa chọn trống`;
    }
    return '';
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const validation = validate();
    if (validation) {
      setError(validation);
      return;
    }
    setError('');
    setSaving(true);
    try {
      const payload = {
        title: form.title.trim(),
        durationMinutes: parseInt(form.durationMinutes, 10),
        status: 'PUBLISHED',
        questions: questions.map((q) => {
          // Giữ lại các lựa chọn có nội dung, đánh dấu đúng theo correctIndex
          const kept = [];
          q.options.forEach((o, i) => {
            if (o.trim()) kept.push({ content: o.trim(), isCorrect: i === q.correctIndex });
          });
          const body = {
            content: q.content.trim(),
            type: 'SINGLE_CHOICE',
            options: kept,
          };
          if (q.explanation.trim()) body.explanation = q.explanation.trim();
          return body;
        }),
      };
      if (form.category.trim()) payload.category = form.category.trim();
      if (form.description.trim()) payload.description = form.description.trim();

      const exam = await examService.create(payload);
      setForm({ title: '', category: '', durationMinutes: 15, description: '' });
      setQuestions([emptyQuestion()]);
      onCreated(exam);
    } catch (err) {
      setError(err.message || 'Không thể tạo đề thi');
      setSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-sm p-4">
      <div className="bg-white rounded-2xl shadow-xl max-w-2xl w-full max-h-[92vh] flex flex-col">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-slate-100 p-6 pb-4">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-indigo-600 flex items-center justify-center text-white text-lg shadow-lg shadow-indigo-500/30">
              <i className="fa-solid fa-pen-to-square"></i>
            </div>
            <div>
              <h3 className="font-bold text-lg text-slate-800">Tạo đề thi thủ công</h3>
              <p className="text-xs text-slate-400">Tự soạn câu hỏi trắc nghiệm — tối đa 100 câu</p>
            </div>
          </div>
          <button onClick={handleClose} className="text-slate-400 hover:text-slate-600" disabled={saving}>
            <i className="fa-solid fa-xmark text-lg"></i>
          </button>
        </div>

        <form onSubmit={handleSubmit} className="flex flex-col min-h-0 flex-1">
          <div ref={scrollRef} className="overflow-y-auto p-6 pt-4 space-y-5">
            {/* Thông tin đề */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="sm:col-span-2">
                <label className="block text-sm font-semibold text-slate-700 mb-1.5">Tiêu đề đề thi *</label>
                <input
                  type="text" required maxLength={200}
                  placeholder="Ví dụ: Kiểm tra 45 phút — Chương hình học"
                  value={form.title}
                  onChange={(e) => setForm({ ...form, title: e.target.value })}
                  className="w-full bg-slate-50 border border-slate-300 rounded-xl px-4 py-2.5 text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                />
              </div>
              <div>
                <label className="block text-sm font-semibold text-slate-700 mb-1.5">Môn học</label>
                <input
                  type="text" maxLength={50} placeholder="Ví dụ: Toán học"
                  value={form.category}
                  onChange={(e) => setForm({ ...form, category: e.target.value })}
                  className="w-full bg-slate-50 border border-slate-300 rounded-xl px-4 py-2.5 text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                />
              </div>
              <div>
                <label className="block text-sm font-semibold text-slate-700 mb-1.5">Thời gian làm bài (phút) *</label>
                <input
                  type="number" required min={1} max={180}
                  value={form.durationMinutes}
                  onChange={(e) => setForm({ ...form, durationMinutes: e.target.value })}
                  className="w-full bg-slate-50 border border-slate-300 rounded-xl px-4 py-2.5 text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                />
              </div>
            </div>

            {/* Danh sách câu hỏi */}
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <h4 className="font-bold text-sm text-slate-800">
                  Câu hỏi <span className="text-slate-400 font-normal">({questions.length}/100)</span>
                </h4>
                <button
                  type="button" onClick={addQuestion}
                  className="px-3 py-1.5 bg-indigo-50 hover:bg-indigo-100 text-indigo-600 text-xs font-semibold rounded-lg transition"
                >
                  <i className="fa-solid fa-plus mr-1"></i> Thêm câu hỏi
                </button>
              </div>

              {questions.map((q, qIdx) => (
                <div key={qIdx} className="p-4 rounded-xl bg-slate-50 border border-slate-200 space-y-3">
                  <div className="flex items-start justify-between gap-3">
                    <h5 className="font-bold text-xs text-indigo-600 pt-2 whitespace-nowrap">Câu {qIdx + 1}</h5>
                    {questions.length > 1 && (
                      <button
                        type="button" onClick={() => removeQuestion(qIdx)}
                        className="text-rose-400 hover:text-rose-600 text-xs px-2 py-1 rounded-lg hover:bg-rose-50 transition"
                        title="Xóa câu hỏi"
                      >
                        <i className="fa-solid fa-trash"></i>
                      </button>
                    )}
                  </div>

                  <textarea
                    rows={2} maxLength={2000} required
                    placeholder="Nội dung câu hỏi..."
                    value={q.content}
                    onChange={(e) => setQuestion(qIdx, { content: e.target.value })}
                    className="w-full bg-white border border-slate-300 rounded-xl px-4 py-2.5 text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none resize-y"
                  />

                  {/* 4 lựa chọn — bấm ô tròn để chọn đáp án đúng */}
                  <div className="space-y-2">
                    {q.options.map((opt, oIdx) => (
                      <div key={oIdx} className="flex items-center gap-2.5">
                        <input
                          type="radio"
                          name={`correct_q${qIdx}`}
                          checked={q.correctIndex === oIdx}
                          onChange={() => setQuestion(qIdx, { correctIndex: oIdx })}
                          className="text-emerald-600 focus:ring-emerald-500 shrink-0"
                          title="Chọn làm đáp án đúng"
                        />
                        <input
                          type="text" maxLength={500}
                          placeholder={`Lựa chọn ${String.fromCharCode(65 + oIdx)}${q.correctIndex === oIdx ? ' (đáp án đúng)' : ''}`}
                          value={opt}
                          onChange={(e) => setOption(qIdx, oIdx, e.target.value)}
                          className={`flex-1 bg-white border rounded-xl px-3.5 py-2 text-sm focus:ring-2 focus:outline-none transition ${
                            q.correctIndex === oIdx
                              ? 'border-emerald-300 bg-emerald-50/50 focus:ring-emerald-500'
                              : 'border-slate-300 focus:ring-indigo-500'
                          }`}
                        />
                      </div>
                    ))}
                    <p className="text-[11px] text-slate-400">
                      <i className="fa-solid fa-circle-check text-emerald-500 mr-1"></i>
                      Bấm ô tròn bên cạnh lựa chọn để đánh dấu đáp án đúng
                    </p>
                  </div>

                  <input
                    type="text" maxLength={1000}
                    placeholder="Giải thích đáp án (tùy chọn — hiện sau khi học sinh nộp bài)"
                    value={q.explanation}
                    onChange={(e) => setQuestion(qIdx, { explanation: e.target.value })}
                    className="w-full bg-white border border-slate-200 rounded-xl px-3.5 py-2 text-xs focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                  />
                </div>
              ))}
            </div>

            {error && (
              <div className="flex items-start gap-3 p-3.5 bg-red-50 border border-red-200 rounded-xl text-red-700 text-sm">
                <i className="fa-solid fa-circle-exclamation mt-0.5 shrink-0"></i>
                <span>{error}</span>
              </div>
            )}
          </div>

          {/* Actions — cố định đáy modal */}
          <div className="border-t border-slate-100 p-4 flex justify-end gap-3 bg-white rounded-b-2xl">
            <button
              type="button" onClick={handleClose} disabled={saving}
              className="px-5 py-2.5 bg-slate-100 hover:bg-slate-200 text-slate-600 font-semibold text-sm rounded-xl transition"
            >
              Hủy
            </button>
            <button
              type="submit" disabled={saving}
              className="px-6 py-2.5 bg-indigo-600 hover:bg-indigo-700 disabled:bg-indigo-400 text-white font-semibold text-sm rounded-xl transition shadow-md flex items-center gap-2"
            >
              {saving ? (
                <><i className="fa-solid fa-spinner animate-spin"></i> Đang lưu...</>
              ) : (
                <><i className="fa-solid fa-check"></i> Lưu đề thi ({questions.length} câu)</>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
