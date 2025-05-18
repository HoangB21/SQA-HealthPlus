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
        connection = DriverManager.getConnection(
                "jdbc:mysql://127.0.0.1:3306/test_HMS2?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                "root", "hieu");

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

    /* RE_RF_01
    Objective: Verify that the refund method correctly inserts a new refund record into the database
               when provided with valid input, generates the next refund_id, and returns true.
    Input: refundInfo = "bill_id B001,payment_type Cash,reason Lost,amount 100"
           Pre-test database state: One record in the refund table with refund_id = "r0006".
    Expected output: The method returns true.
    Expected change in database: A new record is added to the refund table with:
                                 - bill_id = "B001"
                                 - payment_type = "Cash"
                                 - reason = "Lost"
                                 - amount = 100
                                 - refund_id = "r0007"
                                 - date = current timestamp (yyyy-MM-dd HH:mm:ss).
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

    /* RE_RF_02
    Objective: Verify that the refund method handles an SQLException during database operations
               (e.g., when querying the maximum refund_id) and returns false without modifying the database.
    Input: refundInfo = "bill_id B001,payment_type Cash,reason Lost,amount 100"
           Pre-test database state: The refund table is empty.
           Mock behavior: dbOperator.customSelection throws an SQLException.
    Expected output: The method returns false.
    Expected change in database: No records are inserted into the refund table.
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
        when(dbOperator.customSelection(anyString())).thenThrow(new SQLException("Database error: Unable to execute query"));

        // Call the method under test
        boolean result = receptionistInstance.refund(refundInfo);

        // Verify the method returns false when an SQLException occurs
        assertFalse(result, "The refund method should return false when an SQLException is thrown");

        // Verify the database state (no records should be inserted)
        ResultSet rs = stmt.executeQuery("SELECT * FROM refund");

        // Ensure the refund table is still empty
        assertFalse(rs.next(), "No records should be inserted into the refund table when an SQLException occurs");
    }

    /* RE_RF_03
    Objective: Verify that the refund method handles a case where the refund_id returned by the database
               has a length of 1 or less (e.g., "r"), preventing the for loop from executing, leading to
               an exception (e.g., NumberFormatException), and returns false without modifying the database.
    Input: refundInfo = "bill_id B001,payment_type Cash,reason Lost,amount 100"
           Pre-test database state: The refund table is empty.
           Mock behavior: dbOperator.customSelection returns a refund_id of "r" (length 1).
    Expected output: The method returns false due to an exception (e.g., NumberFormatException).
    Expected change in database: No records are inserted into the refund table.
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

        // Verify the method returns false due to an exception (e.g., StringIndexOutOfBoundsException)
        assertFalse(result, "The refund method should return false when refundID length is 1 or less");

        // Verify the database state (no records should be inserted)
        ResultSet rs = stmt.executeQuery("SELECT * FROM refund");

        // Ensure the refund table is still empty
        assertFalse(rs.next(), "No records should be inserted into the refund table when refundID is too short");
    }
}
