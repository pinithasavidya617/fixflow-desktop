package com.fixflow.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;


public class DatabaseConnection {

    private static final Logger LOGGER = Logger.getLogger(DatabaseConnection.class.getName());
    private static final String APP_DIR_NAME = "FixFlow";
    private static final String DB_FILE_NAME = "fixflow.db";

    private static String customJdbcUrl = null;

    private DatabaseConnection() {
    }


    public static synchronized String getJdbcUrl() {
        if (customJdbcUrl != null && !customJdbcUrl.trim().isEmpty()) {
            return customJdbcUrl;
        }

        String os = System.getProperty("os.name", "").toLowerCase();
        File baseDir;

        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            if (appData != null && !appData.trim().isEmpty()) {
                baseDir = new File(appData, APP_DIR_NAME);
            } else {
                baseDir = new File(System.getProperty("user.home"), "AppData/Roaming/" + APP_DIR_NAME);
            }
        } else if (os.contains("mac")) {
            baseDir = new File(System.getProperty("user.home"), "Library/Application Support/" + APP_DIR_NAME);
        } else {
            baseDir = new File(System.getProperty("user.home"), ".fixflow");
        }

        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }

        File dbFile = new File(baseDir, DB_FILE_NAME);
        return "jdbc:sqlite:" + dbFile.getAbsolutePath();
    }


    public static synchronized void setCustomJdbcUrl(String url) {
        customJdbcUrl = url;
    }

    public static Connection getConnection() throws SQLException {
        String url = getJdbcUrl();
        Connection conn = DriverManager.getConnection(url);

        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
            if (!url.contains(":memory:")) {
                stmt.execute("PRAGMA journal_mode = WAL;");
            }
        }

        return conn;
    }
}
