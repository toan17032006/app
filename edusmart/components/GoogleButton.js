'use client';
/**
 * components/GoogleButton.js — Nút "Tiếp tục với Google" (Google Identity Services)
 *
 * Cách hoạt động:
 *   1. Xác định Client ID theo thứ tự:
 *        a) biến môi trường NEXT_PUBLIC_GOOGLE_CLIENT_ID (nếu đặt lúc build)
 *        b) tự lấy từ backend: GET /api/config/google → data.googleClientId
 *           → chỉ cần đặt biến môi trường GOOGLE_CLIENT_ID ở backend + restart,
 *             KHÔNG cần build lại frontend.
 *   2. Load SDK Google (accounts.google.com/gsi/client) và render nút Google chính thức.
 *   3. User chọn tài khoản → nhận ID Token → gọi onCredential(idToken)
 *      → page gửi lên POST /api/auth/google để đổi lấy JWT của hệ thống.
 *
 * Cách lấy Client ID miễn phí (1 lần):
 *   console.cloud.google.com → APIs & Services → Credentials →
 *   Create OAuth client ID → Web application →
 *   Authorized JavaScript origins: http://localhost:8081 (và http://localhost:3000 nếu chạy dev)
 *   → Copy "Client ID" (dạng xxxx.apps.googleusercontent.com)
 *   → Lưu vào file google-client-id.txt cạnh start-app.bat (script tự nạp),
 *     hoặc set biến môi trường GOOGLE_CLIENT_ID.
 */
import { useEffect, useRef, useState } from 'react';
import { BASE_URL } from '@/lib/api';
import { GoogleIcon } from '@/components/GoogleIcon';

const GOOGLE_GSI_SRC = 'https://accounts.google.com/gsi/client';

export default function GoogleButton({ onCredential, label, onError }) {
  const containerRef = useRef(null);
  const scriptBoundRef = useRef(false);
  const [clientId, setClientId] = useState(process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID || null);
  const [resolved, setResolved] = useState(!!process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID);
  const [ready, setReady] = useState(false);

  // ── Bước 1: xác định Client ID (env → backend config) ───────────────────
  useEffect(() => {
    if (resolved) return;
    let cancelled = false;
    (async () => {
      try {
        // Dùng fetch thuần — endpoint công khai, không cần JWT
        const res = await fetch(`${BASE_URL}/api/config/google`);
        const json = await res.json();
        const id = json?.data?.googleClientId || '';
        if (!cancelled) {
          setClientId(id);
          setResolved(true);
        }
      } catch {
        if (!cancelled) {
          setClientId('');
          setResolved(true);
        }
      }
    })();
    return () => { cancelled = true; };
  }, [resolved]);

  // ── Bước 2: có Client ID → load SDK Google và render nút ────────────────
  useEffect(() => {
    if (!resolved || !clientId) return;

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
            width: 380,
            text: label === 'Đăng ký với Google' ? 'signup_with' : 'signin_with',
            locale: 'vi',
          });
          setReady(true);
        }
      } catch (err) {
        onError?.('Không thể khởi tạo Google Sign-In: ' + err.message);
      }
    };

    if (window.google?.accounts?.id) {
      initAndRender();
      return;
    }
    if (!scriptBoundRef.current) {
      scriptBoundRef.current = true;
      let script = document.querySelector(`script[src="${GOOGLE_GSI_SRC}"]`);
      if (!script) {
        script = document.createElement('script');
        script.src = GOOGLE_GSI_SRC;
        script.async = true;
        script.defer = true;
        document.head.appendChild(script);
      }
      script.addEventListener('load', initAndRender);
      // Nếu script đã tải xong từ lần trước (window.google đã có) mà load event
      // không bắn lại → init trực tiếp
      if (window.google?.accounts?.id) initAndRender();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [resolved, clientId, label]);

  const handleClickMissingConfig = () => {
    onError?.(
      'Google Sign-In chưa được cấu hình. Cách bật (2 phút): tạo OAuth 2.0 Client ID tại ' +
      'console.cloud.google.com (Web application, origin http://localhost:8081), lưu Client ID vào file ' +
      'google-client-id.txt cạnh start-app.bat rồi khởi động lại app.'
    );
  };

  // ── Render ────────────────────────────────────────────────────────────────
  // Đang hỏi backend config
  if (!resolved) {
    return (
      <div className="w-full flex items-center justify-center gap-3 px-4 py-3 bg-white border-2 border-slate-200 rounded-xl text-sm font-semibold text-slate-400">
        <i className="fa-solid fa-spinner animate-spin"></i>
        Đang tải...
      </div>
    );
  }

  // Đã resolve mà không có Client ID → nút hướng dẫn khi bấm
  if (!clientId) {
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

  // Có Client ID → chờ SDK render nút Google chính thức
  return (
    <div className="flex justify-center">
      <div ref={containerRef} className={ready ? '' : 'absolute opacity-0 pointer-events-none'} />
      {!ready && (
        <div className="w-full flex items-center justify-center gap-3 px-4 py-3 bg-white border-2 border-slate-200 rounded-xl text-sm font-semibold text-slate-400">
          <i className="fa-solid fa-spinner animate-spin"></i>
          Đang tải Google Sign-In...
        </div>
      )}
    </div>
  );
}
