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
import static org.mockito.Mockito.when;

/**
 * Test class for cancelLabAppointment method in Receptionist class.
 * 
 * Method Purpose:
 * - Cancels an existing lab appointment
 * - Updates lab appointment status to cancelled
 * - Processes automatic refund if payment was made
 * - Updates bill status for refunded appointments
 * 
 * Method Parameters:
 * - appointmentID: String containing the unique identifier of the lab appointment to cancel
 * 
 * Method Return Format:
 * - Success: Returns true if appointment was cancelled and refund processed (if applicable)
 * - Failure: Returns false if any operation fails
 * 
 * Business Rules:
 * 1. Lab appointment must exist to be cancelled
 * 2. Cancelled status is updated in lab_appointment table
 * 3. If appointment has associated bill:
 *    - Automatic refund is processed with payment_type labApp
 *    - Bill status is updated to refunded
 * 4. All database operations must succeed for successful cancellation
 */
public class CancelLabAppointmentTest {

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
     * RE_CLA_01
     * Purpose: Verify successful cancellation of a lab appointment with associated bill and refund
     * 
     * Test Data Setup:
     * - Lab appointment record:
     *   * lab_appointment_id: "lapp036"
     *   * bill_id: "hms0009b"
     *   * cancelled: false
     * - Bill record:
     *   * bill_id: "hms0009b"
     *   * total: 200
     *   * refund: false
     * 
     * Expected Results:
     * 1. Returns true indicating successful cancellation
     * 2. Updates lab_appointment.cancelled to true
     * 3. Creates refund record with:
     *    - Correct bill_id (hms0009b)
     *    - Payment type "labApp"
     *    - Amount 200
     * 4. Updates bill.refund to true
     * 
     * Tests Business Rules:
     * - Lab appointment cancellation status update
     * - Automatic refund processing
     * - Bill status update
     * - Complete transaction success
     */
    @Test
    public void testCancelLabAppointment_SuccessfulCancellation() throws SQLException, ClassNotFoundException {
        Statement stmt = connection.createStatement();

        // Clean up any existing test data
        stmt.executeUpdate("DELETE FROM refund WHERE bill_id = 'hms0009b'");
        stmt.executeUpdate("DELETE FROM lab_appointment WHERE lab_appointment_id = 'lapp036'");
        stmt.executeUpdate("DELETE FROM bill WHERE bill_id = 'hms0009b'");

        // Insert test data
        stmt.executeUpdate(
            "INSERT INTO bill (bill_id, total, refund) VALUES ('hms0009b', 200, false)"
        );
        stmt.executeUpdate(
            "INSERT INTO lab_appointment (lab_appointment_id, bill_id, cancelled) VALUES ('lapp036', 'hms0009b', false)"
        );

        // Call the method under test
        boolean result = receptionistInstance.cancelLabAppointment("lapp036");

        // Verify the method returns true
        assertTrue(result, "The cancelLabAppointment method should return true for successful cancellation");

        // Verify lab appointment is marked as cancelled
        ResultSet rsAppointment = stmt.executeQuery(
            "SELECT cancelled FROM lab_appointment WHERE lab_appointment_id = 'lapp036'"
        );
        assertTrue(rsAppointment.next(), "Lab appointment should exist");
        assertTrue(rsAppointment.getBoolean("cancelled"), 
                  "Lab appointment should be marked as cancelled");

        // Verify refund record is created
        ResultSet rsRefund = stmt.executeQuery(
            "SELECT * FROM refund WHERE bill_id = 'hms0009b'"
        );
        assertTrue(rsRefund.next(), "Refund record should exist");
        assertEquals("labApp", rsRefund.getString("payment_type"), 
                    "Refund payment type should be 'labApp'");
        assertEquals(200, rsRefund.getInt("amount"), 
                    "Refund amount should match bill total");

        // Verify bill is marked as refunded
        ResultSet rsBill = stmt.executeQuery(
            "SELECT refund FROM bill WHERE bill_id = 'hms0009b'"
        );
        assertTrue(rsBill.next(), "Bill should exist");
        assertTrue(rsBill.getBoolean("refund"), 
                  "Bill should be marked as refunded");
    }

    /*
     * RE_CLA_02
     * Purpose: Verify handling of database update failure when cancelling lab appointment
     * 
     * Test Data Setup:
     * - Mock DatabaseOperator to simulate lab appointment update failure
     * 
     * Expected Results:
     * 1. Returns false indicating cancellation failure
     * 2. No changes to lab appointment status
     * 3. No refund record created
     * 4. No changes to bill status
     * 
     * Tests Business Rules:
     * - Database operation failure handling
     * - No partial updates on failure
     * - Proper error state return value
     */
    @Test
    public void testCancelLabAppointment_UpdateFailure() throws SQLException, ClassNotFoundException {
        // Manually set the mocked dbOperator
        receptionistInstance.dbOperator = dbOperator;

        // Mock lab appointment update to fail
        when(dbOperator.customInsertion("UPDATE lab_appointment SET cancelled = true WHERE lab_appointment.lab_appointment_id = 'lapp037';"))
            .thenReturn(false);

        // Call the method under test
        boolean result = receptionistInstance.cancelLabAppointment("lapp037");

        // Verify the method returns false
        assertFalse(result, "The cancelLabAppointment method should return false when update fails");

        // Verify no changes were made to the database
        Statement stmt = connection.createStatement();
        ResultSet rsAppointment = stmt.executeQuery(
            "SELECT COUNT(*) as count FROM lab_appointment WHERE lab_appointment_id = 'lapp037' AND cancelled = true"
        );
        assertTrue(rsAppointment.next(), "Should be able to execute query");
        assertEquals(0, rsAppointment.getInt("count"), 
                    "No lab appointment should be marked as cancelled");

        ResultSet rsRefund = stmt.executeQuery(
            "SELECT COUNT(*) as count FROM refund WHERE bill_id = 'hms0009b'"
        );
        assertTrue(rsRefund.next(), "Should be able to execute query");
        assertEquals(0, rsRefund.getInt("count"), 
                    "No refund record should be created");
    }

    /*
     * RE_CLA_03
     * Purpose: Verify handling of lab appointment cancellation when there is no associated bill (bill_id is NULL)
     * 
     * Test Data Setup:
     * - Mock DatabaseOperator to return successful lab appointment update
     * - Mock database query to return NULL for bill_id
     * 
     * Expected Results:
     * 1. Returns true (lab appointment cancellation succeeds)
     * 2. Updates lab_appointment.cancelled to true (mocked)
     * 3. No refund record created (as there's no bill)
     * 4. No bill status changes (as there's no bill)
     * 
     * Tests Business Rules:
     * - Proper handling of lab appointments without bills
     * - Successful cancellation without refund processing
     */
    @Test
    public void testCancelLabAppointment_NoBillId() throws SQLException, ClassNotFoundException {
        // Manually set the mocked dbOperator
        receptionistInstance.dbOperator = dbOperator;

        // Clear any existing refund records
        Statement stmt = connection.createStatement();
        stmt.executeUpdate("DELETE FROM refund");

        // Mock lab appointment update
        when(dbOperator.customInsertion("UPDATE lab_appointment SET cancelled = true WHERE lab_appointment.lab_appointment_id = 'lapp038';"))
            .thenReturn(true);

        // Mock bill data query to return NULL bill_id
        ArrayList<ArrayList<String>> mockBillData = new ArrayList<>();
        mockBillData.add(new ArrayList<>(Arrays.asList("bill_id", "total")));
        mockBillData.add(new ArrayList<>(Arrays.asList("NULL", "0")));
        when(dbOperator.customSelection("SELECT lab_appointment.bill_id, bill.total FROM lab_appointment INNER JOIN bill ON lab_appointment.bill_id = bill.bill_id WHERE lab_appointment_id = 'lapp038'"))
            .thenReturn(mockBillData);

        // Call the method under test
        boolean result = receptionistInstance.cancelLabAppointment("lapp038");

        // Verify the method returns true
        assertTrue(result, "The cancelLabAppointment method should return true even without a bill");

        // Verify no refund was created
        ResultSet rsRefund = stmt.executeQuery(
            "SELECT COUNT(*) as count FROM refund"
        );
        assertTrue(rsRefund.next(), "Should be able to execute query");
        assertEquals(0, rsRefund.getInt("count"), 
                    "No refund record should be created when there's no bill");
    }

    /*
     * RE_CLA_04
     * Purpose: Verify handling of SQLException during bill data selection
     * 
     * Test Data Setup:
     * - Mock DatabaseOperator for successful lab appointment update
     * - Mock bill data query to throw SQLException
     * 
     * Expected Results:
     * 1. Returns true (lab appointment cancellation succeeds)
     * 2. Updates lab_appointment.cancelled to true (mocked)
     * 3. No refund record created (due to query failure)
     * 4. Handles SQLException gracefully
     * 
     * Tests Business Rules:
     * - Exception handling during bill data retrieval
     * - Successful cancellation despite bill query failure
     */
    @Test
    public void testCancelLabAppointment_BillQueryException() throws SQLException, ClassNotFoundException {
        // Manually set the mocked dbOperator
        receptionistInstance.dbOperator = dbOperator;

        // Clear any existing refund records
        Statement stmt = connection.createStatement();
        stmt.executeUpdate("DELETE FROM refund");

        // Mock lab appointment update
        when(dbOperator.customInsertion("UPDATE lab_appointment SET cancelled = true WHERE lab_appointment.lab_appointment_id = 'lapp039';"))
            .thenReturn(true);

        // Mock bill data query to throw SQLException
        when(dbOperator.customSelection("SELECT lab_appointment.bill_id, bill.total FROM lab_appointment INNER JOIN bill ON lab_appointment.bill_id = bill.bill_id WHERE lab_appointment_id = 'lapp039'"))
            .thenThrow(new SQLException("Failed to retrieve bill data"));

        // Call the method under test
        boolean result = receptionistInstance.cancelLabAppointment("lapp039");

        // Verify the method returns true
        assertTrue(result, "The cancelLabAppointment method should return true despite bill query exception");

        // Verify no refund was created
        ResultSet rsRefund = stmt.executeQuery(
            "SELECT COUNT(*) as count FROM refund"
        );
        assertTrue(rsRefund.next(), "Should be able to execute query");
        assertEquals(0, rsRefund.getInt("count"), 
                    "No refund record should be created due to bill query failure");
    }

    /*
     * RE_CLA_05
     * Purpose: Verify handling when customInsertion for bill update after refund throws SQLException
     * 
     * Test Data Setup:
     * - Mock DatabaseOperator for successful lab appointment update and refund creation
     * - Mock database query to return valid bill data
     * - Mock bill update customInsertion to throw SQLException
     * 
     * Expected Results:
     * 1. Returns true (initial operations succeed)
     * 2. Updates lab_appointment.cancelled to true (mocked)
     * 3. Creates refund record (mocked)
     * 4. Handles SQLException from bill update gracefully (catch block executed)
     * 
     * Tests Business Rules:
     * - Exception handling during bill status update
     * - Graceful error handling without affecting previous successful operations
     */
    @Test
    public void testCancelLabAppointment_BillUpdatePostRefundException() throws SQLException, ClassNotFoundException {
        // Manually set the mocked dbOperator
        receptionistInstance.dbOperator = dbOperator;

        // Clear any existing refund records
        Statement stmt = connection.createStatement();
        stmt.executeUpdate("DELETE FROM refund");

        // Mock specific SQL statements for customInsertion
        when(dbOperator.customInsertion("UPDATE lab_appointment SET cancelled = true WHERE lab_appointment.lab_appointment_id = 'lapp040';"))
            .thenReturn(true); // Lab appointment update
        when(dbOperator.customInsertion("INSERT INTO refund (bill_id,payment_type,reason,amount,refund_id,date) VALUES ('hms0010b','labApp','no_reason','250','r0002','2025-05-18 18:07:00');"))
            .thenReturn(true); // Refund creation
        when(dbOperator.customInsertion("UPDATE bill SET refund = 1 WHERE bill_id = 'hms0010b'"))
            .thenThrow(new SQLException("Failed to update bill status after refund")); // Bill update fails

        // Mock specific SQL statement for customSelection (bill data)
        ArrayList<ArrayList<String>> mockBillData = new ArrayList<>();
        mockBillData.add(new ArrayList<>(Arrays.asList("bill_id", "total")));
        mockBillData.add(new ArrayList<>(Arrays.asList("hms0010b", "250")));
        when(dbOperator.customSelection("SELECT lab_appointment.bill_id, bill.total FROM lab_appointment INNER JOIN bill ON lab_appointment.bill_id = bill.bill_id WHERE lab_appointment_id = 'lapp040'"))
            .thenReturn(mockBillData);

        // Mock specific SQL statement for refund ID generation
        ArrayList<ArrayList<String>> mockRefundIdData = new ArrayList<>();
        mockRefundIdData.add(new ArrayList<>(Arrays.asList("refund_id")));
        mockRefundIdData.add(new ArrayList<>(Arrays.asList("r0001")));
        when(dbOperator.customSelection("SELECT refund_id FROM refund WHERE refund_id = (SELECT MAX(refund_id) FROM bill);"))
            .thenReturn(mockRefundIdData);

        // Call the method under test
        boolean result = receptionistInstance.cancelLabAppointment("lapp040");

        // Verify the method returns true
        assertTrue(result, "The cancelLabAppointment method should return true despite bill update exception");

        // Verify no actual DB changes (all operations mocked)
        ResultSet rsRefund = stmt.executeQuery(
            "SELECT COUNT(*) as count FROM refund WHERE bill_id = 'hms0010b'"
        );
        assertTrue(rsRefund.next(), "Should be able to execute query");
        assertEquals(0, rsRefund.getInt("count"), 
                    "No refund record should exist in actual DB due to mocking");
    }

    /*
     * RE_CLA_06
     * Purpose: Verify handling when refund operation fails
     * 
     * Test Data Setup:
     * - Mock DatabaseOperator for successful lab appointment update
     * - Mock bill data query to return valid bill data
     * - Mock refund insertion to return false
     * 
     * Expected Results:
     * 1. Returns false (refund failure propagates)
     * 2. Updates lab_appointment.cancelled to true (mocked)
     * 3. No refund record created (mocked)
     * 4. No bill status update attempted
     * 
     * Tests Business Rules:
     * - Refund operation failure handling
     * - Propagation of failure to method return value
     */
    @Test
    public void testCancelLabAppointment_RefundFailure() throws SQLException, ClassNotFoundException {
        // Manually set the mocked dbOperator
        receptionistInstance.dbOperator = dbOperator;

        // Clear any existing refund records
        Statement stmt = connection.createStatement();
        stmt.executeUpdate("DELETE FROM refund");

        // Mock specific SQL statements for customInsertion
        when(dbOperator.customInsertion("UPDATE lab_appointment SET cancelled = true WHERE lab_appointment.lab_appointment_id = 'lapp041';"))
            .thenReturn(true); // Lab appointment update
        when(dbOperator.customInsertion("INSERT INTO refund (bill_id,payment_type,reason,amount,refund_id,date) VALUES ('hms0011b','labApp','no_reason','300','r0003','2025-05-18 18:07:00');"))
            .thenReturn(false); // Refund creation fails

        // Mock specific SQL statement for customSelection (bill data)
        ArrayList<ArrayList<String>> mockBillData = new ArrayList<>();
        mockBillData.add(new ArrayList<>(Arrays.asList("bill_id", "total")));
        mockBillData.add(new ArrayList<>(Arrays.asList("hms0011b", "300")));
        when(dbOperator.customSelection("SELECT lab_appointment.bill_id, bill.total FROM lab_appointment INNER JOIN bill ON lab_appointment.bill_id = bill.bill_id WHERE lab_appointment_id = 'lapp041'"))
            .thenReturn(mockBillData);

        // Mock specific SQL statement for refund ID generation
        ArrayList<ArrayList<String>> mockRefundIdData = new ArrayList<>();
        mockRefundIdData.add(new ArrayList<>(Arrays.asList("refund_id")));
        mockRefundIdData.add(new ArrayList<>(Arrays.asList("r0002")));
        when(dbOperator.customSelection("SELECT refund_id FROM refund WHERE refund_id = (SELECT MAX(refund_id) FROM bill);"))
            .thenReturn(mockRefundIdData);

        // Call the method under test
        boolean result = receptionistInstance.cancelLabAppointment("lapp041");

        // Verify the method returns false
        assertFalse(result, "The cancelLabAppointment method should return false when refund fails");

        // Verify no actual DB changes (all operations mocked)
        ResultSet rsRefund = stmt.executeQuery(
            "SELECT COUNT(*) as count FROM refund WHERE bill_id = 'hms0011b'"
        );
        assertTrue(rsRefund.next(), "Should be able to execute query");
        assertEquals(0, rsRefund.getInt("count"), 
                    "No refund record should exist in actual DB due to mocking");
    }
    /*
    * RE_CLA_07
    * Purpose: Verify handling when customInsertion for lab appointment update throws SQLException
    * 
    * Test Data Setup:
    * - Mock DatabaseOperator to throw SQLException for lab appointment update
    * 
    * Expected Results:
    * 1. Returns false indicating cancellation failure
    * 2. No changes to lab appointment status
    * 3. No bill data queried or refund attempted
    * 4. Handles SQLException gracefully (catch block executed)
    * 
    * Tests Business Rules:
    * - Exception handling during initial lab appointment update
    * - No further operations attempted on failure
    * - Proper error state return value
    */
   @Test
   public void testCancelLabAppointment_InitialUpdateException() throws SQLException, ClassNotFoundException {
       // Manually set the mocked dbOperator
       receptionistInstance.dbOperator = dbOperator;
       Statement stmt = connection.createStatement();
       stmt.executeUpdate("DELETE FROM refund");
       // Mock lab appointment update to throw SQLException
       when(dbOperator.customInsertion("UPDATE lab_appointment SET cancelled = true WHERE lab_appointment.lab_appointment_id = 'lapp042';"))
           .thenThrow(new SQLException("Failed to update lab appointment status"));

       // Call the method under test
       boolean result = receptionistInstance.cancelLabAppointment("lapp042");

       // Verify the method returns false
       assertFalse(result, "The cancelLabAppointment method should return false when initial update throws SQLException");

       // Verify no changes were made to the database
       
       ResultSet rsAppointment = stmt.executeQuery(
           "SELECT COUNT(*) as count FROM lab_appointment WHERE lab_appointment_id = 'lapp042' AND cancelled = true"
       );
       assertTrue(rsAppointment.next(), "Should be able to execute query");
       assertEquals(0, rsAppointment.getInt("count"), 
                   "No lab appointment should be marked as cancelled in actual DB");

       // Verify no refund was created
       ResultSet rsRefund = stmt.executeQuery(
           "SELECT COUNT(*) as count FROM refund"
       );
       assertTrue(rsRefund.next(), "Should be able to execute query");
       assertEquals(0, rsRefund.getInt("count"), 
                   "No refund record should be created when update fails");
   }
}