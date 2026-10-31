'use client';
import { useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/lib/hooks/useAuth';
import GoogleButton from '@/components/GoogleButton';

export default function LoginPage() {
  const router = useRouter();
  const { login, loginWithGoogle, loading, error, setError, clearError } = useAuth();

  const [form, setForm] = useState({ email: '', password: '' });
  const [showPassword, setShowPassword] = useState(false);

  // ── Đăng nhập bằng Email ─────────────────────────────────────────────────
  const handleSubmit = async (e) => {
    e.preventDefault();
    clearError();
    const result = await login(form.email, form.password);
    if (result.success) {
      router.push('/');   // Về trang chính sau khi đăng nhập thành công
    }
    // Nếu lỗi → error state tự động cập nhật từ useAuth → hiển thị ở UI
  };

  // ── Đăng nhập bằng Google ─────────────────────────────────────────────────
  // GoogleButton trả về ID Token từ Google Identity Services → gửi lên backend
  // POST /api/auth/google để verify và nhận JWT của hệ thống.
  const handleGoogleCredential = async (idToken) => {
    clearError();
    const result = await loginWithGoogle(idToken);
    if (result.success) {
      router.push('/');
    }
  };

  return (
    <div className="min-h-screen flex">
      {/* ── Left panel – branding ───────────────────────────────────────── */}
      <div className="hidden lg:flex lg:w-1/2 bg-gradient-to-br from-indigo-600 via-violet-600 to-indigo-800 flex-col justify-between p-12 relative overflow-hidden">
        <div className="absolute top-0 right-0 w-96 h-96 bg-white/5 rounded-full -translate-y-1/2 translate-x-1/3" />
        <div className="absolute bottom-0 left-0 w-80 h-80 bg-white/5 rounded-full translate-y-1/3 -translate-x-1/4" />
        <div className="absolute top-1/2 left-1/2 w-64 h-64 bg-white/5 rounded-full -translate-x-1/2 -translate-y-1/2" />
        <div className="relative flex items-center gap-3">
          <div className="w-11 h-11 rounded-xl bg-white/20 backdrop-blur-sm flex items-center justify-center text-white text-xl shadow-lg">
            <i className="fa-solid fa-graduation-cap"></i>
          </div>
          <div>
            <h1 className="font-bold text-xl text-white tracking-wide">EduSmart</h1>
            <p className="text-xs text-indigo-200">Học tập thông minh</p>
          </div>
        </div>
        <div className="relative space-y-6">
          <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-white/15 backdrop-blur-sm text-white text-sm font-medium border border-white/20">
            <i className="fa-solid fa-shield-halved text-emerald-300"></i>
            Nền tảng học tập an toàn &amp; tin cậy
          </div>
          <h2 className="text-4xl font-bold text-white leading-tight">
            Chào mừng bạn <br />quay trở lại! 👋
          </h2>
          <p className="text-indigo-200 text-base leading-relaxed max-w-sm">
            Đăng nhập để tiếp tục hành trình học tập của bạn.
          </p>
          <div className="grid grid-cols-3 gap-4 pt-4">
            {[
              { icon: 'fa-users', value: '10K+', label: 'Học sinh' },
              { icon: 'fa-book-open', value: '500+', label: 'Bài học' },
              { icon: 'fa-trophy', value: '98%', label: 'Hài lòng' },
            ].map((stat) => (
              <div key={stat.label} className="bg-white/10 backdrop-blur-sm rounded-2xl p-4 border border-white/20 text-center">
                <i className={`fa-solid ${stat.icon} text-white/70 text-lg mb-1`}></i>
                <div className="text-xl font-bold text-white">{stat.value}</div>
                <div className="text-xs text-indigo-200 mt-0.5">{stat.label}</div>
              </div>
            ))}
          </div>
        </div>
        <div className="relative bg-white/10 backdrop-blur-sm rounded-2xl p-5 border border-white/20">
          <i className="fa-solid fa-quote-left text-indigo-300 text-2xl mb-2 block"></i>
          <p className="text-white/90 text-sm italic">
            &quot;Đầu tư vào kiến thức mang lại lợi nhuận cao nhất.&quot;
          </p>
          <p className="text-indigo-300 text-xs mt-2">— Benjamin Franklin</p>
        </div>
      </div>

      {/* ── Right panel – form ───────────────────────────────────────────── */}
      <div className="flex-1 flex items-center justify-center p-6 sm:p-10 bg-slate-50">
        <div className="w-full max-w-md space-y-8">
          {/* Mobile logo */}
          <div className="lg:hidden flex items-center gap-3 mb-2">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-indigo-500 to-violet-500 flex items-center justify-center text-white text-lg shadow-md">
              <i className="fa-solid fa-graduation-cap"></i>
            </div>
            <div>
              <h1 className="font-bold text-lg text-slate-800">EduSmart</h1>
              <p className="text-xs text-slate-400">Học tập thông minh</p>
            </div>
          </div>

          <div>
            <h2 className="text-3xl font-bold text-slate-800">Đăng nhập</h2>
            <p className="text-slate-500 mt-1.5 text-sm">
              Chưa có tài khoản?{' '}
              <Link href="/register" className="text-indigo-600 font-semibold hover:text-indigo-700 transition">
                Đăng ký ngay
              </Link>
            </p>
          </div>

          {/* ── Hiển thị lỗi từ API ────────────────────────────────────── */}
          {error && (
            <div className="flex items-start gap-3 p-4 bg-red-50 border border-red-200 rounded-xl text-red-700 text-sm">
              <i className="fa-solid fa-circle-exclamation mt-0.5 shrink-0"></i>
              <span>{error}</span>
            </div>
          )}

          {/* Google Login */}
          <GoogleButton
            label="Tiếp tục với Google"
            onCredential={handleGoogleCredential}
            onError={setError}
          />

          {/* Divider */}
          <div className="flex items-center gap-4">
            <div className="flex-1 h-px bg-slate-200"></div>
            <span className="text-xs text-slate-400 font-medium">HOẶC ĐĂNG NHẬP BẰNG EMAIL</span>
            <div className="flex-1 h-px bg-slate-200"></div>
          </div>

          {/* Form */}
          <form onSubmit={handleSubmit} className="space-y-5">
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
                  onChange={(e) => { setForm({ ...form, email: e.target.value }); clearError(); }}
                  className="w-full pl-10 pr-4 py-3 bg-white border border-slate-300 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 focus:outline-none transition placeholder:text-slate-400"
                />
              </div>
            </div>

            <div className="space-y-1.5">
              <div className="flex items-center justify-between">
                <label className="block text-sm font-semibold text-slate-700">Mật khẩu</label>
                <Link href="/forgot-password" className="text-xs text-indigo-600 font-semibold hover:text-indigo-700 transition">
                  Quên mật khẩu?
                </Link>
              </div>
              <div className="relative">
                <span className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400">
                  <i className="fa-solid fa-lock text-sm"></i>
                </span>
                <input
                  type={showPassword ? 'text' : 'password'}
                  required
                  placeholder="Nhập mật khẩu của bạn"
                  value={form.password}
                  onChange={(e) => { setForm({ ...form, password: e.target.value }); clearError(); }}
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
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full py-3.5 bg-indigo-600 hover:bg-indigo-700 disabled:bg-indigo-400 text-white font-semibold rounded-xl transition shadow-md shadow-indigo-600/30 flex items-center justify-center gap-2 text-sm"
            >
              {loading ? (
                <>
                  <i className="fa-solid fa-spinner animate-spin"></i>
                  Đang đăng nhập...
                </>
              ) : (
                <>
                  Đăng nhập
                  <i className="fa-solid fa-arrow-right"></i>
                </>
              )}
            </button>
          </form>

          <p className="text-center text-xs text-slate-400">
            Bằng cách đăng nhập, bạn đồng ý với{' '}
            <Link href="/terms" className="text-indigo-600 hover:underline">Điều khoản dịch vụ</Link>{' '}
            và{' '}
            <Link href="/privacy" className="text-indigo-600 hover:underline">Chính sách bảo mật</Link>{' '}
            của EduSmart.
          </p>
        </div>
      </div>
    </div>
  );
}
