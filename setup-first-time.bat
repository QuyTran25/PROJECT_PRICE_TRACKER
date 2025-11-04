@echo off
chcp 65001 >nul
echo ═══════════════════════════════════════════════════
echo   🔑 SETUP KEY - LẦN ĐẦU DÙNG PROJECT
echo ═══════════════════════════════════════════════════
echo.
echo Hướng dẫn này sẽ giúp bạn:
echo 1. Tạo AES-256 encryption key
echo 2. Set key vào environment
echo 3. Test xem key hoạt động chưa
echo.
pause

echo.
echo [1/3] 🔐 Đang tạo encryption key...
echo.

REM Compile KeyGenerator nếu chưa
if not exist "tools\AESKeyGenerator.class" (
    javac tools\AESKeyGenerator.java
)

REM Chạy KeyGenerator và lưu output
java tools.AESKeyGenerator > temp_key_output.txt

REM Hiển thị output
type temp_key_output.txt
echo.

REM Extract key từ output (dòng giữa ===)
for /f "tokens=*" %%a in ('findstr /v "=" temp_key_output.txt ^| findstr /v "Key" ^| findstr /v "Generating" ^| findstr /v "Hướng" ^| findstr /v "Copy" ^| findstr /v "Set" ^| findstr /v "Windows" ^| findstr /v "Linux" ^| findstr /v "LƯU" ^| findstr /v "KHÔNG" ^| findstr /v "Server" ^| findstr /v "Chia" ^| findstr /v "Thông" ^| findstr /v "Algorithm" ^| findstr /v "Key Size" ^| findstr /v "Entropy"') do (
    set "GENERATED_KEY=%%a"
    goto :found_key
)

:found_key
del temp_key_output.txt

if "%GENERATED_KEY%"=="" (
    echo ❌ Không thể tạo key tự động
    echo Vui lòng chạy thủ công: java tools.AESKeyGenerator
    pause
    exit /b 1
)

echo.
echo [2/3] 💾 Đang lưu key...
echo.
echo Key của bạn:
echo ═══════════════════════════════════════════════════
echo %GENERATED_KEY%
echo ═══════════════════════════════════════════════════
echo.

REM Set key vào environment
set PRICE_TRACKER_KEY=%GENERATED_KEY%
setx PRICE_TRACKER_KEY "%GENERATED_KEY%" >nul 2>&1

echo ✅ Key đã được set!
echo.
echo ⚠️  LƯU Ý:
echo    • Key đã được lưu vào System Environment
echo    • Hãy lưu key này vào nơi an toàn
echo    • Chia sẻ key cho team qua kênh riêng tư
echo    • KHÔNG commit key vào Git
echo.

echo.
echo [3/3] 🧪 Test encryption...
echo.

REM Compile security classes
javac -d shared\bin -encoding UTF-8 shared\src\com\pricetracker\security\*.java 2>nul
javac -cp shared\bin -d shared\bin test\TestEncryption.java 2>nul

REM Run test
java -cp shared\bin TestEncryption
echo.

echo ═══════════════════════════════════════════════════
echo   ✅ SETUP HOÀN TẤT!
echo ═══════════════════════════════════════════════════
echo.
echo Tiếp theo:
echo 1. Đóng terminal này và mở terminal mới
echo 2. Chạy: quick-start.bat
echo.
pause
