package com.pricetracker.server.handler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * ClientHandler - Xử lý yêu cầu từ một client cụ thể
 * Mỗi client sẽ có một ClientHandler riêng chạy trong thread riêng
 * 
 * Nhiệm vụ:
 * 1. Nhận dữ liệu từ client
 * 2. Xử lý yêu cầu (logic nghiệp vụ)
 * 3. Gửi phản hồi lại cho client
 * 4. Đóng kết nối và kết thúc thread
 */
public class ClientHandler implements Runnable {
    
    private final Socket clientSocket;
    private final int clientId;
    
    // Các luồng I/O
    private BufferedReader in;
    private PrintWriter out;
    
    /**
     * Constructor
     * @param clientSocket Socket kết nối với client
     * @param clientId ID định danh của client
     */
    public ClientHandler(Socket clientSocket, int clientId) {
        this.clientSocket = clientSocket;
        this.clientId = clientId;
    }
    
    /**
     * Phương thức chính của thread - xử lý toàn bộ logic cho một client
     * Thread này chạy độc lập và tự kết thúc sau khi hoàn thành
     */
    @Override
    public void run() {
        System.out.println("    [Client #" + clientId + "] Thread bắt đầu xử lý");
        
        try {
            // Khởi tạo các luồng I/O
            initializeStreams();
            
            // Vòng lặp xử lý các yêu cầu từ client
            // Client có thể gửi nhiều yêu cầu trong cùng một kết nối
            String request;
            while ((request = in.readLine()) != null) {
                
                // Bước 1: Nhận yêu cầu từ client
                System.out.println("    [Client #" + clientId + "] Nhận yêu cầu: " + request);
                
                try {
                    // Bước 2: Xử lý yêu cầu
                    String response = processRequest(request);
                    
                    // Bước 3: Gửi phản hồi cho client
                    sendResponse(response);
                    System.out.println("    [Client #" + clientId + "] Đã gửi phản hồi");
                    
                } catch (Exception e) {
                    System.err.println("    [Client #" + clientId + "] Lỗi xử lý yêu cầu: " + e.getMessage());
                    sendErrorResponse("Lỗi xử lý yêu cầu: " + e.getMessage());
                }
            }
            
        } catch (IOException e) {
            System.err.println("    [Client #" + clientId + "] Lỗi I/O: " + e.getMessage());
        } finally {
            // Đóng kết nối và dọn dẹp tài nguyên
            cleanup();
            System.out.println("<<< [Client #" + clientId + "] Thread kết thúc");
        }
    }
    
    /**
     * Khởi tạo các luồng I/O để giao tiếp với client
     */
    private void initializeStreams() throws IOException {
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        out = new PrintWriter(clientSocket.getOutputStream(), true);
    }
    
    /**
     * Xử lý yêu cầu và trả về kết quả
     * Đây là logic nghiệp vụ chính của hệ thống
     * 
     * TODO: Implement logic xử lý thực tế
     * - Giải mã request (nếu có mã hóa)
     * - Truy vấn database
     * - Mã hóa response (nếu cần)
     */
    private String processRequest(String request) throws Exception {
        // Parse request để xác định loại yêu cầu
        // Format: ACTION|PARAM1|PARAM2|...
        
        String[] parts = request.split("\\|");
        if (parts.length == 0) {
            return "ERROR|Invalid request format";
        }
        
        String action = parts[0];
        
        // Xử lý các loại request
        switch (action) {
            case "PING":
                return "PONG|Server is alive";
                
            case "SEARCH_PRODUCT":
                // TODO: Implement search logic
                return "PRODUCTS|0|No implementation yet";
                
            case "GET_PRODUCT_DETAILS":
                // TODO: Implement get details logic
                return "PRODUCT_DETAILS|No implementation yet";
                
            case "GET_PRICE_HISTORY":
                // TODO: Implement price history logic
                return "PRICE_HISTORY|0|No implementation yet";
                
            case "GET_REVIEWS":
                // TODO: Implement reviews logic
                return "REVIEWS|0|No implementation yet";
                
            case "GET_ALL_PRODUCTS":
                // TODO: Implement get all logic
                return "ALL_PRODUCTS|0|No implementation yet";
                
            default:
                return "ERROR|Unknown action: " + action;
        }
    }
    
    /**
     * Gửi phản hồi cho client
     */
    private void sendResponse(String response) {
        out.println(response);
    }
    
    /**
     * Gửi phản hồi lỗi cho client
     */
    private void sendErrorResponse(String errorMessage) {
        sendResponse("ERROR|" + errorMessage);
    }
    
    /**
     * Dọn dẹp tài nguyên và đóng kết nối
     */
    private void cleanup() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            System.err.println("    [Client #" + clientId + "] Lỗi khi đóng kết nối: " + e.getMessage());
        }
    }
}
