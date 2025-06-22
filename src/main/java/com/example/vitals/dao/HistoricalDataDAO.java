package com.example.vitals.dao;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class HistoricalDataDAO {
    private static final String DB_URL;
    private static final String CREATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS HistoricalMetrics (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                timestamp TEXT NOT NULL,
                cpuUsage REAL,
                memoryUsed REAL,
                memoryTotal REAL,
                memoryAvailable REAL
            );
            """;

    // Static block to initialize DB_URL with proper path
    static {
        DB_URL = "jdbc:sqlite:" + getDatabasePath();
    }

    public HistoricalDataDAO() {
        initializeDatabase();
    }

    /**
     * Get the proper database path in user's local app data directory
     */
    private static String getDatabasePath() {
        try {
            // Get user's local app data directory
            String localAppData = System.getenv("LOCALAPPDATA");
            if (localAppData == null) {
                // Fallback for older Windows versions
                localAppData = System.getProperty("user.home") + "\\AppData\\Local";
            }

            // Create Vitals app directory
            Path appDir = Paths.get(localAppData, "Vitals");
            Files.createDirectories(appDir);

            // Return full database file path
            String dbPath = appDir.resolve("vitals.db").toString();
            System.out.println("Database path: " + dbPath); // For debugging
            return dbPath;

        } catch (Exception e) {
            System.err.println("Error creating app data directory: " + e.getMessage());
            // Fallback to user home directory
            String fallbackPath = System.getProperty("user.home") + "\\vitals.db";
            System.out.println("Using fallback database path: " + fallbackPath);
            return fallbackPath;
        }
    }

    private void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(CREATE_TABLE_SQL);
            System.out.println("Database initialized successfully at: " + DB_URL);
        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void logMetrics(double cpuUsage, double memoryUsed, double memoryTotal, double memoryAvailable) {
        String insertSQL = "INSERT INTO HistoricalMetrics (timestamp, cpuUsage, memoryUsed, memoryTotal, memoryAvailable) VALUES (?, ?, ?, ?, ?);";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            pstmt.setString(1, timestamp);
            pstmt.setDouble(2, cpuUsage);
            pstmt.setDouble(3, memoryUsed);
            pstmt.setDouble(4, memoryTotal);
            pstmt.setDouble(5, memoryAvailable);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error logging metrics: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public List<MetricRecord> getMetricsForPeriod(String period) {
        List<MetricRecord> records = new ArrayList<>();
        String querySQL = "SELECT * FROM HistoricalMetrics WHERE timestamp >= datetime('now', ?) ORDER BY timestamp ASC;";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(querySQL)) {

            pstmt.setString(1, period);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String timestamp = rs.getString("timestamp");
                    double cpuUsage = rs.getDouble("cpuUsage");
                    double memoryUsed = rs.getDouble("memoryUsed");
                    double memoryTotal = rs.getDouble("memoryTotal");
                    double memoryAvailable = rs.getDouble("memoryAvailable");
                    records.add(new MetricRecord(timestamp, cpuUsage, memoryUsed, memoryTotal, memoryAvailable));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving metrics: " + e.getMessage());
            e.printStackTrace();
        }
        return records;
    }

    /**
     * Get the current database file path (useful for debugging)
     */
    public static String getCurrentDatabasePath() {
        return DB_URL;
    }

    // A simple record to encapsulate a metric record from the DB
    public static record MetricRecord(String timestamp, double cpuUsage, double memoryUsed, double memoryTotal, double memoryAvailable) {
    }
}