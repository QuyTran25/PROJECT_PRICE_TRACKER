package com.pricetracker.server.db;

import com.pricetracker.models.Product;
import com.pricetracker.server.utils.TikiScraperUtil;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductDAO {

    public List<Product> getAllProducts() {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT * FROM product LIMIT 10";

        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Product p = new Product();
                p.setProductId(rs.getInt("product_id"));
                p.setName(rs.getString("name"));
                p.setBrand(rs.getString("brand"));
                p.setUrl(rs.getString("url"));
                p.setImageUrl(rs.getString("image_url"));
                list.add(p);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }
    
    /**
     * Get product by ID
     * @param productId The product ID
     * @return Product if found, null otherwise
     */
    public Product getProductById(int productId) {
        String sql = "SELECT * FROM product WHERE product_id = ?";
        
        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, productId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToProduct(rs);
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting product by ID: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Search product by exact Tiki URL
     * @param tikiUrl The Tiki product URL
     * @return Product if found, null otherwise
     */
    public Product searchByUrl(String tikiUrl) {
        String sql = "SELECT * FROM product WHERE url = ?";
        
        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, tikiUrl);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToProduct(rs);
            }
            
        } catch (SQLException e) {
            System.err.println("Error searching by URL: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Get similar products by group_id (for "Similar Products" section)
     * @param groupId The product group ID
     * @param excludeProductId Product ID to exclude (the current product)
     * @param limit Maximum number of similar products to return
     * @return List of similar products
     */
    public List<Product> getSimilarProducts(int groupId, int excludeProductId, int limit) {
        List<Product> results = new ArrayList<>();
        
        String sql = "SELECT * FROM product " +
                     "WHERE group_id = ? AND product_id != ? " +
                     "ORDER BY product_id DESC " +
                     "LIMIT ?";
        
        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, groupId);
            stmt.setInt(2, excludeProductId);
            stmt.setInt(3, limit);
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                results.add(mapResultSetToProduct(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting similar products: " + e.getMessage());
        }
        
        return results;
    }
    
    /**
     * Search products by name (LIKE search)
     * Improved algorithm: First try exact phrase match, then word boundary match, finally fallback to substring
     * @param keyword Search keyword
     * @return List of matching products
     */
    public List<Product> searchByNameLike(String keyword) {
        List<Product> results = new ArrayList<>();
        
        // Use BINARY collation to distinguish between 'áo' and 'ao'
        // Priority 1: Exact phrase match (case-insensitive but accent-sensitive)
        // Priority 2: Word boundary match (space before/after or start/end of string)
        // Priority 3: Substring match with BINARY (accent-sensitive)
        String sql = "SELECT DISTINCT p.* FROM product p " +
                     "LEFT JOIN product_group pg ON p.group_id = pg.group_id " +
                     "WHERE " +
                     // Exact match (highest priority)
                     "p.name COLLATE utf8mb4_bin LIKE ? OR " +
                     "p.name COLLATE utf8mb4_bin LIKE ? OR " +
                     "p.name COLLATE utf8mb4_bin LIKE ? OR " +
                     "p.name COLLATE utf8mb4_bin LIKE ? OR " +
                     // Search in group name (exact and partial match)
                     "pg.group_name COLLATE utf8mb4_bin LIKE ? OR " +
                     "pg.group_name COLLATE utf8mb4_bin LIKE ? " +
                     "LIMIT 50";
        
        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // Product name patterns
            stmt.setString(1, keyword);  // Exact match
            stmt.setString(2, keyword + " %");  // Word at start
            stmt.setString(3, "% " + keyword);  // Word at end
            stmt.setString(4, "% " + keyword + " %");  // Word in middle
            
            // Group name patterns
            stmt.setString(5, keyword);  // Exact group name match
            stmt.setString(6, "%" + keyword + "%");  // Partial group name match
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                results.add(mapResultSetToProduct(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("Error searching by name: " + e.getMessage());
        }
        
        return results;
    }
    
    /**
     * Insert product from Tiki scraping
     * First scrapes the product data, then inserts into database
     * @param tikiUrl The Tiki product URL
     * @return The inserted Product with product_id, or null if failed
     */
    public Product insertProductFromTiki(String tikiUrl) {
        // Scrape product data from Tiki
        Product product = TikiScraperUtil.scrapeProductFromUrl(tikiUrl);
        if (product == null) {
            System.err.println("Failed to scrape product from Tiki");
            return null;
        }
        
        // Insert into database
        String sql = "INSERT INTO product (group_id, name, brand, url, image_url, description, source) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setInt(1, product.getGroupId());
            stmt.setString(2, product.getName());
            stmt.setString(3, product.getBrand());
            stmt.setString(4, product.getUrl());
            stmt.setString(5, product.getImageUrl());
            stmt.setString(6, product.getDescription());
            stmt.setString(7, product.getSource());
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    product.setProductId(generatedKeys.getInt(1));
                    System.out.println("✅ Inserted new product: " + product.getName() + " (ID: " + product.getProductId() + ")");
                    
                    // Also scrape and insert initial price data
                    insertInitialPriceData(product.getProductId(), tikiUrl);
                    
                    return product;
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error inserting product: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Insert initial price data after adding new product
     */
    private void insertInitialPriceData(int productId, String tikiUrl) {
        Object[] priceData = TikiScraperUtil.scrapePriceData(tikiUrl);
        if (priceData == null) {
            return;
        }
        
        double price = (double) priceData[0];
        double originalPrice = (double) priceData[1];
        String dealType = (String) priceData[2];
        
        String sql = "INSERT INTO price_history (product_id, price, original_price, currency, deal_type) " +
                     "VALUES (?, ?, ?, 'VND', ?)";
        
        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, productId);
            stmt.setDouble(2, price);
            stmt.setDouble(3, originalPrice);
            stmt.setString(4, dealType);
            
            stmt.executeUpdate();
            System.out.println("✅ Inserted initial price: " + price + " VND");
            
        } catch (SQLException e) {
            System.err.println("Error inserting initial price: " + e.getMessage());
        }
    }
    
    /**
     * Map ResultSet to Product object
     */
    private Product mapResultSetToProduct(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setProductId(rs.getInt("product_id"));
        p.setGroupId(rs.getInt("group_id"));
        p.setName(rs.getString("name"));
        p.setBrand(rs.getString("brand"));
        p.setUrl(rs.getString("url"));
        p.setImageUrl(rs.getString("image_url"));
        p.setDescription(rs.getString("description"));
        p.setSource(rs.getString("source"));
        p.setFeatured(rs.getBoolean("is_featured"));
        p.setCreatedAt(rs.getTimestamp("created_at"));
        return p;
    }
    
    /**
     * NEW METHOD: Get products by deal type (for discount page)
     * Logic sorting:
     * - ALL: Giảm giá HOT NHẤT (% giảm cao nhất)
     * - FLASH_SALE: VỪA MỚI GIẢM (recorded_at gần nhất)
     * - HOT_DEAL: GIẢM SÂU (% giảm cao + giá trị tiết kiệm lớn)
     * - TRENDING: MỖI DANH MỤC 1 SẢN PHẨM GIẢM GIÁ SÂU NHẤT
     * 
     * @param dealType "FLASH_SALE", "HOT_DEAL", "TRENDING", or "ALL" for all deals
     * @return List of products with the specified deal type
     */
    public List<Product> getProductsByDealType(String dealType) {
        List<Product> results = new ArrayList<>();
        
        String sql;
        
        if ("ALL".equals(dealType)) {
            // TẤT CẢ DEALS HOT: Sản phẩm giảm giá HOT NHẤT (% giảm cao nhất)
            sql = "SELECT DISTINCT p.*, ph.original_price, ph.price, ph.recorded_at " +
                  "FROM product p " +
                  "INNER JOIN price_history ph ON p.product_id = ph.product_id " +
                  "WHERE ph.price_id IN (" +
                  "    SELECT MAX(price_id) FROM price_history GROUP BY product_id" +
                  ") AND ph.original_price > ph.price " +
                  "ORDER BY ((ph.original_price - ph.price) / ph.original_price) DESC " +
                  "LIMIT 100";
                  
        } else if ("FLASH_SALE".equals(dealType)) {
            // FLASH SALE: VỪA MỚI GIẢM GIÁ (recorded_at mới nhất)
            sql = "SELECT DISTINCT p.*, ph.original_price, ph.price, ph.recorded_at " +
                  "FROM product p " +
                  "INNER JOIN price_history ph ON p.product_id = ph.product_id " +
                  "WHERE ph.price_id IN (" +
                  "    SELECT MAX(price_id) FROM price_history GROUP BY product_id" +
                  ") AND ph.deal_type = 'FLASH_SALE' AND ph.original_price > ph.price " +
                  "ORDER BY ph.recorded_at DESC, ((ph.original_price - ph.price) / ph.original_price) DESC " +
                  "LIMIT 100";
                  
        } else if ("HOT_DEAL".equals(dealType)) {
            // HOT DEAL: GIẢM SÂU (kết hợp % giảm và giá trị tiết kiệm)
            sql = "SELECT DISTINCT p.*, ph.original_price, ph.price, ph.recorded_at " +
                  "FROM product p " +
                  "INNER JOIN price_history ph ON p.product_id = ph.product_id " +
                  "WHERE ph.price_id IN (" +
                  "    SELECT MAX(price_id) FROM price_history GROUP BY product_id" +
                  ") AND ph.deal_type = 'HOT_DEAL' AND ph.original_price > ph.price " +
                  "ORDER BY (ph.original_price - ph.price) DESC, " +
                  "         ((ph.original_price - ph.price) / ph.original_price) DESC " +
                  "LIMIT 100";
                  
        } else if ("TRENDING".equals(dealType)) {
            // TRENDING: MỖI DANH MỤC 1 SẢN PHẨM GIẢM GIÁ SÂU NHẤT
            // Logic: Lấy sản phẩm có % giảm giá cao nhất từ mỗi product_group
            sql = "SELECT p.*, ph.original_price, ph.price, ph.recorded_at " +
                  "FROM product p " +
                  "INNER JOIN price_history ph ON p.product_id = ph.product_id " +
                  "INNER JOIN (" +
                  "    SELECT p2.group_id, " +
                  "           MAX((ph2.original_price - ph2.price) / ph2.original_price) as max_discount " +
                  "    FROM product p2 " +
                  "    INNER JOIN price_history ph2 ON p2.product_id = ph2.product_id " +
                  "    WHERE ph2.price_id IN (" +
                  "        SELECT MAX(price_id) FROM price_history GROUP BY product_id" +
                  "    ) AND ph2.original_price > ph2.price " +
                  "    GROUP BY p2.group_id" +
                  ") AS best_per_group ON p.group_id = best_per_group.group_id " +
                  "    AND ((ph.original_price - ph.price) / ph.original_price) = best_per_group.max_discount " +
                  "WHERE ph.price_id IN (" +
                  "    SELECT MAX(price_id) FROM price_history GROUP BY product_id" +
                  ") AND ph.original_price > ph.price " +
                  "GROUP BY p.group_id " +
                  "ORDER BY ((ph.original_price - ph.price) / ph.original_price) DESC " +
                  "LIMIT 100";
        } else {
            // Fallback: Sort by discount percent
            sql = "SELECT DISTINCT p.*, ph.original_price, ph.price, ph.recorded_at " +
                  "FROM product p " +
                  "INNER JOIN price_history ph ON p.product_id = ph.product_id " +
                  "WHERE ph.price_id IN (" +
                  "    SELECT MAX(price_id) FROM price_history GROUP BY product_id" +
                  ") AND ph.deal_type = ? AND ph.original_price > ph.price " +
                  "ORDER BY ((ph.original_price - ph.price) / ph.original_price) DESC " +
                  "LIMIT 100";
        }
        
        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // Chỉ set parameter nếu không phải các case đặc biệt
            if (!"ALL".equals(dealType) && 
                !"FLASH_SALE".equals(dealType) && 
                !"HOT_DEAL".equals(dealType) && 
                !"TRENDING".equals(dealType)) {
                stmt.setString(1, dealType);
            }
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                results.add(mapResultSetToProduct(rs));
            }
            
            System.out.println("✓ Found " + results.size() + " products with deal type: " + dealType);
            
        } catch (SQLException e) {
            System.err.println("Error getting products by deal type: " + e.getMessage());
            e.printStackTrace();
        }
        
        return results; 
    }
}

