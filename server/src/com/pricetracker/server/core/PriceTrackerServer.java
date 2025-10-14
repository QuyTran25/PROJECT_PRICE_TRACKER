package com.pricetracker.server.core;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

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
    
    /**
     * Constructor
     * @param port Cổng mà server sẽ lắng nghe
     */
    public PriceTrackerServer(int port) {
        this.port = port;
        this.isRunning = false;
        this.clientCounter = new AtomicInteger(0);
    }
    
    /**
     * Khởi động server
     * Đây là luồng chính - chỉ làm nhiệm vụ "nhận khách"
     */
    public void start() {
        try {
            // Khởi tạo ServerSocket
            serverSocket = new ServerSocket(port);
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
}
