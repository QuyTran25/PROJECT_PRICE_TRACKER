# 🕷️ Tiki Price Scraper

## Mô tả
Script Python để cào giá sản phẩm từ Tiki và lưu vào database MySQL.

## Yêu cầu
- Python 3.11+
- MySQL Server (XAMPP)
- Database: `price_insight`

## Cài đặt

### 1. Cài đặt thư viện
```bash
pip install requests mysql-connector-python
```

### 2. Cấu hình Database
Đảm bảo XAMPP MySQL đang chạy và database `price_insight` đã tồn tại với các bảng:
- `product` - Danh sách sản phẩm cần cào
- `price_history` - Lưu lịch sử giá
- `scrape_log` - Log quá trình cào

### 3. Cấu hình kết nối
Mở file `scraper.py` và kiểm tra cấu hình database:
```python
DB_CONFIG = {
    'host': 'localhost',
    'user': 'root',
    'password': '',  # Đổi nếu có password
    'database': 'price_insight',
}
```

## Sử dụng

### Chạy scraper
```bash
python scraper.py
```

### Kết quả
Script sẽ:
1. ✅ Kết nối database
2. ✅ Lấy danh sách sản phẩm từ bảng `product`
3. ✅ Cào giá từ Tiki API cho từng sản phẩm
4. ✅ Lưu vào bảng `price_history`
5. ✅ Ghi log vào bảng `scrape_log`
6. ✅ Hiển thị thống kê

### Ví dụ output
```
============================================================
    TIKI PRICE SCRAPER - Tầng 1 Giai đoạn 1
============================================================

✓ Đã kết nối database thành công!
✓ Tìm thấy 80 sản phẩm trong database

[1/80] iPhone 17 Pro Max...
  ✓ Giá: 34,490,000đ (gốc: 34,990,000đ) | Deal: NORMAL

...

============================================================
KẾT QUẢ SCRAPING:
============================================================
  Tổng số sản phẩm:      80
  ✓ Thành công:          75 (93.8%)
  ✗ Thất bại:            3
  ! Bỏ qua:              2
============================================================
```

## Tính năng

### 🎯 Cào dữ liệu
- Tự động trích xuất Tiki product ID từ URL
- Gọi Tiki API để lấy thông tin giá
- Phát hiện loại deal (NORMAL, FLASH_SALE, HOT_DEAL, TRENDING)
- Lưu cả giá hiện tại và giá gốc

### 🛡️ Xử lý lỗi
- Timeout protection
- Retry logic
- Skip sản phẩm lỗi, tiếp tục cào
- Ghi log chi tiết

### ⏱️ Rate limiting
- Delay 2 giây giữa các request
- Tránh bị Tiki block IP

### 📊 Logging
- Console output realtime
- Database logging (bảng `scrape_log`)
- Thống kê chi tiết

## Cấu trúc dữ liệu

### Bảng `price_history`
| Column | Type | Description |
|--------|------|-------------|
| price_id | INT | Primary key |
| product_id | INT | Foreign key → product |
| price | DECIMAL(15,2) | Giá hiện tại |
| original_price | DECIMAL(15,2) | Giá gốc |
| currency | VARCHAR(10) | Loại tiền tệ (VND) |
| deal_type | ENUM | NORMAL/FLASH_SALE/HOT_DEAL/TRENDING |
| recorded_at | DATETIME | Thời gian cào |

## Lịch chạy (Tầng 1 - Giai đoạn 1)

### Chạy thủ công
Chạy script **2-3 lần/ngày** để xây dựng dữ liệu lịch sử giá:
- Sáng: 9:00 AM
- Chiều: 3:00 PM  
- Tối: 9:00 PM

### Lệnh chạy
```bash
python scraper.py
```

## Troubleshooting

### Lỗi kết nối database
```
✗ Lỗi kết nối database: Access denied for user 'root'@'localhost'
```
→ Kiểm tra MySQL trong XAMPP có đang chạy không
→ Kiểm tra username/password trong `DB_CONFIG`

### Lỗi import module
```
ModuleNotFoundError: No module named 'requests'
```
→ Chạy: `pip install requests mysql-connector-python`

### Không cào được giá
```
! API trả về status code: 404
```
→ Sản phẩm không tồn tại trên Tiki
→ Kiểm tra lại URL trong database

### Bị Tiki block
```
! API trả về status code: 429 (Too Many Requests)
```
→ Tăng `DELAY_BETWEEN_REQUESTS` lên 3-5 giây
→ Đợi 1 giờ rồi chạy lại

## Roadmap

### ✅ Giai đoạn 1 (Hiện tại)
- [x] Chạy thủ công bằng lệnh
- [x] Cào tất cả sản phẩm trong DB
- [x] Lưu vào price_history
- [x] Logging cơ bản

### 🔄 Giai đoạn 2 (Tự động hóa)
- [ ] Windows Task Scheduler integration
- [ ] Chạy định kỳ 8-12 giờ/lần
- [ ] Email notification khi hoàn thành
- [ ] Advanced error handling

## License
MIT License - Free to use for educational purposes
