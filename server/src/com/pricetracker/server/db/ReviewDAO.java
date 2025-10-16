package com.pricetracker.server.db;

import com.pricetracker.models.Review;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ReviewDAO - Data Access Object cho đánh giá sản phẩm
 * Xử lý các truy vấn liên quan đến review của khách hàng
 */
public class ReviewDAO {
    private DatabaseConnectionManager dbManager;
    
    public ReviewDAO() {
        this.dbManager = DatabaseConnectionManager.getInstance();
    }
    
    /**
     * Thêm review mới
     */
    public int insertReview(Review review) throws SQLException {
        String sql = "INSERT INTO review (product_id, reviewer_name, rating, review_text) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setInt(1, review.getProductId());
            pstmt.setString(2, review.getReviewerName());
            pstmt.setInt(3, review.getRating());
            pstmt.setString(4, review.getReviewText());
            
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
     * Lấy review theo ID
     */
    public Review getReviewById(int reviewId) throws SQLException {
        String sql = "SELECT * FROM review WHERE review_id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, reviewId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return extractReviewFromResultSet(rs);
            }
            return null;
        }
    }
    
    /**
     * Lấy tất cả review của một sản phẩm
     */
    public List<Review> getReviewsByProductId(int productId) throws SQLException {
        String sql = "SELECT * FROM review WHERE product_id = ? ORDER BY review_date DESC";
        List<Review> reviews = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, productId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                reviews.add(extractReviewFromResultSet(rs));
            }
        }
        return reviews;
    }
    
    /**
     * Lấy review theo rating
     */
    public List<Review> getReviewsByRating(int productId, int rating) throws SQLException {
        String sql = "SELECT * FROM review WHERE product_id = ? AND rating = ? ORDER BY review_date DESC";
        List<Review> reviews = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, productId);
            pstmt.setInt(2, rating);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                reviews.add(extractReviewFromResultSet(rs));
            }
        }
        return reviews;
    }
    
    /**
     * Lấy rating trung bình của sản phẩm
     */
    public double getAverageRating(int productId) throws SQLException {
        String sql = "SELECT AVG(rating) as avg_rating FROM review WHERE product_id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, productId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getDouble("avg_rating");
            }
            return 0.0;
        }
    }
    
    /**
     * Đếm số lượng review của sản phẩm
     */
    public int countReviews(int productId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM review WHERE product_id = ?";
        
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
     * Đếm số lượng review theo từng rating
     */
    public int countReviewsByRating(int productId, int rating) throws SQLException {
        String sql = "SELECT COUNT(*) FROM review WHERE product_id = ? AND rating = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, productId);
            pstmt.setInt(2, rating);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }
    
    /**
     * Lấy N review mới nhất
     */
    public List<Review> getRecentReviews(int productId, int limit) throws SQLException {
        String sql = "SELECT * FROM review WHERE product_id = ? ORDER BY review_date DESC LIMIT ?";
        List<Review> reviews = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, productId);
            pstmt.setInt(2, limit);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                reviews.add(extractReviewFromResultSet(rs));
            }
        }
        return reviews;
    }
    
    /**
     * Cập nhật review
     */
    public boolean updateReview(Review review) throws SQLException {
        String sql = "UPDATE review SET reviewer_name = ?, rating = ?, review_text = ? WHERE review_id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, review.getReviewerName());
            pstmt.setInt(2, review.getRating());
            pstmt.setString(3, review.getReviewText());
            pstmt.setInt(4, review.getReviewId());
            
            return pstmt.executeUpdate() > 0;
        }
    }
    
    /**
     * Xóa review
     */
    public boolean deleteReview(int reviewId) throws SQLException {
        String sql = "DELETE FROM review WHERE review_id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, reviewId);
            return pstmt.executeUpdate() > 0;
        }
    }
    
    /**
     * Xóa tất cả review của sản phẩm
     */
    public boolean deleteReviewsByProductId(int productId) throws SQLException {
        String sql = "DELETE FROM review WHERE product_id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, productId);
            return pstmt.executeUpdate() > 0;
        }
    }
    
    /**
     * Trích xuất Review object từ ResultSet
     */
    private Review extractReviewFromResultSet(ResultSet rs) throws SQLException {
        Review review = new Review();
        review.setReviewId(rs.getInt("review_id"));
        review.setProductId(rs.getInt("product_id"));
        review.setReviewerName(rs.getString("reviewer_name"));
        review.setRating(rs.getInt("rating"));
        review.setReviewText(rs.getString("review_text"));
        review.setReviewDate(rs.getTimestamp("review_date"));
        return review;
    }
}
