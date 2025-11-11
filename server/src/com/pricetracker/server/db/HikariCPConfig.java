package com.pricetracker.server.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * HikariCPConfig - Cấu hình HikariCP Connection Pool
 * HikariCP là connection pool nhanh nhất và được sử dụng rộng rãi nhất
 * 
 * Features:
 * - Auto connection recovery
 * - Connection leak detection
 * - Performance monitoring
 * - Configurable pool size
 */
public class HikariCPConfig {
    private static HikariDataSource dataSource;

    /**
     * Khởi tạo HikariCP với cấu hình tối ưu
     */
    public static void initialize() {
        if (dataSource != null && !dataSource.isClosed()) {
            return; // Đã khởi tạo rồi
        }

        try {
            HikariConfig config = new HikariConfig();
            
            // === Database Connection Settings ===
            config.setJdbcUrl(getProperty("db.url", "jdbc:mysql://localhost:3306/price_insight"));
            config.setUsername(getProperty("db.user", "root"));
            config.setPassword(getProperty("db.password", ""));
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");

            // === Pool Size Configuration ===
            // Maximum connections trong pool
            // ⚡ Tăng từ 10 → 30 để support 50 concurrent users
            // Formula: (50 users / 2) + buffer = ~30 connections
            config.setMaximumPoolSize(Integer.parseInt(getProperty("db.pool.maxSize", "30")));
            
            // Minimum idle connections (connections sẵn sàng chờ)
            // ⚡ Tăng từ 5 → 15 để có đủ connections sẵn sàng
            config.setMinimumIdle(Integer.parseInt(getProperty("db.pool.minIdle", "15")));

            // === Connection Timeout Settings ===
            // Thời gian chờ để lấy connection từ pool (milliseconds)
            config.setConnectionTimeout(30000); // 30 seconds
            
            // Thời gian tối đa một connection có thể idle (milliseconds)
            config.setIdleTimeout(600000); // 10 minutes
            
            // Thời gian tối đa một connection tồn tại (milliseconds)
            config.setMaxLifetime(1800000); // 30 minutes

            // === Performance Tuning ===
            // Connection test query
            config.setConnectionTestQuery("SELECT 1");
            
            // Tên pool để dễ identify trong logs
            config.setPoolName("PriceTrackerPool");
            
            // Auto-commit (recommended: true)
            config.setAutoCommit(true);

            // === Leak Detection ===
            // Cảnh báo khi connection bị leak (không trả lại pool)
            config.setLeakDetectionThreshold(60000); // 60 seconds

            // === MySQL Specific Optimizations ===
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");
            config.addDataSourceProperty("useLocalSessionState", "true");
            config.addDataSourceProperty("rewriteBatchedStatements", "true");
            config.addDataSourceProperty("cacheResultSetMetadata", "true");
            config.addDataSourceProperty("cacheServerConfiguration", "true");
            config.addDataSourceProperty("elideSetAutoCommits", "true");
            config.addDataSourceProperty("maintainTimeStats", "false");

            dataSource = new HikariDataSource(config);
            System.out.println("✓ HikariCP Connection Pool initialized successfully");
            System.out.println("  ├─ Pool Name: " + config.getPoolName());
            System.out.println("  ├─ Max Pool Size: " + config.getMaximumPoolSize());
            System.out.println("  ├─ Min Idle: " + config.getMinimumIdle());
            System.out.println("  └─ Database: " + config.getJdbcUrl());
            
        } catch (Exception e) {
            System.err.println("✗ Failed to initialize HikariCP: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Cannot initialize database connection pool", e);
        }
    }

    /**
     * Lấy DataSource để get connections
     */
    public static HikariDataSource getDataSource() {
        if (dataSource == null || dataSource.isClosed()) {
            initialize();
        }
        return dataSource;
    }

    /**
     * Đóng connection pool (gọi khi shutdown server)
     */
    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("✓ HikariCP Connection Pool closed");
        }
    }

    /**
     * Lấy thông tin monitoring của pool
     */
    public static void printPoolStats() {
        if (dataSource != null && !dataSource.isClosed()) {
            System.out.println("\n=== HikariCP Pool Statistics ===");
            System.out.println("Active Connections: " + dataSource.getHikariPoolMXBean().getActiveConnections());
            System.out.println("Idle Connections: " + dataSource.getHikariPoolMXBean().getIdleConnections());
            System.out.println("Total Connections: " + dataSource.getHikariPoolMXBean().getTotalConnections());
            System.out.println("Threads Awaiting: " + dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection());
            System.out.println("================================\n");
        }
    }

    /**
     * Helper method để đọc config từ file hoặc dùng default
     */
    private static String getProperty(String key, String defaultValue) {
        // Ưu tiên đọc từ system properties
        String value = System.getProperty(key);
        if (value != null && !value.isEmpty()) {
            return value;
        }

        // Fallback: đọc từ config.ini
        try {
            Properties props = new Properties();
            props.load(new FileInputStream("config.ini"));
            value = props.getProperty(key);
            if (value != null && !value.isEmpty()) {
                return value;
            }
        } catch (IOException e) {
            // Không có config file, dùng default
        }

        return defaultValue;
    }

    /**
     * Test connection pool
     */
    public static boolean testConnection() {
        try {
            java.sql.Connection conn = getDataSource().getConnection();
            boolean isValid = conn.isValid(5); // timeout 5 seconds
            conn.close(); // Trả lại pool
            return isValid;
        } catch (Exception e) {
            System.err.println("✗ Connection test failed: " + e.getMessage());
            return false;
        }
    }
}
