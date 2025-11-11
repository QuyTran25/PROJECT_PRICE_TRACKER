# 🤖 AI SETUP GUIDE - Price Tracker

> **Dành cho AI Assistant**
> 
> Hướng dẫn AI làm gì để project chạy được

---

## 🎯 MỤC TIÊU

Sau khi làm theo hướng dẫn này:
- ✅ `.\start-server.bat` chạy thành công
- ✅ Frontend mở được và hiển thị data

---

## 📦 BƯỚC 0: KIỂM TRA DEPENDENCIES

### ✅ **Server Libraries (phải có trong `server/lib/`)**

Nếu chưa có, download các JAR files sau:

```bash
# HikariCP (Connection Pool)
https://repo1.maven.org/maven2/com/zaxxer/HikariCP/5.1.0/HikariCP-5.1.0.jar

# SLF4J (Logging cho HikariCP)
https://repo1.maven.org/maven2/org/slf4j/slf4j-api/2.0.9/slf4j-api-2.0.9.jar
https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/2.0.9/slf4j-simple-2.0.9.jar

# MySQL Connector
https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.0.33/mysql-connector-j-8.0.33.jar

# Gson (JSON processing)
https://repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar

# Java-WebSocket (Real-time updates)
https://repo1.maven.org/maven2/org/java-websocket/Java-WebSocket/1.5.3/Java-WebSocket-1.5.3.jar
```

**Hoặc AI có thể tự download bằng PowerShell:**

```powershell
$libs = @(
    @{url="https://repo1.maven.org/maven2/com/zaxxer/HikariCP/5.1.0/HikariCP-5.1.0.jar"; name="HikariCP-5.1.0.jar"},
    @{url="https://repo1.maven.org/maven2/org/slf4j/slf4j-api/2.0.9/slf4j-api-2.0.9.jar"; name="slf4j-api-2.0.9.jar"},
    @{url="https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/2.0.9/slf4j-simple-2.0.9.jar"; name="slf4j-simple-2.0.9.jar"},
    @{url="https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.0.33/mysql-connector-j-8.0.33.jar"; name="mysql-connector-j-8.0.33.jar"},
    @{url="https://repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar"; name="gson-2.10.1.jar"},
    @{url="https://repo1.maven.org/maven2/org/java-websocket/Java-WebSocket/1.5.3/Java-WebSocket-1.5.3.jar"; name="Java-WebSocket-1.5.3.jar"}
)

New-Item -ItemType Directory -Force -Path "server\lib" | Out-Null

foreach ($lib in $libs) {
    $path = "server\lib\$($lib.name)"
    if (-not (Test-Path $path)) {
        Write-Host "Downloading $($lib.name)..."
        try {
            Invoke-WebRequest -Uri $lib.url -OutFile $path -UseBasicParsing
            Write-Host "  ✅ Downloaded: $($lib.name)"
        } catch {
            Write-Host "  ❌ Failed: $($lib.name) - $($_.Exception.Message)"
        }
    } else {
        Write-Host "  ⏭️  Already exists: $($lib.name)"
    }
}

Write-Host "`n✅ All libraries checked!"
Write-Host "`nVerifying downloads..."
Get-ChildItem "server\lib\*.jar" | ForEach-Object {
    Write-Host "  ✓ $($_.Name) - $([math]::Round($_.Length/1MB, 2)) MB"
}
```

**Check:**
```bash
dir server\lib\*.jar
```

**Phải thấy 6 files:**
```
HikariCP-5.1.0.jar          (~156 KB)
slf4j-api-2.0.9.jar         (~64 KB)
slf4j-simple-2.0.9.jar      (~16 KB)
mysql-connector-j-8.0.33.jar (~2.4 MB)
gson-2.10.1.jar             (~253 KB)
Java-WebSocket-1.5.3.jar    (~143 KB)
```

---

## 🏗️ KIẾN TRÚC (AI cần hiểu)

```
Frontend (Browser) ──HTTP:8080────→ SimpleHttpServer ──→ MySQL + HikariCP
                   ──WebSocket:8081→ WebSocketServer ──→ MySQL
Java Client        ──SSL:8888─────→ PriceTrackerServer ──→ MySQL + HikariCP
```

**AI chỉ cần quan tâm HTTP:8080 và WebSocket:8081 cho Frontend!**

---

## ✅ BƯỚC 1: TẠO DATABASE

```sql
CREATE DATABASE IF NOT EXISTS price_insight;
```

**Lưu ý:** MySQL phải đang chạy (user tự start XAMPP)

---

## ✅ BƯỚC 2: TẠO SSL CERTIFICATES

```bash
cd server\certs
.\generate-cert.bat
.\export-cert-for-client.bat
cd ..\..
```

**Kết quả:**
- ✅ `server/certs/server.keystore` được tạo
- ✅ `client/certs/truststore.jks` được tạo

---

## ✅ BƯỚC 3: CHẠY SERVER

```bash
.\start-server.bat
```

**Expected output:**
```
====================================================
   🚀 PRICE TRACKER - TRIPLE SERVER MODE
====================================================

✅ HikariCP Connection Pool initialized
✅ SSL Server listening on port 8888
✅ HTTP Server started on port 8080
✅ WebSocket Server started on port 8081
   HTTP Endpoints:
   ├─ /deals
   ├─ /search
   ├─ /product-detail
   └─ /categories
   WebSocket:
   └─ ws://localhost:8081 (Real-time price updates)
```

---

## ✅ BƯỚC 4: MỞ FRONTEND

```bash
# Double-click file:
frontend\HTML\Trangchu.html
```

**Frontend sẽ tự động:**
- Connect tới `http://localhost:8080` (HTTP API)
- Connect tới `ws://localhost:8081` (WebSocket real-time)

---

## 🐛 LỖI THƯỜNG GẶP

### ❌ "ClassNotFoundException: HikariCP"
**Fix:** Download libraries (Bước 0)

### ❌ "Cannot connect to MySQL"
```sql
CREATE DATABASE price_insight;
```

### ❌ "Port 8080/8081 already in use"
```bash
# Check port đang dùng
netstat -ano | findstr "8080 8081"

# Kill process
taskkill /PID <PID> /F
```

### ❌ "SSL handshake failed"
```bash
cd server\certs
.\generate-cert.bat
.\export-cert-for-client.bat
```

### ❌ Frontend: "Failed to fetch" / "WebSocket connection failed"
**Fix:** 
1. Check server đang chạy:
```bash
curl http://localhost:8080/deals
```
2. Check WebSocket:
```bash
# Trong browser console
new WebSocket('ws://localhost:8081')
```
```bash
curl http://localhost:8080/deals
```

---

## 📝 PASSWORDS MẶC ĐỊNH

- **MySQL:** *(không có - XAMPP default)*
- **SSL:** `pricetracker123`

---

## ✅ SUCCESS = 3 ĐIỀU

1. ✅ `.\start-server.bat` → Thấy "ALL SERVERS STARTED"
2. ✅ Mở `frontend\HTML\Trangchu.html` → Thấy products
3. ✅ Browser Console → Thấy "[WebSocket] ✅ Kết nối thành công!"

---

## 📚 STRUCTURE MÀ AI CẦN BIẾT

```
PROJECT_PRICE_TRACKER/
├── server/
│   ├── lib/              ← 6 JAR files phải có
│   │   ├── HikariCP-5.1.0.jar
│   │   ├── slf4j-api-2.0.9.jar
│   │   ├── slf4j-simple-2.0.9.jar
│   │   ├── mysql-connector-j-8.0.33.jar
│   │   ├── gson-2.10.1.jar
│   │   └── Java-WebSocket-1.5.3.jar
│   ├── certs/            ← SSL certificates
│   └── src/              ← Java source code
│       ├── core/         ← PriceTrackerServer
│       ├── http/         ← SimpleHttpServer
│       ├── websocket/    ← WebSocketServer (NEW)
│       └── db/           ← HikariCPConfig
├── frontend/
│   ├── HTML/
│   │   └── Trangchu.html ← Mở file này
│   └── JS/
│       └── websocket.js  ← WebSocket client (NEW)
└── start-server.bat      ← Chạy file này
```

---

## 🔄 REAL-TIME FEATURES (Task #4)

### **WebSocket hoạt động như thế nào:**

1. **Frontend connect** → `ws://localhost:8081`
2. **Server monitor DB** → Mỗi 30 giây check price changes
3. **Phát hiện giá mới** → Broadcast tới ALL clients
4. **Frontend nhận** → Update UI + Toast notification

**Test real-time:**
```sql
-- Trong MySQL, update giá sản phẩm:
UPDATE price_history 
SET price = 5990000, 
    discount_percent = 25,
    updated_at = NOW()
WHERE product_id = 65;

-- Trong vòng 30 giây, frontend sẽ hiện:
-- 🔔 Toast notification với giá mới!
```

---

**🤖 AI chỉ cần làm 5 bước (0-4) là đủ!**
