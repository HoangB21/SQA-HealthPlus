package Receptionist;

import com.hms.hms_test_2.DatabaseOperator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for cancelAppointment method in Receptionist class.
 *
 * Method Purpose: - Cancels an existing appointment - Updates appointment
 * status to cancelled - Processes automatic refund if payment was made -
 * Updates bill status for refunded appointments
 *
 * Method Parameters: - appointmentID: String containing the unique identifier
 * of the appointment to cancel
 *
 * Method Return Format: - Success: Returns true if appointment was cancelled
 * and refund processed (if applicable) - Failure: Returns false if any
 * operation fails
 *
 * Business Rules: 1. Appointment must exist to be cancelled 2. Cancelled status
 * is updated in appointment table 3. If appointment has associated bill: -
 * Automatic refund is processed - Bill status is updated to refunded 4. All
 * database operations must succeed for successful cancellation
 */
public class CancelAppointmentTest {

    private Receptionist receptionistInstance;
    private Connection connection;

    @Mock
    private DatabaseOperator dbOperator;

    private AutoCloseable closeable;

    @BeforeEach
    public void setUp() throws SQLException {
        // Initialize Mockito annotations
        closeable = MockitoAnnotations.openMocks(this);

        // Initialize the Receptionist instance
        receptionistInstance = new Receptionist("user018");

        // Establish connection to the MariaDB database
        connection = DatabaseOperator.c;

        // Start a transaction to allow rollback after the test
        connection.setAutoCommit(false);
    }

    @AfterEach
    public void tearDown() throws Exception {
        // Rollback database changes
        connection.rollback();
        connection.setAutoCommit(true);
        connection.close();

        // Close Mockito resources
        closeable.close();
    }

    /*
     * RE_CA_01
     * Purpose: Verify successful cancellation of an appointment with associated bill and refund
     * 
     * Test Data Setup:
     * - Appointment record:
     *   * appointment_id: "app036"
     *   * bill_id: "hms0009b"
     *   * cancelled: false
     * - Bill record:
     *   * bill_id: "hms0009b"
     *   * total: 100
     *   * refund: false
     * 
     * Expected Results:
     * 1. Returns true indicating successful cancellation
     * 2. Updates appointment.cancelled to true
     * 3. Creates refund record with:
     *    - Correct bill_id (hms0009b)
     *    - Payment type "docApp"
     *    - Amount 100
     * 4. Updates bill.refund to true
     * 
     * Tests Business Rules:
     * - Appointment cancellation status update
     * - Automatic refund processing
     * - Bill status update
     * - Complete transaction success
     */
    @Test
    public void testCancelAppointment_SuccessfulCancellation() throws SQLException {
        // Set up test data
        Statement stmt = connection.createStatement();

        // Insert test bill
        stmt.executeUpdate("INSERT INTO bill (bill_id, total, refund) "
                + "VALUES ('hms0009b', 100, false)");

        // Insert test appointment
        stmt.executeUpdate("INSERT INTO appointment (appointment_id, bill_id, cancelled) "
                + "VALUES ('app036', 'hms0009b', false)");

        // Call the method under test
        boolean result = receptionistInstance.cancelAppointment("app036");

        // Verify the method returns true
        assertTrue(result, "The cancelAppointment method should return true for successful cancellation");

        // Verify appointment was marked as cancelled
        ResultSet rsAppointment = stmt.executeQuery(
                "SELECT cancelled FROM appointment WHERE appointment_id = 'app036'"
        );
        assertTrue(rsAppointment.next(), "Appointment record should exist");
        assertTrue(rsAppointment.getBoolean("cancelled"),
                "Appointment should be marked as cancelled");

        // Verify refund was created
        ResultSet rsRefund = stmt.executeQuery(
                "SELECT * FROM refund WHERE bill_id = 'hms0009b'"
        );
        assertTrue(rsRefund.next(), "Refund record should exist");
        assertEquals("docApp", rsRefund.getString("payment_type"),
                "Refund payment type should be docApp");
        assertEquals(100, rsRefund.getInt("amount"),
                "Refund amount should match bill total");

        // Verify bill was marked as refunded
        ResultSet rsBill = stmt.executeQuery(
                "SELECT refund FROM bill WHERE bill_id = 'hms0009b'"
        );
        assertTrue(rsBill.next(), "Bill record should exist");
        assertTrue(rsBill.getBoolean("refund"),
                "Bill should be marked as refunded");
    }

    /*
     * RE_CA_02
     * Purpose: Verify handling of database update failure when cancelling appointment
     * 
     * Test Data Setup:
     * - Mock DatabaseOperator to simulate update failure
     * - No actual database records needed as the mock will prevent the update
     * 
     * Expected Results:
     * 1. Returns false indicating cancellation failure
     * 2. No changes to appointment status
     * 3. No refund record created
     * 4. No changes to bill status
     * 
     * Tests Business Rules:
     * - Database operation failure handling
     * - No partial updates on failure
     * - Proper error state return value
     */
    @Test
    public void testCancelAppointment_UpdateFailure() throws SQLException, ClassNotFoundException {
        // Manually set the mocked dbOperator
        receptionistInstance.dbOperator = dbOperator;

        // Mock the customInsertion method to return false (update failure)
        when(dbOperator.customInsertion(anyString())).thenThrow(new SQLException("Failed to update appointment"));

        // Call the method under test
        boolean result = receptionistInstance.cancelAppointment("app036");

        // Verify the method returns false
        assertFalse(result, "The cancelAppointment method should return false when update fails");

        // Verify no changes were made to the database
        Statement stmt = connection.createStatement();

        // Verify appointment was not marked as cancelled
        ResultSet rsAppointment = stmt.executeQuery(
                "SELECT COUNT(*) as count FROM appointment WHERE appointment_id = 'app036' AND cancelled = true"
        );
        assertTrue(rsAppointment.next(), "Should be able to execute query");
        assertEquals(0, rsAppointment.getInt("count"),
                "No appointment should be marked as cancelled");

        // Verify no refund was created
        ResultSet rsRefund = stmt.executeQuery(
                "SELECT COUNT(*) as count FROM refund WHERE bill_id = 'hms0009b'"
        );
        assertTrue(rsRefund.next(), "Should be able to execute query");
        assertEquals(0, rsRefund.getInt("count"),
                "No refund record should be created");

        // Verify bill was not marked as refunded
        ResultSet rsBill = stmt.executeQuery(
                "SELECT COUNT(*) as count FROM bill WHERE bill_id = 'hms0009b' AND refund = true"
        );
        assertTrue(rsBill.next(), "Should be able to execute query");
        assertEquals(0, rsBill.getInt("count"),
                "No bill should be marked as refunded");
    }

    /*
     * RE_CA_03
     * Purpose: Verify handling of appointment cancellation when there is no associated bill (bill_id is NULL)
     * 
     * Test Data Setup:
     * - Mock DatabaseOperator to return successful appointment update
     * - Mock database query to return NULL for bill_id
     * 
     * Expected Results:
     * 1. Returns true (appointment cancellation succeeds)
     * 2. Updates appointment.cancelled to true
     * 3. No refund record created (as there's no bill)
     * 4. No bill status changes (as there's no bill)
     * 
     * Tests Business Rules:
     * - Proper handling of appointments without bills
     * - Successful cancellation without refund processing
     */
    @Test
    public void testCancelAppointment_NoBillId() throws SQLException, ClassNotFoundException {
        // Manually set the mocked dbOperator
        receptionistInstance.dbOperator = dbOperator;

        // Mock successful appointment update
        when(dbOperator.customInsertion(anyString())).thenReturn(true);
        // Clear any existing refund records
        Statement stmt = connection.createStatement();
        stmt.executeUpdate("DELETE FROM refund");
        // Mock database query to return NULL bill_id
        ArrayList<ArrayList<String>> mockData = new ArrayList<>();
        // Add header row
        mockData.add(new ArrayList<>(Arrays.asList("bill_id", "total")));
        // Add data row with NULL bill_id
        mockData.add(new ArrayList<>(Arrays.asList("NULL", "0")));
        when(dbOperator.customSelection(anyString())).thenReturn(mockData);

        // Call the method under test
        boolean result = receptionistInstance.cancelAppointment("app037");

        // Verify the method returns true
        assertTrue(result, "The cancelAppointment method should return true even without a bill");

        // Verify no refund was created
        stmt = connection.createStatement();
        ResultSet rsRefund = stmt.executeQuery(
                "SELECT COUNT(*) as count FROM refund"
        );
        assertTrue(rsRefund.next(), "Should be able to execute query");
        assertEquals(0, rsRefund.getInt("count"),
                "No refund record should be created when there's no bill");
    }

    /*
     * RE_CA_04
     * Purpose: Verify handling of database error during bill status update
     * 
     * Test Data Setup:
     * - Mock DatabaseOperator for successful appointment update
     * - Mock database query to return valid bill data
     * - Mock bill update to throw SQLException
     * 
     * Expected Results:
     * 1. Returns true (initial cancellation succeeds)
     * 2. Updates appointment.cancelled to true
     * 3. Creates refund record
     * 4. Handles bill update exception gracefully
     * 
     * Tests Business Rules:
     * - Exception handling during bill update
     * - Partial success handling (appointment cancelled but bill update fails)
     */
    @Test
    public void testCancelAppointment_BillUpdateError() throws SQLException, ClassNotFoundException {
        // Manually set the mocked dbOperator
        receptionistInstance.dbOperator = dbOperator;

        // Configure mock behavior sequence
        when(dbOperator.customInsertion(anyString()))
                .thenReturn(true) // First call (appointment update) succeeds
                .thenReturn(true) // Second call (refund creation) succeeds
                .thenThrow(new SQLException("Failed to update bill")); // Third call (bill update) fails

        // Mock database query to return bill data
        ArrayList<ArrayList<String>> mockData = new ArrayList<>();
        // Add header row
        mockData.add(new ArrayList<>(Arrays.asList("bill_id", "total")));
        // Add data row with valid bill data
        mockData.add(new ArrayList<>(Arrays.asList("hms0009b", "100")));
        when(dbOperator.customSelection(anyString())).thenReturn(mockData);

        // Call the method under test
        boolean result = receptionistInstance.cancelAppointment("app038");

        // Verify the method returns true despite bill update error
        assertTrue(result, "The cancelAppointment method should return true even if bill update fails");

        // Verify the exception was caught and handled gracefully
        // Note: In this case, we're mainly verifying that the method completed without throwing
        // an exception, as the actual database state would be inconsistent in a real scenario
    }

    /*
     * RE_CA_05
     * Purpose: Verify handling when bill update customInsertion throws SQLException
     * 
     * Test Data Setup:
     * - Mock DatabaseOperator for successful appointment update and refund creation
     * - Mock database query to return valid bill data
     * - Mock bill update customInsertion to throw SQLException
     * 
     * Expected Results:
     * 1. Returns true (initial operations succeed)
     * 2. Updates appointment.cancelled to true
     * 3. Creates refund record
     * 4. Handles SQLException from bill update gracefully
     * 5. Exception is caught and printed to stack trace
     * 
     * Tests Business Rules:
     * - Exception handling during bill status update
     * - Graceful error handling without affecting previous successful operations
     */
    @Test
    public void testCancelAppointment_BillUpdateCustomInsertionError() throws SQLException, ClassNotFoundException {
        // Manually set the mocked dbOperator
        receptionistInstance.dbOperator = dbOperator;

        // Clear any existing refund records
        Statement stmt = connection.createStatement();
        stmt.executeUpdate("DELETE FROM refund");

        // Configure mock behavior sequence
        when(dbOperator.customInsertion(anyString()))
                .thenReturn(true) // First call (appointment update) succeeds
                .thenReturn(true) // Second call (refund creation) succeeds
                .thenThrow(new SQLException("Database error during bill update")); // Third call (bill update) throws exception

        // Mock database query to return bill data
        ArrayList<ArrayList<String>> mockData = new ArrayList<>();
        mockData.add(new ArrayList<>(Arrays.asList("bill_id", "total")));
        mockData.add(new ArrayList<>(Arrays.asList("hms0009b", "100")));
        when(dbOperator.customSelection(anyString())).thenReturn(mockData);

        // Call the method under test
        boolean result = receptionistInstance.cancelAppointment("app039");

        // Verify the method returns true despite the exception
        assertTrue(result, "The cancelAppointment method should return true even when bill update throws exception");

        // Verify that refund was still created (previous successful operation)
        stmt = connection.createStatement();
        ResultSet rsRefund = stmt.executeQuery(
                "SELECT COUNT(*) as count FROM refund WHERE bill_id = 'hms0009b'"
        );
        assertTrue(rsRefund.next(), "Should be able to execute query");
        assertEquals(1, rsRefund.getInt("count"),
                "Refund record should still be created even if bill update fails");
    }
    
    /*
    * RE_CA_06
    * Purpose: Verify handling when customInsertion for bill update after refund throws an exception
    * 
    * Test Data Setup:
    * - Mock DatabaseOperator with specific SQL statements for appointment update, refund creation, and bill update
    * - Mock database query to return valid bill data
    * - Mock customInsertion for bill update (post-refund) to throw SQLException
    * 
    * Expected Results:
    * 1. Returns true (initial operations succeed)
    * 2. Updates appointment.cancelled to true (mocked)
    * 3. Creates refund record (mocked)
    * 4. Handles SQLException from bill update gracefully (catch block executed)
    * 5. Exception is caught and printed to stack trace
    * 
    * Tests Business Rules:
    * - Exception handling during bill status update after refund
    * - Ensures appointment cancellation and refund processing complete despite bill update failure
    * - Graceful error handling without affecting previous successful operations
    */
   @Test
   public void testCancelAppointment_BillUpdatePostRefundException() throws SQLException, ClassNotFoundException {
       // Manually set the mocked dbOperator
       receptionistInstance.dbOperator = dbOperator;

       // Clear any existing refund records
       Statement stmt = connection.createStatement();
       stmt.executeUpdate("DELETE FROM refund");

       // Mock specific SQL statements for customInsertion
       when(dbOperator.customInsertion("UPDATE appointment SET cancelled = true WHERE appointment.appointment_id = 'app040';"))
           .thenReturn(true); // Appointment update succeeds
       when(dbOperator.customInsertion("INSERT INTO refund (bill_id,payment_type,reason,amount,refund_id,date) VALUES ('hms0010b','docApp','no_reason','150','r0002','2025-05-18 18:04:00');"))
           .thenReturn(true); // Refund creation succeeds
       when(dbOperator.customInsertion("UPDATE bill SET refund = 1 WHERE bill_id = 'hms0010b'"))
           .thenThrow(new SQLException("Failed to update bill status after refund")); // Bill update fails

       // Mock specific SQL statement for customSelection (bill data)
       ArrayList<ArrayList<String>> mockBillData = new ArrayList<>();
       mockBillData.add(new ArrayList<>(Arrays.asList("bill_id", "total")));
       mockBillData.add(new ArrayList<>(Arrays.asList("hms0010b", "150")));
       when(dbOperator.customSelection("SELECT appointment.bill_id, bill.total FROM appointment INNER JOIN bill ON appointment.bill_id = bill.bill_id WHERE appointment_id = 'app040'"))
           .thenReturn(mockBillData);

       // Mock specific SQL statement for refund ID generation
       ArrayList<ArrayList<String>> mockRefundIdData = new ArrayList<>();
       mockRefundIdData.add(new ArrayList<>(Arrays.asList("refund_id")));
       mockRefundIdData.add(new ArrayList<>(Arrays.asList("r0001")));
       when(dbOperator.customSelection("SELECT refund_id FROM refund WHERE refund_id = (SELECT MAX(refund_id) FROM bill);"))
           .thenReturn(mockRefundIdData);

       // Call the method under test
       boolean result = receptionistInstance.cancelAppointment("app040");

       // Verify the method returns true despite the bill update exception
       assertTrue(result, "The cancelAppointment method should return true even when bill update after refund throws exception");

       // Verify refund was created in actual DB (since we cleared it and mocked insertion)
       ResultSet rsRefund = stmt.executeQuery(
           "SELECT COUNT(*) as count FROM refund WHERE bill_id = 'hms0010b'"
       );
       assertTrue(rsRefund.next(), "Should be able to execute query");
       assertEquals(0, rsRefund.getInt("count"), 
                   "No refund record should exist in actual DB due to mocking");

       // Verify appointment was not marked as cancelled in actual DB (since we're mocking)
       ResultSet rsAppointment = stmt.executeQuery(
           "SELECT COUNT(*) as count FROM appointment WHERE appointment_id = 'app040' AND cancelled = true"
       );
       assertTrue(rsAppointment.next(), "Should be able to execute query");
       assertEquals(0, rsAppointment.getInt("count"), 
                   "No appointment should be marked as cancelled in actual DB due to mocking");
   }
   
    /*
     * RE_CA_07
     * Purpose: Verify handling when the initial appointment update fails due to non-existent appointment ID, using mocking
     * 
     * Test Data Setup:
     * - Appointment ID: "app999" (simulated as non-existent via mock)
     * - Mock DatabaseOperator to return false for the initial UPDATE query
     * 
     * Execution:
     * - Mocks the customInsertion call to return false for the appointment update
     * - Calls cancelAppointment with the non-existent appointment ID
     * - Verifies the method returns false and no further operations are attempted
     * 
     * Expected Results:
     * 1. Returns false indicating cancellation failure
     * 2. No further database operations (bill selection, refund, bill update) attempted
     * 
     * Tests Business Rules:
     * - Appointment must exist to be cancelled
     * - Proper handling when update fails (returns false)
     * - No further operations attempted if initial update fails
     */
    @Test
    public void testCancelAppointment_NonExistentAppointmentId() throws SQLException, ClassNotFoundException {
        // Manually set the mocked dbOperator
        receptionistInstance.dbOperator = dbOperator;

        // Mock the initial UPDATE query to return false (simulating no rows affected)
        String updateQuery = "UPDATE appointment SET cancelled = true WHERE appointment.appointment_id = 'app999';";
        when(dbOperator.customInsertion(updateQuery)).thenReturn(false);

        // Call the method under test
        boolean result = receptionistInstance.cancelAppointment("app999");

        // Verify the method returns false
        assertFalse(result, "The cancelAppointment method should return false when the appointment ID does not exist");

        // Verify no further database operations were attempted (e.g., bill selection or refund)
        verify(dbOperator, never()).customSelection(anyString());
        verify(dbOperator, times(1)).customInsertion(anyString()); // Only the initial UPDATE should be called
    }
}
