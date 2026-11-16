package com.fixflow.service;

import com.fixflow.database.InventoryDB;
import com.fixflow.database.PaymentDB;
import com.fixflow.database.RepairDB;
import com.fixflow.enums.PaymentStatus;
import com.fixflow.model.Payment;
import com.fixflow.model.RepairJob;

import java.sql.SQLException;
import java.util.List;


public class PaymentService {

    private PaymentService() {

    }


    public static double calculateTotalCost(int jobId) throws SQLException {
        RepairJob job = RepairDB.getRepairJobById(jobId);
        if (job == null) {
            throw new SQLException("Repair Job #" + jobId + " not found.");
        }
        double labour = job.getLabourCharges();
        double parts = InventoryDB.getTotalPartsCostForJob(jobId);
        return labour + parts;
    }

    public static double getTotalPaid(int jobId) throws SQLException {
        return PaymentDB.getTotalPaidForJob(jobId);
    }

    public static double getRemainingBalance(int jobId) throws SQLException {
        double total = calculateTotalCost(jobId);
        double paid = getTotalPaid(jobId);
        double balance = total - paid;
        return balance > 0.001 ? balance : 0.0;
    }

    public static PaymentStatus getPaymentStatus(int jobId) throws SQLException {
        double total = calculateTotalCost(jobId);
        double paid = getTotalPaid(jobId);

        if (paid <= 0.001) {
            return PaymentStatus.UNPAID;
        } else if (paid >= total - 0.001) {
            return PaymentStatus.PAID;
        } else {
            return PaymentStatus.PARTIALLY_PAID;
        }
    }

    public static boolean isJobFullyPaid(int jobId) throws SQLException {
        return getPaymentStatus(jobId) == PaymentStatus.PAID;
    }

    public static int recordPayment(int jobId, double amount, String paymentDate, String notes) throws SQLException {
        RepairJob job = RepairDB.getRepairJobById(jobId);
        if (job == null) {
            throw new IllegalArgumentException("Repair Job #" + jobId + " does not exist.");
        }

        if (Double.isNaN(amount) || Double.isInfinite(amount) || amount <= 0) {
            throw new IllegalArgumentException("Payment amount must be a valid positive number.");
        }

        double balance = getRemainingBalance(jobId);
        if (balance <= 0.001) {
            throw new IllegalStateException("Repair Job #" + jobId + " is already fully paid.");
        }

        if (amount > balance + 0.001) {
            throw new IllegalArgumentException(String.format(
                    "Payment amount (LKR %.2f) exceeds remaining balance (LKR %.2f). Overpayment is not allowed.",
                    amount, balance));
        }

        Payment payment = new Payment(jobId, paymentDate, amount, notes);
        return PaymentDB.insertPayment(payment);
    }

    public static List<Payment> getPaymentsForJob(int jobId) throws SQLException {
        return PaymentDB.getPaymentsByJobId(jobId);
    }

    public static List<Payment> getAllPayments() throws SQLException {
        return PaymentDB.getAllPayments();
    }
}
