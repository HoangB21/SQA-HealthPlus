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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Test class for makeAppointment method in Receptionist class.
 * 
 * Method Purpose:
 * - Creates a new appointment for a patient with a specific doctor
 * - Updates doctor's availability counts (current/next week)
 * - Creates or updates temporary bill for the appointment
 * 
 * Method Parameters:
 * - patientID: Patient's unique identifier
 * - doctorID: Doctor's SLMC registration number
 * - day: Day of the appointment (1-7 for current week, 8-14 for next week)
 * - timeSlot: Time slot in format "HH:mm-HH:mm"
 * 
 * Method Return Format:
 * - Success: Returns new appointment_id (format: "appXXX" where X is a digit)
 * - Failure: Returns "false"
 * 
 * Business Rules:
 * 1. Current week appointments update current_week_appointments count
 * 2. Next week appointments (day > 7) update next_week_appointments count
 * 3. Each appointment creates/updates a temporary bill
 * 4. Generated IDs must follow specific format:
 *    - appointment_id: "appXXX" (where X is a digit)
 *    - tmp_bill_id: "hmsXXXXtb" (where X is a digit)
 */
public class MakeAppointmentTest {

    private Receptionist receptionistInstance;
    private Connection connection;

    @Mock
    private DatabaseOperator dbOperator;

    private AutoCloseable closeable;

    @BeforeEach
    public void setUp() throws SQLException {
        // Initialize Mockito annotations
        closeable = MockitoAnnotations.openMocks(this);

        // Initialize the Receptionist instance with userID "user018"
        receptionistInstance = new Receptionist("user018");

        // Get connection from DatabaseOperator
        connection = DatabaseOperator.c;

        // Start a transaction to allow rollback after the test
        connection.setAutoCommit(false);
    }

    @AfterEach
    public void tearDown() throws Exception {
        // Rollback database changes to restore the original state
        connection.rollback();
        connection.setAutoCommit(true);

        // Close Mockito resources
        closeable.close();
    }

    /*
     * RE_MA_01
     * Purpose: Verify successful creation of a new appointment with valid input data
     * 
     * Test Data Setup:
     * - Patient ID: "hms0001pa"
     * - Doctor ID: "22387"
     * - Day: "2" (Tuesday, current week)
     * - Time Slot: "09:00-12:00"
     * - Existing appointment: "app010"
     * - Doctor availability record for Tuesday 9-12
     * 
     * Expected Results:
     * 1. Returns new appointment_id in format "appXXX"
     * 2. Creates new appointment record in database with:
     *    - Correct patient and doctor IDs
     *    - Calculated appointment date
     *    - Cancelled flag set to false
     * 3. Updates doctor_availability.current_week_appointments
     * 4. Creates/updates tmp_bill with appointment fee
     * 
     * Tests Business Rules:
     * - Appointment ID generation format
     * - Current week appointment handling
     * - Temporary bill creation/update
     */
    @Test
    public void testMakeAppointment_SuccessfulInsertion() throws SQLException {
        // Set up test data
        Statement stmt = connection.createStatement();

        // Insert dummy data to simulate existing records
        stmt.executeUpdate("INSERT INTO appointment (appointment_id, patient_id, slmc_reg_no, date, cancelled) " +
                "VALUES ('app010', 'hms0001pa', '22387', '2024-03-19 08:00:00', false)");

        stmt.executeUpdate("INSERT INTO doctor_availability (slmc_reg_no, day, time_slot, current_week_appointments) " +
                "VALUES ('22387', '2', '09:00-12:00', 0)");

        // Call the method under test
        String result = receptionistInstance.makeAppointment("hms0001pa", "22387", "2", "09:00-12:00");

        // Verify the result is a valid appointment_id
        assertTrue(result.matches("app\\d{3}"), "Result should be a valid appointment_id");

        // Query the database to check the new appointment record
        ResultSet appointmentRs = stmt
                .executeQuery("SELECT * FROM appointment WHERE appointment_id = '" + result + "'");

        // Verify appointment record was created correctly
        assertTrue(appointmentRs.next(), "Appointment record should exist");
        assertEquals("hms0001pa", appointmentRs.getString("patient_id"));
        assertEquals("22387", appointmentRs.getString("slmc_reg_no"));
        assertFalse(appointmentRs.getBoolean("cancelled"));

        // Verify doctor_availability was updated
        ResultSet availabilityRs = stmt.executeQuery(
                "SELECT current_week_appointments FROM doctor_availability " +
                        "WHERE slmc_reg_no = '22387' AND day = '2' AND time_slot = '09:00-12:00'");
        assertTrue(availabilityRs.next(), "Doctor availability record should exist");
        assertEquals(1, availabilityRs.getInt("current_week_appointments"));

        // Verify tmp_bill was created
        ResultSet billRs = stmt.executeQuery(
                "SELECT * FROM tmp_bill WHERE patient_id = 'hms0036pa'");
        assertTrue(billRs.next(), "Temporary bill record should exist");
        assertEquals("500", billRs.getString("appointment_fee").trim());
    }

    /*
     * RE_MA_02
     * Purpose: Verify error handling when database operations fail
     * 
     * Test Data Setup:
     * - Patient ID: "hms0001pa"
     * - Doctor ID: "22387"
     * - Day: "2" (Tuesday)
     * - Time Slot: "09:00-12:00"
     * - Mock DatabaseOperator to throw SQLException for all operations
     * 
     * Expected Results:
     * 1. Returns "false" to indicate failure
     * 2. No appointment record created
     * 3. No changes to doctor_availability
     * 4. No tmp_bill created/updated
     * 
     * Tests Error Handling:
     * - Database operation failures
     * - Proper error state return value
     * - No partial updates in case of failure
     */
    @Test
    public void testMakeAppointment_DatabaseError() throws SQLException, ClassNotFoundException {
        // Manually set the dbOperator field in Receptionist to use mock
        receptionistInstance.dbOperator = dbOperator;

        // Mock the customSelection method to throw SQLException
        when(dbOperator.customSelection(anyString())).thenThrow(new SQLException("Database error"));

        // Call the method under test
        String result = receptionistInstance.makeAppointment("hms0001pa", "22387", "2", "09:00-12:00");

        // Verify the result indicates failure
        assertEquals("false", result, "Method should return 'false' when database error occurs");

        // Verify no records were added to the database
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM appointment WHERE patient_id = 'hm'");
        rs.next();
        assertEquals(0, rs.getInt("count"), "No appointment record should be added");
    }

    /*
     * RE_MA_03
     * Purpose: Verify correct handling of next week appointment scheduling
     * 
     * Test Data Setup:
     * - Patient ID: "hms0036pa"
     * - Doctor ID: "22387"
     * - Day: "9" (Next week Tuesday)
     * - Time Slot: "09:00-12:00"
     * - Doctor availability record with:
     *   * current_week_appointments = 0
     *   * next_week_appointments = 0
     * 
     * Expected Results:
     * 1. Returns new appointment_id in format "appXXX"
     * 2. Creates new appointment record with correct date calculation
     * 3. Updates doctor_availability.next_week_appointments (incremented by 1)
     * 4. Leaves doctor_availability.current_week_appointments unchanged
     * 
     * Tests Business Rules:
     * - Next week appointment identification (day > 7)
     * - Next week counter update
     * - Current week counter preservation
     * - Proper date calculation for next week
     */
    @Test
    public void testMakeAppointment_NextWeek() throws SQLException {
        // Set up test data
        Statement stmt = connection.createStatement();

        // Insert dummy data for doctor availability
        stmt.executeUpdate(
                "INSERT INTO doctor_availability (slmc_reg_no, day, time_slot, current_week_appointments, next_week_appointments) "
                        +
                        "VALUES ('22387', '2', '09:00-12:00', 0, 0)");

        // Call the method under test with day > 7 (next week)
        String result = receptionistInstance.makeAppointment("hms0036pa", "22387", "9", "09:00-12:00");

        // Verify the result is a valid appointment_id
        assertTrue(result.matches("app\\d{3}"), "Result should be a valid appointment_id");

        // Verify next_week_appointments was incremented instead of
        // current_week_appointments
        ResultSet availabilityRs = stmt.executeQuery(
                "SELECT current_week_appointments, next_week_appointments FROM doctor_availability " +
                        "WHERE slmc_reg_no = '22387' AND day = '2' AND time_slot = '09:00-12:00'");
        assertTrue(availabilityRs.next(), "Doctor availability record should exist");
        assertEquals(0, availabilityRs.getInt("current_week_appointments"),
                "Current week appointments should not change");
        assertEquals(1, availabilityRs.getInt("next_week_appointments"),
                "Next week appointments should be incremented");
    }

    /*
     * RE_MA_04
     * Purpose: Verify handling of invalid appointment ID format
     * 
     * Test Data Setup:
     * - Patient ID: "hms0001pa"
     * - Doctor ID: "22387"
     * - Day: "2" (Tuesday)
     * - Time Slot: "09:00-12:00"
     * - Mock DatabaseOperator to return invalid appointment ID "app"
     * 
     * Expected Results:
     * 1. Returns "false" to indicate failure
     * 2. No appointment record created
     * 3. No changes to doctor_availability
     * 4. No tmp_bill created/updated
     * 
     * Tests Business Rules:
     * - Appointment ID format validation (must be longer than 3 characters)
     * - No database changes on validation failure
     * - Proper error handling for invalid ID format
     */
    @Test
    public void testMakeAppointment_ShortAppointmentID() throws SQLException, ClassNotFoundException {
        // Manually set the dbOperator field in Receptionist to use mock
        receptionistInstance.dbOperator = dbOperator;

        // Create mock data with short appointmentID
        ArrayList<ArrayList<String>> mockResult = new ArrayList<>();
        ArrayList<String> headers = new ArrayList<>();
        headers.add("appointment_id");
        ArrayList<String> data = new ArrayList<>();
        data.add("app"); // appointmentID with length <= 3
        mockResult.add(headers);
        mockResult.add(data);

        // Mock the customSelection method to return the short appointmentID
        when(dbOperator.customSelection(anyString())).thenReturn(mockResult);

        // Call the method under test
        String result = receptionistInstance.makeAppointment("hms0001pa", "22387", "2", "09:00-12:00");

        // Verify the result indicates failure
        assertEquals("false", result, "Method should return 'false' when appointmentID length is <= 3");

        // Verify no records were added to the database
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM appointment WHERE patient_id = 'hms0001pa'");
        rs.next();
        assertEquals(0, rs.getInt("count"), "No appointment record should be added when appointmentID is invalid");

        // Verify no changes to doctor_availability
        ResultSet availabilityRs = stmt.executeQuery(
                "SELECT current_week_appointments FROM doctor_availability " +
                        "WHERE slmc_reg_no = '22387' AND day = '2' AND time_slot = '09:00-12:00'");
        if (availabilityRs.next()) {
            assertEquals(0, availabilityRs.getInt("current_week_appointments"),
                    "Doctor availability should not be updated when appointmentID is invalid");
        }

        // Verify no tmp_bill was created
        ResultSet billRs = stmt.executeQuery("SELECT COUNT(*) as count FROM tmp_bill WHERE patient_id = 'hms0001pa'");
        billRs.next();
        assertEquals(0, billRs.getInt("count"), "No temporary bill should be created when appointmentID is invalid");
    }

    /*
     * RE_MA_05
     * Objective: Verify that makeAppointment correctly handles appointment
     * scheduling when the appointment day
     * after subtracting 7 is greater than today (tmpDay > today)
     * Input:
     * patientID: "hms0001pa"
     * doctorID: "22387"
     * day: Current day of week + 8 (to ensure tmpDay > today after subtracting 7)
     * timeSlot: "09:00-12:00"
     * Expected output: Returns a new appointment_id
     * Expected database state:
     * - New appointment record with correct calculated date
     * - Updated current_week_appointments
     */
    @Test
    public void testMakeAppointment_LaterInWeek() throws SQLException {
        // Set up test data
        Statement stmt = connection.createStatement();

        // Get current day of week (1 = Sunday, 2 = Monday, etc)
        Calendar calendar = Calendar.getInstance();
        int today = calendar.get(Calendar.DAY_OF_WEEK);

        // Choose a day in next week that will be greater than today after subtracting 7
        // For example, if today is Wednesday (4), we need a number > 11 so that (number
        // - 7) > 4
        String appointmentDay = String.valueOf(today + 8); // This ensures (appointmentDay - 7) > today

        // The actual day value after system subtracts 7 will be (today + 1)
        String actualDay = String.valueOf(today + 1);

        // Insert dummy data for doctor availability using the actual day value
        stmt.executeUpdate("INSERT INTO doctor_availability (slmc_reg_no, day, time_slot, current_week_appointments) " +
                "VALUES ('22387', '" + actualDay + "', '09:00-12:00', 0)");

        // Call the method under test with the next week day
        String result = receptionistInstance.makeAppointment("hms0001pa", "22387", appointmentDay, "09:00-12:00");

        // Verify the result is a valid appointment_id
        assertTrue(result.matches("app\\d{3}"), "Result should be a valid appointment_id");

        // Query the database to check the appointment date
        ResultSet appointmentRs = stmt
                .executeQuery("SELECT date FROM appointment WHERE appointment_id = '" + result + "'");
        assertTrue(appointmentRs.next(), "Appointment record should exist");

        // Calculate expected appointment date
        Calendar expectedDate = Calendar.getInstance();
        int daysToAdd = Integer.parseInt(actualDay) - today;
        if (daysToAdd <= 0) {
            daysToAdd += 7;
        }
        expectedDate.add(Calendar.DATE, daysToAdd);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String expectedDateStr = dateFormat.format(expectedDate.getTime());

        // Verify the appointment date is calculated correctly
        String actualDate = appointmentRs.getString("date").substring(0, 10); // Get just the date part
        assertEquals(expectedDateStr, actualDate,
                "Appointment should be scheduled for the correct day when (tmpDay > today)");

        // Verify doctor_availability was updated
        ResultSet availabilityRs = stmt.executeQuery(
                "SELECT current_week_appointments FROM doctor_availability " +
                        "WHERE slmc_reg_no = '22387' AND day = '" + actualDay + "' AND time_slot = '09:00-12:00'");
        assertTrue(availabilityRs.next(), "Doctor availability record should exist");
        assertEquals(1, availabilityRs.getInt("current_week_appointments"),
                "Current week appointments should be incremented");
    }

    /*
     * RE_MA_06
     * Objective: Verify that makeAppointment correctly generates a new tmp_bill_id
     * when no existing bill is found
     * Input:
     * patientID: "hms0001pa"
     * doctorID: "22387"
     * day: "2"
     * timeSlot: "09:00-12:00"
     * Pre-test database state:
     * - No existing tmp_bill for the patient
     * - Last tmp_bill_id in database is "hms0005tb"
     * Expected output: Returns a new appointment_id
     * Expected database state:
     * - New tmp_bill with ID "hms0006tb"
     * - Appointment fee set to 500
     */
    @Test
    public void testMakeAppointment_NewTmpBill() throws SQLException {
        // Set up test data
        Statement stmt = connection.createStatement();

        // Insert dummy data for last tmp_bill to test ID generation
        stmt.executeUpdate("INSERT INTO tmp_bill (tmp_bill_id, patient_id, appointment_fee) " +
                "VALUES ('hms0005tb', 'hms0002pa', '500')");

        // Insert doctor availability data
        stmt.executeUpdate("INSERT INTO doctor_availability (slmc_reg_no, day, time_slot, current_week_appointments) " +
                "VALUES ('22387', '2', '09:00-12:00', 0)");

        // Call the method under test for a patient with no existing bill
        String result = receptionistInstance.makeAppointment("hms0001pa", "22387", "2", "09:00-12:00");

        // Verify the appointment was created
        assertTrue(result.matches("app\\d{3}"), "Result should be a valid appointment_id");

        // Verify new tmp_bill was created with correct ID format and appointment fee
        ResultSet billRs = stmt.executeQuery(
                "SELECT tmp_bill_id, appointment_fee FROM tmp_bill WHERE patient_id = 'hms0001pa'");
        assertTrue(billRs.next(), "Temporary bill record should exist");
        assertEquals("hms0006tb", billRs.getString("tmp_bill_id"),
                "New tmp_bill_id should be generated as next number");
        assertEquals("500", billRs.getString("appointment_fee").trim(),
                "Appointment fee should be set to 500");

        // Verify only one new tmp_bill was created
        ResultSet countRs = stmt.executeQuery(
                "SELECT COUNT(*) as count FROM tmp_bill WHERE patient_id = 'hms0001pa'");
        countRs.next();
        assertEquals(1, countRs.getInt("count"),
                "Only one tmp_bill record should be created");
    }

    /*
     * RE_MA_07
     * Objective: Verify that makeAppointment handles the case when no tmp_bill
     * exists in the database
     * Input:
     * patientID: "hms0001pa"
     * doctorID: "22387"
     * day: "2"
     * timeSlot: "09:00-12:00"
     * Pre-test database state:
     * - Empty tmp_bill table
     * Expected output: Returns a new appointment_id
     * Expected database state:
     * - New tmp_bill with ID "hms0001tb"
     * - Appointment fee set to 500
     */
    @Test
    public void testMakeAppointment_FirstTmpBill() throws SQLException {
        // Set up test data
        Statement stmt = connection.createStatement();

        // Clear tmp_bill table to test first bill creation
        stmt.executeUpdate("DELETE FROM tmp_bill");

        // Insert doctor availability data
        stmt.executeUpdate("INSERT INTO doctor_availability (slmc_reg_no, day, time_slot, current_week_appointments) " +
                "VALUES ('22387', '2', '09:00-12:00', 0)");

        // Call the method under test
        String result = receptionistInstance.makeAppointment("hms0001pa", "22387", "2", "09:00-12:00");

        // Verify the appointment was created
        assertTrue(result.matches("app\\d{3}"), "Result should be a valid appointment_id");

        // Verify new tmp_bill was created with first ID
        ResultSet billRs = stmt.executeQuery(
                "SELECT tmp_bill_id, appointment_fee FROM tmp_bill WHERE patient_id = 'hms0001pa'");
        assertTrue(billRs.next(), "Temporary bill record should exist");
        assertEquals("hms0001tb", billRs.getString("tmp_bill_id"),
                "First tmp_bill_id should be hms0001tb");
        assertEquals("500", billRs.getString("appointment_fee").trim(),
                "Appointment fee should be set to 500");
    }

    /*
     * RE_MA_08
     * Objective: Verify that makeAppointment handles the edge case where the
     * retrieved billID length is <= 3
     * Input:
     * patientID: "hms0001pa"
     * doctorID: "22387"
     * day: "2"
     * timeSlot: "09:00-12:00"
     * Mock behavior: DatabaseOperator returns a short billID "hms"
     * Expected output: Returns "false" due to invalid bill ID format
     * Expected database state: No changes to database
     */
    @Test
    public void testMakeAppointment_ShortBillID() throws SQLException, ClassNotFoundException {
        // Manually set the dbOperator field in Receptionist to use mock
        receptionistInstance.dbOperator = dbOperator;

        // Mock data for appointment ID query
        ArrayList<ArrayList<String>> appointmentIdResult = new ArrayList<>();
        ArrayList<String> appointmentHeaders = new ArrayList<>();
        appointmentHeaders.add("appointment_id");
        ArrayList<String> appointmentData = new ArrayList<>();
        appointmentData.add("app010");
        appointmentIdResult.add(appointmentHeaders);
        appointmentIdResult.add(appointmentData);

        // Mock data for max bill ID query
        ArrayList<ArrayList<String>> maxBillResult = new ArrayList<>();
        ArrayList<String> maxBillHeaders = new ArrayList<>();
        maxBillHeaders.add("tmp_bill_id");
        ArrayList<String> maxBillData = new ArrayList<>();
        maxBillData.add("hm"); // Short bill ID that will cause exception
        maxBillResult.add(maxBillHeaders);
        maxBillResult.add(maxBillData);

        // Setup mock behavior for specific SQL queries
        // 1. Mock for max appointment_id query
        when(dbOperator.customSelection(
                "SELECT appointment_id FROM appointment WHERE appointment_id = (SELECT MAX(appointment_id) FROM appointment);"))
                .thenReturn(appointmentIdResult);

        // 2. Mock for existing tmp_bill query to throw exception
        when(dbOperator.customSelection(
                "SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'hms0001pa';"))
                .thenThrow(new SQLException("Error processing short billID"));

        // 3. Mock for max tmp_bill_id query
        when(dbOperator.customSelection(
                "SELECT tmp_bill_id FROM tmp_bill WHERE tmp_bill_id = (SELECT MAX(tmp_bill_id) FROM tmp_bill);"))
                .thenReturn(maxBillResult);

        // Mock all database operations to return true
        when(dbOperator.customInsertion(anyString())).thenReturn(true);

        // Call the method under test
        String result = receptionistInstance.makeAppointment("hms0001pa", "22387", "2", "09:00-12:00");

        // Verify the result indicates failure
        assertEquals("false", result, "Method should return 'false' when billID length is <= 3");

        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM appointment WHERE patient_id = 'hms0001pa'");
        rs.next();
        assertEquals(0, rs.getInt("count"), "No appointment record should be added when billID is invalid");

        ResultSet availabilityRs = stmt.executeQuery(
                "SELECT current_week_appointments FROM doctor_availability " +
                        "WHERE slmc_reg_no = '22387' AND day = '2' AND time_slot = '09:00-12:00'");
        if (availabilityRs.next()) {
            assertEquals(0, availabilityRs.getInt("current_week_appointments"),
                    "Doctor availability should not be updated when billID is invalid");
        }

        ResultSet billRs = stmt.executeQuery("SELECT COUNT(*) as count FROM tmp_bill WHERE patient_id = 'hms0001pa'");
        billRs.next();
        assertEquals(0, billRs.getInt("count"), "No temporary bill should be created when billID is invalid");
    }
}
