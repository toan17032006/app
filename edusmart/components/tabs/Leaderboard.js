'use client';
import { useState, useEffect } from 'react';
import { leaderboardService } from '@/lib/services/apiServices';
import { avatarFor, gradeDisplay } from '@/lib/constants';

export default function Leaderboard({ userId }) {
  const [period, setPeriod] = useState('all');
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(true);

  // period thay đổi → fetch lại từ backend (week = 7 ngày gần nhất, month = đầu tháng)
  useEffect(() => {
    setLoading(true);
    leaderboardService.getTop(20, period)
      .then(setData)
      .catch(() => setData([]))
      .finally(() => setLoading(false));
  }, [period]);

  const periodText = {
    all: 'Toàn bộ xu tích lũy và GPA của học sinh.',
    week: 'Số xu kiếm được trong 7 ngày gần nhất (làm bài thi + nộp bài tập).',
    month: 'Số xu kiếm được từ đầu tháng đến nay (làm bài thi + nộp bài tập).',
  };

  const podiumOrder = data.length >= 3 ? [data[1], data[0], data[2]] : data;

  const avatarSrc = (user) => user?.avatarUrl || avatarFor(user?.fullName);
  const isMe = (user) => user?.isCurrentUser || user?.userId === userId;

  const podiumCard = (user, place) => {
    if (!user) {
      return (
        <div className="bg-slate-50 border border-dashed border-slate-200 rounded-2xl p-4 text-slate-300 text-xs">
          Chưa có dữ liệu
        </div>
      );
    }
    const styles = {
      1: 'bg-gradient-to-b from-amber-50 to-amber-100/50 border-amber-200 shadow-sm',
      2: 'bg-slate-50 border-slate-200 mt-6',
      3: 'bg-slate-50 border-slate-200 mt-8',
    };
    const badge = {
      1: 'absolute -top-6 w-12 h-12 bg-amber-500 text-white font-bold rounded-full flex items-center justify-center text-lg border-4 border-white shadow-lg',
      2: 'absolute -top-5 w-10 h-10 bg-slate-300 text-slate-700 font-bold rounded-full flex items-center justify-center border-4 border-white shadow-md',
      3: 'absolute -top-5 w-10 h-10 bg-amber-700/80 text-white font-bold rounded-full flex items-center justify-center border-4 border-white shadow-md',
    };
    const avatarBorder = {
      1: 'border-2 border-amber-400',
      2: 'border-2 border-slate-300',
      3: 'border-2 border-amber-700/50',
    };
    return (
      <div className={`${styles[place]} rounded-2xl p-4 flex flex-col items-center justify-end relative text-center`}>
        <div className={badge[place]}>{place === 1 ? <i className="fa-solid fa-crown"></i> : place}</div>
        <img
          src={avatarSrc(user)}
          className={`w-16 h-16 rounded-full object-cover mb-2 ${avatarBorder[place]}`}
          alt={user.fullName}
        />
        <h4 className="font-bold text-sm text-slate-900 truncate w-full">
          {user.fullName}
          {isMe(user) && <span className="ml-1 text-[10px] bg-indigo-50 text-indigo-600 px-1.5 py-0.5 rounded font-medium">Bạn</span>}
        </h4>
        <span className="text-xs text-slate-600">GPA: {user.gpa ?? '—'}</span>
        <span className="text-xs font-semibold text-indigo-600 mt-1">
          {user.coins?.toLocaleString()} xu{place === 1 && isMe(user) ? ' 🎉' : ''}
        </span>
      </div>
    );
  };

  if (loading) return (
    <div className="flex items-center justify-center h-64">
      <i className="fa-solid fa-spinner animate-spin text-2xl text-indigo-500"></i>
    </div>
  );

  return (
    <section className="fade-in">
      <div className="bg-white p-6 sm:p-8 rounded-2xl border border-slate-200 shadow-xs max-w-4xl mx-auto space-y-6">
        {/* Header */}
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 border-b border-slate-100 pb-4">
          <div>
            <h2 className="text-xl font-bold text-slate-800">
              <i className="fa-solid fa-trophy text-amber-500 mr-2"></i>Bảng xếp hạng học tập
            </h2>
            <p className="text-sm text-slate-500 mt-1">{periodText[period]}</p>
          </div>
          <div className="inline-flex bg-slate-100 p-1 rounded-xl">
            {[
              { id: 'week', label: 'Tuần này' },
              { id: 'month', label: 'Tháng này' },
              { id: 'all', label: 'Tổng' },
            ].map((p) => (
              <button
                key={p.id}
                onClick={() => setPeriod(p.id)}
                className={`px-4 py-2 rounded-lg text-xs font-semibold transition ${
                  period === p.id
                    ? 'bg-white text-indigo-600 shadow-sm'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                {p.label}
              </button>
            ))}
          </div>
        </div>

        {/* Podium top 3: [2nd, 1st, 3rd] */}
        <div className="grid grid-cols-3 gap-4 pt-2 pb-6 text-center">
          {podiumCard(podiumOrder[0], 2)}
          {podiumCard(podiumOrder[1], 1)}
          {podiumCard(podiumOrder[2], 3)}
        </div>

        {/* Full leaderboard table */}
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-slate-200 text-xs font-semibold text-slate-400 uppercase tracking-wider">
                <th className="py-3 px-4">Hạng</th>
                <th className="py-3 px-4">Học sinh</th>
                <th className="py-3 px-4">Lớp</th>
                <th className="py-3 px-4 text-center">GPA</th>
                <th className="py-3 px-4 text-right">Điểm {period === 'all' ? 'tích lũy' : 'trong kỳ'}</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 text-sm">
              {data.length === 0 && (
                <tr>
                  <td colSpan={5} className="py-8 text-center text-slate-400">
                    Chưa có dữ liệu xếp hạng cho kỳ này.
                  </td>
                </tr>
              )}
              {data.map((user) => (
                <tr key={user.rank} className={`hover:bg-slate-50 transition ${isMe(user) ? 'bg-indigo-50/50' : ''}`}>
                  <td className={`py-3 px-4 font-bold ${user.rank <= 3 ? 'text-amber-500' : 'text-slate-600'}`}>
                    #{user.rank}
                  </td>
                  <td className="py-3 px-4 font-semibold text-slate-800">
                    <div className="flex items-center gap-2.5">
                      <img
                        src={avatarSrc(user)}
                        className="w-8 h-8 rounded-full object-cover"
                        alt={user.fullName}
                      />
                      <span className="truncate max-w-48">{user.fullName}</span>
                      {isMe(user) && (
                        <span className="text-[10px] bg-indigo-50 text-indigo-600 px-2 py-0.5 rounded font-medium">
                          Bạn
                        </span>
                      )}
                    </div>
                  </td>
                  <td className="py-3 px-4 text-slate-500">{gradeDisplay(user.grade)}</td>
                  <td className="py-3 px-4 text-center font-bold text-emerald-600">{user.gpa ?? '—'}</td>
                  <td className="py-3 px-4 text-right font-bold text-indigo-600">
                    {user.coins?.toLocaleString()} xu
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </section>
  );
}
