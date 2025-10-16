package com.pricetracker.server.db;

import com.pricetracker.models.PriceData;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * PriceHistoryDAO - Data Access Object cho lịch sử giá sản phẩm
 * Xử lý các truy vấn liên quan đến giá sản phẩm theo thời gian
 */
public class PriceHistoryDAO {
    private DatabaseConnectionManager dbManager;
    
    public PriceHistoryDAO() {
        this.dbManager = DatabaseConnectionManager.getInstance();
    }
    
    /**
     * Thêm lịch sử giá mới
     */
    public int insertPriceHistory(PriceData priceData) throws SQLException {
        String sql = "INSERT INTO price_history (product_id, price) VALUES (?, ?)";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setInt(1, priceData.getProductId());
            pstmt.setDouble(2, priceData.getPrice());
            
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
     * Lấy lịch sử giá của một sản phẩm
     */
    public List<PriceData> getPriceHistoryByProductId(int productId) throws SQLException {
        String sql = "SELECT * FROM price_history WHERE product_id = ? ORDER BY recorded_at DESC";
        List<PriceData> priceHistory = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, productId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                priceHistory.add(extractPriceDataFromResultSet(rs));
            }
        }
        return priceHistory;
    }
    
    /**
     * Lấy lịch sử giá trong khoảng thời gian
     */
    public List<PriceData> getPriceHistoryByDateRange(int productId, Timestamp startDate, Timestamp endDate) throws SQLException {
        String sql = "SELECT * FROM price_history WHERE product_id = ? AND recorded_at BETWEEN ? AND ? ORDER BY recorded_at ASC";
        List<PriceData> priceHistory = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, productId);
            pstmt.setTimestamp(2, startDate);
            pstmt.setTimestamp(3, endDate);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                priceHistory.add(extractPriceDataFromResultSet(rs));
            }
        }
        return priceHistory;
    }
    
    /**
     * Lấy giá mới nhất của sản phẩm
     */
    public PriceData getLatestPrice(int productId) throws SQLException {
        String sql = "SELECT * FROM price_history WHERE product_id = ? ORDER BY recorded_at DESC LIMIT 1";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, productId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return extractPriceDataFromResultSet(rs);
            }
            return null;
        }
    }
    
    /**
     * Lấy giá thấp nhất của sản phẩm
     */
    public PriceData getLowestPrice(int productId) throws SQLException {
        String sql = "SELECT * FROM price_history WHERE product_id = ? ORDER BY price ASC LIMIT 1";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, productId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return extractPriceDataFromResultSet(rs);
            }
            return null;
        }
    }
    
    /**
     * Lấy giá cao nhất của sản phẩm
     */
    public PriceData getHighestPrice(int productId) throws SQLException {
        String sql = "SELECT * FROM price_history WHERE product_id = ? ORDER BY price DESC LIMIT 1";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, productId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return extractPriceDataFromResultSet(rs);
            }
            return null;
        }
    }
    
    /**
     * Lấy giá trung bình của sản phẩm
     */
    public double getAveragePrice(int productId) throws SQLException {
        String sql = "SELECT AVG(price) as avg_price FROM price_history WHERE product_id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, productId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getDouble("avg_price");
            }
            return 0.0;
        }
    }
    
    /**
     * Lấy N bản ghi giá gần nhất
     */
    public List<PriceData> getRecentPriceHistory(int productId, int limit) throws SQLException {
        String sql = "SELECT * FROM price_history WHERE product_id = ? ORDER BY recorded_at DESC LIMIT ?";
        List<PriceData> priceHistory = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, productId);
            pstmt.setInt(2, limit);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                priceHistory.add(extractPriceDataFromResultSet(rs));
            }
        }
        return priceHistory;
    }
    
    /**
     * Xóa lịch sử giá của sản phẩm
     */
    public boolean deletePriceHistoryByProductId(int productId) throws SQLException {
        String sql = "DELETE FROM price_history WHERE product_id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, productId);
            return pstmt.executeUpdate() > 0;
        }
    }
    
    /**
     * Đếm số bản ghi lịch sử giá của sản phẩm
     */
    public int countPriceHistory(int productId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM price_history WHERE product_id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, productId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }
    
    /**
     * Trích xuất PriceData object từ ResultSet
     */
    private PriceData extractPriceDataFromResultSet(ResultSet rs) throws SQLException {
        PriceData priceData = new PriceData();
        priceData.setHistoryId(rs.getInt("history_id"));
        priceData.setProductId(rs.getInt("product_id"));
        priceData.setPrice(rs.getDouble("price"));
        priceData.setRecordedAt(rs.getTimestamp("recorded_at"));
        return priceData;
    }
}
