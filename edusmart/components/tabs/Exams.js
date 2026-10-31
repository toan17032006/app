'use client';
import { useState, useEffect, useRef, useCallback } from 'react';
import { examService } from '@/lib/services/apiServices';
import AiExamModal from '@/components/modals/AiExamModal';

// ─── Exam List View ──────────────────────────────────────────────────────────
function ExamList({ onStart, loadingStart, refreshKey, onOpenAiModal }) {
  const [filter, setFilter] = useState('all');
  const [examsData, setExamsData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');

  useEffect(() => {
    examService.getAll()
      .then(setExamsData)
      .catch((err) => setLoadError(err.message || 'Không thể tải danh sách bài kiểm tra'))
      .finally(() => setLoading(false));
  }, [refreshKey]);

  const filtered =
    filter === 'all' ? examsData : examsData.filter((e) => e.category === filter);

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-slate-800">
            <i className="fa-solid fa-file-pen text-indigo-600 mr-2"></i>Kho bài kiểm tra trực tuyến
          </h2>
          <p className="text-sm text-slate-500 mt-1">
            Chọn một bài kiểm tra bên dưới để bắt đầu làm bài. Có đồng hồ đếm ngược và chấm điểm tự động.
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          {/* Nút tạo đề bằng AI */}
          <button
            onClick={onOpenAiModal}
            className="flex items-center gap-2 px-4 py-2 bg-gradient-to-r from-violet-600 to-fuchsia-600 hover:from-violet-700 hover:to-fuchsia-700 text-white text-sm font-semibold rounded-xl shadow-md transition"
          >
            <i className="fa-solid fa-wand-magic-sparkles"></i> Tạo bằng AI
          </button>
          <div className="flex items-center gap-2 bg-white border border-slate-200 px-3 py-2 rounded-xl shadow-xs">
            <i className="fa-solid fa-filter text-slate-400"></i>
            <select
              value={filter}
              onChange={(e) => setFilter(e.target.value)}
              className="bg-transparent text-sm font-medium focus:outline-none text-slate-700"
            >
              <option value="all">Tất cả môn học</option>
              {examsData
                .map((e) => e.category)
                .filter((c, i, arr) => c && arr.indexOf(c) === i)
                .map((c) => (
                  <option key={c} value={c}>{c}</option>
                ))}
            </select>
          </div>
        </div>
      </div>

      {loadError && (
        <div className="p-4 bg-red-50 border border-red-200 rounded-xl text-red-700 text-sm">
          <i className="fa-solid fa-circle-exclamation mr-2"></i>{loadError}
        </div>
      )}
      {loading && (
        <div className="flex items-center justify-center h-40">
          <i className="fa-solid fa-spinner animate-spin text-2xl text-indigo-500"></i>
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {filtered.map((exam) => (
          <div
            key={exam.id}
            className="bg-white p-6 rounded-2xl border border-slate-200 shadow-xs flex flex-col justify-between hover:shadow-md transition"
          >
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold uppercase px-2.5 py-1 bg-indigo-50 text-indigo-600 rounded-md">
                  {exam.category}
                </span>
                <span className="text-xs text-slate-400 font-medium">
                  <i className="fa-solid fa-clock mr-1"></i>
                  {exam.durationMinutes ? `${exam.durationMinutes} phút` : 'Không giới hạn'}
                </span>
              </div>
              <h3 className="font-bold text-base text-slate-800">{exam.title}</h3>
              <p className="text-xs text-slate-500">
                Số lượng câu hỏi:{' '}
                <strong className="text-slate-700">{exam.questionsCount} câu trắc nghiệm</strong>
              </p>
            </div>
            <div className="pt-6 mt-4 border-t border-slate-100 flex items-center justify-between">
              <span className="text-xs text-emerald-600 font-semibold bg-emerald-50 px-2 py-1 rounded">
                +15 xu / câu đúng
              </span>
              <button
                onClick={() => onStart(exam.id)}
                disabled={!!loadingStart}
                className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 disabled:bg-indigo-400 text-white text-xs font-semibold rounded-xl transition shadow-sm flex items-center gap-2"
              >
                {loadingStart === exam.id ? (
                  <><i className="fa-solid fa-spinner animate-spin"></i> Đang mở...</>
                ) : (
                  <>Vào làm bài <i className="fa-solid fa-arrow-right"></i></>
                )}
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

// ─── Exam Room View ───────────────────────────────────────────────────────────
// questions: [{ id, content, options: [string] }] — dữ liệu thật từ backend
function ExamRoom({ exam, noLimit, remainingSeconds, onAnswer, onSubmit, submitting, onQuit }) {
  const timerRef = useRef(null);

  useEffect(() => {
    if (remainingSeconds > 0 && timerRef.current === null) {
      // Đếm ngược do component cha quản lý (xem useEffect dưới của component cha)
    }
    return () => {};
  }, [remainingSeconds]);

  const answeredCount = Object.keys(exam.answers || {}).length;

  return (
    <div className="space-y-6">
      {/* Sticky header */}
      <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-xs flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 sticky top-4 z-20">
        <div>
          <span className="text-xs font-semibold uppercase px-2.5 py-1 bg-indigo-50 text-indigo-600 rounded-md">
            {exam.category}
          </span>
          <h3 className="text-lg font-bold text-slate-800 mt-1">{exam.title}</h3>
          <p className="text-xs text-slate-400 mt-1">
            Đã trả lời: <strong>{answeredCount}/{exam.questions.length}</strong> câu
          </p>
        </div>
        <div className="flex items-center gap-4">
          <div className={`px-4 py-2 rounded-xl flex items-center gap-2 font-bold border ${
            noLimit
              ? 'bg-emerald-50 border-emerald-200 text-emerald-600'
              : 'bg-rose-50 border-rose-200 text-rose-600'
          }`}>
            <i className="fa-solid fa-clock"></i>
            <span>{noLimit ? 'Không giới hạn' : `${exam.minutes}:${exam.seconds}`}</span>
          </div>
          <button
            onClick={onQuit}
            className="px-4 py-2 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-600 text-sm font-semibold transition"
          >
            Thoát
          </button>
        </div>
      </div>

      {/* Questions */}
      <div className="bg-white p-6 sm:p-8 rounded-2xl border border-slate-200 shadow-xs max-w-4xl mx-auto space-y-8">
        <div className="space-y-8">
          {exam.questions.map((q, qIndex) => (
            <div key={q.id} className="space-y-3 p-4 rounded-xl bg-slate-50 border border-slate-200">
              <h4 className="font-bold text-sm text-slate-800">
                Câu {qIndex + 1}: {q.content}
              </h4>
              <div className="space-y-2 pt-2">
                {q.options.map((opt, optIndex) => (
                  <label
                    key={optIndex}
                    className="flex items-center gap-3 p-3 rounded-xl bg-white border border-slate-200 hover:border-indigo-300 cursor-pointer transition text-sm text-slate-700"
                  >
                    <input
                      type="radio"
                      name={`question_${q.id}`}
                      value={optIndex}
                      checked={exam.answers[qIndex] === optIndex}
                      onChange={() => onAnswer(qIndex, optIndex)}
                      className="text-indigo-600 focus:ring-indigo-500"
                    />
                    <span>{opt}</span>
                  </label>
                ))}
              </div>
            </div>
          ))}
        </div>
        <div className="pt-6 border-t border-slate-100 flex justify-end">
          <button
            onClick={() => onSubmit(false)}
            disabled={submitting}
            className="px-8 py-3.5 bg-emerald-600 hover:bg-emerald-700 disabled:bg-emerald-400 text-white font-semibold rounded-xl shadow-md shadow-emerald-600/30 transition flex items-center gap-2"
          >
            {submitting ? (
              <><i className="fa-solid fa-spinner animate-spin"></i> Đang nộp bài...</>
            ) : (
              <><i className="fa-solid fa-paper-plane"></i> Nộp bài kiểm tra</>
            )}
          </button>
        </div>
      </div>
    </div>
  );
}

// ─── Exam Result View — kết quả thật từ backend (đã lưu vào lịch sử) ─────────
function ExamResult({ result, onBack }) {
  const [showReview, setShowReview] = useState(false);

  return (
    <div className="space-y-6 max-w-2xl mx-auto">
      <div className="bg-white p-8 rounded-2xl border border-slate-200 shadow-lg text-center space-y-6">
        <div className="w-20 h-20 bg-emerald-100 text-emerald-600 rounded-full flex items-center justify-center text-4xl mx-auto shadow-inner">
          <i className="fa-solid fa-award"></i>
        </div>
        <div>
          <h2 className="text-2xl font-bold text-slate-800">Hoàn thành bài kiểm tra!</h2>
          <p className="text-sm text-slate-500 mt-1">
            Kết quả đã được lưu vào lịch sử làm bài của bạn.
          </p>
        </div>

        <div className="bg-slate-50 p-6 rounded-2xl border border-slate-100 grid grid-cols-3 gap-4">
          <div>
            <p className="text-xs text-slate-400 font-medium">Số điểm</p>
            <h3 className="text-2xl font-bold text-indigo-600 mt-1">{result.score}/10</h3>
          </div>
          <div>
            <p className="text-xs text-slate-400 font-medium">Số câu đúng</p>
            <h3 className="text-2xl font-bold text-emerald-600 mt-1">{result.correctCount}/{result.totalCount}</h3>
          </div>
          <div>
            <p className="text-xs text-slate-400 font-medium">Xu thưởng</p>
            <h3 className="text-2xl font-bold text-amber-500 mt-1">+{result.coinsEarned} xu</h3>
          </div>
        </div>

        {/* Chi tiết từng câu */}
        {result.answers?.length > 0 && (
          <div className="text-left">
            <button
              onClick={() => setShowReview((v) => !v)}
              className="w-full flex items-center justify-between px-4 py-3 bg-slate-50 hover:bg-slate-100 border border-slate-200 rounded-xl text-sm font-semibold text-slate-700 transition"
            >
              <span><i className="fa-solid fa-list-check mr-2"></i>Xem chi tiết từng câu</span>
              <i className={`fa-solid fa-chevron-${showReview ? 'up' : 'down'}`}></i>
            </button>
            {showReview && (
              <div className="mt-3 space-y-2">
                {result.answers.map((a, i) => (
                  <div
                    key={a.questionId}
                    className={`p-3.5 rounded-xl border text-sm ${
                      a.isCorrect ? 'bg-emerald-50 border-emerald-200' : 'bg-rose-50 border-rose-200'
                    }`}
                  >
                    <p className="font-semibold text-slate-800">
                      <i className={`fa-solid ${a.isCorrect ? 'fa-circle-check text-emerald-500' : 'fa-circle-xmark text-rose-500'} mr-2`}></i>
                      Câu {i + 1}: {a.questionText}
                    </p>
                    <p className="text-xs text-slate-500 mt-1">
                      {a.selectedIndex != null ? `Bạn chọn: ${a.selectedIndex + 1}` : 'Bạn bỏ qua câu này'}
                      {!a.isCorrect && ` — Đáp án đúng: ${a.correctIndex + 1}`}
                    </p>
                    {a.explanation && (
                      <p className="text-xs text-slate-600 bg-white/70 border border-slate-200 rounded-lg p-2.5 mt-2">
                        <i className="fa-solid fa-lightbulb text-amber-500 mr-1.5"></i>
                        {a.explanation}
                      </p>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        <div className="flex justify-center gap-4 pt-2">
          <button
            onClick={onBack}
            className="px-6 py-3 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold rounded-xl transition shadow-md"
          >
            <i className="fa-solid fa-arrow-left mr-2"></i>Quay lại danh sách bài kiểm tra
          </button>
        </div>
      </div>
    </div>
  );
}

// ─── Main Exams Tab ───────────────────────────────────────────────────────────
export default function Exams({ userId, onCoinsEarned }) {
  const [view, setView] = useState('list'); // 'list' | 'room' | 'result'
  const [room, setRoom] = useState(null);   // dữ liệu phòng thi từ backend
  const [result, setResult] = useState(null);
  const [loadingStart, setLoadingStart] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [aiModalOpen, setAiModalOpen] = useState(false);
  const [refreshKey, setRefreshKey] = useState(0); // tăng lên để ExamList tải lại danh sách

  // AI tạo xong → đóng modal, tải lại danh sách và mở ngay phòng thi mới
  const handleAiCreated = async (exam) => {
    setAiModalOpen(false);
    setRefreshKey((k) => k + 1);
    if (exam?.id) await handleStart(exam.id);
  };

  // Đồng hồ đếm ngược — quản lý ở component cha để submit timeout gọi được
  const [remainingSeconds, setRemainingSeconds] = useState(0);
  const submitRef = useRef(null);

  const noLimit = !!room && (!room.durationMinutes || room.durationMinutes <= 0);

  useEffect(() => {
    if (view !== 'room' || !room || noLimit) return;
    const timer = setInterval(() => {
      setRemainingSeconds((prev) => {
        if (prev <= 1) {
          clearInterval(timer);
          submitRef.current?.(true); // hết giờ → tự động nộp với timedOut=true
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
    return () => clearInterval(timer);
  }, [view, room, noLimit]);

  const handleStart = async (examId) => {
    setError('');
    setLoadingStart(examId);
    try {
      const detail = await examService.start(userId, examId);
      setRoom({
        examId,
        attemptId: detail.attemptId,
        title: detail.title,
        category: detail.category,
        durationMinutes: detail.durationMinutes,
        questions: detail.questions.map((q) => ({
          id: q.id,
          content: q.content,
          options: (q.options || []).map((o) => o.content),
        })),
        answers: {},
      });
      setRemainingSeconds(
        detail.durationMinutes && detail.durationMinutes > 0 ? detail.durationMinutes * 60 : 0
      );
      setView('room');
    } catch (err) {
      setError(err.message || 'Không thể bắt đầu bài thi');
    } finally {
      setLoadingStart(null);
    }
  };

  const handleQuit = () => {
    setRoom(null);
    setView('list');
  };

  // Nộp bài lên backend — backend chấm điểm, cộng xu, lưu lịch sử
  const handleSubmit = async (timedOut) => {
    if (!room || submitting) return;
    setSubmitting(true);
    setError('');
    try {
      const payload = {
        attemptId: room.attemptId,
        answers: room.questions.map((q, i) => ({
          questionId: q.id,
          selectedOptionIndex: room.answers[i] ?? null,
        })),
      };
      const data = await examService.submit(userId, payload, timedOut);
      onCoinsEarned(data.coinsEarned || 0);
      setResult(data);
      setRoom(null);
      setView('result');
    } catch (err) {
      setError(err.message || 'Không thể nộp bài');
      // Nộp lỗi (VD: đã nộp rồi) → về danh sách
      setRoom(null);
      setView('list');
    } finally {
      setSubmitting(false);
    }
  };

  submitRef.current = handleSubmit;

  const handleBack = () => {
    setResult(null);
    setView('list');
  };

  const minutes = noLimit ? '00' : String(Math.floor(remainingSeconds / 60)).padStart(2, '0');
  const seconds = noLimit ? '00' : String(remainingSeconds % 60).padStart(2, '0');

  return (
    <section className="fade-in">
      {error && view !== 'result' && (
        <div className="mb-4 p-4 bg-red-50 border border-red-200 rounded-xl text-red-700 text-sm">
          <i className="fa-solid fa-circle-exclamation mr-2"></i>{error}
        </div>
      )}
      {view === 'list' && (
        <ExamList
          onStart={handleStart}
          loadingStart={loadingStart}
          refreshKey={refreshKey}
          onOpenAiModal={() => setAiModalOpen(true)}
        />
      )}
      {view === 'room' && room && (
        <ExamRoom
          exam={{ ...room, minutes, seconds }}
          noLimit={noLimit}
          remainingSeconds={remainingSeconds}
          onAnswer={(qIndex, optIndex) =>
            setRoom((prev) => ({ ...prev, answers: { ...prev.answers, [qIndex]: optIndex } }))
          }
          onSubmit={handleSubmit}
          submitting={submitting}
          onQuit={handleQuit}
        />
      )}
      {view === 'result' && result && (
        <ExamResult result={result} onBack={handleBack} />
      )}

      <AiExamModal
        isOpen={aiModalOpen}
        onClose={() => setAiModalOpen(false)}
        onCreated={handleAiCreated}
      />
    </section>
  );
}
