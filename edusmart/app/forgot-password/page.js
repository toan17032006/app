'use client';
/**
 * Trang Quên mật khẩu / Đặt lại mật khẩu.
 *
 * Bước 1: Nhập email → POST /api/auth/forgot-password
 * Bước 2: Vì hệ thống chưa cấu hình mail server, backend trả về resetToken
 *         ngay trong response → trang tự tạo link "Đặt lại mật khẩu".
 *         (Khi đã cấu hình mail: người dùng nhận link qua email.)
 * Bước 3: Form mật khẩu mới → POST /api/auth/reset-password → đăng nhập lại.
 */
import { useState } from 'react';
import Link from 'next/link';
import { authService } from '@/lib/services/authService';

const PASSWORD_RULES = [
  { label: 'Ít nhất 8 ký tự', test: (p) => p.length >= 8 },
  { label: 'Có chữ hoa (A–Z)', test: (p) => /[A-Z]/.test(p) },
  { label: 'Có chữ số (0–9)', test: (p) => /[0-9]/.test(p) },
];

export default function ForgotPasswordPage() {
  const [step, setStep] = useState('email'); // email | reset | done
  const [email, setEmail] = useState('');
  const [sending, setSending] = useState(false);
  const [info, setInfo] = useState('');
  const [error, setError] = useState('');

  const [token, setToken] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [resetting, setResetting] = useState(false);

  const allRulesPassed = PASSWORD_RULES.every((r) => r.test(newPassword));
  const passwordMismatch = confirmPassword && newPassword !== confirmPassword;

  // ── Bước 1: yêu cầu token ────────────────────────────────────────────────
  const handleRequest = async (e) => {
    e.preventDefault();
    setError(''); setInfo(''); setSending(true);
    try {
      const data = await authService.forgotPassword(email);
      const resetToken = data?.resetToken;
      if (resetToken) {
        setToken(resetToken);
        setInfo('Đã xác nhận email. Hãy đặt mật khẩu mới cho tài khoản của bạn bên dưới.');
        setStep('reset');
      } else {
        setInfo('Nếu email tồn tại trong hệ thống, bạn sẽ nhận được hướng dẫn đặt lại mật khẩu. Vui lòng kiểm tra email.');
        setStep('email');
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setSending(false);
    }
  };

  // ── Bước 3: đặt mật khẩu mới ─────────────────────────────────────────────
  const handleReset = async (e) => {
    e.preventDefault();
    setError(''); setResetting(true);
    try {
      await authService.resetPassword(token, newPassword, confirmPassword);
      setStep('done');
    } catch (err) {
      setError(err.message);
    } finally {
      setResetting(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-50 p-6">
      <div className="w-full max-w-md space-y-6">
        {/* Logo */}
        <div className="flex items-center justify-center gap-3">
          <div className="w-11 h-11 rounded-xl bg-gradient-to-tr from-indigo-500 to-violet-500 flex items-center justify-center text-white text-xl shadow-lg shadow-indigo-500/30">
            <i className="fa-solid fa-graduation-cap"></i>
          </div>
          <h1 className="font-bold text-xl text-slate-800 tracking-wide">EduSmart</h1>
        </div>

        <div className="bg-white p-8 rounded-2xl border border-slate-200 shadow-lg space-y-5">
          {/* ── Step: email ── */}
          {step === 'email' && (
            <>
              <div className="space-y-1.5">
                <div className="w-14 h-14 rounded-2xl bg-indigo-50 text-indigo-600 flex items-center justify-center text-2xl">
                  <i className="fa-solid fa-key"></i>
                </div>
                <h2 className="text-2xl font-bold text-slate-800 pt-2">Quên mật khẩu?</h2>
                <p className="text-slate-500 text-sm">
                  Nhập email đã đăng ký — chúng tôi sẽ tạo link đặt lại mật khẩu cho bạn.
                </p>
              </div>

              {error && (
                <div className="flex items-start gap-3 p-4 bg-red-50 border border-red-200 rounded-xl text-red-700 text-sm">
                  <i className="fa-solid fa-circle-exclamation mt-0.5 shrink-0"></i>
                  <span>{error}</span>
                </div>
              )}
              {info && (
                <div className="flex items-start gap-3 p-4 bg-emerald-50 border border-emerald-200 rounded-xl text-emerald-700 text-sm">
                  <i className="fa-solid fa-circle-check mt-0.5 shrink-0"></i>
                  <span>{info}</span>
                </div>
              )}

              <form onSubmit={handleRequest} className="space-y-4">
                <div className="space-y-1.5">
                  <label className="block text-sm font-semibold text-slate-700">Email</label>
                  <div className="relative">
                    <span className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400">
                      <i className="fa-solid fa-envelope text-sm"></i>
                    </span>
                    <input
                      type="email" required placeholder="vidu@email.com"
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      className="w-full pl-10 pr-4 py-3 bg-white border border-slate-300 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 focus:outline-none transition placeholder:text-slate-400"
                    />
                  </div>
                </div>
                <button
                  type="submit" disabled={sending}
                  className="w-full py-3.5 bg-indigo-600 hover:bg-indigo-700 disabled:bg-indigo-400 text-white font-semibold rounded-xl transition shadow-md shadow-indigo-600/30 flex items-center justify-center gap-2 text-sm"
                >
                  {sending ? (<><i className="fa-solid fa-spinner animate-spin"></i>Đang xử lý...</>) : 'Gửi yêu cầu đặt lại mật khẩu'}
                </button>
              </form>
            </>
          )}

          {/* ── Step: đặt mật khẩu mới ── */}
          {step === 'reset' && (
            <>
              <div className="space-y-1.5">
                <div className="w-14 h-14 rounded-2xl bg-emerald-50 text-emerald-600 flex items-center justify-center text-2xl">
                  <i className="fa-solid fa-shield-halved"></i>
                </div>
                <h2 className="text-2xl font-bold text-slate-800 pt-2">Đặt lại mật khẩu</h2>
                <p className="text-slate-500 text-sm">Token hết hạn sau 30 phút và chỉ dùng 1 lần.</p>
              </div>

              {error && (
                <div className="flex items-start gap-3 p-4 bg-red-50 border border-red-200 rounded-xl text-red-700 text-sm">
                  <i className="fa-solid fa-circle-exclamation mt-0.5 shrink-0"></i>
                  <span>{error}</span>
                </div>
              )}
              {info && (
                <div className="flex items-start gap-3 p-4 bg-emerald-50 border border-emerald-200 rounded-xl text-emerald-700 text-sm">
                  <i className="fa-solid fa-circle-check mt-0.5 shrink-0"></i>
                  <span>{info}</span>
                </div>
              )}

              <form onSubmit={handleReset} className="space-y-4">
                <div className="space-y-1.5">
                  <label className="block text-sm font-semibold text-slate-700">Mật khẩu mới</label>
                  <div className="relative">
                    <span className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400">
                      <i className="fa-solid fa-lock text-sm"></i>
                    </span>
                    <input
                      type={showPassword ? 'text' : 'password'} required
                      placeholder="Tối thiểu 8 ký tự"
                      value={newPassword}
                      onChange={(e) => setNewPassword(e.target.value)}
                      className="w-full pl-10 pr-11 py-3 bg-white border border-slate-300 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 focus:outline-none transition placeholder:text-slate-400"
                    />
                    <button
                      type="button" onClick={() => setShowPassword((v) => !v)}
                      className="absolute right-3.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 transition"
                    >
                      <i className={`fa-solid ${showPassword ? 'fa-eye-slash' : 'fa-eye'} text-sm`}></i>
                    </button>
                  </div>
                  {newPassword && (
                    <div className="space-y-1 pt-1">
                      {PASSWORD_RULES.map((rule) => {
                        const passed = rule.test(newPassword);
                        return (
                          <div key={rule.label} className="flex items-center gap-2 text-xs">
                            <i className={`fa-solid ${passed ? 'fa-circle-check text-emerald-500' : 'fa-circle-xmark text-slate-300'} text-sm`}></i>
                            <span className={passed ? 'text-emerald-600 font-medium' : 'text-slate-400'}>{rule.label}</span>
                          </div>
                        );
                      })}
                    </div>
                  )}
                </div>

                <div className="space-y-1.5">
                  <label className="block text-sm font-semibold text-slate-700">Xác nhận mật khẩu</label>
                  <input
                    type={showPassword ? 'text' : 'password'} required
                    placeholder="Nhập lại mật khẩu mới"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    className={`w-full px-4 py-3 bg-white border rounded-xl text-sm focus:ring-2 focus:outline-none transition placeholder:text-slate-400 ${
                      passwordMismatch
                        ? 'border-rose-400 focus:ring-rose-500'
                        : 'border-slate-300 focus:ring-indigo-500 focus:border-indigo-500'
                    }`}
                  />
                  {passwordMismatch && (
                    <p className="text-xs text-rose-500">Mật khẩu không khớp</p>
                  )}
                </div>

                <button
                  type="submit" disabled={resetting || !allRulesPassed || passwordMismatch}
                  className="w-full py-3.5 bg-indigo-600 hover:bg-indigo-700 disabled:bg-slate-300 disabled:cursor-not-allowed text-white font-semibold rounded-xl transition shadow-md shadow-indigo-600/30 flex items-center justify-center gap-2 text-sm"
                >
                  {resetting ? (<><i className="fa-solid fa-spinner animate-spin"></i>Đang lưu...</>) : 'Đặt lại mật khẩu'}
                </button>
              </form>
            </>
          )}

          {/* ── Step: done ── */}
          {step === 'done' && (
            <div className="text-center space-y-5">
              <div className="w-16 h-16 bg-emerald-100 text-emerald-600 rounded-full flex items-center justify-center text-3xl mx-auto">
                <i className="fa-solid fa-check"></i>
              </div>
              <div>
                <h2 className="text-2xl font-bold text-slate-800">Thành công! 🎉</h2>
                <p className="text-slate-500 text-sm mt-1">
                  Mật khẩu đã được đổi. Hãy đăng nhập bằng mật khẩu mới.
                </p>
              </div>
              <Link
                href="/login"
                className="inline-block px-6 py-3 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold rounded-xl transition shadow-md text-sm"
              >
                <i className="fa-solid fa-arrow-right-to-bracket mr-2"></i>Về trang đăng nhập
              </Link>
            </div>
          )}
        </div>

        <p className="text-center text-sm text-slate-500">
          <Link href="/login" className="text-indigo-600 font-semibold hover:text-indigo-700">
            <i className="fa-solid fa-arrow-left mr-1"></i> Quay lại đăng nhập
          </Link>
        </p>
      </div>
    </div>
  );
}
