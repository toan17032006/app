'use client';
/**
 * components/modals/AiExamModal.js — Modal "Tạo bài kiểm tra bằng AI"
 *
 * Người dùng tải lên file tài liệu (PDF / DOC / DOCX / TXT), AI Agent đọc và:
 *   - Chế độ ĐỀ THI: tạo câu hỏi trắc nghiệm theo tài liệu, thời gian phòng thi
 *     TỰ KHỚP với thời gian làm bài ghi trong tài liệu (vd đề ghi 90 phút → phòng thi 90 phút);
 *     cũng có thể tự nhập thời gian.
 *   - Chế độ BÀI TẬP: câu hỏi trắc nghiệm + GIẢI THÍCH đáp án, không giới hạn giờ.
 *
 * Có thể chọn "lớp" (Năm nhất → Năm tư, 10→12) để AI điều chỉnh độ khó phù hợp.
 */
import { useRef, useState } from 'react';
import { aiService } from '@/lib/services/apiServices';

const GRADE_OPTIONS = [
  'Năm nhất', 'Năm hai', 'Năm ba', 'Năm tư',
  'Lớp 10', 'Lớp 11', 'Lớp 12',
];

export default function AiExamModal({ isOpen, onClose, onCreated }) {
  const [mode, setMode] = useState('EXAM');           // EXAM | HOMEWORK
  const [file, setFile] = useState(null);
  const [questionCount, setQuestionCount] = useState(10);
  const [autoDuration, setAutoDuration] = useState(true);
  const [durationMinutes, setDurationMinutes] = useState(15);
  const [grade, setGrade] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const fileInputRef = useRef(null);

  if (!isOpen) return null;

  const reset = () => {
    setFile(null);
    setError('');
    setLoading(false);
    setMode('EXAM');
    setQuestionCount(10);
    setAutoDuration(true);
    setGrade('');
  };

  const handleClose = () => {
    if (loading) return; // đang chờ AI thì không đóng
    reset();
    onClose();
  };

  const handlePickFile = (e) => {
    const f = e.target.files?.[0];
    if (!f) return;
    const ok = /\.(pdf|docx?|txt|md)$/i.test(f.name);
    if (!ok) {
      setError('Chỉ hỗ trợ file PDF, DOC, DOCX hoặc TXT');
      return;
    }
    if (f.size > 15 * 1024 * 1024) {
      setError('File tối đa 15MB');
      return;
    }
    setError('');
    setFile(f);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!file) {
      setError('Vui lòng chọn file tài liệu trước');
      return;
    }
    setError('');
    setLoading(true);
    try {
      const formData = new FormData();
      formData.append('file', file);
      formData.append('mode', mode);
      // Đề thi + "Theo tài liệu" → KHÔNG gửi questionCount, AI tạo đúng số câu như trong đề
      const showCountInput = !(mode === 'EXAM' && autoDuration);
      if (showCountInput) {
        const count = Math.min(100, Math.max(3, parseInt(questionCount, 10) || 10));
        formData.append('questionCount', String(count));
      }
      if (grade) formData.append('grade', grade);
      if (mode === 'EXAM' && !autoDuration) {
        formData.append('durationMinutes', String(durationMinutes));
      }

      const exam = await aiService.generateExam(formData);
      reset();
      onCreated(exam); // cha sẽ đóng modal + mở luôn phòng thi mới
    } catch (err) {
      setError(err.message || 'Không thể tạo bài kiểm tra');
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-sm p-4">
      <div className="bg-white rounded-2xl shadow-xl max-w-lg w-full max-h-[92vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-slate-100 p-6 pb-4">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-violet-500 to-fuchsia-500 flex items-center justify-center text-white text-lg shadow-lg shadow-fuchsia-500/30">
              <i className="fa-solid fa-wand-magic-sparkles"></i>
            </div>
            <div>
              <h3 className="font-bold text-lg text-slate-800">Tạo bài kiểm tra bằng AI</h3>
              <p className="text-xs text-slate-400">AI đọc tài liệu và tự ra đề trắc nghiệm</p>
            </div>
          </div>
          <button onClick={handleClose} className="text-slate-400 hover:text-slate-600" disabled={loading}>
            <i className="fa-solid fa-xmark text-lg"></i>
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 pt-4 space-y-5">
          {/* Chế độ */}
          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-2">Bạn muốn tạo gì?</label>
            <div className="grid grid-cols-2 gap-3">
              <button
                type="button"
                onClick={() => setMode('EXAM')}
                className={`p-3.5 rounded-xl border-2 text-left transition ${
                  mode === 'EXAM'
                    ? 'border-indigo-500 bg-indigo-50'
                    : 'border-slate-200 hover:border-slate-300'
                }`}
              >
                <span className="block text-sm font-bold text-slate-800">
                  <i className="fa-solid fa-file-pen text-indigo-600 mr-1.5"></i>Đề thi
                </span>
                <span className="block text-xs text-slate-500 mt-1">
                  Phòng thi có đồng hồ — thời gian tự khớp tài liệu
                </span>
              </button>
              <button
                type="button"
                onClick={() => setMode('HOMEWORK')}
                className={`p-3.5 rounded-xl border-2 text-left transition ${
                  mode === 'HOMEWORK'
                    ? 'border-indigo-500 bg-indigo-50'
                    : 'border-slate-200 hover:border-slate-300'
                }`}
              >
                <span className="block text-sm font-bold text-slate-800">
                  <i className="fa-solid fa-book-open text-emerald-600 mr-1.5"></i>Bài tập
                </span>
                <span className="block text-xs text-slate-500 mt-1">
                  Luyện tập không giới hạn giờ, có giải thích đáp án
                </span>
              </button>
            </div>
          </div>

          {/* Chọn file */}
          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-2">Tài liệu nguồn</label>
            <div
              onClick={() => fileInputRef.current?.click()}
              className={`cursor-pointer rounded-xl border-2 border-dashed p-5 text-center transition ${
                file ? 'border-emerald-300 bg-emerald-50/50' : 'border-slate-300 hover:border-indigo-400 hover:bg-slate-50'
              }`}
            >
              <input
                ref={fileInputRef}
                type="file"
                accept=".pdf,.doc,.docx,.txt,.md"
                className="hidden"
                onChange={handlePickFile}
              />
              {file ? (
                <>
                  <i className="fa-solid fa-file-circle-check text-2xl text-emerald-500"></i>
                  <p className="text-sm font-semibold text-slate-700 mt-2 truncate max-w-xs mx-auto">{file.name}</p>
                  <p className="text-xs text-slate-400">{(file.size / 1024 / 1024).toFixed(2)} MB — bấm để đổi file khác</p>
                </>
              ) : (
                <>
                  <i className="fa-solid fa-cloud-arrow-up text-2xl text-slate-400"></i>
                  <p className="text-sm font-medium text-slate-600 mt-2">Bấm để chọn file</p>
                  <p className="text-xs text-slate-400">PDF, DOC, DOCX, TXT — tối đa 15MB</p>
                </>
              )}
            </div>
          </div>

          {/* Số câu hỏi + lớp — chế độ Đề thi + "Theo tài liệu" thì AI tự theo đề */}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-semibold text-slate-700 mb-2">Số câu hỏi</label>
              {mode === 'EXAM' && autoDuration ? (
                <div className="w-full bg-indigo-50/60 border border-indigo-200 rounded-xl px-4 py-2.5 text-sm text-indigo-700 font-medium">
                  <i className="fa-solid fa-wand-magic-sparkles mr-1.5"></i>Theo đề thi trong tài liệu
                </div>
              ) : (
                <input
                  type="number" min={3} max={100} value={questionCount}
                  onChange={(e) => setQuestionCount(e.target.value)}
                  className="w-full bg-slate-50 border border-slate-300 rounded-xl px-4 py-2.5 text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                />
              )}
            </div>
            <div>
              <label className="block text-sm font-semibold text-slate-700 mb-2">Đối tượng (lớp)</label>
              <select
                value={grade}
                onChange={(e) => setGrade(e.target.value)}
                className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3 py-2.5 text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none text-slate-700"
              >
                <option value="">— Tự động —</option>
                {GRADE_OPTIONS.map((g) => (
                  <option key={g} value={g}>{g}</option>
                ))}
              </select>
            </div>
          </div>

          {/* Thời gian — chỉ cho chế độ Đề thi */}
          {mode === 'EXAM' && (
            <div>
              <label className="block text-sm font-semibold text-slate-700 mb-2">
                Thời gian làm bài
              </label>
              <div className="flex items-center gap-3">
                <label className="flex items-center gap-2 text-sm text-slate-600 cursor-pointer">
                  <input
                    type="radio" checked={autoDuration}
                    onChange={() => setAutoDuration(true)}
                    className="text-indigo-600 focus:ring-indigo-500"
                  />
                  Theo tài liệu
                </label>
                <label className="flex items-center gap-2 text-sm text-slate-600 cursor-pointer">
                  <input
                    type="radio" checked={!autoDuration}
                    onChange={() => setAutoDuration(false)}
                    className="text-indigo-600 focus:ring-indigo-500"
                  />
                  Tự chọn
                </label>
                {!autoDuration && (
                  <div className="flex items-center gap-1.5 flex-1">
                    <input
                      type="number" min={5} max={300} value={durationMinutes}
                      onChange={(e) => setDurationMinutes(e.target.value)}
                      className="w-20 bg-slate-50 border border-slate-300 rounded-lg px-3 py-1.5 text-sm focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                    />
                    <span className="text-xs text-slate-500">phút</span>
                  </div>
                )}
              </div>
              {autoDuration && (
                <p className="text-xs text-slate-400 mt-1.5">
                  <i className="fa-solid fa-circle-info mr-1"></i>
                  AI sẽ tự dùng đúng thời gian làm bài ghi trong tài liệu (nếu có)
                </p>
              )}
            </div>
          )}

          {/* Lỗi */}
          {error && (
            <div className="flex items-start gap-3 p-3.5 bg-red-50 border border-red-200 rounded-xl text-red-700 text-sm">
              <i className="fa-solid fa-circle-exclamation mt-0.5 shrink-0"></i>
              <span>{error}</span>
            </div>
          )}

          {/* Actions */}
          <div className="flex justify-end gap-3 pt-1">
            <button
              type="button" onClick={handleClose} disabled={loading}
              className="px-5 py-2.5 bg-slate-100 hover:bg-slate-200 text-slate-600 font-semibold text-sm rounded-xl transition"
            >
              Hủy
            </button>
            <button
              type="submit" disabled={loading || !file}
              className="px-5 py-2.5 bg-gradient-to-r from-violet-600 to-fuchsia-600 hover:from-violet-700 hover:to-fuchsia-700 disabled:from-slate-300 disabled:to-slate-300 text-white font-semibold text-sm rounded-xl transition shadow-md flex items-center gap-2"
            >
              {loading ? (
                <>
                  <i className="fa-solid fa-spinner animate-spin"></i>
                  AI đang đọc tài liệu...
                </>
              ) : (
                <>
                  <i className="fa-solid fa-wand-magic-sparkles"></i>
                  Tạo bằng AI
                </>
              )}
            </button>
          </div>

          {loading && (
            <p className="text-xs text-center text-slate-400 -mt-2">
              AI đang đọc và ra đề — có thể mất 30 giây đến vài phút tùy độ dài tài liệu và tải của nhà cung cấp. Đừng đóng cửa sổ.
            </p>
          )}
        </form>
      </div>
    </div>
  );
}
