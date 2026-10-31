'use client';
/**
 * components/GoogleButton.js — Nút "Tiếp tục với Google" (Google Identity Services)
 *
 * Cách hoạt động:
 *   1. Load script https://accounts.google.com/gsi/client
 *   2. Đọc client ID từ biến môi trường NEXT_PUBLIC_GOOGLE_CLIENT_ID
 *   3. Render nút Google chính thức → user chọn tài khoản → nhận credential (ID Token)
 *   4. Gọi onCredential(idToken) — page sẽ gửi token này lên POST /api/auth/google
 *
 * Cấu hình client ID (1 lần):
 *   1. Truy cập https://console.cloud.google.com/apis/credentials → tạo OAuth 2.0 Client
 *      (Web application, Authorized JavaScript origins: http://localhost:3000
 *       và domain khi deploy)
 *   2. Tạo file .env.local trong thư mục edusmart:
 *      NEXT_PUBLIC_GOOGLE_CLIENT_ID=xxxxxxxx.apps.googleusercontent.com
 *   → Nút Google hoạt động. Nếu chưa cấu hình → bấm nút sẽ hiện hướng dẫn.
 */
import { useEffect, useRef, useState } from 'react';

const GOOGLE_GSI_SRC = 'https://accounts.google.com/gsi/client';

export default function GoogleButton({ onCredential, label, onError }) {
  const containerRef = useRef(null);
  const [ready, setReady] = useState(false);
  const [missingConfig, setMissingConfig] = useState(false);

  const clientId = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID;

  useEffect(() => {
    if (!clientId) {
      setMissingConfig(true);
      return;
    }

    // Load SDK Google 1 lần
    if (window.google?.accounts?.id) {
      initAndRender();
      return;
    }
    let script = document.querySelector(`script[src="${GOOGLE_GSI_SRC}"]`);
    if (!script) {
      script = document.createElement('script');
      script.src = GOOGLE_GSI_SRC;
      script.async = true;
      script.defer = true;
      document.head.appendChild(script);
    }
    script.addEventListener('load', initAndRender);

    return () => script?.removeEventListener('load', initAndRender);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [clientId]);

  const initAndRender = () => {
    try {
      window.google.accounts.id.initialize({
        client_id: clientId,
        callback: (response) => {
          if (response?.credential) onCredential(response.credential);
        },
      });
      if (containerRef.current) {
        window.google.accounts.id.renderButton(containerRef.current, {
          theme: 'outline',
          size: 'large',
          width: 400,
          text: label === 'Đăng ký với Google' ? 'signup_with' : 'signin_with',
          locale: 'vi',
        });
        setReady(true);
      }
    } catch (err) {
      onError?.('Không thể khởi tạo Google Sign-In: ' + err.message);
    }
  };

  const handleClickMissingConfig = () => {
    onError?.(
      'Google Sign-In chưa được cấu hình. Hãy tạo OAuth 2.0 Client ID tại console.cloud.google.com ' +
        'và đặt biến môi trường NEXT_PUBLIC_GOOGLE_CLIENT_ID trong file .env.local (xem components/GoogleButton.js).'
    );
  };

  // Chưa cấu hình client ID → hiện nút trang trí + hướng dẫn khi bấm
  if (missingConfig) {
    return (
      <button
        type="button"
        onClick={handleClickMissingConfig}
        className="w-full flex items-center justify-center gap-3 px-4 py-3 bg-white border-2 border-slate-200 rounded-xl text-sm font-semibold text-slate-700 hover:bg-slate-50 hover:border-slate-300 transition shadow-sm"
      >
        <GoogleIcon />
        {label}
      </button>
    );
  }

  // Container để Google render nút chính thức
  return (
    <div className="flex justify-center">
      <div ref={containerRef} className={ready ? '' : 'opacity-0 pointer-events-none h-0'} />
      {!ready && (
        <div className="w-full flex items-center justify-center gap-3 px-4 py-3 bg-white border-2 border-slate-200 rounded-xl text-sm font-semibold text-slate-400">
          <i className="fa-solid fa-spinner animate-spin"></i>
          Đang tải Google Sign-In...
        </div>
      )}
    </div>
  );
}

export function GoogleIcon() {
  return (
    <svg className="w-5 h-5" viewBox="0 0 24 24">
      <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
      <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
      <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
      <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
    </svg>
  );
}
