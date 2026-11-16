package com.fixflow.service;

import com.fixflow.database.RepairDB;
import com.fixflow.database.TechnicianDB;
import com.fixflow.enums.RepairStatus;
import com.fixflow.exception.InvalidStatusTransitionException;
import com.fixflow.model.RepairJob;
import com.fixflow.model.Technician;

import java.sql.SQLException;
import java.util.List;


public class RepairService {

    private RepairService() {
        // Business service
    }


    public static int createRepairJob(RepairJob job) throws SQLException {
        return RepairDB.createRepairJob(job);
    }


    public static double calculateTotalCost(int jobId) throws SQLException {
        return PaymentService.calculateTotalCost(jobId);
    }
    public static void updateStatus(int jobId, RepairStatus targetStatus, String notes)
            throws SQLException, InvalidStatusTransitionException {

        RepairJob job = RepairDB.getRepairJobById(jobId);
        if (job == null) {
            throw new IllegalArgumentException("Repair Job #" + jobId + " not found.");
        }

        // Validate lifecycle transition rules in model
        job.validateTransition(targetStatus);

        // Guard: A device cannot be marked COLLECTED if there is an outstanding balance
        if (targetStatus == RepairStatus.COLLECTED) {
            if (!PaymentService.isJobFullyPaid(jobId)) {
                double balance = PaymentService.getRemainingBalance(jobId);
                throw new IllegalStateException(String.format(
                        "Cannot collect device: Repair Job #%d has an outstanding balance of LKR %.2f.",
                        jobId, balance));
            }
        }

        // Guard: Cancellation requires a reason
        if (targetStatus == RepairStatus.CANCELLED) {
            if (notes == null || notes.trim().isEmpty()) {
                throw new IllegalArgumentException("A cancellation reason must be provided when cancelling a repair job.");
            }
        }

        // Apply update in DB
        RepairDB.updateStatus(jobId, targetStatus, notes);
    }

    public static boolean assignTechnician(int jobId, int technicianId) throws SQLException {
        Technician tech = TechnicianDB.getTechnicianById(technicianId);
        if (tech == null) {
            throw new IllegalArgumentException("Technician #" + technicianId + " not found.");
        }
        if (!tech.isActive()) {
            throw new IllegalArgumentException("Cannot assign technician '" + tech.getName() + "': Technician is marked as Inactive.");
        }
        return RepairDB.assignTechnician(jobId, technicianId);
    }
    public static void recordDiagnosis(int jobId, String findings, Double estimatedCost, double labourCharges,
                                       String estCompletionDate) throws SQLException {
        RepairDB.recordDiagnosis(jobId, findings, estimatedCost, labourCharges, estCompletionDate);
    }

    public static RepairJob getRepairJobById(int jobId) throws SQLException {
        return RepairDB.getRepairJobById(jobId);
    }


    public static List<RepairJob> getAllRepairJobs() throws SQLException {
        return RepairDB.getAllRepairJobs();
    }


    public static List<RepairJob> getRepairJobsForTechnician(int technicianId) throws SQLException {
        return RepairDB.getRepairJobsForTechnician(technicianId);
    }


    public static List<RepairJob> searchRepairJobs(String query, RepairStatus statusFilter, Integer technicianIdFilter)
            throws SQLException {
        return RepairDB.searchRepairJobs(query, statusFilter, technicianIdFilter);
    }

    public static boolean deleteRepairJob(int jobId) throws SQLException {
        return RepairDB.deleteRepairJob(jobId);
    }
}
