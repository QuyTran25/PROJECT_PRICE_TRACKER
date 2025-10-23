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
        String sql = "SELECT DISTINCT p.* FROM product p WHERE " +
                     // Exact match (highest priority)
                     "p.name COLLATE utf8mb4_bin LIKE ? OR " +
                     // Word at start
                     "p.name COLLATE utf8mb4_bin LIKE ? OR " +
                     // Word at end
                     "p.name COLLATE utf8mb4_bin LIKE ? OR " +
                     // Word in middle
                     "p.name COLLATE utf8mb4_bin LIKE ? " +
                     "LIMIT 20";
        
        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // Exact match
            stmt.setString(1, keyword);
            // Word at start (keyword followed by space)
            stmt.setString(2, keyword + " %");
            // Word at end (space before keyword)
            stmt.setString(3, "% " + keyword);
            // Word in middle (space before AND after)
            stmt.setString(4, "% " + keyword + " %");
            
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
}
