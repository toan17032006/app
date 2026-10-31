#!/usr/bin/env bash
# ════════════════════════════════════════════════════════════════
#  EduSmart — Build toàn bộ app thành 1 file JAR duy nhất (Linux/macOS)
#  Kết quả: hoctap/target/hoctap-0.0.1-SNAPSHOT.jar
#  Yêu cầu: Node.js 20+ và Java 21+
# ════════════════════════════════════════════════════════════════
set -e
cd "$(dirname "$0")"

echo "[1/4] Build frontend (Next.js static export)..."
cd edusmart
npm install
npm run build
cd ..

echo "[2/4] Copy frontend vào backend (classpath:/static)..."
rm -rf hoctap/src/main/resources/static
mkdir -p hoctap/src/main/resources/static
cp -r edusmart/out/. hoctap/src/main/resources/static/

echo "[3/4] Build backend JAR (Spring Boot)..."
cd hoctap
chmod +x mvnw
./mvnw clean package -DskipTests
cd ..

echo "[4/4] HOÀN THÀNH!"
echo "   File JAR: hoctap/target/hoctap-0.0.1-SNAPSHOT.jar"
echo "   Chạy app: ./start-app.sh"
