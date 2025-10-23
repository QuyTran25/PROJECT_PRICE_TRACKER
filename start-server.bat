@echo off
chcp 65001 >nul
echo ================================================
echo    🚀 PRICE TRACKER - HTTP SERVER STARTER
echo ================================================
echo.

cd /d "%~dp0server"

echo [1/3] 📦 Compiling Java files...
echo.

javac -encoding UTF-8 -d bin -cp "lib/*;../shared/src" ^
    src/com/pricetracker/server/http/SimpleHttpServer.java ^
    src/com/pricetracker/server/db/*.java ^
    src/com/pricetracker/server/utils/*.java ^
    src/com/pricetracker/server/crypto/*.java ^
    src/com/pricetracker/server/core/*.java ^
    src/com/pricetracker/server/handler/*.java ^
    ../shared/src/com/pricetracker/models/*.java

if %errorlevel% neq 0 (
    echo.
    echo ❌ Compilation FAILED!
    echo Please check the error messages above.
    pause
    exit /b 1
)

echo.
echo ✅ Compilation successful!
echo.
echo [2/3] 🔍 Checking MySQL connection...
echo Make sure XAMPP MySQL is running on port 3306
echo.

echo [3/3] 🌐 Starting HTTP Server...
echo.
echo ================================================
echo    ✨ SERVER IS STARTING...
echo ================================================
echo.
echo 📡 Frontend can connect at: http://localhost:8080/search
echo 🌐 Open your browser: http://127.0.0.1:5500/frontend/HTML/Trangchu.html
echo.
echo Press Ctrl+C to stop the server
echo ================================================
echo.

java -cp "bin;lib/*;../shared/src" com.pricetracker.server.http.SimpleHttpServer

pause
