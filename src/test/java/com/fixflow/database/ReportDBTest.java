package com.fixflow.database;

import com.fixflow.enums.RepairStatus;
import com.fixflow.model.*;
import com.fixflow.service.PaymentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ReportDBTest {

    private File tempDbFile;

    @BeforeEach
    void setUp() throws Exception {
        tempDbFile = File.createTempFile("fixflow_rep_test_", ".db");
        DatabaseConnection.setCustomJdbcUrl("jdbc:sqlite:" + tempDbFile.getAbsolutePath());
        DatabaseInitializer.initializeDatabase();
    }

    @AfterEach
    void tearDown() {
        DatabaseConnection.setCustomJdbcUrl(null);
        if (tempDbFile != null && tempDbFile.exists()) {
            tempDbFile.delete();
        }
    }

    private int setupTestScenario() throws Exception {

        Customer c1 = new Customer("Peter Parker", "0770011223", "peter@dailybugle.com", "Queens, NY");
        int c1Id = CustomerDB.createCustomer(c1);
        Device d1 = new Device(c1Id, "Phone", "Sony", "Xperia 1", "SN-SONY-01", "Black", "Charger");
        int d1Id = DeviceDB.createDevice(d1);


        RepairJob job1 = new RepairJob(c1Id, d1Id, "Broken camera lens");
        job1.setLabourCharges(4000.0);
        int j1Id = RepairDB.createRepairJob(job1);

        SparePart p1 = new SparePart("Sony Camera Module", "Cameras", "Xperia 1", 2500.0, 5000.0, 4, 2);
        int p1Id = InventoryDB.createPart(p1);
        InventoryDB.issuePartToJob(j1Id, p1Id, 1, "Technician"); // Stock becomes 3

        RepairDB.updateStatus(j1Id, RepairStatus.DIAGNOSING, "Diagnosing camera");
        RepairDB.updateStatus(j1Id, RepairStatus.IN_PROGRESS, "Replacing module");
        RepairDB.updateStatus(j1Id, RepairStatus.COMPLETED, "Completed repair");


        PaymentService.recordPayment(j1Id, 9000.0, "2026-11-10 12:00:00", "Settled full");
        RepairDB.updateStatus(j1Id, RepairStatus.COLLECTED, "Collected by customer");


        Device d2 = new Device(c1Id, "Laptop", "Dell", "Latitude", "SN-DELL-02", "Silver", "Charger");
        int d2Id = DeviceDB.createDevice(d2);
        RepairJob job2 = new RepairJob(c1Id, d2Id, "Trackpad unresponsive");
        job2.setLabourCharges(3000.0);
        int j2Id = RepairDB.createRepairJob(job2);
        RepairDB.updateStatus(j2Id, RepairStatus.DIAGNOSING, "Diagnosing trackpad");
        RepairDB.updateStatus(j2Id, RepairStatus.IN_PROGRESS, "Awaiting supplier part");


        SparePart p2 = new SparePart("Dell Trackpad", "Trackpads", "Latitude", 1500.0, 3000.0, 1, 5);
        InventoryDB.createPart(p2);


        SparePart p3 = new SparePart("Universal USB-C Cable", "Cables", "Universal", 200.0, 800.0, 0, 3);
        InventoryDB.createPart(p3);

        return j1Id;
    }

    @Test
    void testDashboardMetricsAggregation() throws Exception {
        setupTestScenario();

        DashboardMetrics metrics = ReportDB.getDashboardMetrics(null);
        assertNotNull(metrics);


        assertEquals(2, metrics.getTotalJobs());

        assertEquals(1, metrics.getActiveJobs());

        assertEquals(1, metrics.getCollectedJobs());
        assertEquals(1, metrics.getStatusCount(RepairStatus.COLLECTED));
        assertEquals(1, metrics.getStatusCount(RepairStatus.IN_PROGRESS));

        assertEquals(9000.0, metrics.getTotalRevenue(), 0.001);


        assertEquals(2, metrics.getLowStockCount(), "p2 (qty 1) and p3 (qty 0) are <= min stock level");
        assertEquals(1, metrics.getOutOfStockCount());


        assertEquals(9000.0, metrics.getTotalInventoryValuation(), 0.001);
    }

    @Test
    void testRepairJobsReportFiltering() throws Exception {
        setupTestScenario();


        List<RepairJob> all = ReportDB.getRepairJobsReport(null, null, null, null);
        assertEquals(2, all.size());


        List<RepairJob> inProgress = ReportDB.getRepairJobsReport(null, null, RepairStatus.IN_PROGRESS, null);
        assertEquals(1, inProgress.size());
        assertEquals(RepairStatus.IN_PROGRESS, inProgress.get(0).getCurrentStatus());


        String today = LocalDate.now().toString();
        List<RepairJob> todayJobs = ReportDB.getRepairJobsReport(today, today, null, null);
        assertEquals(2, todayJobs.size());
    }

    @Test
    void testRevenueReport() throws Exception {
        setupTestScenario();

        List<Payment> payments = ReportDB.getRevenueReport(null, null);
        assertEquals(1, payments.size());

        Payment p = payments.get(0);
        assertEquals(9000.0, p.getAmountPaid(), 0.001);
        assertEquals(4000.0, p.getLabourCharges(), 0.001);
        assertEquals("Peter Parker", p.getCustomerName());
    }

    @Test
    void testInventoryReportFiltering() throws Exception {
        setupTestScenario();


        List<SparePart> allParts = ReportDB.getInventoryReport(null, "ALL");
        assertEquals(3, allParts.size());

        List<SparePart> outOfStock = ReportDB.getInventoryReport(null, "OUT_OF_STOCK");
        assertEquals(1, outOfStock.size());
        assertEquals("Universal USB-C Cable", outOfStock.get(0).getPartName());


        List<SparePart> inStock = ReportDB.getInventoryReport(null, "IN_STOCK");
        assertEquals(1, inStock.size());
        assertEquals("Sony Camera Module", inStock.get(0).getPartName());
    }
}
