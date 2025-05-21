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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Test class for updateProfileInfo method in Receptionist class.
 * 
 * Method Purpose:
 * - Updates profile information in the person table for the user identified by userID
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
 * 2. SQL UPDATE statement targets the person table using person_id linked to user_id
 */
public class UpdateProfileInfoTest {

    private Receptionist receptionistInstance;
    private Connection connection;
    
    @Mock
    private DatabaseOperator dbOperator;
    private AutoCloseable closeable;
    
    @BeforeEach
    public void setUp() throws SQLException {
        // Initialize Mockito annotations
        closeable = MockitoAnnotations.openMocks(this);
        // Initialize the Receptionist instance with a test userID
        receptionistInstance = new Receptionist("user018");

        // Establish connection to the MariaDB database
        connection = DatabaseOperator.c;

        // Start a transaction to allow rollback after the test
        connection.setAutoCommit(false);
    }

    @AfterEach
    public void tearDown() throws SQLException {
        // Rollback database changes
        connection.rollback();
        connection.setAutoCommit(true);
        connection.close();
    }

    /*
     * RE_UPI_01
     * Purpose: Verify successful profile update with actual database confirmation
     * 
     * Test Data Setup:
     * - Input string: "first_name John#last_name Doe#email john.doe@example.com"
     * - userID: "user999"
     * - Database records:
     *   - sys_user: user_id='user999', person_id='hms999', user_type='receptionist'
     *   - person: person_id='hms999', first_name='OldFirst', last_name='OldLast', email='old@example.com'
     * 
     * Expected Results:
     * 1. Returns true indicating successful update
     * 2. Updates person table: first_name='John', last_name='Doe', email='john.doe@example.com'
     * 3. Database query confirms the updated values
     * 
     * Tests Business Rules:
     * - Correct SQL construction from input string
     * - Successful database update
     * - Accurate update of person table fields
     */
    @Test
    public void testUpdateProfileInfo_SuccessWithDatabaseConfirmation() throws SQLException {
        // Test data
        String info = "first_name John#last_name Doe#email john.doe@example.com";

        // Call the method under test
        boolean result = receptionistInstance.updateProfileInfo(info);

        // Verify the method returns true
        assertTrue(result, "The updateProfileInfo method should return true for successful update");

        // Query the database to confirm the update
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(
            "SELECT first_name, last_name, email FROM person WHERE person_id = 'hms00098'"
        );
        assertTrue(rs.next(), "Person record should exist");
        assertEquals("John", rs.getString("first_name"), "First name should be updated to John");
        assertEquals("Doe", rs.getString("last_name"), "Last name should be updated to Doe");
        assertEquals("john.doe@example.com", rs.getString("email"), "Email should be updated to john.doe@example.com");
    }
    
    /*
     * RE_UPI_02
     * Purpose: Verify handling when customInsertion for profile update throws SQLException
     * 
     * Test Data Setup:
     * - Input string: "first_name John#last_name Doe#email john.doe@example.com"
     * - userID: "user018"
     * - Mock DatabaseOperator to throw SQLException for the UPDATE query
     * 
     * Execution:
     * - Sets up mock to throw SQLException for the constructed SQL
     * - Calls updateProfileInfo with the input string
     * - Queries the person table to confirm no changes occurred
     * 
     * Expected Results:
     * 1. Returns false indicating update failure
     * 2. No changes to person table (mocked operation)
     * 3. Handles SQLException gracefully (catch block executed)
     * 
     * Tests Business Rules:
     * - Exception handling during database update
     * - No unintended side effects on failure
     * - Proper error state return value
     */
    @Test
    public void testUpdateProfileInfo_UpdateException() throws SQLException, ClassNotFoundException {
        // Manually set the mocked dbOperator
        receptionistInstance.dbOperator = dbOperator;

        // Test data
        String info = "first_name John#last_name Doe#email john.doe@example.com";

        // Mock update to throw SQLException
        when(dbOperator.customInsertion(anyString()))
            .thenThrow(new SQLException("Failed to update person table"));

        // Call the method under test
        boolean result = receptionistInstance.updateProfileInfo(info);

        // Verify the method returns false
        assertFalse(result, "The updateProfileInfo method should return false when update throws SQLException");

        // Query the database to confirm no changes occurred
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(
            "SELECT first_name, last_name, email FROM person WHERE person_id = 'hms00098'"
        );
        assertTrue(rs.next(), "Person record should exist");
        assertEquals("Dulanji", rs.getString("first_name"), "First name should remain Dulanji");
        assertEquals("Manohari", rs.getString("last_name"), "Last name should remain Manohari");
        assertEquals("dulanji89@yahoo.com", rs.getString("email"), "Email should remain dulanji89@yahoo.com");
    }
}