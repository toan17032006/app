@echo off
REM ════════════════════════════════════════════════════════════════
REM  EduSmart — Chạy app bằng 1 JAR duy nhất
REM  KHÔNG cần Docker, KHÔNG cần MySQL (dữ liệu lưu H2 tại ./data)
REM  App mở tại: http://localhost:8081
REM  Đăng nhập thử: hs1@edusmart.vn / HocSinh@123
REM ════════════════════════════════════════════════════════════════
setlocal
cd /d "%~dp0"

if not exist "hoctap\target\hoctap-0.0.1-SNAPSHOT.jar" (
    echo Chua co file JAR! Hay chay build-all.bat truoc.
    pause
    exit /b 1
)

REM Profile h2 = database nhung san, tu tao file ./data/edusmart
REM Muon dung MySQL: set DB_URL, DB_USERNAME, DB_PASSWORD va bo dong SPRING_PROFILES_ACTIVE
set SPRING_PROFILES_ACTIVE=h2

REM ══ AI Agent (Gemini) ════════════════════════════════════════════
REM Neu co file ai-key.txt (cung thu muc, chua API key Gemini) -> tu nap key
REM Lay key mien phi tai: https://aistudio.google.com/apikey
if exist "ai-key.txt" (
    set /p AI_API_KEY=<ai-key.txt
)

REM ══ Google Sign-In ═══════════════════════════════════════════════
REM Neu co file google-client-id.txt (chua OAuth Client ID dang
REM xxxx.apps.googleusercontent.com) -> tu nap de bat nut Dang nhap bang Google
if exist "google-client-id.txt" (
    set /p GOOGLE_CLIENT_ID=<google-client-id.txt
)

echo.
echo  EduSmart dang khoi dong... mo http://localhost:8081 sau ~10 giay
echo  (Ctrl+C de thoat)
echo.
java -jar "hoctap\target\hoctap-0.0.1-SNAPSHOT.jar"
pause
