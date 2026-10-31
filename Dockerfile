# ══════════════════════════════════════════════════════════════════
#  EduSmart — Docker image cho TOÀN BỘ app (frontend + backend 1 container)
#  Build:  docker build -t edusmart .
#  Chạy:   docker run -p 8081:8081 -v edusmart-data:/app/data edusmart
#  (Ai KHÔNG có Docker: dùng build-all.bat + start-app.bat — không cần Docker)
# ══════════════════════════════════════════════════════════════════

# ── Stage 1: build frontend Next.js (static export) ─────────────────
FROM node:22-alpine AS frontend
WORKDIR /fe
COPY edusmart/package*.json ./
RUN npm ci || npm install
COPY edusmart/ ./
RUN npm run build

# ── Stage 2: build backend Spring Boot (Maven wrapper, không cần cài Maven) ──
FROM eclipse-temurin:21-jdk AS backend
WORKDIR /be
COPY hoctap/.mvn .mvn
COPY hoctap/mvnw hoctap/pom.xml ./
COPY hoctap/src ./src
# Nhúng frontend static export vào JAR
COPY --from=frontend /fe/out ./src/main/resources/static
RUN chmod +x mvnw && ./mvnw -q package -DskipTests

# ── Stage 3: image chạy nhẹ (chỉ JRE) ────────────────────────────────
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=backend /be/target/hoctap-0.0.1-SNAPSHOT.jar app.jar

# Mặc định dùng H2 (database nhúng) — không cần MySQL container.
# Muốn dùng MySQL: set SPRING_PROFILES_ACTIVE=rỗng + DB_URL/DB_USERNAME/DB_PASSWORD
ENV SPRING_PROFILES_ACTIVE=h2 \
    UPLOAD_DIR=/app/uploads \
    JPA_SHOW_SQL=false
EXPOSE 8081
VOLUME ["/app/data", "/app/uploads"]
ENTRYPOINT ["java", "-jar", "app.jar"]
