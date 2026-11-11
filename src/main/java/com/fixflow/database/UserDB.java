package com.fixflow.database;

import com.fixflow.enums.UserRole;
import com.fixflow.model.User;
import com.fixflow.util.PasswordUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserDB {

    private static final Logger LOGGER = Logger.getLogger(UserDB.class.getName());
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


    public record RoleCredential(String username, String plainPassword) {}


    public static boolean isSetupRequired() throws SQLException {
        String sql = "SELECT COUNT(DISTINCT role) FROM users";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                int count = rs.getInt(1);
                return count < UserRole.values().length;
            }
        }
        return true;
    }


    public static User authenticate(String username, String plainPassword) throws SQLException {
        if (username == null || username.trim().isEmpty() || plainPassword == null || plainPassword.isEmpty()) {
            return null;
        }

        String sql = "SELECT user_id, role, username, password_hash, created_at, updated_at " +
                     "FROM users WHERE LOWER(username) = LOWER(?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    if (PasswordUtil.verifyPassword(plainPassword, storedHash)) {
                        return mapResultSetToUser(rs);
                    }
                }
            }
        }
        return null;
    }


    public static void setupAllRoles(Map<UserRole, RoleCredential> credentials) throws SQLException {
        String now = LocalDateTime.now().format(DATE_FORMATTER);
        String sql = "INSERT INTO users (role, username, password_hash, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?) " +
                     "ON CONFLICT(role) DO UPDATE SET " +
                     "username = excluded.username, " +
                     "password_hash = excluded.password_hash, " +
                     "updated_at = excluded.updated_at";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (UserRole role : UserRole.values()) {
                    RoleCredential cred = credentials.get(role);
                    if (cred == null || cred.username() == null || cred.plainPassword() == null) {
                        throw new IllegalArgumentException("Missing credentials for role: " + role.name());
                    }

                    String passwordHash = PasswordUtil.hashPassword(cred.plainPassword());
                    ps.setString(1, role.name());
                    ps.setString(2, cred.username().trim());
                    ps.setString(3, passwordHash);
                    ps.setString(4, now);
                    ps.setString(5, now);
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit();
                LOGGER.info("All 5 shared roles successfully configured.");
            } catch (Exception e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Failed to configure role credentials, rolled back.", e);
                throw new SQLException("Failed to complete initial setup: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }


    public static boolean updatePassword(UserRole role, String newPlainPassword) throws SQLException {
        return updateCredentials(role, null, newPlainPassword);
    }

    public static boolean updateCredentials(UserRole role, String newUsername, String newPlainPassword) throws SQLException {
        if (newPlainPassword == null || newPlainPassword.trim().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long.");
        }

        String newHash = PasswordUtil.hashPassword(newPlainPassword);
        String now = LocalDateTime.now().format(DATE_FORMATTER);

        String sql;
        if (newUsername != null && !newUsername.trim().isEmpty()) {
            sql = "UPDATE users SET username = ?, password_hash = ?, updated_at = ? WHERE role = ?";
        } else {
            sql = "UPDATE users SET password_hash = ?, updated_at = ? WHERE role = ?";
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (newUsername != null && !newUsername.trim().isEmpty()) {
                ps.setString(1, newUsername.trim());
                ps.setString(2, newHash);
                ps.setString(3, now);
                ps.setString(4, role.name());
            } else {
                ps.setString(1, newHash);
                ps.setString(2, now);
                ps.setString(3, role.name());
            }
            return ps.executeUpdate() > 0;
        }
    }


    public static User getUserByRole(UserRole role) throws SQLException {
        String sql = "SELECT user_id, role, username, password_hash, created_at, updated_at " +
                     "FROM users WHERE role = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        }
        return null;
    }


    public static List<User> getAllUsers() throws SQLException {
        List<User> list = new ArrayList<>();
        String sql = "SELECT user_id, role, username, password_hash, created_at, updated_at FROM users ORDER BY user_id";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToUser(rs));
            }
        }
        return list;
    }

    private static User mapResultSetToUser(ResultSet rs) throws SQLException {
        int id = rs.getInt("user_id");
        String roleStr = rs.getString("role");
        UserRole role = UserRole.valueOf(roleStr);
        String username = rs.getString("username");
        String passwordHash = rs.getString("password_hash");
        String createdAt = rs.getString("created_at");
        String updatedAt = rs.getString("updated_at");
        return new User(id, role, username, passwordHash, createdAt, updatedAt);
    }
}

