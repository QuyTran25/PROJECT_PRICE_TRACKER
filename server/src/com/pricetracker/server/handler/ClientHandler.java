package com.pricetracker.server.handler;

import com.pricetracker.models.PriceHistory;
import com.pricetracker.models.Product;
import com.pricetracker.server.db.PriceHistoryDAO;
import com.pricetracker.server.db.ProductDAO;
import com.pricetracker.server.db.ProductGroupDAO;
import org.json.JSONArray;
import org.json.JSONObject;

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
     * Updated with SEARCH_BY_URL and SEARCH_BY_NAME handlers
     * Returns JSON response for frontend
     */
    private String processRequest(String request) throws Exception {
        // Parse request để xác định loại yêu cầu
        // Format: ACTION|PARAM1|PARAM2|...
        
        String[] parts = request.split("\\|");
        if (parts.length == 0) {
            return buildErrorResponse("Invalid request format");
        }
        
        String action = parts[0];
        
        // Xử lý các loại request
        switch (action) {
            case "PING":
                return "PONG|Server is alive";
                
            case "SEARCH_BY_URL":
                // Format: SEARCH_BY_URL|<tiki_url>
                if (parts.length < 2) {
                    return buildErrorResponse("Missing URL parameter");
                }
                return handleSearchByUrl(parts[1]);
                
            case "SEARCH_BY_NAME":
                // Format: SEARCH_BY_NAME|<keyword>
                if (parts.length < 2) {
                    return buildErrorResponse("Missing search keyword");
                }
                return handleSearchByName(parts[1]);
                
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
                return buildErrorResponse("Unknown action: " + action);
        }
    }
    
    /**
     * Handle SEARCH_BY_URL request
     * Case 1: Product exists → return product card data
     * Case 2: Product new → scrape, insert DB, return notification + card
     */
    private String handleSearchByUrl(String tikiUrl) {
        try {
            ProductDAO productDAO = new ProductDAO();
            
            // Check if product exists in database
            Product product = productDAO.searchByUrl(tikiUrl);
            
            if (product != null) {
                // Case 1: Product exists
                System.out.println("✅ Found existing product: " + product.getName());
                return buildProductResponse(product, false);
            } else {
                // Case 2: New product - scrape and insert
                System.out.println("🔍 New product detected, scraping from Tiki...");
                product = productDAO.insertProductFromTiki(tikiUrl);
                
                if (product != null) {
                    System.out.println("✅ Successfully added new product: " + product.getName());
                    return buildProductResponse(product, true);
                } else {
                    return buildErrorResponse("Failed to scrape product from Tiki. Please check URL.");
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            return buildErrorResponse("Error processing URL search: " + e.getMessage());
        }
    }
    
    /**
     * Handle SEARCH_BY_NAME request
     * Case 3: Matches found → return all matching products
     * Case 4: No matches → return friendly error message
     */
    private String handleSearchByName(String keyword) {
        try {
            ProductDAO productDAO = new ProductDAO();
            
            // Search for products matching keyword
            java.util.List<Product> products = productDAO.searchByNameLike(keyword);
            
            if (products.isEmpty()) {
                // Case 4: No matches found
                return buildErrorResponse("Sản phẩm bạn tìm hiện chưa có trong dữ liệu của chúng tôi. Hãy thử sản phẩm khác nhé ^^");
            } else {
                // Case 3: Matches found
                System.out.println("✅ Found " + products.size() + " products matching: " + keyword);
                return buildMultipleProductsResponse(products);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            return buildErrorResponse("Error processing name search: " + e.getMessage());
        }
    }
    
    /**
     * Build JSON response for single product
     * Format: {"success": true, "isNew": boolean, "product": {...}}
     */
    private String buildProductResponse(Product product, boolean isNew) {
        try {
            JSONObject response = new JSONObject();
            response.put("success", true);
            response.put("isNew", isNew);
            response.put("product", buildProductJSON(product));
            
            return response.toString();
            
        } catch (Exception e) {
            e.printStackTrace();
            return buildErrorResponse("Error building response");
        }
    }
    
    /**
     * Build JSON response for multiple products
     * Format: {"success": true, "count": n, "products": [...]}
     */
    private String buildMultipleProductsResponse(java.util.List<Product> products) {
        try {
            JSONObject response = new JSONObject();
            response.put("success", true);
            response.put("count", products.size());
            
            JSONArray productArray = new JSONArray();
            for (Product product : products) {
                productArray.put(buildProductJSON(product));
            }
            response.put("products", productArray);
            
            return response.toString();
            
        } catch (Exception e) {
            e.printStackTrace();
            return buildErrorResponse("Error building response");
        }
    }
    
    /**
     * Build JSON object for a single product
     * Includes product data + current price + group name
     */
    private JSONObject buildProductJSON(Product product) {
        JSONObject json = new JSONObject();
        
        // Product basic info
        json.put("product_id", product.getProductId());
        json.put("group_id", product.getGroupId());
        json.put("name", product.getName());
        json.put("brand", product.getBrand());
        json.put("url", product.getUrl());
        json.put("image_url", product.getImageUrl());
        json.put("description", product.getDescription());
        json.put("source", product.getSource());
        
        // Get group name
        ProductGroupDAO groupDAO = new ProductGroupDAO();
        String groupName = groupDAO.getGroupNameById(product.getGroupId());
        json.put("group_name", groupName);
        
        // Get current price data
        PriceHistoryDAO priceDAO = new PriceHistoryDAO();
        PriceHistory currentPrice = priceDAO.getCurrentPrice(product.getProductId());
        
        if (currentPrice != null) {
            json.put("price", currentPrice.getPrice());
            json.put("original_price", currentPrice.getOriginalPrice());
            json.put("deal_type", currentPrice.getDealType());
            
            // Calculate discount percentage
            if (currentPrice.getOriginalPrice() > 0) {
                double discount = ((currentPrice.getOriginalPrice() - currentPrice.getPrice()) 
                                   / currentPrice.getOriginalPrice()) * 100;
                json.put("discount_percent", Math.round(discount));
            } else {
                json.put("discount_percent", 0);
            }
        } else {
            json.put("price", 0);
            json.put("original_price", 0);
            json.put("deal_type", "Normal");
            json.put("discount_percent", 0);
        }
        
        return json;
    }
    
    /**
     * Build error response JSON
     * Format: {"success": false, "error": "message"}
     */
    private String buildErrorResponse(String errorMessage) {
        JSONObject response = new JSONObject();
        response.put("success", false);
        response.put("error", errorMessage);
        return response.toString();
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
