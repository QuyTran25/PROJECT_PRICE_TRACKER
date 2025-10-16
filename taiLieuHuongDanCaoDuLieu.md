Tài Liệu Kỹ Thuật: Kế Hoạch Triển Khai Module Thu Thập Dữ Liệu
Dự án: Price Tracker System
Ngày: 16/10/2025
1. Tổng Quan
Tài liệu này mô tả chi tiết kiến trúc và luồng hoạt động của module thu thập dữ liệu. Để đáp ứng hai yêu cầu nghiệp vụ chính: (1) Cung cấp dữ liệu lịch sử để phân tích biến động giá và (2) Hiển thị giá mới nhất tại thời điểm người dùng truy vấn, chúng ta sẽ áp dụng kiến trúc 2 tầng.
Tầng 1 - Thu Thập Nền (Background Collection): Chịu trách nhiệm xây dựng kho dữ liệu lịch sử một cách ổn định và tự động.
Tầng 2 - Thu Thập Theo Yêu Cầu (On-Demand Fetching): Chịu trách nhiệm lấy giá mới nhất khi người dùng cần, đảm bảo tính thời sự và mang lại trải nghiệm tốt nhất.
2. Tầng 1: Thu Thập Dữ Liệu Nền
2.1. Mục Đích
Xây dựng kho dữ liệu lịch sử: Tầng này là nền tảng, tạo ra dữ liệu thô (price_history) để Server có thể truy vấn và vẽ biểu đồ biến động giá.
Đảm bảo dữ liệu luôn có sẵn: Ngay cả khi việc cào dữ liệu real-time gặp lỗi, hệ thống vẫn có dữ liệu lịch sử để hiển thị cho người dùng.
Giảm tải và tránh bị chặn: Giới hạn việc cào dữ liệu vào một số sản phẩm và theo lịch trình cụ thể để tránh tạo ra lượng truy cập lớn bất thường đến server Tiki.
2.2. Cách Thức Hoạt Động
Đối tượng: Script sẽ cào dữ liệu của một danh sách sản phẩm được chọn trước (~100-200 sản phẩm phổ biến) đã được lưu trong bảng products của chúng ta.
Công cụ: Script Python (scraper.py) sử dụng thư viện requests và thư viện kết nối CSDL (ví dụ: mysql-connector-python).
Tần suất: Chạy định kỳ, mỗi 8-12 tiếng một lần.
2.3. Luồng Xử Lý Của Script scraper.py
Kết nối CSDL: Script khởi tạo kết nối đến database của dự án.
Lấy danh sách sản phẩm: Thực hiện câu lệnh SELECT product_id FROM products để lấy ID của tất cả các sản phẩm hệ thống đang theo dõi.
Lặp qua từng sản phẩm:
Với mỗi product_id trong danh sách:
Tạo URL API của Tiki (ví dụ: https://tiki.vn/api/v2/products/{product_id}).
Gọi API bằng requests.get(), luôn đính kèm headers có User-Agent để giả lập trình duyệt.
Kiểm tra kết quả:
Nếu request thành công (status code 200), trích xuất giá (price) từ file JSON trả về.
Thực hiện câu lệnh INSERT INTO price_history (product_id, price) VALUES (%s, %s) để lưu giá vừa lấy được vào CSDL.
Nếu request thất bại hoặc không có giá, ghi log lỗi và chuyển sang sản phẩm tiếp theo.
Đóng kết nối: Sau khi lặp qua tất cả sản phẩm, đóng kết nối CSDL.
2.4. Kế Hoạch Triển Khai
Việc triển khai sẽ gồm 2 giai đoạn để đảm bảo sự ổn định:
Giai đoạn 1 - Chạy thủ công (Ưu tiên hàng đầu):
Làm thế nào: Thành viên được phân công sẽ mở terminal và chạy lệnh python scraper.py 2-3 lần mỗi ngày.
Mục đích: Đây là cách đơn giản nhất để bắt đầu thu thập dữ liệu, dễ dàng theo dõi và gỡ lỗi. Giai đoạn này giúp chúng ta tập trung vào việc hoàn thiện các tính năng chính khác.
Giai đoạn 2 - Cài đặt tự động (Bước hoàn thiện):
Làm thế nào: Vài ngày trước khi demo, chúng ta sẽ sử dụng Task Scheduler trên Windows để lên lịch cho script tự động chạy.
Mục đích: Để chứng minh khả năng tự động hóa của hệ thống trong buổi báo cáo.
3. Tầng 2: Thu Thập Dữ Liệu Theo Yêu Cầu
3.1. Mục Đích
Cung cấp giá mới nhất: Giải quyết vấn đề dữ liệu ở Tầng 1 có thể bị "cũ" vài tiếng. Tầng này đảm bảo người dùng luôn nhận được thông tin cập nhật nhất có thể.
Tăng trải nghiệm người dùng: Hệ thống cho cảm giác "sống" và phản hồi nhanh, không bắt người dùng chờ đợi.
Mở rộng phạm vi: Cho phép người dùng kiểm tra giá của cả những sản phẩm không nằm trong danh sách cào nền ở Tầng 1.
3.2. Luồng Xử Lý Chi Tiết (Client ↔ Server)
Đây là luồng hoạt động khi người dùng click vào một sản phẩm trên giao diện Client:
Bước 1: Client Gửi Yêu Cầu
Người dùng chọn một sản phẩm.
Client (Java Swing/FX) gửi một yêu cầu đến Server, đính kèm product_id của sản phẩm đó.
Bước 2: Server Phản Hồi Ngay Lập Tức
Server nhận product_id.
Ngay lập tức, Server truy vấn CSDL để lấy bản ghi giá mới nhất hiện có:
SELECT price, recorded_at FROM price_history WHERE product_id = ? ORDER BY recorded_at DESC LIMIT 1;
Server gửi kết quả (giá và thời gian ghi nhận) này về cho Client.
=> Kết quả: Giao diện Client hiển thị thông tin này gần như tức thì. Người dùng không phải chờ.
Bước 3: Server Kích Hoạt Tác Vụ Nền (Bất đồng bộ)
Sau khi đã gửi phản hồi ở Bước 2, Server kiểm tra thời gian recorded_at của dữ liệu vừa gửi.
IF (Thời gian hiện tại - recorded_at > 8 tiếng) THEN:
Server tạo một Thread (luồng) mới.
Luồng mới này sẽ thực hiện tác vụ cào giá real-time cho product_id đó từ Tiki.
Lý do dùng Thread: Để tác vụ cào dữ liệu (có thể mất vài giây) không làm block luồng chính của server, giúp server luôn sẵn sàng phục vụ các client khác.
Bước 4: Client Cập Nhật Dữ Liệu Mới
Client sau khi hiển thị dữ liệu ban đầu (từ Bước 2) sẽ có một cơ chế đơn giản để nhận giá mới.
Giải pháp đề xuất: Sau khoảng 5 giây, Client có thể tự động gửi một request nhỏ khác đến Server hỏi: "Đã có giá mới hơn cho sản phẩm X chưa?".
Nếu tác vụ nền ở Bước 3 đã chạy xong và lưu giá mới vào CSDL, Server sẽ trả về giá mới này.
Giao diện Client nhận được giá mới và tự động cập nhật lại con số trên màn hình, có thể kèm hiệu ứng nhỏ để thu hút sự chú ý.
3.3. Sơ đồ Luồng Hoạt Động
User         Client                     Server                       Tiki API
 |              |                          |                            |
 |---Click SP-->|                          |                            |
 |              |---GET /product/{id}----->|                            |
 |              |                          |---(1) Query DB (Giá cũ)--->|
 |              |                          |                            |
 |              |<--(2) Trả về Giá Cũ------|                            |
 |<--Hiển thị---|                          |                            |
 |   Giá Cũ    |                          |---(3) IF (Giá quá cũ) THEN |
 |              |                          |   |--Tạo Thread mới-------|
 |              |                          |     |                      |
 |              |                          |     |---(4) Cào Giá Mới--->|
 |              |                          |     |                      |
 |              |                          |     |<----(5) Trả Giá Mới--|
 |              |                          |     |                      |
 |              |                          |     |---(6) Lưu vào DB---->|
 |              |                          |     |                      |
 |              |                          |-----<--Thread kết thúc----|
 |              |                          |                            |
 | (Sau 5s)     |                          |                            |
 |              |---GET /product/update/{id}-->|                          |
 |              |                          |---(7) Query DB (Giá mới)-->|
 |              |                          |                            |
 |              |<--(8) Trả về Giá Mới-----|                            |
 |<--Cập nhật---|                          |                            |
 |  Giá Mới    |                          |                            |
