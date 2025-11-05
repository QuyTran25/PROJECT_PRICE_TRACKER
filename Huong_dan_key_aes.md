# 🔐 PRICE TRACKER - HƯỚNG DẪN SETUP & SỬ DỤNG

## � BẢNG TỔNG HỢP: KHI NÀO CẦN/KHÔNG CẦN KEY

| Tình huống | Component | Cần Key? | Lý do |
|-----------|-----------|----------|-------|
| User vào website tìm SP | Browser → HTTP Server | ❌ KHÔNG | HTTP thuần, không mã hóa |
| Python cào dữ liệu | Scraper → MySQL | ❌ KHÔNG | Chỉ lưu DB, không qua Java |
| Xem DB trong phpMyAdmin | Browser → MySQL | ❌ KHÔNG | Web interface MySQL |
| Chạy Java Desktop Client | NetworkClient → Socket | ✅ CẦN | Gọi KeyManager.getKey() |
| Chạy Socket Server | PriceTrackerServer | ✅ CẦN | ClientHandler cần key |
| Test encryption | TestEncryption.java | ✅ CẦN | Test AESUtil |
| Compile code | javac ... | ❌ KHÔNG | Chỉ biên dịch |
| Query DB trực tiếp | mysql -u root | ❌ KHÔNG | MySQL client |

### 🎯 QUY TẮC ĐƠN GIẢN:

#### ❌ KHÔNG cần key khi:
- ✓ Code KHÔNG gọi `KeyManager.getKey()`
- ✓ Code KHÔNG dùng `AESUtil.encrypt/decrypt`
- ✓ Chỉ làm việc với HTTP/MySQL
- ✓ Frontend (HTML/JS)
- ✓ Python scripts

#### ✅ CẦN key khi:
- ✓ Code GỌI `KeyManager.getKey()`
- ✓ Code DÙNG `AESUtil.encrypt/decrypt`
- ✓ Java Socket communication
- ✓ NetworkClient hoặc ClientHandler
- ✓ Test encryption

---

## �📋 MỤC LỤC
1. [Tổng quan hệ thống](#tổng-quan)
2. [Cài đặt môi trường](#cài-đặt)
3. [Setup mã hóa AES](#setup-mã-hóa)
4. [Chạy ứng dụng](#chạy-ứng-dụng)
5. [Cấu trúc project](#cấu-trúc)

---

## 🎯 TỔNG QUAN

**Price Tracker** là hệ thống theo dõi giá sản phẩm từ Tiki với các tính năng:

- ✅ Theo dõi lịch sử giá sản phẩm
- ✅ Biểu đồ biến động giá
- ✅ Đánh giá và nhận xét
- ✅ Thu thập dữ liệu tự động
- ✅ **Mã hóa AES-256/GCM** (bảo mật communication)

**Kiến trúc:**
```
Client (Java/Web) ←→ [AES Encryption] ←→ Server (Java) ←→ MySQL
                                               ↓
                                        Python Scraper
```

---

## 🔧 CÀI ĐẶT

### Yêu cầu hệ thống:
- **Java JDK:** 11 trở lên
- **MySQL:** 8.0+ (XAMPP)
- **Python:** 3.8+ (cho scraper)
- **IDE:** VS Code hoặc IntelliJ IDEA

### Cài đặt dependencies:

```bash
# Python packages
cd scraper
pip install -r requirements.txt

# MySQL: Import database
# 1. Mở XAMPP, start MySQL
# 2. Vào phpMyAdmin
# 3. Import file: cauTrucCSDL.md
```

---

## 🔐 SETUP MÃ HÓA AES
Lưu ý trong quá trình làm việc, terminal set key không được xóa, nếu xóa phải set khóa mới, và terminal đó chỉ được dùng để set key, muốn dùng cái khác thì phải mở terminal khác

### **BƯỚC 1: TẠO KEY** 🔑

Mở Terminal trong VS Code (`Ctrl + ~`):

```bash
# Di chuyển vào thư mục project
cd d:\LTM\PROJECT_PRICE_TRACKER

# Compile file tạo key
javac tools\AESKeyGenerator.java

# Chạy để sinh key
java tools.AESKeyGenerator
```

**Kết quả sẽ hiển thị:**
```
🔐 Generating AES-256 Key...

✅ Key đã được tạo thành công! lấy khóa ở trong 2 đường này hoặc 2 đường "?"

═══════════════════════════════════════════════════════════════
J+MGnG6P7Q5p8nzu1NqgvpQLbKUtwRmWPtmg/lloHOc=
═══════════════════════════════════════════════════════════════
```

**📝 COPY KEY** và lưu lại an toàn!

---

### **BƯỚC 2: SET KEY VÀO ENVIRONMENT** �

**Trong terminal VS Code (terminal vừa chạy ở Bước 1):**

#### Nếu dùng CMD:
```cmd
set PRICE_TRACKER_KEY=J+MGnG6P7Q5p8nzu1NqgvpQLbKUtwRmWPtmg/lloHOc=
```

#### Nếu dùng terminal vscode:
```terminal
$env:PRICE_TRACKER_KEY="J+MGnG6P7Q5p8nzu1NqgvpQLbKUtwRmWPtmg/lloHOc="
```

**Kiểm tra đã set thành công:**
```cmd
# CMD
echo %PRICE_TRACKER_KEY%

# PowerShell
echo $env:PRICE_TRACKER_KEY
```

> ⚠️ **LƯU Ý:** Key chỉ tồn tại trong terminal hiện tại. Mở terminal mới phải set lại!

---

### **BƯỚC 3: TEST MÃ HÓA** ✅

```bash
# Compile security classes
javac -d bin -encoding UTF-8 shared\src\com\pricetracker\security\*.java

# Compile test
javac -cp bin -d bin test\TestEncryption.java

# Chạy test
java -cp bin TestEncryption
```

**Kết quả mong đợi:**
```
🔐 TEST MÃ HÓA AES-256/GCM

TEST 1: Mã hóa chuỗi tiếng Việt
✅ PASS - Giải mã chính xác

TEST 2: Mã hóa JSON data
✅ PASS - JSON giải mã chính xác

TEST 3: IV Uniqueness
✅ PASS - Mỗi lần mã hóa cho ra kết quả khác nhau

TEST 4: Tampering Detection
✅ PASS - GCM phát hiện được tampering

═══════════════════════════════════════════════════════════════
           ✅ TẤT CẢ TESTS PASS!
═══════════════════════════════════════════════════════════════
```

> ✅ **Nếu tất cả tests PASS** → Mã hóa đã sẵn sàng!

---

### **BƯỚC 4: COMPILE PROJECT** 📦

```bash
# Compile shared modules (models + security)
javac -d shared\bin -encoding UTF-8 shared\src\com\pricetracker\**\*.java

# Compile server
javac -d server\bin -cp "shared\bin;server\lib\*" -encoding UTF-8 server\src\com\pricetracker\server\**\*.java

# Compile client (nếu có)
javac -d client\bin -cp "shared\bin;client\lib\*" -encoding UTF-8 client\src\com\pricetracker\client\**\*.java
```

---

## 🚀 CHẠY ỨNG DỤNG

### **1. Khởi động MySQL**
```
1. Mở XAMPP
2. Start MySQL
3. Kiểm tra database: price_insight
```

---

### **2. Chạy Server**

**Trong terminal đã set key (Bước 2):**

```bash
cd server
java -cp "..\shared\bin;bin;lib\*" com.pricetracker.server.Main
```

**Hoặc dùng script:**
```bash
start-server.bat
```

**Kết quả thành công:**
```
===========================================
  PRICE TRACKER SERVER
===========================================
Server đang khởi động trên port: 8888

✅ Encryption key loaded successfully
✓ Server đã sẵn sàng và đang lắng nghe tại port 8888
✓ Thread pool đã được khởi tạo với 50 luồng
✓ Đang chờ kết nối từ client...
```

---

### **3. Chạy Client**

**Terminal mới (nhớ set key trước!):**

```bash
# Set key
set PRICE_TRACKER_KEY=J+MGnG6P7Q5p8nzu1NqgvpQLbKUtwRmWPtmg/lloHOc=

# Chạy client
cd client
java -cp "..\shared\bin;bin;lib\*" com.pricetracker.client.Main
```

---

### **4. Chạy Frontend (Web)**

```
1. Cài Live Server extension trong VS Code
2. Mở file: frontend/HTML/Trangchu.html
3. Click chuột phải → "Open with Live Server"
4. Truy cập: http://127.0.0.1:5500/frontend/HTML/Trangchu.html
```

---

### **5. Chạy Scraper (Tùy chọn)**

```bash
cd scraper
python scraper.py
```

---

## 📁 CẤU TRÚC PROJECT

```
PROJECT_PRICE_TRACKER/
├── � client/                    # Java Client (Desktop)
│   └── src/com/pricetracker/client/
│       ├── Main.java
│       ├── net/
│       │   └── NetworkClient.java    # ✅ Đã tích hợp mã hóa
│       └── ui/
│
├── 📂 server/                    # Java Server
│   └── src/com/pricetracker/server/
│       ├── Main.java
│       ├── core/
│       │   └── PriceTrackerServer.java
│       ├── handler/
│       │   └── ClientHandler.java    # ✅ Đã tích hợp mã hóa
│       └── db/
│
├── 📂 shared/                    # Code dùng chung
│   └── src/com/pricetracker/
│       ├── models/              # Product, PriceHistory, etc.
│       └── security/            # 🔐 MÃ HÓA AES
│           ├── KeyManager.java
│           ├── AESUtil.java
│           └── SecureLogger.java
│
├── 📂 frontend/                  # Web UI
│   ├── HTML/
│   ├── CSS/
│   └── JS/
│
├── 📂 scraper/                   # Python Scraper
│   ├── scraper.py
│   └── requirements.txt
│
├── 📂 tools/                     # Utilities
│   └── AESKeyGenerator.java    # Tool tạo key
│
├── 📂 test/                      # Test files
│   └── TestEncryption.java
│
├── 📄 cauTrucCSDL.md            # Database schema
├── 📄 start-server.bat          # Script khởi động server
└── 📄 README.md                 # File này
```

---

## 👥 CHO TEAM

### **Chia sẻ project cho team:**

1. **Leader tạo key và chia sẻ:**
   ```bash
   java tools.AESKeyGenerator
   ```
   
2. **Gửi key cho team qua Discord/Telegram:**
   ```
   Key cho team (Dev):
   J+MGnG6P7Q5p8nzu1NqgvpQLbKUtwRmWPtmg/lloHOc=
   ```

3. **Team member:**
   ```bash
   # Clone project
   git clone <repo-url>
   
   # Set key
   set PRICE_TRACKER_KEY=<key-từ-leader>
   
   # Compile và chạy
   ```

> ⚠️ **Server và Client phải dùng CÙNG key!**

---

## 🐛 TROUBLESHOOTING

### **Lỗi: "Missing encryption key!"**

**Nguyên nhân:** Chưa set environment variable.

**Cách fix:**
```bash
set PRICE_TRACKER_KEY=J+MGnG6P7Q5p8nzu1NqgvpQLbKUtwRmWPtmg/lloHOc=
```

---

### **Lỗi: "Tag mismatch" hoặc "Authentication failed"**

**Nguyên nhân:** Client và Server dùng khác key.

**Cách fix:** Đảm bảo cả 2 dùng **CÙNG key**.

---

### **Lỗi: "Connection refused"**

**Nguyên nhân:** Server chưa chạy hoặc port bị chiếm.

**Cách fix:**
1. Kiểm tra server đã start chưa
2. Kiểm tra port 8888 có bị chiếm không

---

## 📚 TÀI LIỆU THÊM

- **Chi tiết mã hóa:** [SETUP_ENCRYPTION.md](SETUP_ENCRYPTION.md)
- **Hướng dẫn nhanh:** [ENCRYPTION_QUICKSTART.md](ENCRYPTION_QUICKSTART.md)
- **Database:** [cauTrucCSDL.md](cauTrucCSDL.md)
- **Scraper:** [taiLieuHuongDanCaoDuLieu.md](taiLieuHuongDanCaoDuLieu.md)

---

## ✨ TÍNH NĂNG BẢO MẬT

- ✅ **AES-256/GCM** - Chuẩn quốc tế
- ✅ **Authenticated Encryption** - Phát hiện tampering
- ✅ **Key Management** - Không lưu trong code
- ✅ **IV Unique** - Mỗi lần mã hóa khác nhau
- ✅ **Base64 Encoding** - Dễ truyền tải

---

## 📞 HỖ TRỢ

Gặp vấn đề? Liên hệ:
- 📧 Email: [your-email]
- 💬 Discord: [your-discord]
- 🐛 Issues: [GitHub Issues]

---

**Version:** 2.0 (with AES-256 Encryption)  
**Last Updated:** November 4, 2025  
**Status:** ✅ Production Ready

# ⚡ QUICK START - 3 BƯỚC

## 🚀 Lần đầu sử dụng:

### Bước 1: Setup key (1 lần duy nhất)
```bash
setup-first-time.bat
```
**→ Follow hướng dẫn, copy key được tạo**

---

### Bước 2: Khởi động server
```bash
quick-start.bat
```
**→ Server tự động compile và chạy**

---

### Bước 3: Mở frontend
```
Mở file: frontend/HTML/Trangchu.html
Click chuột phải → Open with Live Server
```

---

## 🔄 Lần sau sử dụng:

```bash
# Chỉ cần chạy 1 lệnh:
quick-start.bat
```

---

## ❓ Gặp lỗi?

### "Missing encryption key!"
```bash
# Set lại key (thay YOUR_KEY bằng key của bạn)
set PRICE_TRACKER_KEY=YOUR_KEY
```

### "MySQL not running"
```
1. Mở XAMPP
2. Click "Start" cho MySQL
3. Chạy lại quick-start.bat
```

---

## 👥 Cho team:

**Gửi cho team:**
1. Link GitHub repo
2. Key: `J+MGnG6P7Q5p8nzu1NqgvpQLbKUtwRmWPtmg/lloHOc=`

**Team làm:**
```bash
# 1. Clone
git clone <repo-url>

# 2. Set key
set PRICE_TRACKER_KEY=<key-từ-leader>

# 3. Chạy
quick-start.bat
```

---

**Xem hướng dẫn đầy đủ:** [README.md](README.md)
