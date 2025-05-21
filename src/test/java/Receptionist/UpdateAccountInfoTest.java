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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Test class for updateAccountInfo method in Receptionist class.
 * 
 * Method Purpose:
 * - Updates account information in the sys_user table for the user identified by userID
 * - Constructs SQL UPDATE statement dynamically from input string
 * 
 * Method Parameters:
 * - info: String containing column-value pairs in the format "column1 value1#column2 value2#..."
 * 
 * Method Return Format:
 * - Success: Returns true if the update is successful
 * - Failure: Returns false if a database exception occurs
 * 
 * Business Rules:
 * 1. Input string must be correctly formatted with column-value pairs separated by '#'
 * 2. SQL UPDATE statement targets the sys_user table using user_id
 * 3. Only valid columns in sys_user table can be updated
 * 4. Database operations must handle exceptions without crashing
 */
public class UpdateAccountInfoTest {

    private Receptionist receptionistInstance;
    private Connection connection;

    @Mock
    private DatabaseOperator dbOperator;

    private AutoCloseable closeable;

    @BeforeEach
    public void setUp() throws SQLException {
        // Initialize Mockito annotations
        closeable = MockitoAnnotations.openMocks(this);

        // Initialize the Receptionist instance with the existing userID
        receptionistInstance = new Receptionist("user018");

        // Establish connection to the MariaDB database
        connection = DatabaseOperator.c;

        // Start a transaction to allow rollback after the test
        connection.setAutoCommit(false);
    }

    @AfterEach
    public void tearDown() throws SQLException, Exception {
        // Rollback database changes
        connection.rollback();
        connection.setAutoCommit(true);
        connection.close();

        // Close Mockito resources
        closeable.close();
    }

    /*
     * RE_UAI_01
     * Purpose: Verify successful account info update with actual database confirmation
     * 
     * Test Data Setup:
     * - Input string: "user_name john.doe#password newpass123#other_info updated diploma"
     * - userID: "hms0018u"
     * - Existing database record (sys_user):
     *   - user_id='hms0018u', person_id='hms00098', user_name='user018', user_type='receptionist',
     *     other_info='diploma in british council', password='1234', profile_pic='user018ProfPic.png'
     * 
     * Execution:
     * - Calls updateAccountInfo with the input string
     * - Queries the sys_user table to confirm the updated values
     * 
     * Expected Results:
     * 1. Returns true indicating successful update
     * 2. Updates sys_user table: user_name='john.doe', password='newpass123', other_info='updated diploma'
     *    for user_id='hms0018u'
     * 3. Database query confirms the updated values
     * 
     * Tests Business Rules:
     * - Correct SQL construction from input string
     * - Successful database update
     * - Accurate update of sys_user table fields for the specified user_id
     */
    @Test
    public void testUpdateAccountInfo_SuccessWithDatabaseConfirmation() throws SQLException {
        // Test data
        String info = "user_name john.doe#password newpass123#other_info updated diploma";

        // Call the method under test
        boolean result = receptionistInstance.updateAccountInfo(info);

        // Verify the method returns true
        assertTrue(result, "The updateAccountInfo method should return true for successful update");

        // Query the database to confirm the update
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(
            "SELECT user_name, password, other_info FROM sys_user WHERE user_id = 'hms0018u'"
        );
        assertTrue(rs.next(), "Sys_user record should exist");
        assertEquals("john.doe", rs.getString("user_name"), "Username should be updated to john.doe");
        assertEquals("newpass123", rs.getString("password"), "Password should be updated to newpass123");
        assertEquals("updated diploma", rs.getString("other_info"), "Other info should be updated to updated diploma");
    }

    /*
     * RE_UAI_02
     * Purpose: Verify handling when customInsertion for account info update throws SQLException
     * 
     * Test Data Setup:
     * - Input string: "user_name john.doe#password newpass123#other_info updated diploma"
     * - userID: "hms0018u"
     * - Mock DatabaseOperator to throw SQLException for the UPDATE query
     * 
     * Execution:
     * - Sets up mock to throw SQLException for the constructed SQL
     * - Calls updateAccountInfo with the input string
     * - Queries the sys_user table to confirm no changes occurred
     * 
     * Expected Results:
     * 1. Returns false indicating update failure
     * 2. No changes to sys_user table (mocked operation)
     * 3. Handles SQLException gracefully (catch block executed)
     * 
     * Tests Business Rules:
     * - Exception handling during database update
     * - No unintended side effects on failure
     * - Proper error state return value
     */
    @Test
    public void testUpdateAccountInfo_UpdateException() throws SQLException, ClassNotFoundException {
        // Manually set the mocked dbOperator
        receptionistInstance.dbOperator = dbOperator;

        // Test data
        String info = "user_name john.doe#password newpass123#other_info updated diploma";
        String expectedSql = "UPDATE sys_user SET user_name='john.doe',password='newpass123',other_info='updated diploma' WHERE user_id = 'hms0018u';";

        // Mock update to throw SQLException
        when(dbOperator.customInsertion(expectedSql))
            .thenThrow(new SQLException("Failed to update sys_user table"));

        // Call the method under test
        boolean result = receptionistInstance.updateAccountInfo(info);

        // Verify the method returns false
        assertFalse(result, "The updateAccountInfo method should return false when update throws SQLException");

        // Query the database to confirm no changes occurred
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(
            "SELECT user_name, password, other_info FROM sys_user WHERE user_id = 'hms0018u'"
        );
        assertTrue(rs.next(), "Sys_user record should exist");
        assertEquals("user018", rs.getString("user_name"), "Username should remain user018");
        assertEquals("1234", rs.getString("password"), "Password should remain 1234");
        assertEquals("diploma in british council", rs.getString("other_info"), "Other info should remain diploma in british council");
    }
}