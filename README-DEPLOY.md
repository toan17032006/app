# EduSmart — Hướng dẫn triển khai & chạy cho người khác

App gồm **frontend Next.js** (thư mục `edusmart/`) và **backend Spring Boot** (thư mục `hoctap/`).
Sau khi build, **toàn bộ app gói thành 1 file JAR duy nhất** — người dùng chỉ cần
**Java 21+** là chạy được, **không cần Docker, không cần MySQL, không cần Node.js**.

---

## Cách 1 — CHẠY NGAY bằng 1 JAR (khuyến nghị, không cần Docker/MySQL) ⭐

### Người nhận app chỉ cần làm:
1. Cài [Java 21+](https://adoptium.net/) (bản JRE là đủ).
2. Nhận 2 thứ từ bạn:
   - File `hoctap/target/hoctap-0.0.1-SNAPSHOT.jar` (đã build)
   - File `start-app.bat` (Windows) hoặc `start-app.sh` (Linux/macOS)
3. Đặt 2 file cùng 1 thư mục, chạy:

```bat
:: Windows
start-app.bat
```
```bash
# Linux / macOS
chmod +x start-app.sh && ./start-app.sh
```

4. Mở **http://localhost:8081** — xong! 🎉

Chi tiết kỹ thuật: script set `SPRING_PROFILES_ACTIVE=h2` — database nhúng H2
tự tạo file `data/edusmart.mv.db` ngay cạnh file JAR, **dữ liệu giữ nguyên sau khi
tắt/mở lại**. File bài tập upload lưu trong thư mục `uploads/`.

Lần đầu chạy, app tự tạo **dữ liệu mẫu** (4 bài kiểm tra trắc nghiệm, bài tập,
thời gian biểu, 4 tài khoản học sinh) — đăng nhập thử luôn:

| Tài khoản | Mật khẩu |
|---|---|
| `hs1@edusmart.vn` | `HocSinh@123` |
| `hs2@edusmart.vn` | `HocSinh@123` |
| `hs3@edusmart.vn` | `HocSinh@123` |
| `hs4@edusmart.vn` | `HocSinh@123` |

### Bạn (người build) build JAR như sau:
Cần: **Node.js 20+** và **Java 21+** (JDK để build).

```bat
:: Windows
build-all.bat
```
```bash
# Linux / macOS
chmod +x build-all.sh && ./build-all.sh
```

Script: build frontend static export → nhúng vào backend → `mvnw package`
→ ra JAR tại `hoctap/target/hoctap-0.0.1-SNAPSHOT.jar`.

---

## Cách 2 — Dùng Docker (cho máy người nhận CÓ Docker)

```bash
# Database nhúng H2 (đơn giản nhất):
docker compose up -d --build          # → http://localhost:8081

# Hoặc kèm MySQL riêng:
JWT_SECRET="chuoi-bat-ky-dai-hon-32-ky-tu" docker compose -f docker-compose.mysql.yml up -d --build
```

---

## Cách 3 — Chạy dev (sửa code, hot reload)

Mở **2 terminal**:

```bash
# Terminal 1 — backend (cần MySQL đang chạy tại localhost:3306, user root)
cd hoctap
./mvnw.cmd spring-boot:run        # Windows  |  Linux/macOS: ./mvnw spring-boot:run

# Terminal 2 — frontend (tự tải lại khi sửa code)
cd edusmart
npm install
npm run dev                        # → http://localhost:3000
```

Frontend dev mặc định gọi API tại `http://localhost:8081` — đổi ở file
`edusmart/.env.local` (copy từ `.env.example`).

> Không có MySQL trên máy dev? Backend cũng chạy được với H2:
> `SPRING_PROFILES_ACTIVE=h2 ./mvnw.cmd spring-boot:run`

---

## Cấu hình khi deploy thật (biến môi trường)

Backend đọc hết config từ biến môi trường — **không cần sửa file**:

| Biến | Mặc định | Ý nghĩa |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | — | Đặt `h2` = database nhúng (không cần MySQL) |
| `DB_URL` | `jdbc:mysql://localhost:3306/edusmart...` | Kết nối MySQL |
| `DB_USERNAME` / `DB_PASSWORD` | `root` / `123456` | Đăng nhập MySQL |
| `JWT_SECRET` | chuỗi demo | **Bắt buộc đổi** khi chạy thật (≥32 ký tự) |
| `SERVER_PORT` | `8081` | Port HTTP |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://localhost:3001` | Origin frontend được gọi API. Dùng `*` để mở mọi origin; khi frontend nhúng cùng JAR thì không cần đổi |
| `UPLOAD_DIR` | `./uploads` | Nơi lưu file bài tập |
| `SEED_DATA` | `true` | `false` = không tạo dữ liệu mẫu khi DB trống |
| `MAIL_ENABLED` | `false` | `true` khi đã tích hợp mail gửi token reset |

Ví dụ chạy production với MySQL riêng:

```bash
java -jar hoctap.jar \
  -DSPRING_PROFILES_ACTIVE= \
  -DDB_URL="jdbc:mysql://db.mysite.com:3306/edusmart?useSSL=true&serverTimezone=Asia/Ho_Chi_Minh" \
  -DDB_USERNAME=edusmart -DDB_PASSWORD=*** \
  -DJWT_SECRET="chuoi-ngau-nhien-dai-hon-32-ky-tu-an-toan" \
  -DCORS_ALLOWED_ORIGINS="https://mysite.com" \
  -DUPLOAD_DIR=/var/data/edusmart/uploads
```

> **Google Login:** backend đã sẵn sàng (`POST /api/auth/google`). Để bật nút
> Google trên frontend: tạo OAuth 2.0 Client ID trên Google Cloud Console và đặt
> `NEXT_PUBLIC_GOOGLE_CLIENT_ID` trong `edusmart/.env.local` rồi build lại
> (hướng dẫn chi tiết trong `edusmart/components/GoogleButton.js`).
> Không cấu hình → nút Google vẫn hiện nhưng báo hướng dẫn khi bấm.

---

## Tính năng AI Agent — tạo đề thi / bài tập từ file 🤖

Nút **"Tạo bằng AI"** trong tab Bài kiểm tra: tải lên file **PDF, DOC, DOCX, TXT** (≤15MB), AI đọc và tự ra đề:

- **Chế độ Đề thi**: câu hỏi trắc nghiệm bám sát tài liệu; **thời gian phòng thi tự khớp thời gian làm bài GHI TRONG tài liệu** (vd đề ghi "Thời gian làm bài: 90 phút" → phòng thi 90 phút), hoặc tự chọn thời gian. Chấm tự động theo đáp án.
- **Chế độ Bài tập**: câu hỏi trắc nghiệm **có GIẢI THÍCH đáp án** (hiện sau khi nộp), không giới hạn thời gian.
- Chọn **lớp/đối tượng** (Năm nhất → Năm tư, Lớp 10 → 12) để AI điều chỉnh độ khó.
- File mẫu để thử: `samples/test-theory.pdf` (tài liệu lý thuyết), `samples/test-exam-90phut.pdf` (đề thi ghi 90 phút), kèm `samples/mock-ai-server.py` (mock server để test không cần API key).

### Cấu hình AI (biến môi trường — chuẩn OpenAI-compatible, dùng được nhiều nhà cung cấp)

| Biến | Mặc định | Ý nghĩa |
|---|---|---|
| `AI_API_KEY` | *(trống — AI tắt)* | API key. **Gemini miễn phí**: lấy tại aistudio.google.com |
| `AI_BASE_URL` | `https://generativelanguage.googleapis.com/v1beta/openai` | Endpoint OpenAI-compatible |
| `AI_MODEL` | `gemini-flash-latest` | Tên model |
| `AI_MAX_TOKENS` | `8192` | Tăng nếu model "suy nghĩ" lâu (qwen3...) |

Ví dụ:

```bash
# Gemini (miễn phí, khuyến nghị) — chỉ cần key:
set AI_API_KEY=AIza...

# Ollama chạy tại máy:
set AI_BASE_URL=http://localhost:11434/v1
set AI_MODEL=qwen3:4b
set AI_API_KEY=ollama
```

## Các tính năng đã hoàn thiện

- **Đăng ký / đăng nhập** email + JWT (access/refresh tự động), **Google Sign-In**
- **Quên / đặt lại mật khẩu** (token 1 lần, hết hạn 30 phút — trả token trực tiếp vì chưa có mail server)
- **Thời gian biểu** CRUD đầy đủ, lọc theo ngày, lịch hôm nay trên Dashboard
- **Bài kiểm tra trắc nghiệm**: vào thi có đồng hồ đếm ngược, **chấm điểm trên server**,
  cộng xu +15/câu đúng, xem đáp án chi tiết sau khi nộp, **lưu lịch sử làm bài**
- **Bài tập về nhà**: lọc trạng thái, cảnh báo quá hạn, **nộp bài kèm file** (≤5MB),
  xem lại file đã nộp, +20 xu khi nộp
- **Bảng xếp hạng**: Tổng / **Tuần này** / **Tháng này** (xếp theo xu kiếm được trong kỳ),
  highlight chính mình, avatar từ tên
- **Thông báo**: chuông header hiện bài tập sắp đến hạn / quá hạn
- **AI Agent tạo đề thi/bài tập từ file** (PDF/DOC/DOCX/TXT — chi tiết ở trên)
- **Swagger API docs**: http://localhost:8081/swagger-ui.html
- Frontend nhúng trong JAR — deploy 1 file duy nhất

## Cấu trúc thư mục

```
apphoctap/
├── build-all.bat / .sh     # Build 1 JAR duy nhất
├── start-app.bat / .sh     # Chạy JAR (H2, không cần MySQL/Docker)
├── Dockerfile              # Image cả app (cho ai có Docker)
├── docker-compose.yml      # Chạy với H2
├── docker-compose.mysql.yml# Chạy với MySQL
├── edusmart/               # Frontend Next.js 16 (React 19, Tailwind 4)
│   ├── .env.example        # Biến môi trường mẫu
│   └── app/ components/ lib/
└── hoctap/                 # Backend Spring Boot 4 (Java 21)
    ├── src/main/java/backend/hoctap/
    │   ├── config/         # Security, CORS, WebMvc, SpaForward, DataSeeder
    │   ├── controller/ service/ repository/ entity/ dto/ filter/ util/
    │   └── ...
    └── src/main/resources/
        ├── application.properties      # config chính (đọc biến môi trường)
        └── application-h2.properties   # profile database nhúng
```
