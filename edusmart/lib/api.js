/**
 * lib/api.js — Trung tâm cấu hình HTTP Client
 *
 * Tại sao dùng file này thay vì gọi fetch() trực tiếp?
 * → Chỉ cần đổi BASE_URL 1 chỗ khi deploy production
 * → Tự động đính kèm JWT token vào mọi request
 * → Xử lý 401 (hết hạn token) tập trung 1 chỗ
 *
 * BASE_URL đọc từ biến môi trường NEXT_PUBLIC_API_URL (đặt trong .env.local
 * hoặc khi build). Mặc định là http://localhost:8081 — backend Spring Boot.
 * Khi đóng gói frontend vào cùng 1 JAR với backend → để trống (dùng same-origin).
 */

export const BASE_URL = (process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8081').replace(/\/+$/, '');

/** Chuyển đường dẫn file từ backend (/uploads/...) thành URL đầy đủ */
export function fileUrl(path) {
  if (!path) return '';
  if (/^https?:\/\//.test(path)) return path; // đã là URL đầy đủ
  return `${BASE_URL}${path}`;
}

// ─────────────────────────────────────────────────────────────────────────────
// TOKEN HELPERS — Lưu/đọc/xóa token từ localStorage
// ─────────────────────────────────────────────────────────────────────────────

export const tokenHelper = {
  getAccess:  () => localStorage.getItem('accessToken'),
  getRefresh: () => localStorage.getItem('refreshToken'),
  getUser:    () => {
    const u = localStorage.getItem('user');
    return u ? JSON.parse(u) : null;
  },

  save: (authResponse) => {
    localStorage.setItem('accessToken',  authResponse.accessToken);
    localStorage.setItem('refreshToken', authResponse.refreshToken);
    localStorage.setItem('user',         JSON.stringify(authResponse.user));
  },

  clear: () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
  },

  isLoggedIn: () => !!localStorage.getItem('accessToken'),
};

// ─────────────────────────────────────────────────────────────────────────────
// apiFetch — Wrapper quanh fetch() với JWT auto-inject & refresh
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Gọi API với JWT tự động.
 *
 * @param {string} path     - Đường dẫn, ví dụ: '/api/schedules'
 * @param {object} options  - Giống fetch() options: method, body, ...
 * @returns {Promise<any>}  - Dữ liệu từ trường `data` trong ApiResponse
 */
export async function apiFetch(path, options = {}) {
  const token = tokenHelper.getAccess();

  // FormData (upload file) → browser tự set Content-Type với boundary,
  // KHÔNG được ghi đè bằng application/json
  const isFormData = options.body instanceof FormData;

  const headers = {
    ...(isFormData ? {} : { 'Content-Type': 'application/json' }),
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...options.headers,
  };

  const res = await fetch(`${BASE_URL}${path}`, {
    ...options,
    headers,
  });

  // Token hết hạn → thử refresh
  if (res.status === 401) {
    // Chỉ refresh nếu KHÔNG phải đang gọi endpoint auth (tránh vòng lặp vô hạn)
    if (!path.startsWith('/api/auth/')) {
      const refreshed = await tryRefreshToken();
      if (refreshed) {
        return apiFetch(path, options);
      }
    }
    tokenHelper.clear();
    // Chỉ redirect nếu đang ở browser (không phải server-side)
    if (typeof window !== 'undefined') {
      window.location.href = '/login';
    }
    throw new Error('Phiên đăng nhập đã hết hạn');
  }

  // 204 No Content hoặc response rỗng → không parse JSON
  if (res.status === 204 || res.headers.get('content-length') === '0') {
    return null;
  }

  // Đọc body dưới dạng text trước, rồi mới parse JSON
  // Tránh lỗi "Unexpected end of JSON input" khi body rỗng
  const text = await res.text();
  if (!text) {
    if (!res.ok) throw new Error(`Lỗi ${res.status}: Server không trả về dữ liệu`);
    return null;
  }

  let json;
  try {
    json = JSON.parse(text);
  } catch {
    throw new Error(`Lỗi ${res.status}: Phản hồi không hợp lệ từ server`);
  }

  // Backend trả về { success, message, data }
  if (!json.success) {
    throw new Error(json.message || 'Có lỗi xảy ra');
  }

  return json.data;
}

/** Thử lấy token mới bằng refreshToken */
async function tryRefreshToken() {
  const refreshToken = tokenHelper.getRefresh();
  if (!refreshToken) return false;

  try {
    const res = await fetch(`${BASE_URL}/api/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken }),
    });
    const json = await res.json();
    if (json.success) {
      tokenHelper.save(json.data);
      return true;
    }
  } catch {}
  return false;
}
