'use client';
import { useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/lib/hooks/useAuth';
import GoogleButton from '@/components/GoogleButton';

const PASSWORD_RULES = [
  { label: 'Ít nhất 8 ký tự', test: (p) => p.length >= 8 },
  { label: 'Có chữ hoa (A–Z)', test: (p) => /[A-Z]/.test(p) },
  { label: 'Có chữ số (0–9)', test: (p) => /[0-9]/.test(p) },
];

export default function RegisterPage() {
  const router = useRouter();
  const {
    register: registerUser, loginWithGoogle,
    loading, error, setError, clearError,
  } = useAuth();

  const [form, setForm] = useState({
    fullName: '',
    email: '',
    password: '',
    confirmPassword: '',
    grade: '',
    agreeTerms: false,
  });
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [step, setStep] = useState(1); // 1 or 2

  const passwordMatch = form.password && form.confirmPassword && form.password === form.confirmPassword;
  const passwordMismatch = form.confirmPassword && form.password !== form.confirmPassword;
  const allRulesPassed = PASSWORD_RULES.every((r) => r.test(form.password));

  const handleNext = (e) => {
    e.preventDefault();
    setStep(2);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.agreeTerms) return;
    clearError();
    const result = await registerUser(form.fullName, form.email, form.password, form.grade);
    if (result.success) {
      router.push('/');  // Đăng ký thành công → vào app chính
    }
  };

  // ── Đăng ký / đăng nhập bằng Google ──────────────────────────────────────
  // GoogleButton trả về ID Token → backend POST /api/auth/google tự động
  // tạo tài khoản mới nếu email chưa tồn tại (nên dùng được cho cả đăng ký).
  const handleGoogleCredential = async (idToken) => {
    clearError();
    const result = await loginWithGoogle(idToken);
    if (result.success) {
      router.push('/');
    }
  };

  return (
    <div className="min-h-screen flex">
      {/* ── Left panel – branding ────────────────────────────────────────── */}
      <div className="hidden lg:flex lg:w-5/12 bg-gradient-to-br from-violet-600 via-indigo-600 to-blue-700 flex-col justify-between p-12 relative overflow-hidden">
        {/* Decorations */}
        <div className="absolute -top-20 -right-20 w-80 h-80 bg-white/5 rounded-full" />
        <div className="absolute -bottom-20 -left-10 w-72 h-72 bg-white/5 rounded-full" />

        {/* Logo */}
        <div className="relative flex items-center gap-3">
          <div className="w-11 h-11 rounded-xl bg-white/20 backdrop-blur-sm flex items-center justify-center text-white text-xl">
            <i className="fa-solid fa-graduation-cap"></i>
          </div>
          <div>
            <h1 className="font-bold text-xl text-white tracking-wide">EduSmart</h1>
            <p className="text-xs text-indigo-200">Học tập thông minh</p>
          </div>
        </div>

        {/* Center */}
        <div className="relative space-y-6">
          <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-white/15 text-white text-sm font-medium border border-white/20">
            <i className="fa-solid fa-rocket text-amber-300"></i>
            Bắt đầu hành trình học tập
          </div>
          <h2 className="text-4xl font-bold text-white leading-tight">
            Tạo tài khoản <br />miễn phí ngay! 🎓
          </h2>
          <p className="text-indigo-200 text-base leading-relaxed max-w-sm">
            Gia nhập cộng đồng hàng nghìn học sinh đang học tập cùng EduSmart. Hoàn toàn miễn phí!
          </p>

          {/* Feature list */}
          <ul className="space-y-3">
            {[
              { icon: 'fa-circle-check', color: 'text-emerald-300', label: 'Truy cập 500+ bài học miễn phí' },
              { icon: 'fa-circle-check', color: 'text-emerald-300', label: 'Bài kiểm tra có chấm điểm tự động' },
              { icon: 'fa-circle-check', color: 'text-emerald-300', label: 'Bảng xếp hạng & tích điểm phần thưởng' },
              { icon: 'fa-circle-check', color: 'text-emerald-300', label: 'Quản lý thời gian biểu cá nhân' },
            ].map((item) => (
              <li key={item.label} className="flex items-center gap-3 text-white/90 text-sm">
                <i className={`fa-solid ${item.icon} ${item.color} flex-shrink-0`}></i>
                {item.label}
              </li>
            ))}
          </ul>
        </div>

        {/* Testimonial */}
        <div className="relative bg-white/10 backdrop-blur-sm rounded-2xl p-5 border border-white/20 space-y-3">
          <div className="flex items-center gap-3">
            <img
              src="https://placehold.co/40x40/6366f1/ffffff?text=NV"
              className="w-10 h-10 rounded-full border-2 border-white/50"
              alt="Testimonial"
            />
            <div>
              <p className="text-white font-semibold text-sm">Nguyễn Văn Minh</p>
              <p className="text-indigo-200 text-xs">Học sinh lớp 12 · Hà Nội</p>
            </div>
            <div className="ml-auto flex gap-0.5">
              {[1,2,3,4,5].map((s) => (
                <i key={s} className="fa-solid fa-star text-amber-400 text-xs"></i>
              ))}
            </div>
          </div>
          <p className="text-white/85 text-sm italic">
            "EduSmart giúp mình tăng GPA từ 3.2 lên 3.85 chỉ trong một học kỳ. Cực kỳ hiệu quả!"
          </p>
        </div>
      </div>

      {/* ── Right panel – form ───────────────────────────────────────────── */}
      <div className="flex-1 flex items-center justify-center p-6 sm:p-10 bg-slate-50 overflow-y-auto">
        <div className="w-full max-w-md space-y-7 py-4">
          {/* Mobile logo */}
          <div className="lg:hidden flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-indigo-500 to-violet-500 flex items-center justify-center text-white text-lg shadow-md">
              <i className="fa-solid fa-graduation-cap"></i>
            </div>
            <div>
              <h1 className="font-bold text-lg text-slate-800">EduSmart</h1>
              <p className="text-xs text-slate-400">Học tập thông minh</p>
            </div>
          </div>

          {/* Heading + Step indicator */}
          <div className="space-y-3">
            <div className="flex items-center gap-2">
              <div className={`flex items-center justify-center w-7 h-7 rounded-full text-xs font-bold ${step >= 1 ? 'bg-indigo-600 text-white' : 'bg-slate-200 text-slate-500'}`}>1</div>
              <div className={`flex-1 h-1 rounded-full ${step >= 2 ? 'bg-indigo-600' : 'bg-slate-200'}`}></div>
              <div className={`flex items-center justify-center w-7 h-7 rounded-full text-xs font-bold ${step >= 2 ? 'bg-indigo-600 text-white' : 'bg-slate-200 text-slate-500'}`}>2</div>
            </div>
            <div>
              <h2 className="text-2xl font-bold text-slate-800">
                {step === 1 ? 'Tạo tài khoản' : 'Đặt mật khẩu'}
              </h2>
              <p className="text-slate-500 mt-1 text-sm">
                {step === 1 ? (
                  <>Đã có tài khoản?{' '}
                    <Link href="/login" className="text-indigo-600 font-semibold hover:text-indigo-700 transition">
                      Đăng nhập
                    </Link>
                  </>
                ) : (
                  'Tạo mật khẩu mạnh để bảo vệ tài khoản của bạn.'
                )}
              </p>
            </div>
          </div>

          {/* ── STEP 1 ── */}
          {step === 1 && (
            <>
              {/* Google */}
              <GoogleButton
                label="Đăng ký với Google"
                onCredential={handleGoogleCredential}
                onError={setError}
              />

              <div className="flex items-center gap-4">
                <div className="flex-1 h-px bg-slate-200"></div>
                <span className="text-xs text-slate-400 font-medium">HOẶC DÙNG EMAIL</span>
                <div className="flex-1 h-px bg-slate-200"></div>
              </div>

              <form onSubmit={handleNext} className="space-y-4">
                {/* Full Name */}
                <div className="space-y-1.5">
                  <label className="block text-sm font-semibold text-slate-700">Họ và tên</label>
                  <div className="relative">
                    <span className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400">
                      <i className="fa-solid fa-user text-sm"></i>
                    </span>
                    <input
                      type="text"
                      required
                      placeholder="Nguyễn Văn A"
                      value={form.fullName}
                      onChange={(e) => setForm({ ...form, fullName: e.target.value })}
                      className="w-full pl-10 pr-4 py-3 bg-white border border-slate-300 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 focus:outline-none transition placeholder:text-slate-400"
                    />
                  </div>
                </div>

                {/* Email */}
                <div className="space-y-1.5">
                  <label className="block text-sm font-semibold text-slate-700">Email</label>
                  <div className="relative">
                    <span className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400">
                      <i className="fa-solid fa-envelope text-sm"></i>
                    </span>
                    <input
                      type="email"
                      required
                      placeholder="vidu@email.com"
                      value={form.email}
                      onChange={(e) => setForm({ ...form, email: e.target.value })}
                      className="w-full pl-10 pr-4 py-3 bg-white border border-slate-300 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 focus:outline-none transition placeholder:text-slate-400"
                    />
                  </div>
                </div>

                {/* Grade / Class */}
                <div className="space-y-1.5">
                  <label className="block text-sm font-semibold text-slate-700">Lớp / Năm học</label>
                  <div className="relative">
                    <span className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400">
                      <i className="fa-solid fa-school text-sm"></i>
                    </span>
                    <select
                      required
                      value={form.grade}
                      onChange={(e) => setForm({ ...form, grade: e.target.value })}
                      className="w-full pl-10 pr-4 py-3 bg-white border border-slate-300 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 focus:outline-none transition text-slate-700 appearance-none"
                    >
                      <option value="">-- Chọn lớp học --</option>
                      <option value="Năm nhất">Năm nhất (Đại học)</option>
                      <option value="Năm hai">Năm hai (Đại học)</option>
                      <option value="Năm ba">Năm ba (Đại học)</option>
                      <option value="Năm tư">Năm tư (Đại học)</option>
                      <option value="10A1">Lớp 10A1</option>
                      <option value="10A2">Lớp 10A2</option>
                      <option value="11A1">Lớp 11A1</option>
                      <option value="11A2">Lớp 11A2</option>
                      <option value="11B1">Lớp 11B1</option>
                      <option value="12A1">Lớp 12A1</option>
                      <option value="12A2">Lớp 12A2</option>
                      <option value="12A3">Lớp 12A3</option>
                    </select>
                    <span className="absolute right-3.5 top-1/2 -translate-y-1/2 text-slate-400 pointer-events-none">
                      <i className="fa-solid fa-chevron-down text-xs"></i>
                    </span>
                  </div>
                </div>

                <button
                  type="submit"
                  className="w-full py-3.5 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold rounded-xl transition shadow-md shadow-indigo-600/30 flex items-center justify-center gap-2 text-sm"
                >
                  Tiếp tục
                  <i className="fa-solid fa-arrow-right"></i>
                </button>
              </form>
            </>
          )}

          {/* ── STEP 2 ── */}
          {step === 2 && (
            <form onSubmit={handleSubmit} className="space-y-5">
              {/* Back button */}
              <button
                type="button"
                onClick={() => setStep(1)}
                className="flex items-center gap-2 text-sm text-slate-500 hover:text-slate-700 transition font-medium"
              >
                <i className="fa-solid fa-arrow-left text-xs"></i>
                Quay lại
              </button>

              {/* Account summary */}
              <div className="bg-indigo-50 border border-indigo-200 rounded-xl px-4 py-3 flex items-center gap-3">
                <div className="w-9 h-9 rounded-full bg-indigo-600 text-white flex items-center justify-center font-bold text-sm flex-shrink-0">
                  {form.fullName?.charAt(0)?.toUpperCase() || 'U'}
                </div>
                <div className="overflow-hidden">
                  <p className="text-sm font-semibold text-slate-800 truncate">{form.fullName}</p>
                  <p className="text-xs text-indigo-600 truncate">{form.email}</p>
                </div>
                <span className="ml-auto text-xs bg-indigo-600 text-white px-2 py-0.5 rounded-md font-medium flex-shrink-0">
                  {form.grade}
                </span>
              </div>

              {/* Password */}
              <div className="space-y-1.5">
                <label className="block text-sm font-semibold text-slate-700">Mật khẩu</label>
                <div className="relative">
                  <span className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400">
                    <i className="fa-solid fa-lock text-sm"></i>
                  </span>
                  <input
                    type={showPassword ? 'text' : 'password'}
                    required
                    minLength={8}
                    placeholder="Tối thiểu 8 ký tự"
                    value={form.password}
                    onChange={(e) => setForm({ ...form, password: e.target.value })}
                    className="w-full pl-10 pr-11 py-3 bg-white border border-slate-300 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 focus:outline-none transition placeholder:text-slate-400"
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword((v) => !v)}
                    className="absolute right-3.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 transition"
                  >
                    <i className={`fa-solid ${showPassword ? 'fa-eye-slash' : 'fa-eye'} text-sm`}></i>
                  </button>
                </div>

                {/* Password strength rules */}
                {form.password && (
                  <div className="space-y-1.5 pt-1">
                    {PASSWORD_RULES.map((rule) => {
                      const passed = rule.test(form.password);
                      return (
                        <div key={rule.label} className="flex items-center gap-2 text-xs">
                          <i className={`fa-solid ${passed ? 'fa-circle-check text-emerald-500' : 'fa-circle-xmark text-slate-300'} text-sm`}></i>
                          <span className={passed ? 'text-emerald-600 font-medium' : 'text-slate-400'}>
                            {rule.label}
                          </span>
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>

              {/* Confirm Password */}
              <div className="space-y-1.5">
                <label className="block text-sm font-semibold text-slate-700">Xác nhận mật khẩu</label>
                <div className="relative">
                  <span className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400">
                    <i className="fa-solid fa-shield-halved text-sm"></i>
                  </span>
                  <input
                    type={showConfirm ? 'text' : 'password'}
                    required
                    placeholder="Nhập lại mật khẩu"
                    value={form.confirmPassword}
                    onChange={(e) => setForm({ ...form, confirmPassword: e.target.value })}
                    className={`w-full pl-10 pr-11 py-3 bg-white border rounded-xl text-sm focus:ring-2 focus:outline-none transition placeholder:text-slate-400 ${
                      passwordMismatch
                        ? 'border-rose-400 focus:ring-rose-500 focus:border-rose-500'
                        : passwordMatch
                        ? 'border-emerald-400 focus:ring-emerald-500 focus:border-emerald-500'
                        : 'border-slate-300 focus:ring-indigo-500 focus:border-indigo-500'
                    }`}
                  />
                  <button
                    type="button"
                    onClick={() => setShowConfirm((v) => !v)}
                    className="absolute right-3.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 transition"
                  >
                    <i className={`fa-solid ${showConfirm ? 'fa-eye-slash' : 'fa-eye'} text-sm`}></i>
                  </button>
                </div>
                {passwordMismatch && (
                  <p className="text-xs text-rose-500 flex items-center gap-1">
                    <i className="fa-solid fa-circle-exclamation"></i>
                    Mật khẩu không khớp
                  </p>
                )}
                {passwordMatch && (
                  <p className="text-xs text-emerald-600 flex items-center gap-1">
                    <i className="fa-solid fa-circle-check"></i>
                    Mật khẩu khớp
                  </p>
                )}
              </div>

              {/* Terms */}
              <label className="flex items-start gap-2.5 cursor-pointer">
                <input
                  type="checkbox"
                  required
                  checked={form.agreeTerms}
                  onChange={(e) => setForm({ ...form, agreeTerms: e.target.checked })}
                  className="w-4 h-4 mt-0.5 rounded border-slate-300 text-indigo-600 focus:ring-indigo-500 flex-shrink-0"
                />
                <span className="text-sm text-slate-600 leading-relaxed">
                  Tôi đồng ý với{' '}
                  <Link href="/terms" className="text-indigo-600 font-semibold hover:underline">Điều khoản dịch vụ</Link>{' '}
                  và{' '}
                  <Link href="/privacy" className="text-indigo-600 font-semibold hover:underline">Chính sách bảo mật</Link>{' '}
                  của EduSmart
                </span>
              </label>

              {/* Submit */}
              <button
                type="submit"
                disabled={loading || passwordMismatch || !allRulesPassed || !form.agreeTerms}
                className="w-full py-3.5 bg-indigo-600 hover:bg-indigo-700 disabled:bg-slate-300 disabled:cursor-not-allowed text-white font-semibold rounded-xl transition shadow-md shadow-indigo-600/30 flex items-center justify-center gap-2 text-sm"
              >
                {loading ? (
                  <>
                    <i className="fa-solid fa-spinner animate-spin"></i>
                    Đang tạo tài khoản...
                  </>
                ) : (
                  <>
                    <i className="fa-solid fa-user-plus"></i>
                    Tạo tài khoản
                  </>
                )}
              </button>
            </form>
          )}
        </div>
      </div>
    </div>
  );
}
