@echo off
REM ════════════════════════════════════════════════════════════════
REM  EduSmart — Build toàn bộ app thành 1 file JAR duy nhất
REM  Kết quả: hoctap\target\hoctap-0.0.1-SNAPSHOT.jar
REM  Yêu cầu: Node.js 20+ và Java 21+ đã cài trên máy
REM ════════════════════════════════════════════════════════════════
setlocal
cd /d "%~dp0"

echo.
echo [1/4] Build frontend (Next.js static export)...
cd edusmart
call npm install || goto :err
call npm run build || goto :err
cd ..

echo.
echo [2/4] Copy frontend vao backend (classpath:/static)...
if exist hoctap\src\main\resources\static rmdir /s /q hoctap\src\main\resources\static
xcopy /e /i /q /y edusmart\out hoctap\src\main\resources\static >nul || goto :err

echo.
echo [3/4] Build backend JAR (Spring Boot)...
cd hoctap
call mvnw.cmd clean package -DskipTests || goto :err
cd ..

echo.
echo [4/4] HOAN THANH!
echo    File JAR: hoctap\target\hoctap-0.0.1-SNAPSHOT.jar
echo    Chay app: start-app.bat   (hoac: java -jar hoctap\target\hoctap-0.0.1-SNAPSHOT.jar)
echo.
goto :eof

:err
echo.
echo  *** BUILD THAT BAI — xem thong bao loi o tren ***
exit /b 1
