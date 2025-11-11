package com.pricetracker.server.core;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.SSLServerSocket;

import com.pricetracker.server.crypto.SSLManager;
import com.pricetracker.server.handler.ClientHandler;

/**
 * PriceTrackerServer - Lớp Server chính
 * Quản lý kết nối đa luồng và điều phối các ClientHandler
 * 
 * Nhiệm vụ chính:
 * - Mở ServerSocket và lắng nghe kết nối từ client
 * - Tạo ClientHandler riêng biệt cho mỗi client kết nối
 * - Quản lý thread pool để tối ưu hiệu suất
 */
public class PriceTrackerServer {
    
    private final int port;
    private ServerSocket serverSocket;
    private volatile boolean isRunning;
    
    // Thread pool để quản lý các luồng ClientHandler
    private ExecutorService threadPool;
    
    // Số lượng luồng tối đa trong pool
    private static final int MAX_THREADS = 50;
    
    // Đếm số lượng client đã kết nối
    private AtomicInteger clientCounter;
    
    // SSL Manager
    private SSLManager sslManager;
    
    // Enable/Disable SSL (có thể config via system property)
    private final boolean enableSSL;
    
    /**
     * Constructor
     * @param port Cổng mà server sẽ lắng nghe
     */
    public PriceTrackerServer(int port) {
        this(port, !"false".equals(System.getProperty("ssl.enabled", "true")));
    }
    
    /**
     * Constructor với SSL option
     * @param port Cổng mà server sẽ lắng nghe
     * @param enableSSL Bật/tắt SSL
     */
    public PriceTrackerServer(int port, boolean enableSSL) {
        this.port = port;
        this.isRunning = false;
        this.clientCounter = new AtomicInteger(0);
        this.enableSSL = enableSSL;
    }
    
    /**
     * Khởi động server
     * Đây là luồng chính - chỉ làm nhiệm vụ "nhận khách"
     */
    public void start() {
        try {
            // Khởi tạo ServerSocket (SSL hoặc Plain)
            if (enableSSL) {
                System.out.println("🔒 Đang khởi tạo SSL/TLS Server...");
                try {
                    sslManager = new SSLManager();
                    serverSocket = sslManager.getServerSocketFactory().createServerSocket(port);
                    
                    // Config SSL parameters
                    if (serverSocket instanceof SSLServerSocket) {
                        SSLServerSocket sslServerSocket = (SSLServerSocket) serverSocket;
                        
                        // Chỉ enable protocols mạnh
                        sslServerSocket.setEnabledProtocols(new String[] {
                            "TLSv1.3", "TLSv1.2"
                        });
                        
                        // Optional: Yêu cầu client authentication
                        // sslServerSocket.setNeedClientAuth(true);
                        
                        // Hiển thị SSL info
                        sslManager.printSSLInfo(sslServerSocket);
                    }
                    
                    System.out.println("✓ SSL/TLS đã được kích hoạt");
                } catch (Exception e) {
                    System.err.println("✗ Lỗi khởi tạo SSL: " + e.getMessage());
                    System.err.println("⚠️  Fallback sang non-SSL mode...");
                    serverSocket = new ServerSocket(port);
                }
            } else {
                System.out.println("⚠️  Chạy ở NON-SSL mode (không khuyến khích cho production)");
                serverSocket = new ServerSocket(port);
            }
            
            isRunning = true;
            
            // Khởi tạo thread pool để quản lý các ClientHandler
            threadPool = Executors.newFixedThreadPool(MAX_THREADS);
            
            System.out.println("✓ Server đã sẵn sàng và đang lắng nghe tại port " + port);
            System.out.println("✓ Thread pool đã được khởi tạo với " + MAX_THREADS + " luồng");
            System.out.println("✓ Đang chờ kết nối từ client...\n");
            
            // Vòng lặp vô tận - luồng chính chỉ làm nhiệm vụ lắng nghe
            while (isRunning) {
                try {
                    // Chờ và chấp nhận kết nối từ client
                    // Phương thức accept() sẽ block cho đến khi có client kết nối
                    Socket clientSocket = serverSocket.accept();
                    
                    // Tăng số đếm client
                    int clientId = clientCounter.incrementAndGet();
                    
                    // Lấy thông tin client
                    String clientAddress = clientSocket.getInetAddress().getHostAddress();
                    int clientPort = clientSocket.getPort();
                    
                    System.out.println(">>> [Client #" + clientId + "] Kết nối mới từ " 
                                     + clientAddress + ":" + clientPort);
                    
                    // Tạo ClientHandler mới để xử lý client này
                    // Đây là "nhân viên" sẽ phục vụ "khách hàng" này
                    ClientHandler handler = new ClientHandler(clientSocket, clientId);
                    
                    // Giao nhiệm vụ cho thread pool
                    // Thread pool sẽ tự động gán một luồng để chạy ClientHandler
                    threadPool.execute(handler);
                    
                    // Luồng chính ngay lập tức quay lại vòng lặp
                    // để sẵn sàng đón client tiếp theo
                    // KHÔNG cần chờ ClientHandler xử lý xong
                    
                } catch (IOException e) {
                    if (isRunning) {
                        System.err.println("✗ Lỗi khi chấp nhận kết nối: " + e.getMessage());
                    }
                }
            }
            
        } catch (IOException e) {
            System.err.println("✗ Không thể khởi động server trên port " + port);
            System.err.println("✗ Lỗi: " + e.getMessage());
            e.printStackTrace();
        } finally {
            shutdown();
        }
    }
    
    /**
     * Dừng server một cách an toàn
     */
    public void shutdown() {
        System.out.println("\n=== Đang tắt server ===");
        isRunning = false;
        
        // Đóng thread pool
        if (threadPool != null && !threadPool.isShutdown()) {
            System.out.println("Đang đóng thread pool...");
            threadPool.shutdown();
            try {
                // Chờ tối đa 30 giây để các thread hoàn thành
                if (!threadPool.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS)) {
                    threadPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                threadPool.shutdownNow();
            }
        }
        
        // Đóng ServerSocket
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                System.out.println("Đang đóng server socket...");
                serverSocket.close();
            } catch (IOException e) {
                System.err.println("Lỗi khi đóng server socket: " + e.getMessage());
            }
        }
        
        System.out.println("✓ Server đã tắt hoàn toàn");
        System.out.println("✓ Tổng số client đã phục vụ: " + clientCounter.get());
    }
    
    /**
     * Kiểm tra server có đang chạy không
     */
    public boolean isRunning() {
        return isRunning;
    }
    
    /**
     * Lấy số lượng client đã kết nối
     */
    public int getClientCount() {
        return clientCounter.get();
    }
    
    /**
     * Main method - Entry point của server
     * Chạy 3 servers:
     * - SSL Server (8888) cho Java Client
     * - HTTP Server (8080) cho Frontend
     * - WebSocket Server (8081) cho Real-time Updates
     */
    public static void main(String[] args) {
        System.out.println("====================================================");
        System.out.println("   🚀 PRICE TRACKER - TRIPLE SERVER MODE");
        System.out.println("   SSL/TLS + HikariCP + WebSocket Real-time");
        System.out.println("====================================================\n");
        
        // Khởi tạo DatabaseConnectionManager để init HikariCP pool
        try {
            System.out.println("🔧 Initializing HikariCP Connection Pool...");
            com.pricetracker.server.db.DatabaseConnectionManager.getInstance();
            System.out.println();
        } catch (Exception e) {
            System.err.println("✗ Failed to initialize database connection pool!");
            System.err.println("✗ Error: " + e.getMessage());
            System.err.println("\n⚠️  Server will continue but database operations will fail.");
            System.err.println("⚠️  Make sure MySQL is running on localhost:3306\n");
        }
        
        // Đọc ports từ system property hoặc dùng mặc định
        // int sslPort = Integer.parseInt(System.getProperty("ssl.port", "8888"));
        int httpPort = Integer.parseInt(System.getProperty("http.port", "8080"));
        
        // ❌ DISABLED: SSL Server không cần thiết cho web demo
        // Desktop Client không được sử dụng, tiết kiệm ~50MB RAM + 50 threads
        // Chỉ giữ HTTP Server (8080) và WebSocket Server (8081)
        /*
        // 1. Start SSL Server (port 8888) - For Java Client
        System.out.println("🔒 Starting SSL Server for Java Client...");
        PriceTrackerServer sslServer = new PriceTrackerServer(sslPort);
        Thread sslThread = new Thread(() -> sslServer.start(), "SSL-Server-Thread");
        sslThread.start();
        
        // Wait a bit for SSL server to initialize
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        */
        
        // 2. Start HTTP Server (port 8080) - For Frontend
        System.out.println("\n🌐 Starting HTTP Server for Frontend...");
        com.pricetracker.server.http.SimpleHttpServer httpServer = 
            new com.pricetracker.server.http.SimpleHttpServer();
        try {
            httpServer.start();
        } catch (Exception e) {
            System.err.println("✗ Failed to start HTTP server: " + e.getMessage());
        }
        
        // 3. Start WebSocket Server (port 8081) - For Real-time Updates
        System.out.println("\n⚡ Starting WebSocket Server for Real-time Updates...");
        com.pricetracker.server.websocket.PriceWebSocketServer wsServer = 
            new com.pricetracker.server.websocket.PriceWebSocketServer();
        wsServer.start();
        
        // 4. Start Price Update Service (monitors database)
        System.out.println("📊 Starting Price Update Monitoring Service...");
        com.pricetracker.server.websocket.PriceUpdateService updateService = 
            new com.pricetracker.server.websocket.PriceUpdateService(wsServer);
        updateService.start();
        
        // Print summary
        System.out.println("\n" + "=".repeat(65));
        System.out.println("   ✨ SERVERS STARTED SUCCESSFULLY! (SSL Server DISABLED)");
        System.out.println("=".repeat(65));
        System.out.println("🌐 HTTP Server:      port " + httpPort + " (Frontend API)");
        System.out.println("   ├─ /deals          - Get discount products");
        System.out.println("   ├─ /search         - Search products");
        System.out.println("   ├─ /product-detail - Product details");
        System.out.println("   └─ /categories     - Product categories");
        System.out.println();
        System.out.println("⚡ WebSocket Server: port 8081 (Real-time price updates)");
        System.out.println("   ├─ Broadcasts price changes to all connected clients");
        System.out.println("   └─ Checks database every 30 seconds");
        System.out.println();
        System.out.println("💡 Note: SSL Server (port 8888) disabled to save resources");
        System.out.println("   └─ Web demo only uses HTTP + WebSocket");
        System.out.println("=".repeat(65));
        System.out.println("\nPress Ctrl+C to stop all servers...\n");
        
        // Thêm shutdown hook để đóng servers gracefully
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n🛑 Shutdown signal received...");
            
            // Stop Price Update Service first
            System.out.println("⏸️  Stopping Price Update Service...");
            updateService.stop();
            
            // Stop WebSocket server
            System.out.println("⏸️  Stopping WebSocket Server...");
            wsServer.shutdown();
            
            // Stop HTTP server
            System.out.println("⏸️  Stopping HTTP Server...");
            httpServer.stop();
            
            // ❌ SSL Server đã bị disable, không cần stop
            // System.out.println("⏸️  Stopping SSL Server...");
            // sslServer.shutdown();
            
            // Đóng HikariCP pool
            try {
                com.pricetracker.server.db.HikariCPConfig.shutdown();
            } catch (Exception e) {
                System.err.println("Error shutting down database pool: " + e.getMessage());
            }
            
            System.out.println("✅ All servers stopped gracefully");
        }));
        
        // Keep main thread alive (wait for HTTP/WebSocket servers)
        // ❌ SSL thread đã bị disable
        /*
        try {
            sslThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        */
        
        // Keep process running - servers are running in their own threads
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
