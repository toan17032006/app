/** @type {import('next').NextConfig} */
const nextConfig = {
  // Static export: `next build` sinh thư mục out/ chứa toàn bộ trang HTML/JS/CSS.
  // Thư mục này được copy vào backend Spring Boot (classpath:/static) để chạy
  // TOÀN BỘ app bằng 1 file JAR duy nhất — không cần Node khi deploy.
  // App là SPA thuần (mọi component đều 'use client', gọi API ở client) nên
  // static export không làm mất tính năng nào.
  output: 'export',
  images: { unoptimized: true },
};

export default nextConfig;
