/**
 * lib/hooks/useAuth.js
 *
 * Custom Hook quản lý toàn bộ trạng thái xác thực.
 *
 * Tại sao cần custom hook?
 * → Tách logic khỏi UI component
 * → Dùng lại được ở nhiều component khác nhau
 * → Dễ test độc lập
 */
'use client';
import { useState, useEffect, useCallback } from 'react';
import { tokenHelper } from '@/lib/api';
import { authService } from '@/lib/services/authService';

export function useAuth() {
  const [user, setUser]       = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError]     = useState('');

  // Khi component mount → đọc user từ localStorage (nếu đã đăng nhập trước đó)
  useEffect(() => {
    const savedUser = tokenHelper.getUser();
    if (savedUser) setUser(savedUser);
  }, []);

  const clearError = () => setError('');

  /** Đăng nhập email */
  const login = useCallback(async (email, password) => {
    setLoading(true);
    setError('');
    try {
      const data = await authService.login(email, password);
      tokenHelper.save(data);   // Lưu token + user vào localStorage
      setUser(data.user);
      return { success: true };
    } catch (err) {
      setError(err.message);
      return { success: false, error: err.message };
    } finally {
      setLoading(false);
    }
  }, []);

  /** Đăng ký */
  const register = useCallback(async (fullName, email, password, grade) => {
    setLoading(true);
    setError('');
    try {
      const data = await authService.register(fullName, email, password, grade);
      tokenHelper.save(data);
      setUser(data.user);
      return { success: true };
    } catch (err) {
      setError(err.message);
      return { success: false, error: err.message };
    } finally {
      setLoading(false);
    }
  }, []);

  /** Đăng nhập Google */
  const loginWithGoogle = useCallback(async (idToken) => {
    setLoading(true);
    setError('');
    try {
      const data = await authService.loginWithGoogle(idToken);
      tokenHelper.save(data);
      setUser(data.user);
      return { success: true };
    } catch (err) {
      setError(err.message);
      return { success: false, error: err.message };
    } finally {
      setLoading(false);
    }
  }, []);

  /** Đăng xuất */
  const logout = useCallback(() => {
    tokenHelper.clear();
    setUser(null);
    window.location.href = '/login';
  }, []);

  return {
    user,
    loading,
    error,
    setError,
    clearError,
    login,
    register,
    loginWithGoogle,
    logout,
    isLoggedIn: !!user,
  };
}
