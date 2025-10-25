package com.pricetracker.server.http;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.Headers;
import com.pricetracker.server.db.ProductDAO;
import com.pricetracker.server.db.PriceHistoryDAO;
import com.pricetracker.server.db.ProductGroupDAO;
import com.pricetracker.server.db.ReviewDAO;
import com.pricetracker.server.utils.TikiScraperUtil;
import com.pricetracker.models.Product;
import com.pricetracker.models.PriceHistory;
import com.pricetracker.models.Review;
import org.json.JSONObject;
import org.json.JSONArray;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Simple HTTP Server wrapper for the Price Tracker
 * Allows frontend to communicate via HTTP instead of raw TCP socket
 */
public class SimpleHttpServer {
    private static final int HTTP_PORT = 8080;
    private HttpServer server;
    private ProductDAO productDAO;
    private PriceHistoryDAO priceHistoryDAO;
    private ProductGroupDAO productGroupDAO;
    private ReviewDAO reviewDAO;


    public SimpleHttpServer() {
        this.productDAO = new ProductDAO();
        this.priceHistoryDAO = new PriceHistoryDAO();
        this.productGroupDAO = new ProductGroupDAO();
        this.reviewDAO = new ReviewDAO();

    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(HTTP_PORT), 0);
        
        // CORS and search endpoint
        server.createContext("/search", this::handleSearch);
        
        // NEW: Deals endpoint for discount page
        server.createContext("/deals", this::handleDeals);
        
        // NEW: Product detail endpoint
        server.createContext("/product-detail", this::handleProductDetail);
        
        server.setExecutor(null); // creates a default executor
        server.start();
        
        System.out.println("✓ HTTP Server started on port " + HTTP_PORT);
        System.out.println("  Frontend can now connect via: http://localhost:" + HTTP_PORT + "/search");
        System.out.println("  Frontend can also access deals via: http://localhost:" + HTTP_PORT + "/deals");
        System.out.println("  Frontend can also access product detail via: http://localhost:" + HTTP_PORT + "/product-detail");
    }

    private void handleSearch(HttpExchange exchange) throws IOException {
        // Add CORS headers
        Headers headers = exchange.getResponseHeaders();
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Methods", "POST, OPTIONS");
        headers.add("Access-Control-Allow-Headers", "Content-Type");
        headers.add("Content-Type", "application/json; charset=UTF-8");

        // Handle preflight OPTIONS request
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(200, -1);
            return;
        }

        // Only accept POST
        if (!"POST".equals(exchange.getRequestMethod())) {
            String response = "{\"success\": false, \"error\": \"Method not allowed\"}";
            sendResponse(exchange, 405, response);
            return;
        }

        try {
            // Read request body
            InputStream is = exchange.getRequestBody();
            String requestBody = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                    .lines()
                    .reduce("", (acc, line) -> acc + line);

            System.out.println("📥 Received request: " + requestBody);

            JSONObject requestJson = new JSONObject(requestBody);
            String action = requestJson.getString("action");
            String query = requestJson.getString("query");

            JSONObject responseJson;

            if ("SEARCH_BY_URL".equals(action)) {
                responseJson = handleSearchByUrl(query);
            } else if ("SEARCH_BY_NAME".equals(action)) {
                responseJson = handleSearchByName(query);
            } else {
                responseJson = new JSONObject();
                responseJson.put("success", false);
                responseJson.put("error", "Unknown action: " + action);
            }

            String response = responseJson.toString();
            System.out.println("📤 Sending response: " + response);
            sendResponse(exchange, 200, response);

        } catch (Exception e) {
            e.printStackTrace();
            String errorResponse = String.format(
                "{\"success\": false, \"error\": \"Server error: %s\"}", 
                e.getMessage().replace("\"", "\\\"")
            );
            sendResponse(exchange, 500, errorResponse);
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private JSONObject handleSearchByUrl(String tikiUrl) {
        JSONObject response = new JSONObject();
        
        try {
            System.out.println("🔍 Searching by URL: " + tikiUrl);
            
            // First check if product exists in database
            Product existingProduct = productDAO.searchByUrl(tikiUrl);
            
            if (existingProduct != null) {
                // Product exists - but ALWAYS scrape latest price from Tiki for realtime data!
                System.out.println("✓ Found existing product: " + existingProduct.getName());
                System.out.println("📊 Scraping latest price from Tiki...");
                
                // Scrape realtime price
                Object[] priceData = TikiScraperUtil.scrapePriceData(tikiUrl);
                PriceHistory currentPrice;
                
                if (priceData != null) {
                    // Use realtime scraped data
                    double price = (double) priceData[0];
                    double originalPrice = (double) priceData[1];
                    String dealType = (String) priceData[2];
                    
                    currentPrice = new PriceHistory();
                    currentPrice.setProductId(existingProduct.getProductId());
                    currentPrice.setPrice(price);
                    currentPrice.setOriginalPrice(originalPrice);
                    currentPrice.setCurrency("VND");
                    currentPrice.setDealType(dealType);
                    
                    System.out.println("✅ Realtime price: " + price + " VND (was: " + originalPrice + " VND)");
                } else {
                    // Fallback to database price if scraping fails
                    System.out.println("⚠️ Failed to scrape realtime price, using database value");
                    currentPrice = priceHistoryDAO.getCurrentPrice(existingProduct.getProductId());
                }
                
                String groupName = productGroupDAO.getGroupNameById(existingProduct.getGroupId());
                
                response.put("success", true);
                response.put("isNew", false);
                response.put("product", buildProductJSON(existingProduct, currentPrice, groupName));
                
            } else {
                // Product doesn't exist - scrape from Tiki and insert
                System.out.println("⚠ Product not found, scraping from Tiki...");
                
                Product newProduct = productDAO.insertProductFromTiki(tikiUrl);
                
                if (newProduct != null) {
                    System.out.println("✓ New product added: " + newProduct.getName());
                    
                    PriceHistory currentPrice = priceHistoryDAO.getCurrentPrice(newProduct.getProductId());
                    String groupName = productGroupDAO.getGroupNameById(newProduct.getGroupId());
                    
                    response.put("success", true);
                    response.put("isNew", true);
                    response.put("product", buildProductJSON(newProduct, currentPrice, groupName));
                } else {
                    response.put("success", false);
                    response.put("error", "Không thể lấy thông tin sản phẩm từ Tiki. Vui lòng kiểm tra lại URL.");
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("error", "Lỗi hệ thống: " + e.getMessage());
        }
        
        return response;
    }

    private JSONObject handleSearchByName(String searchQuery) {
        JSONObject response = new JSONObject();
        
        try {
            System.out.println("🔍 Searching by name: " + searchQuery);
            
            List<Product> products = productDAO.searchByNameLike(searchQuery);
            
            if (products.isEmpty()) {
                response.put("success", false);
                response.put("error", "Không tìm thấy sản phẩm nào phù hợp. Hãy thử sản phẩm khác nhé ^^");
            } else {
                System.out.println("✓ Found " + products.size() + " products");
                
                JSONArray productsArray = new JSONArray();
                
                for (Product product : products) {
                    PriceHistory currentPrice = priceHistoryDAO.getCurrentPrice(product.getProductId());
                    String groupName = productGroupDAO.getGroupNameById(product.getGroupId());
                    
                    productsArray.put(buildProductJSON(product, currentPrice, groupName));
                }
                
                response.put("success", true);
                response.put("count", products.size());
                response.put("products", productsArray);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("error", "Lỗi hệ thống: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * NEW: Handle deals endpoint - Get products with discounts
     * Supports filtering by deal_type: FLASH_SALE, HOT_DEAL, TRENDING, or ALL
     */
    private void handleDeals(HttpExchange exchange) throws IOException {
        // Add CORS headers
        Headers headers = exchange.getResponseHeaders();
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        headers.add("Access-Control-Allow-Headers", "Content-Type");
        headers.add("Content-Type", "application/json; charset=UTF-8");

        // Handle preflight OPTIONS request
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(200, -1);
            return;
        }

        try {
            String dealType = "ALL"; // Default: get all deals
            
            // Check if POST request with body
            if ("POST".equals(exchange.getRequestMethod())) {
                InputStream is = exchange.getRequestBody();
                String requestBody = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                        .lines()
                        .reduce("", (acc, line) -> acc + line);

                if (!requestBody.isEmpty()) {
                    JSONObject requestJson = new JSONObject(requestBody);
                    if (requestJson.has("deal_type")) {
                        dealType = requestJson.getString("deal_type");
                    }
                }
            }
            
            System.out.println("📥 Received deals request - Deal type: " + dealType);

            JSONObject responseJson = handleGetDeals(dealType);
            
            String response = responseJson.toString();
            System.out.println("📤 Sending deals response with " + 
                             responseJson.optInt("count", 0) + " products");
            sendResponse(exchange, 200, response);

        } catch (Exception e) {
            e.printStackTrace();
            String errorResponse = String.format(
                "{\"success\": false, \"error\": \"Server error: %s\"}", 
                e.getMessage().replace("\"", "\\\"")
            );
            sendResponse(exchange, 500, errorResponse);
        }
    }

    /**
     * NEW: Get products with deals/discounts
     * @param dealType Filter by deal type: "FLASH_SALE", "HOT_DEAL", "TRENDING", or "ALL"
     * @return JSONObject with products list
     */
    private JSONObject handleGetDeals(String dealType) {
        JSONObject response = new JSONObject();
        
        try {
            System.out.println("🎁 Fetching deals - Type: " + dealType);
            
            List<Product> products = productDAO.getProductsByDealType(dealType);
            
            if (products.isEmpty()) {
                response.put("success", false);
                response.put("error", "Hiện tại chưa có sản phẩm giảm giá nào. Vui lòng quay lại sau!");
            } else {
                System.out.println("✓ Found " + products.size() + " deal products");
                
                JSONArray productsArray = new JSONArray();
                
                for (Product product : products) {
                    PriceHistory currentPrice = priceHistoryDAO.getCurrentPrice(product.getProductId());
                    String groupName = productGroupDAO.getGroupNameById(product.getGroupId());
                    
                    productsArray.put(buildProductJSON(product, currentPrice, groupName));
                }
                
                response.put("success", true);
                response.put("count", products.size());
                response.put("deal_type", dealType);
                response.put("products", productsArray);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("error", "Lỗi hệ thống: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * NEW: Handle product detail endpoint - Get detailed product information
     * Requires product_id in request body
     */
    private void handleProductDetail(HttpExchange exchange) throws IOException {
        // Add CORS headers
        Headers headers = exchange.getResponseHeaders();
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        headers.add("Access-Control-Allow-Headers", "Content-Type");
        headers.add("Content-Type", "application/json; charset=UTF-8");

        // Handle preflight OPTIONS request
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(200, -1);
            return;
        }

        try {
            if (!"POST".equals(exchange.getRequestMethod())) {
                String errorResponse = "{\"success\": false, \"error\": \"Method not allowed. Use POST.\"}";
                sendResponse(exchange, 405, errorResponse);
                return;
            }
            
            // Read request body
            InputStream is = exchange.getRequestBody();
            String requestBody = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                    .lines()
                    .reduce("", (acc, line) -> acc + line);

            if (requestBody.isEmpty()) {
                String errorResponse = "{\"success\": false, \"error\": \"Missing product_id in request body\"}";
                sendResponse(exchange, 400, errorResponse);
                return;
            }
            
            JSONObject requestJson = new JSONObject(requestBody);
            if (!requestJson.has("product_id")) {
                String errorResponse = "{\"success\": false, \"error\": \"Missing product_id in request body\"}";
                sendResponse(exchange, 400, errorResponse);
                return;
            }
            
            int productId = requestJson.getInt("product_id");
            System.out.println("📥 Received product detail request - Product ID: " + productId);

            JSONObject responseJson = handleGetProductDetail(productId);
            
            String response = responseJson.toString();
            System.out.println("📤 Sending product detail response");
            sendResponse(exchange, 200, response);

        } catch (Exception e) {
            e.printStackTrace();
            String errorResponse = String.format(
                "{\"success\": false, \"error\": \"Server error: %s\"}", 
                e.getMessage().replace("\"", "\\\"")
            );
            sendResponse(exchange, 500, errorResponse);
        }
    }

    /**
     * NEW: Get detailed product information including price history, reviews, and similar products
     * @param productId The product ID
     * @return JSONObject with complete product details
     */
    private JSONObject handleGetProductDetail(int productId) {
        JSONObject response = new JSONObject();
        
        try {
            System.out.println("🔍 Fetching product detail - ID: " + productId);
            
            // Get product basic info
            Product product = productDAO.getProductById(productId);
            
            if (product == null) {
                response.put("success", false);
                response.put("error", "Không tìm thấy sản phẩm này!");
                return response;
            }
            
            System.out.println("✓ Found product: " + product.getName());
            
            // Get current price
            PriceHistory currentPrice = priceHistoryDAO.getCurrentPrice(productId);
            
            // Get price history
            List<PriceHistory> priceHistory = priceHistoryDAO.getPriceHistoryByProductId(productId);
            
            // Get reviews
            List<Review> reviews = reviewDAO.getReviewsByProductId(productId);
            int reviewCount = reviewDAO.countReviewsByProductId(productId);
            
            // Get similar products (same group) - 16 products for 4 rows
            List<Product> similarProducts = productDAO.getSimilarProducts(
                product.getGroupId(), 
                productId, 
                16  // Limit to 16 similar products (4 rows x 4 columns)
            );
            
            // Get group name
            String groupName = productGroupDAO.getGroupNameById(product.getGroupId());
            
            // Build response JSON
            response.put("success", true);
            
            // Product info
            JSONObject productJson = new JSONObject();
            productJson.put("product_id", product.getProductId());
            productJson.put("group_id", product.getGroupId());
            productJson.put("group_name", groupName);
            productJson.put("name", product.getName());
            productJson.put("brand", product.getBrand() != null ? product.getBrand() : "");
            productJson.put("url", product.getUrl());
            productJson.put("image_url", product.getImageUrl());
            productJson.put("description", product.getDescription() != null ? product.getDescription() : "");
            productJson.put("source", product.getSource());
            
            response.put("product", productJson);
            
            // Current price info
            JSONObject priceJson = new JSONObject();
            if (currentPrice != null) {
                priceJson.put("current_price", currentPrice.getPrice());
                priceJson.put("original_price", currentPrice.getOriginalPrice());
                priceJson.put("currency", currentPrice.getCurrency());
                priceJson.put("deal_type", currentPrice.getDealType() != null ? currentPrice.getDealType() : "Normal");
                
                // Calculate discount percentage
                int discountPercent = 0;
                if (currentPrice.getOriginalPrice() > currentPrice.getPrice() && currentPrice.getOriginalPrice() > 0) {
                    discountPercent = (int) Math.round(((currentPrice.getOriginalPrice() - currentPrice.getPrice()) / currentPrice.getOriginalPrice()) * 100);
                }
                priceJson.put("discount_percent", discountPercent);
            } else {
                priceJson.put("current_price", 0);
                priceJson.put("original_price", 0);
                priceJson.put("currency", "VND");
                priceJson.put("deal_type", "Normal");
                priceJson.put("discount_percent", 0);
            }
            response.put("price", priceJson);
            
            // Price history array
            JSONArray priceHistoryArray = new JSONArray();
            for (PriceHistory ph : priceHistory) {
                JSONObject phJson = new JSONObject();
                phJson.put("price", ph.getPrice());
                phJson.put("original_price", ph.getOriginalPrice());
                phJson.put("captured_at", ph.getCapturedAt().toString());
                phJson.put("deal_type", ph.getDealType() != null ? ph.getDealType() : "Normal");
                priceHistoryArray.put(phJson);
            }
            response.put("price_history", priceHistoryArray);
            
            // Reviews
            JSONObject reviewsJson = new JSONObject();
            reviewsJson.put("count", reviewCount);
            
            JSONArray reviewsArray = new JSONArray();
            for (Review review : reviews) {
                JSONObject rJson = new JSONObject();
                rJson.put("reviewer_name", review.getReviewerName());
                rJson.put("rating", review.getRating());
                rJson.put("review_text", review.getReviewText());
                rJson.put("review_date", review.getReviewDate().toString());
                reviewsArray.put(rJson);
            }
            reviewsJson.put("reviews", reviewsArray);
            response.put("reviews", reviewsJson);
            
            // Similar products
            JSONArray similarProductsArray = new JSONArray();
            for (Product sp : similarProducts) {
                PriceHistory spPrice = priceHistoryDAO.getCurrentPrice(sp.getProductId());
                JSONObject spJson = buildProductJSON(sp, spPrice, groupName);
                similarProductsArray.put(spJson);
            }
            response.put("similar_products", similarProductsArray);
            
            System.out.println("✓ Product detail prepared: " + reviewCount + " reviews, " + 
                             priceHistory.size() + " price records, " + 
                             similarProducts.size() + " similar products");
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("error", "Lỗi hệ thống: " + e.getMessage());
        }
        
        return response;
    }

    private JSONObject buildProductJSON(Product product, PriceHistory priceHistory, String groupName) {
        JSONObject json = new JSONObject();
        
        // Product basic info
        json.put("product_id", product.getProductId());
        json.put("group_id", product.getGroupId());
        json.put("group_name", groupName);
        json.put("name", product.getName());
        json.put("brand", product.getBrand() != null ? product.getBrand() : "");
        json.put("url", product.getUrl());
        json.put("image_url", product.getImageUrl());
        json.put("description", product.getDescription() != null ? product.getDescription() : "");
        json.put("source", product.getSource());
        
        // Price information
        if (priceHistory != null) {
            double currentPrice = priceHistory.getPrice();
            double originalPrice = priceHistory.getOriginalPrice();
            
            json.put("price", currentPrice);
            json.put("original_price", originalPrice);
            json.put("currency", priceHistory.getCurrency());
            json.put("deal_type", priceHistory.getDealType() != null ? priceHistory.getDealType() : "Normal");
            
            // Calculate discount percentage
            int discountPercent = 0;
            if (originalPrice > currentPrice && originalPrice > 0) {
                discountPercent = (int) Math.round(((originalPrice - currentPrice) / originalPrice) * 100);
            }
            json.put("discount_percent", discountPercent);
        } else {
            json.put("price", 0);
            json.put("original_price", 0);
            json.put("currency", "VND");
            json.put("deal_type", "Normal");
            json.put("discount_percent", 0);
        }
        
        return json;
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("HTTP Server stopped");
        }
    }

    public static void main(String[] args) {
        try {
            SimpleHttpServer httpServer = new SimpleHttpServer();
            httpServer.start();
            
            System.out.println("\nPress Ctrl+C to stop the server...");
            
            // Keep the server running
            Thread.currentThread().join();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
