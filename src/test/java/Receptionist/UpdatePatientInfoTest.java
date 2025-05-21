package Receptionist;

import com.hms.hms_test_2.DatabaseOperator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Test class for updatePatientInfo method in Receptionist class.
 * 
 * Method Purpose:
 * - Updates patient information in the person table
 * - Uses patient_id to find the corresponding person_id
 * - Executes an UPDATE query on the person table
 * 
 * Method Parameters:
 * - patientID: String - The ID of the patient whose information needs to be updated
 * - info: String - The SQL update clause (column and new value)
 * 
 * Method Return:
 * - boolean: true if update successful, false if failed
 * 
 * Business Rules:
 * 1. Must update the correct person record based on patient ID
 * 2. Must handle database exceptions gracefully
 * 3. Must maintain referential integrity
 * 4. Must return true only if update is successful
 * 
 * Note: This test class uses direct database operations instead of mocking
 */
public class UpdatePatientInfoTest {

    private Receptionist receptionistInstance;
    private static final String TEST_PATIENT_ID = "hms0001pa";
    private String originalEmail;
    private String originalAddress;

    @Mock
    private DatabaseOperator mockDbOperator;
    private AutoCloseable closeable;

    @BeforeEach
    public void setUp() {
        // Initialize Mockito annotations
        closeable = MockitoAnnotations.openMocks(this);

        // Initialize the Receptionist instance
        receptionistInstance = new Receptionist("user018");

        // For non-mock tests, use real database
        try {
            // Store original values for restoration after tests
            String sql = "SELECT email, address FROM person " +
                        "WHERE person_id = (SELECT person_id FROM patient WHERE patient_id = '" + TEST_PATIENT_ID + "');";
            var result = receptionistInstance.dbOperator.customSelection(sql);
            if (result != null && result.size() > 1) {
                originalEmail = result.get(1).get(0);
                originalAddress = result.get(1).get(1);
            }
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    @AfterEach
    public void tearDown() throws Exception {
        try {
            // Restore original values
            if (originalEmail != null) {
                receptionistInstance.updatePatientInfo(TEST_PATIENT_ID, "email = '" + originalEmail + "'");
            }
            if (originalAddress != null) {
                receptionistInstance.updatePatientInfo(TEST_PATIENT_ID, "address = '" + originalAddress + "'");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Close Mockito resources
        closeable.close();
    }

    /*
     * RE_UPI_01
     * Purpose: Verify successful update of patient's email
     * 
     * Test Data:
     * - PatientID: hms0001pa (existing in database)
     * - Info: Update email field
     * 
     * Execution:
     * - Updates email for existing patient
     * - Verifies update was successful
     * - Verifies new value in database
     * 
     * Expected Results:
     * 1. Method returns true
     * 2. Database contains updated email
     */
    @Test
    public void testUpdatePatientInfo_UpdateEmail() throws SQLException, ClassNotFoundException {
        // Test data
        String newEmail = "newemail@test.com";
        String updateClause = "email = '" + newEmail + "'";

        // Perform update
        boolean result = receptionistInstance.updatePatientInfo(TEST_PATIENT_ID, updateClause);

        // Verify update was successful
        assertTrue(result, "Update operation should return true");

        // Verify the new value in database
        String sql = "SELECT email FROM person " +
                    "WHERE person_id = (SELECT person_id FROM patient WHERE patient_id = '" + TEST_PATIENT_ID + "');";
        var dbResult = receptionistInstance.dbOperator.customSelection(sql);
        
        assertNotNull(dbResult, "Database query should return results");
        assertTrue(dbResult.size() > 1, "Should have header and data row");
        assertEquals(newEmail, dbResult.get(1).get(0), "Email should be updated in database");
    }

    /*
     * RE_UPI_02
     * Purpose: Verify successful update of multiple fields
     * 
     * Test Data:
     * - PatientID: hms0001pa (existing in database)
     * - Info: Update both email and address fields
     * 
     * Execution:
     * - Updates multiple fields for existing patient
     * - Verifies update was successful
     * - Verifies new values in database
     * 
     * Expected Results:
     * 1. Method returns true
     * 2. Database contains all updated values
     */
    @Test
    public void testUpdatePatientInfo_UpdateMultipleFields() throws SQLException, ClassNotFoundException {
        // Test data
        String newEmail = "multi@test.com";
        String newAddress = "123 Test St";
        String updateClause = "email = '" + newEmail + "', address = '" + newAddress + "'";

        // Perform update
        boolean result = receptionistInstance.updatePatientInfo(TEST_PATIENT_ID, updateClause);

        // Verify update was successful
        assertTrue(result, "Update operation should return true");

        // Verify the new values in database
        String sql = "SELECT email, address FROM person " +
                    "WHERE person_id = (SELECT person_id FROM patient WHERE patient_id = '" + TEST_PATIENT_ID + "');";
        var dbResult = receptionistInstance.dbOperator.customSelection(sql);
        
        assertNotNull(dbResult, "Database query should return results");
        assertTrue(dbResult.size() > 1, "Should have header and data row");
        assertEquals(newEmail, dbResult.get(1).get(0), "Email should be updated in database");
        assertEquals(newAddress, dbResult.get(1).get(1), "Address should be updated in database");
    }

    /*
     * RE_UPI_03
     * Purpose: Verify handling of non-existent patient ID
     * 
     * Test Data:
     * - PatientID: NONEXISTENT
     * - Info: Valid update clause
     * 
     * Execution:
     * - Attempts to update non-existent patient
     * - Verifies operation fails gracefully
     * 
     * Expected Results:
     * 1. Method returns false
     * 2. No database changes made
     */
    @Test
    public void testUpdatePatientInfo_NonExistentPatient() {
        // Test data
        String nonExistentID = "NONEXISTENT";
        String updateClause = "email = 'test@test.com'";

        // Perform update
        boolean result = receptionistInstance.updatePatientInfo(nonExistentID, updateClause);

        // Verify update failed
        assertFalse(result, "Update should fail for non-existent patient");
    }

    /*
     * RE_UPI_04
     * Purpose: Verify handling of invalid update clause
     * 
     * Test Data:
     * - PatientID: hms0001pa (existing in database)
     * - Info: Invalid SQL update clause
     * 
     * Execution:
     * - Attempts update with invalid SQL
     * - Verifies operation fails gracefully
     * 
     * Expected Results:
     * 1. Method returns false
     * 2. No database changes made
     */
    @Test
    public void testUpdatePatientInfo_InvalidUpdateClause() {
        // Test data
        String invalidClause = "invalid_column = 'value'"; // Column doesn't exist

        // Perform update
        boolean result = receptionistInstance.updatePatientInfo(TEST_PATIENT_ID, invalidClause);

        // Verify update failed
        assertFalse(result, "Update should fail for invalid column name");
    }

    /*
     * RE_UPI_05
     * Purpose: Verify handling of SQLException during update
     * 
     * Test Data:
     * - PatientID: hms0001pa
     * - Info: Valid update clause
     * - Mock DatabaseOperator to throw SQLException
     * 
     * Execution:
     * - Sets up mock to throw SQLException
     * - Attempts to update patient info
     * - Verifies operation fails gracefully
     * 
     * Expected Results:
     * 1. Method returns false
     * 2. Exception is caught and handled properly
     */
    @Test
    public void testUpdatePatientInfo_DatabaseException() throws SQLException, ClassNotFoundException {
        // Setup test instance with mock
        Receptionist receptionistWithMock = new Receptionist("user018");
        receptionistWithMock.dbOperator = mockDbOperator;

        // Setup mock to throw exception
        when(mockDbOperator.customInsertion(anyString()))
            .thenThrow(new SQLException("Simulated database error"));

        // Test data
        String updateClause = "email = 'test@example.com'";

        // Perform update
        boolean result = receptionistWithMock.updatePatientInfo(TEST_PATIENT_ID, updateClause);

        // Verify update failed gracefully
        assertFalse(result, "Update should return false when database exception occurs");
    }
} 