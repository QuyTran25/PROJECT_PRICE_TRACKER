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

Nếu chưa có, download:

```bash
# HikariCP + SLF4J
https://repo1.maven.org/maven2/com/zaxxer/HikariCP/5.1.0/HikariCP-5.1.0.jar
https://repo1.maven.org/maven2/org/slf4j/slf4j-api/2.0.9/slf4j-api-2.0.9.jar
https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/2.0.9/slf4j-simple-2.0.9.jar
```

**Hoặc AI có thể tự download bằng PowerShell:**

```powershell
$libs = @(
    @{url="https://repo1.maven.org/maven2/com/zaxxer/HikariCP/5.1.0/HikariCP-5.1.0.jar"; name="HikariCP-5.1.0.jar"},
    @{url="https://repo1.maven.org/maven2/org/slf4j/slf4j-api/2.0.9/slf4j-api-2.0.9.jar"; name="slf4j-api-2.0.9.jar"},
    @{url="https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/2.0.9/slf4j-simple-2.0.9.jar"; name="slf4j-simple-2.0.9.jar"}
)

New-Item -ItemType Directory -Force -Path "server\lib" | Out-Null

foreach ($lib in $libs) {
    $path = "server\lib\$($lib.name)"
    if (-not (Test-Path $path)) {
        Write-Host "Downloading $($lib.name)..."
        Invoke-WebRequest -Uri $lib.url -OutFile $path
    }
}

Write-Host "`n✅ Libraries ready!"
```

**Check:**
```bash
dir server\lib\*.jar
# Phải thấy: HikariCP-5.1.0.jar, slf4j-api-2.0.9.jar, slf4j-simple-2.0.9.jar
# Plus: mysql-connector-j-*.jar, json-*.jar (có sẵn)
```

---

## 🏗️ KIẾN TRÚC (AI cần hiểu)

```
Frontend (Browser) ──HTTP:8080──→ SimpleHttpServer ──→ MySQL + HikariCP
Java Client        ──SSL:8888───→ PriceTrackerServer ──→ MySQL + HikariCP
```

**AI chỉ cần quan tâm HTTP:8080 cho Frontend!**

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
✅ HikariCP Connection Pool initialized
✅ SSL Server listening on port 8888
✅ HTTP Server started on port 8080
   ├─ /deals
   ├─ /search
   ├─ /product-detail
   └─ /categories
```

---

## ✅ BƯỚC 4: MỞ FRONTEND

```bash
# Double-click file:
frontend\HTML\Trangchu.html
```

**Frontend sẽ tự động connect tới `http://localhost:8080`**

---

## 🐛 LỖI THƯỜNG GẶP

### ❌ "ClassNotFoundException: HikariCP"
**Fix:** Download libraries (Bước 0)

### ❌ "Cannot connect to MySQL"
```sql
CREATE DATABASE price_insight;
```

### ❌ "Port 8080 already in use"
```bash
netstat -ano | findstr 8080
taskkill /PID <PID> /F
```

### ❌ "SSL handshake failed"
```bash
cd server\certs
.\generate-cert.bat
.\export-cert-for-client.bat
```

### ❌ Frontend: "Failed to fetch"
**Fix:** Check server đang chạy:
```bash
curl http://localhost:8080/deals
```

---

## 📝 PASSWORDS MẶC ĐỊNH

- **MySQL:** *(không có - XAMPP default)*
- **SSL:** `pricetracker123`

---

## ✅ SUCCESS = 2 ĐIỀU

1. ✅ `.\start-server.bat` → Thấy "ALL SERVERS STARTED"
2. ✅ Mở `frontend\HTML\Trangchu.html` → Thấy products

---

## 📚 STRUCTURE MÀ AI CẦN BIẾT

```
PROJECT_PRICE_TRACKER/
├── server/
│   ├── lib/              ← 5 JAR files phải có
│   │   ├── HikariCP-5.1.0.jar
│   │   ├── slf4j-api-2.0.9.jar
│   │   ├── slf4j-simple-2.0.9.jar
│   │   ├── mysql-connector-j-*.jar
│   │   └── json-*.jar
│   ├── certs/            ← SSL certificates
│   └── src/              ← Java source code
├── frontend/
│   └── HTML/
│       └── Trangchu.html ← Mở file này
└── start-server.bat      ← Chạy file này
```

---

**🤖 AI chỉ cần làm 5 bước (0-4) là đủ!**
