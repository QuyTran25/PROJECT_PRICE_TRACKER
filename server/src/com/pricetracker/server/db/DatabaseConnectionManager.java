package com.pricetracker.server.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DatabaseConnectionManager - Quản lý kết nối database
 * Singleton pattern để đảm bảo chỉ có một instance duy nhất
 */
public class DatabaseConnectionManager {
    private static DatabaseConnectionManager instance;
    
    // Thông tin kết nối MySQL XAMPP
    private static final String DB_URL = "jdbc:mysql://localhost:3306/price_insight";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = ""; // XAMPP mặc định không có password
    private static final String DB_DRIVER = "com.mysql.cj.jdbc.Driver";
    
    private Connection connection;
    
    // Private constructor cho Singleton
    private DatabaseConnectionManager() {
        try {
            // Load MySQL JDBC Driver
            Class.forName(DB_DRIVER);
            System.out.println("✓ MySQL JDBC Driver loaded successfully");
        } catch (ClassNotFoundException e) {
            System.err.println("✗ MySQL JDBC Driver not found!");
            e.printStackTrace();
        }
    }
    
    /**
     * Lấy instance duy nhất của DatabaseConnectionManager
     */
    public static synchronized DatabaseConnectionManager getInstance() {
        if (instance == null) {
            instance = new DatabaseConnectionManager();
        }
        return instance;
    }
    
    /**
     * Lấy kết nối đến database
     * Tự động tạo kết nối mới nếu kết nối cũ đã đóng
     */
    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("✓ Connected to MySQL database: " + DB_URL);
        }
        return connection;
    }
    
    /**
     * Đóng kết nối database
     */
    public void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("✓ Database connection closed");
            } catch (SQLException e) {
                System.err.println("✗ Error closing database connection");
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Kiểm tra kết nối database
     */
    public boolean testConnection() {
        try {
            Connection conn = getConnection();
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("✗ Database connection test failed");
            e.printStackTrace();
            return false;
        }
    }
}
