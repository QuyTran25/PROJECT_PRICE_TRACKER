# 🤖 AI Setup Guide - Price Tracker SSL/TLS

> **Dành cho AI Assistant (như GitHub Copilot Chat)**
> 
> File này chứa đầy đủ context để AI có thể tự động setup SSL/TLS cho project.
> User chỉ cần paste file này + password vào chat, AI sẽ tự động làm phần còn lại.

---

## 📋 CONTEXT - AI CẦN BIẾT

### Project Structure:
```
PROJECT_PRICE_TRACKER/
├── server/
│   ├── certs/
│   │   ├── generate-cert.bat           # Script tạo server cert
│   │   ├── export-cert-for-client.bat  # Script export cho client
│   │   ├── keystore.config.example     # Template config
│   │   └── START_HERE.txt
│   └── src/com/pricetracker/server/
│       ├── crypto/SSLManager.java
│       └── core/PriceTrackerServer.java
│
├── client/
│   ├── certs/
│   │   ├── truststore.config.example
│   │   └── START_HERE.txt
│   └── src/com/pricetracker/client/
│       ├── crypto/SSLClientManager.java
│       └── net/NetworkClient.java
│
└── ssl.config.example                  # Master config template
```

### Security Implementation:
- **Server:** Sử dụng `SSLServerSocket` với keystore
- **Client:** Sử dụng `SSLSocket` với truststore
- **Certificates:** Self-signed cho development, CA-signed cho production
- **Default password:** `pricetracker123` (PHẢI đổi trong production)

---

## 🎯 NHIỆM VỤ CỦA AI

Khi user cung cấp **PASSWORD MỚI**, AI cần:

### 1. Update file `server/certs/generate-cert.bat`

**Dòng cần sửa:**
```batch
# Dòng 14
set KEYSTORE_PASS=pricetracker123

# Dòng 17 (nếu user cung cấp domain)
set DNAME="CN=localhost, OU=PriceTracker, O=PriceTracker, L=HCM, S=HCM, C=VN"
```

**Đổi thành:**
```batch
set KEYSTORE_PASS=<PASSWORD_USER_CUNG_CAP>
set DNAME="CN=<DOMAIN_USER_CUNG_CAP>, OU=PriceTracker, O=PriceTracker, L=HCM, S=HCM, C=VN"
```

### 2. Update file `server/certs/export-cert-for-client.bat`

**Dòng cần sửa:**
```batch
# Dòng 12
set SERVER_PASS=pricetracker123

# Dòng 18
set TRUSTSTORE_PASS=pricetracker123
```

**Đổi thành:**
```batch
set SERVER_PASS=<PASSWORD_USER_CUNG_CAP>
set TRUSTSTORE_PASS=<PASSWORD_USER_CUNG_CAP>
```

### 3. Copy và tạo file config từ example

**Tạo `ssl.config` từ `ssl.config.example`:**
```ini
KEYSTORE_PASSWORD=<PASSWORD_USER_CUNG_CAP>
TRUSTSTORE_PASSWORD=<PASSWORD_USER_CUNG_CAP>
```

### 4. Chạy các lệnh sau (guide user)

Sau khi update files, AI cần hướng dẫn user chạy:

```bash
cd server/certs
.\generate-cert.bat
.\export-cert-for-client.bat
```

### 5. Verify kết quả

Kiểm tra các files được tạo:
- ✅ `server/certs/server.keystore`
- ✅ `server/certs/server.crt`
- ✅ `client/certs/truststore.jks`

---

## 📝 TEMPLATE RESPONSE CHO AI

Khi user paste file này + password, AI nên trả lời:

```markdown
Tôi đã hiểu! Bạn muốn setup SSL với password mới là: **[PASSWORD]**

Tôi sẽ giúp bạn update các file cần thiết:

### Bước 1: Update server/certs/generate-cert.bat
[Show code changes]

### Bước 2: Update server/certs/export-cert-for-client.bat
[Show code changes]

### Bước 3: Tạo ssl.config
[Show new file content]

### Bước 4: Chạy các lệnh
Bạn cần chạy:
\`\`\`bash
cd server/certs
.\generate-cert.bat
.\export-cert-for-client.bat
\`\`\`

Sau đó kiểm tra các files được tạo:
- server/certs/server.keystore
- server/certs/server.crt
- client/certs/truststore.jks

Có cần tôi giải thích thêm không?
```

---

## 🔍 KEYWORDS CHO AI DETECT

AI nên trigger khi user nói:

- "setup ssl với password là..."
- "đổi password ssl thành..."
- "tạo certificate với password..."
- "config ssl password..."
- "password mới: ..."
- "KEYSTORE_PASS=..."

---

## 📖 DETAILED INSTRUCTIONS

### A. Development Setup (Default Password)

Nếu user chỉ muốn chạy nhanh với default:

```bash
cd server/certs
.\generate-cert.bat              # Dùng password mặc định
.\export-cert-for-client.bat     # Export với password mặc định
```

Files được tạo sẽ dùng password: `pricetracker123`

### B. Production Setup (Custom Password)

Khi user cung cấp password mới (ví dụ: `MySecure@Pass123`):

**1. Edit `server/certs/generate-cert.bat`:**
```batch
# Line 14: Change password
set KEYSTORE_PASS=MySecure@Pass123

# Line 17: Change domain (if provided)
set DNAME="CN=myserver.com, OU=IT, O=Company, L=HCM, S=HCM, C=VN"
```

**2. Edit `server/certs/export-cert-for-client.bat`:**
```batch
# Line 12
set SERVER_PASS=MySecure@Pass123

# Line 18
set TRUSTSTORE_PASS=MySecure@Pass123
```

**3. Run scripts:**
```bash
cd server/certs
.\generate-cert.bat
.\export-cert-for-client.bat
```

**4. Start server with new password:**
```bash
java -Dssl.keystore.password=MySecure@Pass123 -jar server.jar
```

**5. Start client with new password:**
```bash
java -Dssl.truststore.password=MySecure@Pass123 -jar client.jar
```

---

## 🛠️ FILE MODIFICATIONS REFERENCE

### File: `server/certs/generate-cert.bat`

**Original (lines 12-17):**
```batch
set KEYSTORE_FILE=server.keystore
set KEYSTORE_PASS=pricetracker123
set KEY_ALIAS=pricetracker
set VALIDITY_DAYS=365
set DNAME="CN=localhost, OU=PriceTracker, O=PriceTracker, L=HCM, S=HCM, C=VN"
```

**Variables AI có thể thay đổi:**
- `KEYSTORE_PASS`: Password cho keystore
- `DNAME` - `CN`: Domain name (localhost → real domain)
- `VALIDITY_DAYS`: Số ngày certificate valid (mặc định 365)

### File: `server/certs/export-cert-for-client.bat`

**Original (lines 10-18):**
```batch
set SERVER_KEYSTORE=server\certs\server.keystore
set SERVER_PASS=pricetracker123
set SERVER_ALIAS=pricetracker
set CERT_FILE=server\certs\server.crt

set CLIENT_TRUSTSTORE=client\certs\truststore.jks
set TRUSTSTORE_PASS=pricetracker123
```

**Variables AI có thể thay đổi:**
- `SERVER_PASS`: Password của server keystore
- `TRUSTSTORE_PASS`: Password cho client truststore

---

## 🚨 COMMON SCENARIOS

### Scenario 1: User chỉ cung cấp password

**User input:**
```
Password mới: SecurePass@2025!
```

**AI action:**
1. Update `KEYSTORE_PASS` và `SERVER_PASS`, `TRUSTSTORE_PASS` = `SecurePass@2025!`
2. Giữ nguyên `DNAME` = `CN=localhost`
3. Guide user chạy scripts

### Scenario 2: User cung cấp password + domain

**User input:**
```
Password: SecurePass@2025!
Domain: pricetracker.company.com
```

**AI action:**
1. Update passwords
2. Update `DNAME` = `CN=pricetracker.company.com, ...`
3. Guide user chạy scripts

### Scenario 3: User muốn thay đổi nhiều thứ

**User input:**
```
Password: MyP@ss123
Domain: api.myserver.com
Validity: 730 ngày
Organization: MyCompany
```

**AI action:**
Update all fields:
- `KEYSTORE_PASS` = `MyP@ss123`
- `CN` = `api.myserver.com`
- `VALIDITY_DAYS` = `730`
- `O` = `MyCompany`

---

## 🔐 SECURITY REMINDERS FOR AI

Khi setup, AI nên nhắc user:

1. ✅ **Không commit** file sau vào Git:
   - `*.keystore`
   - `*.jks`
   - `*.crt`
   - `ssl.config` (có password)

2. ✅ **Backup** keystore sau khi tạo

3. ✅ **Check expiration:**
   ```bash
   keytool -list -v -keystore server.keystore -storepass PASSWORD | findstr "Valid"
   ```

4. ✅ **File permissions:** Restrict access to keystore files

---

## 🐛 TROUBLESHOOTING GUIDE FOR AI

### Error: "keytool not found"

**AI response:**
```
Lỗi này xảy ra vì JDK chưa có trong PATH.

Fix:
set PATH=%PATH%;C:\Program Files\Java\jdk-17\bin

Hoặc dùng full path:
"C:\Program Files\Java\jdk-17\bin\keytool.exe" -list ...
```

### Error: "PKIX path building failed"

**AI response:**
```
Client không trust server certificate.

Fix:
cd server\certs
.\export-cert-for-client.bat

Kiểm tra client\certs\truststore.jks đã tồn tại chưa.
```

### Error: "Certificate has expired"

**AI response:**
```
Certificate hết hạn. Tạo lại:

cd server\certs
.\generate-cert.bat
.\export-cert-for-client.bat
```

### Error: "Incorrect password"

**AI response:**
```
Password không đúng. Kiểm tra:

1. Password trong ssl.config
2. Password khi start server/client
3. Password trong batch files

Nếu quên password, phải tạo lại keystore.
```

---

## 💡 EXAMPLE CONVERSATION

**User:**
```
Tôi muốn setup SSL với password là: MySecure@Pass2025!
Domain: pricetracker.vn
```

**AI response:**
```
Được! Tôi sẽ giúp bạn setup SSL với:
- Password: MySecure@Pass2025!
- Domain: pricetracker.vn

### Bước 1: Update generate-cert.bat

Mở file server/certs/generate-cert.bat và sửa:

Dòng 14:
set KEYSTORE_PASS=MySecure@Pass2025!

Dòng 17:
set DNAME="CN=pricetracker.vn, OU=PriceTracker, O=PriceTracker, L=HCM, S=HCM, C=VN"

### Bước 2: Update export-cert-for-client.bat

Mở file server/certs/export-cert-for-client.bat và sửa:

Dòng 12:
set SERVER_PASS=MySecure@Pass2025!

Dòng 18:
set TRUSTSTORE_PASS=MySecure@Pass2025!

### Bước 3: Chạy scripts

cd server\certs
.\generate-cert.bat
.\export-cert-for-client.bat

### Bước 4: Start server

java -Dssl.keystore.password=MySecure@Pass2025! -jar server.jar

### Bước 5: Start client

java -Dssl.truststore.password=MySecure@Pass2025! -jar client.jar

Xong! Có cần giải thích gì thêm không?
```

---

## 📚 ADDITIONAL CONTEXT

### Code Implementation:

**Server SSL Init:**
```java
// SSLManager.java
SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
KeyStore keyStore = KeyStore.getInstance("JKS");
keyStore.load(new FileInputStream(keystorePath), password);
```

**Client SSL Init:**
```java
// SSLClientManager.java
KeyStore trustStore = KeyStore.getInstance("JKS");
trustStore.load(new FileInputStream(truststorePath), password);
TrustManagerFactory.init(trustStore);
```

### System Properties:

```bash
# Server
-Dssl.enabled=true
-Dssl.keystore.path=server/certs/server.keystore
-Dssl.keystore.password=YOUR_PASSWORD

# Client
-Dssl.enabled=true
-Dssl.truststore.path=client/certs/truststore.jks
-Dssl.truststore.password=YOUR_PASSWORD
-Dssl.trustAll=false  # verify certificates
```

---

## ✅ VERIFICATION CHECKLIST

AI nên guide user check:

```bash
# 1. Verify keystore created
dir server\certs\server.keystore

# 2. List keystore contents
keytool -list -keystore server\certs\server.keystore -storepass PASSWORD

# 3. Verify truststore created
dir client\certs\truststore.jks

# 4. List truststore contents
keytool -list -keystore client\certs\truststore.jks -storepass PASSWORD

# 5. Check certificate validity
keytool -list -v -keystore server\certs\server.keystore -storepass PASSWORD | findstr "Valid"
```

---

## 🎯 SUCCESS CRITERIA

Setup thành công khi:

1. ✅ Server khởi động với log: `✓ SSL/TLS đã được kích hoạt`
2. ✅ Client connect với log: `✅ SSL/TLS connection established`
3. ✅ Không có lỗi certificate trong logs
4. ✅ TLS handshake thành công
5. ✅ Data được mã hóa (verify bằng Wireshark nếu cần)

---

**🤖 AI: Đọc file này để hiểu context, sau đó giúp user setup based on password họ cung cấp!**
