package com.fixflow.database;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseInitializerTest {

    private File tempDbFile;

    @BeforeEach
    void setUp() throws Exception {
        tempDbFile = File.createTempFile("fixflow_init_test_", ".db");
        DatabaseConnection.setCustomJdbcUrl("jdbc:sqlite:" + tempDbFile.getAbsolutePath());
    }

    @AfterEach
    void tearDown() {
        DatabaseConnection.setCustomJdbcUrl(null);
        if (tempDbFile != null && tempDbFile.exists()) {
            tempDbFile.delete();
        }
    }

    @Test
    void testFreshSchemaInitialization() throws Exception {
        DatabaseInitializer.initializeDatabase();

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            try (ResultSet rs = stmt.executeQuery("PRAGMA foreign_keys;")) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1), "PRAGMA foreign_keys must be enabled (1)");
            }

            Set<String> tables = new HashSet<>();
            try (ResultSet rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table';")) {
                while (rs.next()) {
                    tables.add(rs.getString(1).toLowerCase());
                }
            }

            assertTrue(tables.contains("customers"), "customers table must exist");
            assertTrue(tables.contains("devices"), "devices table must exist");
            assertTrue(tables.contains("technicians"), "technicians table must exist");
            assertTrue(tables.contains("repair_jobs"), "repair_jobs table must exist");
            assertTrue(tables.contains("spare_parts"), "spare_parts table must exist");
            assertTrue(tables.contains("repair_part_usage"), "repair_part_usage table must exist");
            assertTrue(tables.contains("payments"), "payments table must exist");
            assertTrue(tables.contains("users"), "users table must exist");

            assertFalse(tables.contains("warranties"), "warranties table must not exist");
            assertFalse(tables.contains("repair_status_history"), "repair_status_history table must not exist");
        }
    }

    @Test
    void testMigrationRemovesUniqueJobIdOnPayments() throws Exception {
        DatabaseInitializer.initializeDatabase();

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE payments;");
            stmt.execute("CREATE TABLE payments (" +
                    "payment_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "job_id INTEGER NOT NULL UNIQUE, " +
                    "payment_date TEXT NOT NULL, " +
                    "amount_paid REAL NOT NULL CHECK (amount_paid >= 0), " +
                    "payment_method TEXT NOT NULL DEFAULT 'CASH', " +
                    "payment_status TEXT NOT NULL DEFAULT 'PAID', " +
                    "created_at TEXT NOT NULL, " +
                    "FOREIGN KEY (job_id) REFERENCES repair_jobs(job_id) ON DELETE RESTRICT" +
                    ");");

            stmt.execute("INSERT INTO customers (customer_id, full_name, phone, created_at) VALUES (1, 'Test', '077', datetime('now'));");
            stmt.execute("INSERT INTO devices (device_id, customer_id, category, brand, model, created_at) VALUES (1, 1, 'Phone', 'Brand', 'Model', datetime('now'));");
            stmt.execute("INSERT INTO repair_jobs (job_id, customer_id, device_id, date_received, initial_problem, current_status, created_at, updated_at) " +
                    "VALUES (1, 1, 1, datetime('now'), 'Broken', 'RECEIVED', datetime('now'), datetime('now'));");

            stmt.execute("INSERT INTO payments (job_id, payment_date, amount_paid, payment_method, payment_status, created_at) " +
                    "VALUES (1, '2026-01-01 10:00:00', 500.0, 'CASH', 'PAID', '2026-01-01 10:00:00');");
        }

        DatabaseInitializer.initializeDatabase();

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO payments (job_id, payment_date, amount_paid, payment_method, notes, created_at) " +
                    "VALUES (1, '2026-01-02 10:00:00', 250.0, 'CASH', 'Partial payment 2', '2026-01-02 10:00:00');");

            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM payments WHERE job_id = 1;")) {
                assertTrue(rs.next());
                assertEquals(2, rs.getInt(1), "Should allow multiple payments for the same job_id");
            }
        }
    }

    @Test
    void testFailedWarrantyMigrationDoesNotDropWarrantiesTable() throws Exception {
        DatabaseInitializer.initializeDatabase();

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS warranties (" +
                    "warranty_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "job_id INTEGER NOT NULL, " +
                    "start_date TEXT NOT NULL, " +
                    "expiry_date TEXT NOT NULL, " +
                    "status TEXT NOT NULL DEFAULT 'ACTIVE', " +
                    "created_at TEXT NOT NULL" +
                    ");");
            stmt.execute("INSERT INTO customers (customer_id, full_name, phone, created_at) VALUES (1, 'C1', '077', datetime('now'));");
            stmt.execute("INSERT INTO devices (device_id, customer_id, category, brand, model, created_at) VALUES (1, 1, 'Laptop', 'B', 'M', datetime('now'));");
            stmt.execute("INSERT INTO repair_jobs (job_id, customer_id, device_id, date_received, initial_problem, current_status, created_at, updated_at) " +
                    "VALUES (1, 1, 1, datetime('now'), 'Broken', 'RECEIVED', datetime('now'), datetime('now'));");
            stmt.execute("INSERT INTO warranties (job_id, start_date, expiry_date, status, created_at) " +
                    "VALUES (1, '2026-01-01', '2027-01-01', 'ACTIVE', datetime('now'));");

            stmt.execute("CREATE TRIGGER fail_warranty_copy BEFORE UPDATE OF warranty_start_date ON devices " +
                    "BEGIN SELECT RAISE(ABORT, 'Simulated copy failure'); END;");
        }

        assertThrows(SQLException.class, () -> DatabaseInitializer.initializeDatabase());

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            try (ResultSet rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='warranties';")) {
                assertTrue(rs.next(), "warranties table must NOT be dropped when copying data fails");
            }
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM warranties;")) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1), "warranties record must remain intact");
            }
        }
    }

    @Test
    void testSuccessfulWarrantyMigrationTransfersDataToDevice() throws Exception {
        DatabaseInitializer.initializeDatabase();

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS warranties (" +
                    "warranty_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "job_id INTEGER NOT NULL, " +
                    "start_date TEXT NOT NULL, " +
                    "expiry_date TEXT NOT NULL, " +
                    "status TEXT NOT NULL DEFAULT 'ACTIVE', " +
                    "created_at TEXT NOT NULL" +
                    ");");
            stmt.execute("INSERT INTO customers (customer_id, full_name, phone, created_at) VALUES (1, 'C1', '077', datetime('now'));");
            stmt.execute("INSERT INTO devices (device_id, customer_id, category, brand, model, created_at) VALUES (1, 1, 'Laptop', 'B', 'M', datetime('now'));");
            stmt.execute("INSERT INTO repair_jobs (job_id, customer_id, device_id, date_received, initial_problem, current_status, created_at, updated_at) " +
                    "VALUES (1, 1, 1, datetime('now'), 'Broken', 'RECEIVED', datetime('now'), datetime('now'));");
            stmt.execute("INSERT INTO warranties (job_id, start_date, expiry_date, status, created_at) " +
                    "VALUES (1, '2026-01-15', '2027-01-15', 'ACTIVE', datetime('now'));");
        }

        DatabaseInitializer.initializeDatabase();

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            try (ResultSet rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='warranties';")) {
                assertFalse(rs.next(), "warranties table must be dropped after successful copy");
            }
            try (ResultSet rs = stmt.executeQuery("SELECT warranty_start_date, warranty_expiry_date FROM devices WHERE device_id = 1;")) {
                assertTrue(rs.next());
                assertEquals("2026-01-15", rs.getString("warranty_start_date"));
                assertEquals("2027-01-15", rs.getString("warranty_expiry_date"));
            }
        }
    }

    @Test
    void testLegacyRepairJobsCheckConstraintMigration() throws Exception {
        DatabaseInitializer.initializeDatabase();

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE payments;");
            stmt.execute("DROP TABLE repair_part_usage;");
            stmt.execute("DROP TABLE repair_jobs;");

            stmt.execute("CREATE TABLE repair_jobs (" +
                    "job_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "customer_id INTEGER NOT NULL, " +
                    "device_id INTEGER NOT NULL, " +
                    "technician_id INTEGER, " +
                    "date_received TEXT NOT NULL, " +
                    "initial_problem TEXT NOT NULL, " +
                    "current_status TEXT NOT NULL CHECK (current_status IN ('RECEIVED', 'DIAGNOSING', 'WAITING_FOR_APPROVAL', 'WAITING_FOR_PARTS', 'REPAIRING', 'READY_FOR_COLLECTION', 'COLLECTED', 'CANCELLED')), " +
                    "diagnosis_findings TEXT, " +
                    "estimated_cost REAL, " +
                    "labour_charges REAL DEFAULT 0.0, " +
                    "estimated_completion_date TEXT, " +
                    "actual_completion_date TEXT, " +
                    "created_at TEXT NOT NULL, " +
                    "updated_at TEXT NOT NULL, " +
                    "FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE RESTRICT, " +
                    "FOREIGN KEY (device_id) REFERENCES devices(device_id) ON DELETE RESTRICT" +
                    ");");

            stmt.execute("INSERT INTO customers (customer_id, full_name, phone, created_at) VALUES (1, 'Cust', '077', datetime('now'));");
            stmt.execute("INSERT INTO devices (device_id, customer_id, category, brand, model, created_at) VALUES (1, 1, 'PC', 'Dell', 'Optiplex', datetime('now'));");

            stmt.execute("INSERT INTO repair_jobs (job_id, customer_id, device_id, date_received, initial_problem, current_status, created_at, updated_at) " +
                    "VALUES (1, 1, 1, datetime('now'), 'Prob 1', 'WAITING_FOR_PARTS', datetime('now'), datetime('now'));");
            stmt.execute("INSERT INTO repair_jobs (job_id, customer_id, device_id, date_received, initial_problem, current_status, created_at, updated_at) " +
                    "VALUES (2, 1, 1, datetime('now'), 'Prob 2', 'READY_FOR_COLLECTION', datetime('now'), datetime('now'));");
            stmt.execute("INSERT INTO repair_jobs (job_id, customer_id, device_id, date_received, initial_problem, current_status, created_at, updated_at) " +
                    "VALUES (3, 1, 1, datetime('now'), 'Prob 3', 'REPAIRING', datetime('now'), datetime('now'));");
        }

        DatabaseInitializer.initializeDatabase();

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            try (ResultSet rs = stmt.executeQuery("SELECT current_status FROM repair_jobs WHERE job_id = 1;")) {
                assertTrue(rs.next());
                assertEquals("IN_PROGRESS", rs.getString(1), "WAITING_FOR_PARTS should be mapped to IN_PROGRESS");
            }
            try (ResultSet rs = stmt.executeQuery("SELECT current_status FROM repair_jobs WHERE job_id = 2;")) {
                assertTrue(rs.next());
                assertEquals("COMPLETED", rs.getString(1), "READY_FOR_COLLECTION should be mapped to COMPLETED");
            }
            try (ResultSet rs = stmt.executeQuery("SELECT current_status FROM repair_jobs WHERE job_id = 3;")) {
                assertTrue(rs.next());
                assertEquals("IN_PROGRESS", rs.getString(1), "REPAIRING should be mapped to IN_PROGRESS");
            }

            assertDoesNotThrow(() -> {
                stmt.execute("INSERT INTO repair_jobs (job_id, customer_id, device_id, date_received, initial_problem, current_status, created_at, updated_at) " +
                        "VALUES (4, 1, 1, datetime('now'), 'New prob', 'IN_PROGRESS', datetime('now'), datetime('now'));");
            });

            try (ResultSet rs = stmt.executeQuery("SELECT notes FROM repair_jobs WHERE job_id = 1;")) {
                assertTrue(rs.next());
            }
        }
    }
}
