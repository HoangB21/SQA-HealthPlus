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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Test class for doctorAppointmentAvailableTime method in Receptionist class.
 * 
 * Method Purpose:
 * - Retrieves and calculates available appointment times for a specific doctor
 * - Processes doctor's schedule from doctor_availability table
 * - Calculates next available appointment times based on existing appointments
 * 
 * Method Parameters:
 * - doctorID: The doctor's registration number (slmc_reg_no)
 * 
 * Method Return Format:
 * ArrayList<ArrayList<String>> where:
 * - First row is header: ["day", "session_start", "app_time"]
 * - Subsequent rows contain:
 *   [0] day - Full date in format "EEEE MMM dd"
 *   [1] session_start - Start time of the session
 *   [2] app_time - Calculated appointment time based on existing appointments
 * 
 * Business Rules:
 * - Each appointment takes 5 minutes
 * - Next appointment time = session start time + (number of current appointments * 5 minutes)
 * - Days are numbered 1-7 (Monday to Sunday)
 */
public class DoctorAppointmentAvailableTimeTest {

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
     * RE_DAT_01
     * Purpose: Verify successful retrieval and calculation of appointment times
     * 
     * Test Data Setup:
     * - Doctor ID: "19993"
     * - Single availability record:
     *   - Day: Next day from current
     *   - Time slot: "09:00-12:00"
     *   - Current appointments: 2
     * 
     * Expected Results:
     * 1. Result contains header and one data row
     * 2. Header row matches expected format
     * 3. Data row contains:
     *    - Correct next day date
     *    - Session start time: "09:00"
     *    - Appointment time: "09:10" (start + 2 appointments * 5 minutes)
     * 
     * Tests Business Logic:
     * - Next day calculation
     * - Appointment time calculation
     * - Date formatting
     */
    @Test
    public void testDoctorAppointmentAvailableTime_SuccessfulRetrieval() throws SQLException {
        // Set up test data
        String doctorID = "19993";
        Statement stmt = connection.createStatement();

        // Get current day of week (1-7)
        Calendar cal = Calendar.getInstance();
        int currentDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        int testDay = (currentDayOfWeek % 7) + 1; // Next day

        // Clean up existing records for this doctor
        stmt.executeUpdate("DELETE FROM doctor_availability WHERE slmc_reg_no = '19993'");

        // Insert test data
        stmt.executeUpdate(
                "INSERT INTO doctor_availability (slmc_reg_no, day, time_slot, current_week_appointments) " +
                        "VALUES ('19993', '" + testDay + "', '09:00-12:00', 2)");

        // Calculate expected date
        int daysToAdd = testDay > currentDayOfWeek ? testDay - currentDayOfWeek : 7 - currentDayOfWeek + testDay;

        Calendar expectedCal = Calendar.getInstance();
        expectedCal.add(Calendar.DATE, daysToAdd);
        String expectedDay = new SimpleDateFormat("EEEE MMM dd").format(expectedCal.getTime());

        // Call the method
        ArrayList<ArrayList<String>> result = receptionistInstance.doctorAppointmentAvailableTime(doctorID);

        // Verify results
        assertNotNull(result, "Result should not be null");

        // Verify header row
        ArrayList<String> headerRow = result.get(0);
        assertEquals("day", headerRow.get(0));
        assertEquals("session_start", headerRow.get(1));
        assertEquals("app_time", headerRow.get(2));

        // Verify data row
        ArrayList<String> dataRow = result.get(1);
        assertEquals(expectedDay, dataRow.get(0), "Day should match expected format");
        assertEquals("09:00", dataRow.get(1), "Session start time should match input");
        assertEquals("09:10", dataRow.get(2),
                "Appointment time should be start time + 10 minutes (2 appointments * 5 minutes)");
    }

    /*
     * RE_DAT_02
     * Purpose: Verify error handling when database operations fail
     * 
     * Test Setup:
     * - Mock DatabaseOperator to throw SQLException
     * - Doctor ID: "19993"
     * 
     * Expected Results:
     * 1. Exception should be caught internally
     * 2. Result should contain only header row
     * 3. No data rows should be present
     * 
     * Tests Error Handling:
     * - Database operation failure
     * - Graceful error recovery
     * - Proper result structure maintenance
     */
    @Test
    public void testDoctorAppointmentAvailableTime_ThrowsSQLException() throws SQLException, ClassNotFoundException {
        // Set up mock behavior
        receptionistInstance.dbOperator = dbOperator;
        when(dbOperator.customSelection(anyString())).thenThrow(new SQLException("Database error"));

        // Call the method
        ArrayList<ArrayList<String>> result = receptionistInstance.doctorAppointmentAvailableTime("19993");

        // Verify results
        assertNotNull(result, "Result should not be null even after SQLException");
        assertEquals(1, result.size(), "Should only contain header row");

        // Verify header row
        ArrayList<String> headerRow = result.get(0);
        assertEquals("day", headerRow.get(0));
        assertEquals("session_start", headerRow.get(1));
        assertEquals("app_time", headerRow.get(2));
    }

    /*
     * RE_DAT_03
     * Purpose: Verify handling of null data from database
     * 
     * Test Setup:
     * - Mock DatabaseOperator to return null
     * - Doctor ID: "19993"
     * 
     * Expected Results:
     * 1. Method handles null data gracefully
     * 2. Result contains only header row
     * 3. Header structure remains intact
     * 
     * Tests Null Handling:
     * - Null check in data processing
     * - Default header creation
     * - Empty result set handling
     */
    @Test
    public void testDoctorAppointmentAvailableTime_NullData() throws SQLException, ClassNotFoundException {
        // Set up mock behavior
        receptionistInstance.dbOperator = dbOperator;
        when(dbOperator.customSelection(anyString())).thenReturn(null);

        // Call the method
        ArrayList<ArrayList<String>> result = receptionistInstance.doctorAppointmentAvailableTime("19993");

        // Verify results
        assertNotNull(result, "Result should not be null even with null data");
        assertEquals(1, result.size(), "Should only contain header row");

        // Verify header row
        ArrayList<String> headerRow = result.get(0);
        assertEquals("day", headerRow.get(0));
        assertEquals("session_start", headerRow.get(1));
        assertEquals("app_time", headerRow.get(2));
    }

    /*
     * RE_DAT_04
     * Purpose: Verify next day calculation when available day is after current day
     * 
     * Test Data Setup:
     * - Doctor ID: "19993"
     * - Availability record:
     *   - Day: Current day + 2
     *   - Time slot: "09:00-12:00"
     *   - Current appointments: 1
     * 
     * Expected Results:
     * 1. Correct calculation of next appointment day
     * 2. Proper handling of week wraparound
     * 3. Accurate appointment time calculation
     * 
     * Tests Specific Logic:
     * - if (availableDay > dayOfWeek) condition
     * - Next day calculation algorithm
     * - Week boundary handling
     */
    @Test
    public void testDoctorAppointmentAvailableTime_AvailableDayGreaterThanCurrent() throws SQLException {
        // Set up test data
        String doctorID = "19993";
        Statement stmt = connection.createStatement();

        // Clean up existing records for this doctor
        stmt.executeUpdate("DELETE FROM doctor_availability WHERE slmc_reg_no = '19993'");

        // Get current day of week and ensure test day is 2 days after
        Calendar cal = Calendar.getInstance();
        int currentDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        int testDay;

        // If we're near the end of the week, wrap around to beginning
        if (currentDayOfWeek >= 6) { // Friday or Saturday
            testDay = ((currentDayOfWeek + 2) % 7) + 1; // Wrap around to beginning of week
        } else {
            testDay = currentDayOfWeek + 2; // Just add 2 days
        }

        // Insert test data
        stmt.executeUpdate(
                "INSERT INTO doctor_availability (slmc_reg_no, day, time_slot, current_week_appointments) " +
                        "VALUES ('19993', '" + testDay + "', '09:00-12:00', 1)");

        // Calculate expected date (should be 2 days from now)
        Calendar expectedCal = Calendar.getInstance();
        // If we wrapped around, add days accordingly
        if (currentDayOfWeek >= 6) {
            expectedCal.add(Calendar.DATE, (7 - currentDayOfWeek) + testDay);
        } else {
            expectedCal.add(Calendar.DATE, 2);
        }
        String expectedDay = new SimpleDateFormat("EEEE MMM dd").format(expectedCal.getTime());

        // Call the method
        ArrayList<ArrayList<String>> result = receptionistInstance.doctorAppointmentAvailableTime(doctorID);

        // Verify results
        assertNotNull(result, "Result should not be null");

        // Verify header row
        ArrayList<String> headerRow = result.get(0);
        assertEquals("day", headerRow.get(0));
        assertEquals("session_start", headerRow.get(1));
        assertEquals("app_time", headerRow.get(2));

        // Verify data row
        ArrayList<String> dataRow = result.get(1);
        assertEquals(expectedDay, dataRow.get(0), "Day should be 2 days after current day");
        assertEquals("09:00", dataRow.get(1), "Session start time should match input");
        assertEquals("09:05", dataRow.get(2),
                "Appointment time should be start time + 5 minutes (1 appointment * 5 minutes)");
    }

    /*
     * RE_DAT_05
     * Purpose: Verify handling of invalid time format in database
     * 
     * Test Data Setup:
     * - Doctor ID: "19993"
     * - Availability record with invalid time:
     *   - Day: Next day
     *   - Time slot: "xxxx" (invalid format)
     *   - Current appointments: 1
     * 
     * Expected Results:
     * 1. ParseException is caught internally
     * 2. Invalid record is skipped
     * 3. Only header row is returned
     * 
     * Tests Error Handling:
     * - Time parsing error handling
     * - Invalid data skipping
     * - Result integrity maintenance
     */
    @Test
    public void testDoctorAppointmentAvailableTime_InvalidTimeFormat() throws SQLException {
        // Set up test data
        String doctorID = "19993";
        Statement stmt = connection.createStatement();

        // Clean up existing records for this doctor
        stmt.executeUpdate("DELETE FROM doctor_availability WHERE slmc_reg_no = '19993'");

        // Get current day of week and calculate test day
        Calendar cal = Calendar.getInstance();
        int currentDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        int testDay = (currentDayOfWeek % 7) + 1; // Next day

        // Insert test data with invalid time format
        stmt.executeUpdate(
                "INSERT INTO doctor_availability (slmc_reg_no, day, time_slot, current_week_appointments) " +
                        "VALUES ('19993', '" + testDay + "', 'xxxx', 1)");

        // Call the method
        ArrayList<ArrayList<String>> result = receptionistInstance.doctorAppointmentAvailableTime(doctorID);

        // Verify results
        assertNotNull(result, "Result should not be null even with parse exception");
        assertEquals(1, result.size(), "Should only contain header row when time parsing fails");

        // Verify header row
        ArrayList<String> headerRow = result.get(0);
        assertEquals("day", headerRow.get(0));
        assertEquals("session_start", headerRow.get(1));
        assertEquals("app_time", headerRow.get(2));
    }

    /*
     * RE_DAT_06
     * Purpose: Verify next day calculation when available day is less than or equal to current day
     * 
     * Test Data Setup:
     * - Doctor ID: "19993"
     * - Availability record:
     *   - Day: Current day or previous day
     *   - Time slot: "09:00-12:00"
     *   - Current appointments: 1
     * 
     * Expected Results:
     * 1. Correct calculation of next appointment day (should be next week)
     * 2. Proper handling of week wraparound
     * 3. Accurate appointment time calculation
     * 
     * Tests Specific Logic:
     * - if (availableDay <= dayOfWeek) condition
     * - Next week calculation algorithm
     * - Week boundary handling
     */
    @Test
    public void testDoctorAppointmentAvailableTime_AvailableDayLessThanOrEqualCurrent() throws SQLException {
        // Set up test data
        String doctorID = "19993";
        Statement stmt = connection.createStatement();

        // Clean up existing records for this doctor
        stmt.executeUpdate("DELETE FROM doctor_availability WHERE slmc_reg_no = '19993'");

        // Get current day of week and set test day to current or previous day
        Calendar cal = Calendar.getInstance();
        int currentDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        int testDay = currentDayOfWeek; // Same day as current

        // Insert test data
        stmt.executeUpdate(
                "INSERT INTO doctor_availability (slmc_reg_no, day, time_slot, current_week_appointments) " +
                        "VALUES ('19993', '" + testDay + "', '09:00-12:00', 1)");

        // Calculate expected date (should be next week)
        Calendar expectedCal = Calendar.getInstance();
        expectedCal.add(Calendar.DATE, 7); // Add 7 days to get to next week
        String expectedDay = new SimpleDateFormat("EEEE MMM dd").format(expectedCal.getTime());

        // Call the method
        ArrayList<ArrayList<String>> result = receptionistInstance.doctorAppointmentAvailableTime(doctorID);

        // Verify results
        assertNotNull(result, "Result should not be null");

        // Verify header row
        ArrayList<String> headerRow = result.get(0);
        assertEquals("day", headerRow.get(0));
        assertEquals("session_start", headerRow.get(1));
        assertEquals("app_time", headerRow.get(2));

        // Verify data row
        ArrayList<String> dataRow = result.get(1);
        assertEquals(expectedDay, dataRow.get(0), "Day should be next week's same day");
        assertEquals("09:00", dataRow.get(1), "Session start time should match input");
        assertEquals("09:05", dataRow.get(2),
                "Appointment time should be start time + 5 minutes (1 appointment * 5 minutes)");
    }
}
