#!/usr/bin/env bash
# ════════════════════════════════════════════════════════════════
#  EduSmart — Chạy app bằng 1 JAR duy nhất (Linux/macOS)
#  KHÔNG cần Docker, KHÔNG cần MySQL (dữ liệu lưu H2 tại ./data)
#  App mở tại: http://localhost:8081
#  Đăng nhập thử: hs1@edusmart.vn / HocSinh@123
# ════════════════════════════════════════════════════════════════
cd "$(dirname "$0")"

if [ ! -f "hoctap/target/hoctap-0.0.1-SNAPSHOT.jar" ]; then
    echo "Chưa có file JAR! Hãy chạy ./build-all.sh trước."
    exit 1
fi

# Profile h2 = database nhúng sẵn, tự tạo file ./data/edusmart
# Muốn dùng MySQL: export DB_URL=... DB_USERNAME=... DB_PASSWORD=... và bỏ dòng dưới
export SPRING_PROFILES_ACTIVE=h2

echo ""
echo " EduSmart đang khởi động... mở http://localhost:8081 sau ~10 giây"
echo " (Ctrl+C để thoát)"
echo ""
java -jar "hoctap/target/hoctap-0.0.1-SNAPSHOT.jar"
