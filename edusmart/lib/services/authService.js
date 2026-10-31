/**
 * lib/services/authService.js
 * Mọi thứ liên quan đến xác thực đều ở đây.
 */
import { apiFetch, tokenHelper } from '@/lib/api';

export const authService = {
  /** Đăng nhập bằng email + mật khẩu */
  login: (email, password) =>
    apiFetch('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    }),

  /** Đăng ký tài khoản mới */
  register: (fullName, email, password, grade) =>
    apiFetch('/api/auth/register', {
      method: 'POST',
      body: JSON.stringify({ fullName, email, password, confirmPassword: password, grade }),
    }),

  /** Đăng nhập bằng Google (gửi ID Token từ Google SDK) */
  loginWithGoogle: (idToken) =>
    apiFetch('/api/auth/google', {
      method: 'POST',
      body: JSON.stringify({ idToken }),
    }),

  /** Yêu cầu token đặt lại mật khẩu (data.resetToken có giá trị khi chưa cấu hình mail) */
  forgotPassword: (email) =>
    apiFetch('/api/auth/forgot-password', {
      method: 'POST',
      body: JSON.stringify({ email }),
    }),

  /** Đặt mật khẩu mới bằng reset token */
  resetPassword: (token, newPassword, confirmPassword) =>
    apiFetch('/api/auth/reset-password', {
      method: 'POST',
      body: JSON.stringify({ token, newPassword, confirmPassword }),
    }),

  /** Đăng xuất — chỉ cần xóa token ở client */
  logout: () => {
    tokenHelper.clear();
    window.location.href = '/login';
  },
};
