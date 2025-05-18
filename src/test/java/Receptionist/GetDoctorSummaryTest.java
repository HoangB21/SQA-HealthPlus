package Receptionist;

import com.hms.hms_test_2.DatabaseOperator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class for getDoctorSummary method in Receptionist class.
 * 
 * Method Purpose:
 * - Retrieves a summary of all doctors' information and their availability
 * - Combines data from doctor, person, and doctor_availability tables
 * - Processes and formats the data for display
 * 
 * Method Return Format:
 * ArrayList<ArrayList<String>> where each inner ArrayList contains:
 * [0] slmc_reg_no - Doctor's registration number
 * [1] experienced_areas - Doctor's specialization
 * [2] first_name - Doctor's first name
 * [3] last_name - Doctor's last name
 * [4] available_days_count - Number of unique days doctor is available
 * [5] available_days - Space-separated string of available days (1-7)
 */
public class GetDoctorSummaryTest {

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

        // Establish connection to the database
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
     * RE_GDS_01
     * Purpose: Verify basic successful retrieval and formatting of doctor information
     * 
     * Test Data Setup:
     * - Uses existing doctor record with slmc_reg_no '19993'
     * - Relies on existing data in person and doctor_availability tables
     * 
     * Expected Results:
     * 1. Result should not be null
     * 2. Result should contain at least header and one data row
     * 3. Doctor data should match expected format:
     *    - Registration number: '19993'
     *    - Specialization: 'endocrineology'
     *    - First and last name should be present
     *    - Available days count should be valid number
     *    - Available days string should be properly formatted
     */
    @Test
    public void testGetDoctorSummary_SuccessfulRetrieval() throws SQLException {
        // Call the method
        ArrayList<ArrayList<String>> result = receptionistInstance.getDoctorSummary();

        // Verify results
        assertNotNull(result, "Result should not be null");
        assertTrue(result.size() >= 2, "Should contain at least header row and one data row");

        // Find the test doctor's row
        ArrayList<String> doctorRow = null;
        for (int i = 1; i < result.size(); i++) {
            if (result.get(i).get(0).equals("19993")) {
                doctorRow = result.get(i);
                break;
            }
        }

        // Verify doctor data
        assertNotNull(doctorRow, "Test doctor should be found in results");
        assertEquals("19993", doctorRow.get(0), "Should have correct registration number");
        assertEquals("endocrineology", doctorRow.get(1), "Should have correct experienced areas");
        
        // First name and last name will come from person table which is linked via user_id
        assertNotNull(doctorRow.get(2), "Should have first name");
        assertNotNull(doctorRow.get(3), "Should have last name");
        
        // Available days count and list will come from doctor_availability table
        assertTrue(Integer.parseInt(doctorRow.get(4)) >= 0, "Should have valid number of available days");
        // Days list can be empty if no availability
        assertNotNull(doctorRow.get(5), "Should have days list (can be empty)");
    }

    /*
     * RE_GDS_02
     * Purpose: Verify correct handling of duplicate days in doctor's schedule
     * 
     * Test Data Setup:
     * 1. Clean existing availability data
     * 2. Insert two time slots for same day:
     *    - Day 1, morning slot (09:00-12:00)
     *    - Day 1, afternoon slot (14:00-17:00)
     * 
     * Expected Results:
     * 1. Available days count should be 1 (not 2)
     * 2. Available days string should contain "1" exactly once
     * 3. No duplicate days should appear in the result
     * 
     * Tests Specific Logic:
     * - if (!tmpData2.contains(tmp)) condition in the method
     * - Duplicate day filtering functionality
     */
    @Test
    public void testGetDoctorSummary_DuplicateDays() throws SQLException {
        // Set up test data
        Statement stmt = connection.createStatement();

        // Clean up existing availability data for test doctor
        stmt.executeUpdate("DELETE FROM doctor_availability WHERE slmc_reg_no = '19993'");

        // Insert test availability with duplicate days
        stmt.executeUpdate(
            "INSERT INTO doctor_availability (time_slot_id, slmc_reg_no, day, time_slot, current_week_appointments) VALUES " +
            "('t0035', '19993', '1', '09:00-12:00', 0)" // First slot on day 1
        );
        
        stmt.executeUpdate(
            "INSERT INTO doctor_availability (time_slot_id, slmc_reg_no, day, time_slot, current_week_appointments) VALUES " +
            "('t0036', '19993', '1', '14:00-17:00', 0)"     // Second slot on day 1
        );

        // Call the method
        ArrayList<ArrayList<String>> result = receptionistInstance.getDoctorSummary();

        // Find the test doctor's row
        ArrayList<String> doctorRow = null;
        for (int i = 1; i < result.size(); i++) {
            if (result.get(i).get(0).equals("19993")) {
                doctorRow = result.get(i);
                break;
            }
        }

        // Verify doctor data
        assertNotNull(doctorRow, "Test doctor should be found in results");
        
        // Verify day counting - should only count unique days
        assertEquals("1", doctorRow.get(4), "Should count day 1 only once despite multiple time slots");
        
        // Verify days string - should only list day 1 once
        String daysString = doctorRow.get(5).trim();
        assertEquals("1", daysString, "Should list day 1 only once in the days string");
        
        // Additional verification that day 1 appears only once
        String[] days = daysString.split(" ");
        assertEquals(1, days.length, "Days string should contain exactly one day");
        assertEquals("1", days[0], "The only day should be 1");
    }

    /*
     * RE_GDS_03
     * Purpose: Verify exception handling for database errors
     * 
     * Test Setup:
     * - Mock DatabaseOperator to throw SQLException
     * - Simulates database connection or query execution failure
     * 
     * Expected Results:
     * 1. Exception should be caught (not thrown to caller)
     * 2. Method should return null
     * 3. Exception should be logged (printStackTrace)
     * 
     * Tests Error Handling:
     * - try-catch block in the method
     * - Graceful handling of database errors
     * - Appropriate null return value
     */
    @Test
    public void testGetDoctorSummary_DatabaseException() throws SQLException, ClassNotFoundException {
        // Set up mock behavior
        receptionistInstance.dbOperator = dbOperator;
        when(dbOperator.customSelection(anyString()))
            .thenThrow(new SQLException("Database connection failed"));

        // Call the method
        ArrayList<ArrayList<String>> result = receptionistInstance.getDoctorSummary();

        // Verify results
        assertNull(result, "Result should be null when database exception occurs");
    }
} 