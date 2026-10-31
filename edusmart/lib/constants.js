/**
 * lib/constants.js — Các hằng số + helper dùng chung
 *
 * Backend lưu ngày trong tuần dạng enum MON..SUN (Schedule.DayOfWeek),
 * UI hiển thị tiếng Việt. File này là nơi dịch giữa 2 bên.
 */

export const DAY_ENUMS = ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN'];

/** MON → 'Thứ 2' ... SUN → 'Chủ Nhật' */
export const DAY_LABELS = {
  MON: 'Thứ 2',
  TUE: 'Thứ 3',
  WED: 'Thứ 4',
  THU: 'Thứ 5',
  FRI: 'Thứ 6',
  SAT: 'Thứ 7',
  SUN: 'Chủ Nhật',
};

export function dayLabel(enumValue) {
  return DAY_LABELS[enumValue] || enumValue || '';
}

/** Hôm nay theo enum của backend (MON..SUN) — theo múi giờ người dùng */
export function todayEnum() {
  // JS: 0=Chủ Nhật..6=Thứ Bảy → MON..SUN
  const map = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT'];
  return map[new Date().getDay()];
}

/** Trạng thái bài tập: PENDING/SUBMITTED → nhãn tiếng Việt */
export const HOMEWORK_STATUS_LABELS = {
  PENDING: 'Chưa nộp',
  SUBMITTED: 'Đã nộp',
};

export function homeworkStatusLabel(status) {
  return HOMEWORK_STATUS_LABELS[status] || status;
}

/** Hiển thị lớp: "Năm nhất" giữ nguyên, mã lớp khác thì thêm tiền tố "Lớp" */
export function gradeDisplay(grade) {
  if (!grade) return '—';
  return grade.startsWith('Năm') ? grade : `Lớp ${grade}`;
}

/** Năm học hiện tại, ví dụ 2026-10 → "2026-2027" (năm học bắt đầu tháng 9) */
export function currentSchoolYear() {
  const now = new Date();
  const startYear = now.getMonth() >= 8 ? now.getFullYear() : now.getFullYear() - 1;
  return `${startYear}-${startYear + 1}`;
}

/**
 * Avatar SVG data-URI với 2 chữ cái đầu của tên — dùng khi user chưa có
 * ảnh đại diện (tránh phụ thuộc placehold.co, hoạt động offline).
 */
const AVATAR_COLORS = ['#6366f1', '#8b5cf6', '#ec4899', '#f59e0b', '#10b981', '#3b82f6', '#ef4444'];

export function avatarFor(fullName) {
  const initials = (fullName || 'U')
    .trim()
    .split(/\s+/)
    .map((w) => w[0])
    .slice(0, 2)
    .join('')
    .toUpperCase();
  let hash = 0;
  for (const ch of fullName || '') hash = (hash * 31 + ch.charCodeAt(0)) | 0;
  const color = AVATAR_COLORS[Math.abs(hash) % AVATAR_COLORS.length];
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="64" height="64"><rect width="64" height="64" rx="32" fill="${color}"/><text x="32" y="41" font-family="Arial, sans-serif" font-size="26" font-weight="bold" fill="#ffffff" text-anchor="middle">${initials}</text></svg>`;
  return `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`;
}
