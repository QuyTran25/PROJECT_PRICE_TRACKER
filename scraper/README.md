# 🤖 HƯỚNG DẪN SỬ DỤNG - AUTO SCRAPER

> **Mục đích:** Tự động cào dữ liệu giá từ **Tiki** và **Lazada** 3 lần/ngày (8h, 16h, 0h)

---

## 🚀 CÁCH SỬ DỤNG

### 1️⃣ **Lần đầu tiên - Cài đặt:**

```bash
# Bước 1: Cài Python packages
pip install -r requirements.txt

# Bước 2: Tạo file cấu hình
copy config.ini.example config.ini
notepad config.ini  # Sửa thông tin email, database

# Bước 3: Kiểm tra database
python check_db.py

# Bước 4: Chạy thử scraper
run_scraper.bat             # Cào Tiki
run_scraper_lazada.bat      # Cào Lazada
```

### 2️⃣ **Cài Task Scheduler (Tự động):**

**Cách 1: Tự động (30 giây)**
```bash
# Click chuột phải > Run as Administrator
install_scheduler.bat
```

**Cách 2: Thủ công (2 phút)**
1. Nhấn `Win + R` → gõ `taskschd.msc`
2. Action → Import Task → chọn `task_scheduler.xml`
3. Tab Actions → Edit → Sửa `FULL_PATH_TO_SCRAPER` thành `d:\PROJECT_PRICE_TRACKER\scraper`
4. Click OK

### 3️⃣ **Kiểm tra Task:**

```bash
check_scheduler.bat
```

---

## 📅 LỊCH CHẠY

**Hôm nay (04/11/2025):** 8h, **13h** (test), 16h, 0h  
**Từ ngày mai (05/11 - 29/11/2025):** 8h, 16h, 0h  
**Sau 29/11:** Tắt tự động

---

## 📧 EMAIL THÔNG BÁO

Sau mỗi lần chạy, email sẽ được gửi đến:
- tranhtq0763@ut.edu.vn
- trangntt2921@ut.edu.vn
- khoanda5875@ut.edu.vn
- khainq4248@ut.edu.vn
- sanglv2108@ut.edu.vn

---

## 📂 CẤU TRÚC FILES

```
scraper/
├── scraper.py              # Script cào Tiki (Tầng 1)
├── scraper_lazada.py       # Script cào Lazada (Tầng 1)
├── check_db.py             # Kiểm tra MySQL
├── send_email.py           # Gửi email thông báo
├── run_scraper.bat         # Chạy scraper Tiki
├── run_scraper_lazada.bat  # Chạy scraper Lazada
├── install_scheduler.bat   # Cài Task Scheduler
├── check_scheduler.bat     # Kiểm tra Task Scheduler
├── task_scheduler.xml      # Template Task Scheduler
├── config.ini.example      # Template cấu hình (push Git)
├── config.ini              # Cấu hình thật (KHÔNG push)
├── requirements.txt        # Python dependencies
├── initial_setup.py        # Setup ban đầu
└── logs/                   # Log files
```

---

## 🔧 CẤU HÌNH (config.ini)

```ini
[DATABASE]
host = localhost
database = price_insight
user = root
password = 

[SCRAPER]
delay_between_requests = 2
max_retries = 3
retry_delay = 60
```

---

## 🐛 XỬ LÝ LỖI

### ❌ Lỗi: "Không kết nối được database"
```bash
# Giải pháp:
1. Bật XAMPP → Start MySQL
2. python check_db.py
```

### ❌ Lỗi: "Email không gửi được"
```bash
# Kiểm tra:
1. Email/password đúng chưa?
2. smtp_server = smtp-mail.outlook.com (hoặc smtp.office365.com)
3. python send_email.py success  # Test
```

### ❌ Lỗi: "Task không chạy"
```bash
# Kiểm tra:
1. Win + R → taskschd.msc
2. Tìm task "PriceTracker_AutoScraper"
3. Xem tab History để biết lý do
```

---

## 📊 XEM LOG

```bash
# Xem log ngày hôm nay
type logs\scraper_20251104.log

# Xem 50 dòng cuối
powershell Get-Content logs\scraper_*.log -Tail 50
```

---

## 🔒 BẢO MẬT

⚠️ **QUAN TRỌNG:**
- ❌ **KHÔNG** commit file `config.ini` (đã thêm vào `.gitignore`)
- ❌ **KHÔNG** commit folder `logs/`
- ✅ Chỉ commit `config.ini.example` (template)

---

## 💡 TIPS

**Chạy thủ công:**
```bash
# Tiki
python scraper.py           # Chỉ cào Tiki
run_scraper.bat            # Cào Tiki + gửi email

# Lazada
python scraper_lazada.py    # Chỉ cào Lazada
run_scraper_lazada.bat     # Cào Lazada + gửi email
```

**Chạy ngay không chờ lịch:**
```bash
schtasks /Run /TN "PriceTracker_AutoScraper"
```

**Tắt Task Scheduler:**
```bash
schtasks /Change /TN "PriceTracker_AutoScraper" /DISABLE
```

**Bật lại:**
```bash
schtasks /Change /TN "PriceTracker_AutoScraper" /ENABLE
```

---

## 🆕 TÍNH NĂNG MỚI: LAZADA SCRAPER

### Đặc điểm:
- ✅ **Dữ liệu 100% thật** từ Lazada.vn (đã verify)
- ✅ **Image URL đầy đủ** cho tất cả sản phẩm
- ✅ **Kiến trúc 2-tầng** giống Tiki:
  - **Tầng 1 (Python):** Background scraping - `scraper_lazada.py`
  - **Tầng 2 (Java):** On-demand scraping - `LazadaScraperUtil.java`
- ✅ **Deal type detection:** Flash Sale, HOT Deal, Trending
- ✅ **Retry mechanism:** 3 attempts, 60s delay
- ✅ **Database logging:** scrape_log, error_log

### So sánh Tiki vs Lazada:

| Tính năng           | Tiki                | Lazada              |
|---------------------|---------------------|---------------------|
| Data source         | API (JSON)          | HTML parsing (Regex)|
| Price extraction    | ✅ 100%             | ✅ 100%             |
| Image URL           | ✅ Đầy đủ           | ✅ Đầy đủ           |
| Deal detection      | ✅ 4 loại           | ✅ 4 loại           |
| 2-tier architecture | ✅ Python + Java    | ✅ Python + Java    |

---

**Phát triển bởi:** Nhóm 19 - Bài tập lớn Lập trình mạng  
**Cập nhật:** 04/11/2025 - Thêm Lazada scraper với đầy đủ tính năng
