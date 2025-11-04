@echo off
chcp 65001 >nul
echo ═══════════════════════════════════════════════════
echo   🚀 PRICE TRACKER - QUICK START
echo ═══════════════════════════════════════════════════
echo.

REM Kiểm tra key đã set chưa
if not defined PRICE_TRACKER_KEY (
    echo ❌ CHƯA SET KEY!
    echo.
    echo Bước 1: Tạo key
    echo    java tools.AESKeyGenerator
    echo.
    echo Bước 2: Copy key và set
    echo    set PRICE_TRACKER_KEY=^<key-vừa-tạo^>
    echo.
    echo Bước 3: Chạy lại script này
    echo    quick-start.bat
    echo.
    pause
    exit /b 1
)

echo ✅ Key đã được set
echo Key: %PRICE_TRACKER_KEY:~0,30%...
echo.

REM Compile nếu cần
echo [1/4] 📦 Đang compile...
if not exist "shared\bin\com\pricetracker\security\AESUtil.class" (
    echo    └─ Compiling security modules...
    javac -d shared\bin -encoding UTF-8 shared\src\com\pricetracker\security\*.java 2>nul
)

if not exist "shared\bin\com\pricetracker\models\Product.class" (
    echo    └─ Compiling shared models...
    javac -d shared\bin -encoding UTF-8 shared\src\com\pricetracker\models\*.java 2>nul
)

if not exist "server\bin\com\pricetracker\server\Main.class" (
    echo    └─ Compiling server...
    javac -d server\bin -cp "shared\bin;server\lib\*" -encoding UTF-8 server\src\com\pricetracker\server\*.java server\src\com\pricetracker\server\**\*.java 2>nul
)

echo    └─ ✅ Compile hoàn tất
echo.

REM Kiểm tra MySQL
echo [2/4] 🔍 Kiểm tra MySQL...
netstat -an | findstr ":3306" >nul
if errorlevel 1 (
    echo    └─ ⚠️  MySQL chưa chạy!
    echo       Vui lòng mở XAMPP và start MySQL
    echo.
    pause
    exit /b 1
) else (
    echo    └─ ✅ MySQL đang chạy
)
echo.

REM Test encryption
echo [3/4] 🔐 Test mã hóa...
java -cp shared\bin TestEncryption >nul 2>&1
if errorlevel 1 (
    echo    └─ ⚠️  Encryption test failed
) else (
    echo    └─ ✅ Encryption OK
)
echo.

REM Khởi động server
echo [4/4] 🚀 Khởi động server...
echo.
echo ═══════════════════════════════════════════════════
echo   ✨ SERVER ĐANG CHẠY...
echo ═══════════════════════════════════════════════════
echo.
echo 📡 Server: http://localhost:8888
echo 🌐 Frontend: http://127.0.0.1:5500/frontend/HTML/Trangchu.html
echo.
echo Press Ctrl+C để dừng server
echo ═══════════════════════════════════════════════════
echo.

cd server
java -cp "..\shared\bin;bin;lib\*" com.pricetracker.server.Main
