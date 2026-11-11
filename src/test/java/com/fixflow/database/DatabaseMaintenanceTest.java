package com.fixflow.database;

import com.fixflow.enums.UserRole;
import com.fixflow.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseMaintenanceTest {

    private File tempDbFile;
    private File tempBackupFile;

    @BeforeEach
    void setUp() throws Exception {
        tempDbFile = File.createTempFile("fixflow_maint_test_", ".db");
        tempBackupFile = File.createTempFile("fixflow_maint_backup_", ".db");
        DatabaseManager.setCustomJdbcUrl("jdbc:sqlite:" + tempDbFile.getAbsolutePath());
        DatabaseManager.initializeDatabase();

        Map<UserRole, UserDB.RoleCredential> creds = new EnumMap<>(UserRole.class);
        creds.put(UserRole.ADMINISTRATOR, new UserDB.RoleCredential("admin", "admin123"));
        creds.put(UserRole.MANAGER, new UserDB.RoleCredential("manager", "manager123"));
        creds.put(UserRole.RECEPTIONIST, new UserDB.RoleCredential("receptionist", "recep123"));
        creds.put(UserRole.TECHNICIAN, new UserDB.RoleCredential("technician", "tech123"));
        creds.put(UserRole.INVENTORY_STAFF, new UserDB.RoleCredential("inventory", "inv123"));
        UserDB.setupAllRoles(creds);
    }

    @AfterEach
    void tearDown() {
        DatabaseManager.setCustomJdbcUrl(null);
        if (tempDbFile != null && tempDbFile.exists()) tempDbFile.delete();
        if (tempBackupFile != null && tempBackupFile.exists()) tempBackupFile.delete();
    }

    @Test
    void testUpdateSharedRoleCredentials() throws Exception {
        User userBefore = UserDB.authenticate("manager", "manager123");
        assertNotNull(userBefore);
        assertEquals(UserRole.MANAGER, userBefore.getRole());

        boolean updated = UserDB.updateCredentials(UserRole.MANAGER, "head_manager", "newSecretPass2026");
        assertTrue(updated);

        assertNull(UserDB.authenticate("manager", "manager123"));
        assertNull(UserDB.authenticate("head_manager", "manager123"));

        User userAfter = UserDB.authenticate("head_manager", "newSecretPass2026");
        assertNotNull(userAfter);
        assertEquals("head_manager", userAfter.getUsername());
        assertEquals(UserRole.MANAGER, userAfter.getRole());

        assertThrows(IllegalArgumentException.class, () ->
                UserDB.updateCredentials(UserRole.MANAGER, "head_manager", "123")
        );
    }

    @Test
    void testDatabaseBackupAndValidation() throws Exception {
        DatabaseManager.backupDatabase(tempBackupFile);

        assertTrue(tempBackupFile.exists(), "Backup file must be created on disk");
        assertTrue(tempBackupFile.length() > 500, "Backup file must contain database data");

        boolean isValid = DatabaseManager.validateDatabaseFile(tempBackupFile);
        assertTrue(isValid, "Backup file must validate successfully with all core tables present");
    }

    @Test
    void testValidateCorruptedOrInvalidDatabaseFile() throws Exception {
        File dummyFile = File.createTempFile("dummy_corrupt_", ".db");
        dummyFile.deleteOnExit();

        try (FileWriter fw = new FileWriter(dummyFile)) {
            fw.write("This is not a SQLite database file.");
        }

        boolean isValid = DatabaseManager.validateDatabaseFile(dummyFile);
        assertFalse(isValid, "Arbitrary text file must fail validation");

        assertFalse(DatabaseManager.validateDatabaseFile(new File("non_existent_file.db")));
    }
}

