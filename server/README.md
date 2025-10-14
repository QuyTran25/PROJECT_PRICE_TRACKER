# SERVER ĐA LUỒNG - PRICE TRACKER

## 📁 CẤU TRÚC

```
server/src/com/pricetracker/server/
├── Main.java                          # Entry point - Khởi động server
├── core/
│   └── PriceTrackerServer.java       # Luồng chính - Lắng nghe kết nối
└── handler/
    └── ClientHandler.java            # Worker thread - Xử lý từng client
```

## 🎯 MÔ TẢ CHỨC NĂNG

### 1. Main.java
- Điểm khởi động ứng dụng server
- Parse port từ command line (mặc định: 8888)
- Khởi tạo và chạy PriceTrackerServer

### 2. PriceTrackerServer.java (Luồng chính)
**Nhiệm vụ: CHỈ LÀM VIỆC "NHẬN KHÁCH"**

```java
while(true) {
    Socket client = serverSocket.accept();  // Chờ client kết nối
    ClientHandler handler = new ClientHandler(client, id);
    threadPool.execute(handler);            // Giao cho thread pool
    // Ngay lập tức quay lại lắng nghe client tiếp theo
}
```

- Sử dụng ExecutorService (Thread Pool) với 50 threads
- Không xử lý logic nghiệp vụ
- Chỉ tạo ClientHandler và giao cho thread pool

### 3. ClientHandler.java (Worker Thread)
**Nhiệm vụ: "NHÂN VIÊN" PHỤC VỤ TỪNG "KHÁCH HÀNG"**

```java
@Override
public void run() {
    // 1. Nhận request từ client
    // 2. Xử lý request (logic nghiệp vụ)
    // 3. Gửi response về client
    // 4. Đóng kết nối và kết thúc thread
}
```

- Mỗi client có một thread riêng biệt
- Chạy độc lập, không ảnh hưởng luồng chính
- Tự động kết thúc sau khi xử lý xong

## 🔄 LUỒNG HOẠT ĐỘNG

```
Main.java
    ↓
PriceTrackerServer (Main Thread)
    ↓
while(true) {
    accept() → Tạo ClientHandler → threadPool.execute()
                                          ↓
                                    ClientHandler (Worker Thread)
                                          ↓
                                    Xử lý request
                                          ↓
                                    Gửi response
                                          ↓
                                    Thread kết thúc
}
```

## 🚀 CÁCH CHẠY

```bash
# Compile
javac -d bin src/com/pricetracker/server/**/*.java

# Chạy (port mặc định 8888)
java -cp bin com.pricetracker.server.Main

# Chạy với port tùy chỉnh
java -cp bin com.pricetracker.server.Main 9000
```

## 🧪 TEST

```bash
# Sử dụng telnet để test
telnet localhost 8888

# Gửi request
PING

# Kết quả mong đợi
PONG|Server is alive
```

## 📝 GHI CHÚ

- **Thread Pool Size:** 50 threads
- **Port mặc định:** 8888
- **Protocol:** ACTION|PARAM1|PARAM2|...
- **Trạng thái:** ✅ Hoàn thành cơ bản

## 🔧 TODO

Logic xử lý trong ClientHandler hiện tại chỉ là skeleton:
- [ ] Implement mã hóa/giải mã
- [ ] Implement kết nối database
- [ ] Implement các handler cụ thể (search, get details, etc.)
