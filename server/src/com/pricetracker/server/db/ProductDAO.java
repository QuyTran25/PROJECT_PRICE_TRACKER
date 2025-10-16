package com.pricetracker.server.db;

import com.pricetracker.models.Product;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ProductDAO - Data Access Object cho sản phẩm
 * Xử lý tất cả các truy vấn liên quan đến sản phẩm trong database
 */
public class ProductDAO {
    private DatabaseConnectionManager dbManager;
    
    public ProductDAO() {
        this.dbManager = DatabaseConnectionManager.getInstance();
    }
    
    /**
     * Thêm sản phẩm mới vào database
     */
    public int insertProduct(Product product) throws SQLException {
        String sql = "INSERT INTO product (group_id, name, brand, url, image_url, " +
                    "description, source, is_featured) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setInt(1, product.getGroupId());
            pstmt.setString(2, product.getName());
            pstmt.setString(3, product.getBrand());
            pstmt.setString(4, product.getUrl());
            pstmt.setString(5, product.getImageUrl());
            pstmt.setString(6, product.getDescription());
            pstmt.setString(7, product.getSource());
            pstmt.setBoolean(8, product.isFeatured());
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
            return -1;
        }
    }
    
    /**
     * Lấy sản phẩm theo ID
     */
    public Product getProductById(int productId) throws SQLException {
        String sql = "SELECT * FROM product WHERE product_id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, productId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return extractProductFromResultSet(rs);
            }
            return null;
        }
    }
    
    /**
     * Lấy tất cả sản phẩm
     */
    public List<Product> getAllProducts() throws SQLException {
        String sql = "SELECT * FROM product ORDER BY created_at DESC";
        List<Product> products = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                products.add(extractProductFromResultSet(rs));
            }
        }
        return products;
    }
    
    /**
     * Tìm kiếm sản phẩm theo tên
     */
    public List<Product> searchProductsByName(String keyword) throws SQLException {
        String sql = "SELECT * FROM product WHERE name LIKE ? ORDER BY name";
        List<Product> products = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, "%" + keyword + "%");
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                products.add(extractProductFromResultSet(rs));
            }
        }
        return products;
    }
    
    /**
     * Lấy sản phẩm theo nhóm
     */
    public List<Product> getProductsByGroup(int groupId) throws SQLException {
        String sql = "SELECT * FROM product WHERE group_id = ? ORDER BY name";
        List<Product> products = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, groupId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                products.add(extractProductFromResultSet(rs));
            }
        }
        return products;
    }
    
    /**
     * Cập nhật thông tin sản phẩm
     */
    public boolean updateProduct(Product product) throws SQLException {
        String sql = "UPDATE product SET group_id = ?, name = ?, brand = ?, " +
                    "url = ?, image_url = ?, description = ?, source = ?, is_featured = ? " +
                    "WHERE product_id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, product.getGroupId());
            pstmt.setString(2, product.getName());
            pstmt.setString(3, product.getBrand());
            pstmt.setString(4, product.getUrl());
            pstmt.setString(5, product.getImageUrl());
            pstmt.setString(6, product.getDescription());
            pstmt.setString(7, product.getSource());
            pstmt.setBoolean(8, product.isFeatured());
            pstmt.setInt(9, product.getProductId());
            
            return pstmt.executeUpdate() > 0;
        }
    }
    
    /**
     * Xóa sản phẩm
     */
    public boolean deleteProduct(int productId) throws SQLException {
        String sql = "DELETE FROM product WHERE product_id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, productId);
            return pstmt.executeUpdate() > 0;
        }
    }
    
    /**
     * Đếm tổng số sản phẩm
     */
    public int countProducts() throws SQLException {
        String sql = "SELECT COUNT(*) FROM product";
        
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }
    
    /**
     * Trích xuất Product object từ ResultSet
     */
    private Product extractProductFromResultSet(ResultSet rs) throws SQLException {
        Product product = new Product();
        product.setProductId(rs.getInt("product_id"));
        product.setGroupId(rs.getInt("group_id"));
        product.setName(rs.getString("name"));
        product.setBrand(rs.getString("brand"));
        product.setUrl(rs.getString("url"));
        product.setImageUrl(rs.getString("image_url"));
        product.setDescription(rs.getString("description"));
        product.setSource(rs.getString("source"));
        product.setFeatured(rs.getBoolean("is_featured"));
        product.setCreatedAt(rs.getTimestamp("created_at"));
        return product;
    }
}
