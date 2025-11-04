@echo off
chcp 65001 >nul
echo.
echo ================================================
echo    LAZADA PRICE SCRAPER - Auto Runner
echo ================================================
echo.

REM Lấy đường dẫn thư mục hiện tại
set SCRIPT_DIR=%~dp0
cd /d "%SCRIPT_DIR%"

echo [1/3] 📂 Kiểm tra môi trường...
echo      Working directory: %CD%
echo.

REM Kiểm tra Python
python --version >nul 2>&1
if errorlevel 1 (
    echo ❌ ERROR: Python chưa được cài đặt
    echo    Vui lòng cài Python từ https://www.python.org/
    pause
    exit /b 1
)
echo      ✓ Python đã cài đặt
echo.

echo [2/3] 🕷️  Chạy Lazada Scraper...
echo.
python scraper_lazada.py
set SCRAPER_EXIT_CODE=%errorlevel%
echo.

echo [3/3] 📧 Gửi email thông báo...
echo.
if %SCRAPER_EXIT_CODE% equ 0 (
    echo ✅ Scraper chạy thành công
    python send_email.py success "Lazada scraper completed successfully"
) else (
    echo ❌ Scraper gặp lỗi
    python send_email.py failed "Lazada scraper failed with exit code %SCRAPER_EXIT_CODE%"
)
echo.

echo ================================================
echo    HOÀN TẤT - Kiểm tra log trong thư mục logs/
echo ================================================
echo.

REM Tự động thoát nếu được chạy bởi Task Scheduler
REM Nếu chạy thủ công, pause để xem kết quả
if "%1"=="auto" (
    timeout /t 3 >nul
) else (
    pause
)

exit /b %SCRAPER_EXIT_CODE%
