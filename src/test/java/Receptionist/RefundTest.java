package Receptionist;

import com.hms.hms_test_2.DatabaseOperator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Test class for refund method in Receptionist class.
 * 
 * Method Purpose:
 * - Processes refund requests for bills
 * - Creates refund records with unique identifiers
 * - Tracks refund details including payment type, reason, and amount
 * 
 * Method Parameters:
 * - refundInfo: String containing refund details in format:
 *   "bill_id VALUE,payment_type VALUE,reason VALUE,amount VALUE"
 * 
 * Method Return Format:
 * - Success: Returns true
 * - Failure: Returns false
 * 
 * Business Rules:
 * 1. Each refund must have a unique refund_id in format "rXXXX"
 * 2. Refund amount must be a valid positive number
 * 3. Payment type must be either "Cash" or "Card"
 * 4. Refund date is automatically set to current timestamp
 * 5. All fields (bill_id, payment_type, reason, amount) are required
 */
public class RefundTest {

    private Receptionist receptionistInstance;
    private Connection connection;

    // Mock the DatabaseOperator (assumed class of dbOperator)
    @Mock
    private DatabaseOperator dbOperator;

    // For Mockito cleanup
    private AutoCloseable closeable;

    @BeforeEach
    public void setUp() throws SQLException {
        // Initialize Mockito annotations
        closeable = MockitoAnnotations.openMocks(this);

        // Initialize the Receptionist instance with userID "user018"
        receptionistInstance = new Receptionist("user018");

        // Establish connection to the MariaDB database
        connection = DatabaseOperator.c;

        // Start a transaction to allow rollback after the test
        connection.setAutoCommit(false);
    }

    @AfterEach
    public void tearDown() throws Exception {
        // Rollback database changes to restore the original state
        connection.rollback();
        connection.setAutoCommit(true);
        connection.close();

        // Close Mockito resources
        closeable.close();
    }

    /*
     * RE_RF_01
     * Purpose: Verify successful creation of a new refund record with valid input data
     * 
     * Test Data Setup:
     * - Refund Info: "bill_id B001,payment_type Cash,reason Lost,amount 100"
     * - Existing refund record:
     *   * refund_id: "r0006"
     *   * bill_id: "hms0007b"
     *   * payment_type: "Card"
     *   * amount: 50
     *   * date: "2025-01-01 00:00:00"
     * 
     * Expected Results:
     * 1. Returns true indicating successful operation
     * 2. Creates new refund record in database with:
     *    - Generated refund_id: "r0007"
     *    - Correct bill_id, payment_type, reason, and amount
     *    - Current timestamp as refund date
     * 3. Only one new record is added
     * 
     * Tests Business Rules:
     * - Refund ID generation format (rXXXX)
     * - Valid payment type handling (Cash)
     * - Timestamp generation
     * - Required field validation
     */
    @Test
    public void testRefund_SuccessfulInsertion() throws SQLException {
        // Set up for test case RE_RF_01
        // Insert dummy data into the refund table to simulate an existing record
        Statement stmt = connection.createStatement();
        stmt.executeUpdate("INSERT INTO refund (refund_id, bill_id, payment_type, reason, amount, date) "
                + "VALUES ('r0006', 'hms0007b', 'Card', 'Test', 50, '2025-01-01 00:00:00')");

        // Input for the refund method
        String refundInfo = "bill_id B001,payment_type Cash,reason Lost,amount 100";

        // Call the method under test
        boolean result = receptionistInstance.refund(refundInfo);

        // Verify the method returns true
        assertTrue(result, "The refund method should return true when the record is successfully inserted");

        // Query the database to check the new record
        ResultSet rs = stmt.executeQuery("SELECT * FROM refund WHERE refund_id = 'r0007'");

        // Verify that the new record exists
        assertTrue(rs.next(), "A new record with refund_id = 'r0007' should exist in the database");

        // Verify the values in the new record
        assertEquals("B001", rs.getString("bill_id"), "bill_id should be B001");
        assertEquals("Cash", rs.getString("payment_type"), "payment_type should be Cash");
        assertEquals("Lost", rs.getString("reason"), "reason should be Lost");
        assertEquals(100, rs.getInt("amount"), "amount should be 100");
        assertEquals("r0007", rs.getString("refund_id"), "refund_id should be r0007");

        // Verify the date (compare approximately since the exact time may vary)
        String expectedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(Calendar.getInstance().getTime());
        String actualDate = rs.getString("date").substring(0, 19); // Extract yyyy-MM-dd HH:mm:ss
        assertTrue(actualDate.startsWith(expectedDate.substring(0, 10)),
                "date should match the current date: " + expectedDate);

        // Ensure no extra records were added
        assertFalse(rs.next(), "Only one new record should be added");
    }

    /*
     * RE_RF_02
     * Purpose: Verify error handling when database operations fail
     * 
     * Test Data Setup:
     * - Refund Info: "bill_id B001,payment_type Cash,reason Lost,amount 100"
     * - Empty refund table (cleared before test)
     * - Mock DatabaseOperator to throw SQLException for all operations
     * 
     * Expected Results:
     * 1. Returns false to indicate failure
     * 2. No refund record created in database
     * 3. Database remains in original state
     * 
     * Tests Error Handling:
     * - Database operation failures
     * - Proper error state return value
     * - Data integrity preservation
     * - Transaction rollback functionality
     */
    @Test
    public void testRefund_ThrowsSQLException() throws SQLException, ClassNotFoundException {
        // Set up for test case RE_RF_02
        // Manually set the dbOperator field in Receptionist to use mock
        receptionistInstance.dbOperator = dbOperator;

        // Ensure the refund table is empty before the test
        Statement stmt = connection.createStatement();
        stmt.executeUpdate("DELETE FROM refund");

        // Input for the refund method
        String refundInfo = "bill_id B001,payment_type Cash,reason Lost,amount 100";

        // Mock the customSelection method to throw an SQLException
        when(dbOperator.customSelection(anyString()))
                .thenThrow(new SQLException("Database error: Unable to execute query"));

        // Call the method under test
        boolean result = receptionistInstance.refund(refundInfo);

        // Verify the method returns false when an SQLException occurs
        assertFalse(result, "The refund method should return false when an SQLException is thrown");

        // Verify the database state (no records should be inserted)
        ResultSet rs = stmt.executeQuery("SELECT * FROM refund");

        // Ensure the refund table is still empty
        assertFalse(rs.next(), "No records should be inserted into the refund table when an SQLException occurs");
    }

    /*
     * RE_RF_03
     * Purpose: Verify handling of invalid refund ID format
     * 
     * Test Data Setup:
     * - Refund Info: "bill_id B001,payment_type Cash,reason Lost,amount 100"
     * - Empty refund table (cleared before test)
     * - Mock DatabaseOperator to return invalid refund ID "r"
     * - Mock data structure:
     *   * Empty header row
     *   * Data row with single value "r"
     * 
     * Expected Results:
     * 1. Returns false to indicate failure
     * 2. No refund record created in database
     * 3. Database remains empty
     * 
     * Tests Business Rules:
     * - Refund ID format validation (must be longer than 1 character)
     * - No database changes on validation failure
     * - Proper error handling for invalid ID format
     * - Data integrity preservation
     */
    @Test
    public void testRefund_ShortRefundID() throws SQLException, ClassNotFoundException {
        // Set up for test case RE_RF_03
        // Manually set the dbOperator field in Receptionist to use mock
        receptionistInstance.dbOperator = dbOperator;

        // Ensure the refund table is empty before the test
        Statement stmt = connection.createStatement();
        stmt.executeUpdate("DELETE FROM refund");

        // Input for the refund method
        String refundInfo = "bill_id B001,payment_type Cash,reason Lost,amount 100";

        // Mock the customSelection method to return a short refundID ("r")
        ArrayList<ArrayList<String>> mockResult = new ArrayList<>();
        ArrayList<String> innerList = new ArrayList<>();
        innerList.add("r"); // refundID with length 1
        mockResult.add(new ArrayList<>()); // First element (header or empty)
        mockResult.add(innerList); // Second element (data)
        when(dbOperator.customSelection(anyString())).thenReturn(mockResult);

        // Call the method under test
        boolean result = receptionistInstance.refund(refundInfo);

        // Verify the method returns false due to an exception (e.g.,
        // StringIndexOutOfBoundsException)
        assertFalse(result, "The refund method should return false when refundID length is 1 or less");

        // Verify the database state (no records should be inserted)
        ResultSet rs = stmt.executeQuery("SELECT * FROM refund");

        // Ensure the refund table is still empty
        assertFalse(rs.next(), "No records should be inserted into the refund table when refundID is too short");
    }
}
