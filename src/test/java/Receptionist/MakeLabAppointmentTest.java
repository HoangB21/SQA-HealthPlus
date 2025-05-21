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
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.contains;

public class MakeLabAppointmentTest {

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
     * RE_MLA_01
     * Objective: Verify that makeLabAppointment successfully creates a new lab
     * appointment
     * with valid input data
     * Input:
     * patientID: "hms0001pa"
     * doctorID: "22387"
     * testID: "test001"
     * day: "2" (Tuesday)
     * timeSlot: "09:00-12:00"
     * Pre-test database state:
     * - Existing lab appointment with ID "lapp010"
     * - Lab test availability for Tuesday 9-12
     * Expected output: Returns a new lab_appointment_id (e.g., "lapp011")
     * Expected database state:
     * - New record in lab_appointment table with provided info
     * - Updated current_week_appointments count in lab_appointment_timetable
     * - New record in tmp_bill table with laboratory fee
     */
    @Test
    public void testMakeLabAppointment_SuccessfulInsertion() throws SQLException {
        // Set up test data
        Statement stmt = connection.createStatement();

        // Insert dummy data to simulate existing records
        stmt.executeUpdate("INSERT INTO lab_test (test_id, test_name, test_fee) " +
                "VALUES ('test001', 'Blood Test', '1000')");

        stmt.executeUpdate(
                "INSERT INTO lab_appointment (lab_appointment_id, test_id, patient_id, doctor_id, date, cancelled) " +
                        "VALUES ('lapp010', 'test001', 'hms0001pa', '22387', '2024-03-19 08:00:00', false)");

        stmt.executeUpdate(
                "INSERT INTO lab_appointment_timetable (app_test_id, app_day, time_slot, current_week_appointments) " +
                        "VALUES ('test001', '2', '09:00-12:00', 0)");

        // Call the method under test
        String result = receptionistInstance.makeLabAppointment("hms0001pa", "22387", "test001", "2", "09:00-12:00");

        // Verify the result is a valid lab_appointment_id
        assertTrue(result.matches("lapp\\d{3}"), "Result should be a valid lab_appointment_id");

        // Query the database to check the new appointment record
        ResultSet appointmentRs = stmt
                .executeQuery("SELECT * FROM lab_appointment WHERE lab_appointment_id = '" + result + "'");

        // Verify appointment record was created correctly
        assertTrue(appointmentRs.next(), "Lab appointment record should exist");
        assertEquals("hms0001pa", appointmentRs.getString("patient_id"));
        assertEquals("22387", appointmentRs.getString("doctor_id"));
        assertEquals("test001", appointmentRs.getString("test_id"));
        assertFalse(appointmentRs.getBoolean("cancelled"));

        // Verify lab_appointment_timetable was updated
        ResultSet availabilityRs = stmt.executeQuery(
                "SELECT current_week_appointments FROM lab_appointment_timetable " +
                        "WHERE app_test_id = 'test001' AND app_day = '2' AND time_slot = '09:00-12:00'");
        assertTrue(availabilityRs.next(), "Lab appointment timetable record should exist");
        assertEquals(1, availabilityRs.getInt("current_week_appointments"));

        // Verify tmp_bill was created with laboratory fee
        ResultSet billRs = stmt.executeQuery(
                "SELECT * FROM tmp_bill WHERE patient_id = 'hms0001pa'");
        assertTrue(billRs.next(), "Temporary bill record should exist");
        assertEquals("1000", billRs.getString("laboratory_fee").trim());
    }

    /*
     * RE_MLA_02
     * Objective: Verify that makeLabAppointment handles database errors gracefully
     * Input: Same valid input as successful case
     * Mock behavior: DatabaseOperator throws SQLException
     * Expected output: Returns "false"
     * Expected database state: No changes to database
     */
    @Test
    public void testMakeLabAppointment_DatabaseError() throws SQLException, ClassNotFoundException {
        // Manually set the dbOperator field in Receptionist to use mock
        receptionistInstance.dbOperator = dbOperator;

        // Mock the customSelection method to throw SQLException
        when(dbOperator.customSelection(anyString())).thenThrow(new SQLException("Database error"));
        when(dbOperator.customInsertion(anyString())).thenThrow(new SQLException("Database error"));

        // Call the method under test
        String result = receptionistInstance.makeLabAppointment("hms0001pa", "22387", "test001", "2", "09:00-12:00");

        // Verify the result indicates failure
        assertEquals("false", result, "Method should return 'false' when database error occurs");

        // Verify no records were added to the database
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt
                .executeQuery("SELECT COUNT(*) as count FROM lab_appointment WHERE patient_id = 'hms0001pa'");
        rs.next();
        assertEquals(0, rs.getInt("count"), "No lab appointment record should be added");
    }

    /*
     * RE_MLA_03
     * Objective: Verify that makeLabAppointment handles next week appointments
     * correctly
     * Input:
     * patientID: "hms0001pa"
     * doctorID: "22387"
     * testID: "test001"
     * day: "9" (Next week Tuesday)
     * timeSlot: "09:00-12:00"
     * Expected output: Returns a new lab_appointment_id
     * Expected database state: Updated next_week_appointments instead of
     * current_week_appointments
     */
    @Test
    public void testMakeLabAppointment_NextWeek() throws SQLException {
        // Set up test data
        Statement stmt = connection.createStatement();

        // Insert dummy data for lab test availability
        stmt.executeUpdate("INSERT INTO lab_test (test_id, test_name, test_fee) " +
                "VALUES ('test001', 'Blood Test', '1000')");

        stmt.executeUpdate(
                "INSERT INTO lab_appointment_timetable (app_test_id, app_day, time_slot, current_week_appointments, next_week_appointments) "
                        + "VALUES ('test001', '2', '09:00-12:00', 0, 0)");

        // Call the method under test with day > 7 (next week)
        String result = receptionistInstance.makeLabAppointment("hms0001pa", "22387", "test001", "9", "09:00-12:00");

        // Verify the result is a valid lab_appointment_id
        assertTrue(result.matches("lapp\\d{3}"), "Result should be a valid lab_appointment_id");

        // Verify next_week_appointments was incremented instead of
        // current_week_appointments
        ResultSet availabilityRs = stmt.executeQuery(
                "SELECT current_week_appointments, next_week_appointments FROM lab_appointment_timetable " +
                        "WHERE app_test_id = 'test001' AND app_day = '2' AND time_slot = '09:00-12:00'");
        assertTrue(availabilityRs.next(), "Lab appointment timetable record should exist");
        assertEquals(0, availabilityRs.getInt("current_week_appointments"),
                "Current week appointments should not change");
        assertEquals(1, availabilityRs.getInt("next_week_appointments"),
                "Next week appointments should be incremented");
    }

    /*
     * RE_MLA_04
     * Objective: Verify that makeLabAppointment handles the edge case where the
     * retrieved lab_appointment_id length is <= 4 (e.g., "lapp")
     * Input: Same valid input as successful case
     * Mock behavior: DatabaseOperator returns a short lab_appointment_id
     * Expected output: Returns "false"
     * Expected database state: No changes to database
     */
    @Test
    public void testMakeLabAppointment_ShortAppointmentID() throws SQLException, ClassNotFoundException {
        // Manually set the dbOperator field in Receptionist to use mock
        receptionistInstance.dbOperator = dbOperator;

        // Create mock data with short lab_appointment_id
        ArrayList<ArrayList<String>> mockResult = new ArrayList<>();
        ArrayList<String> headers = new ArrayList<>();
        headers.add("lab_appointment_id");
        ArrayList<String> data = new ArrayList<>();
        data.add("lapp"); // lab_appointment_id with length <= 4
        mockResult.add(headers);
        mockResult.add(data);

        // Mock the customSelection method to return the short lab_appointment_id
        when(dbOperator.customSelection(anyString())).thenReturn(mockResult);

        // Call the method under test
        String result = receptionistInstance.makeLabAppointment("hms0001pa", "22387", "test001", "2", "09:00-12:00");

        // Verify the result indicates failure
        assertEquals("false", result, "Method should return 'false' when lab_appointment_id length is <= 4");

        // Verify no records were added to the database
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt
                .executeQuery("SELECT COUNT(*) as count FROM lab_appointment WHERE patient_id = 'hms0001pa'");
        rs.next();
        assertEquals(0, rs.getInt("count"), "No lab appointment record should be added");
    }

    /*
     * RE_MLA_05
     * Objective: Verify that makeLabAppointment correctly handles appointment
     * scheduling
     * when the appointment day is later in the current week
     * Input: Same as successful case but with a day later in the week
     * Expected output: Returns a new lab_appointment_id
     * Expected database state: Appointment scheduled for the correct date
     */
    @Test
    public void testMakeLabAppointment_LaterInWeek() throws SQLException {
        // Set up test data
        Statement stmt = connection.createStatement();

        // Insert dummy data for lab test availability

        stmt.executeUpdate("INSERT INTO lab_test (test_id, test_name, test_fee) " +
                "VALUES ('test001', 'Blood Test', '1000')");

        stmt.executeUpdate(
                "INSERT INTO lab_appointment_timetable (app_test_id, app_day, time_slot, current_week_appointments) "
                        + "VALUES ('test001', '5', '09:00-12:00', 0)");

        // Call the method under test with a later day in the week (Friday = 5)
        String result = receptionistInstance.makeLabAppointment("hms0001pa", "22387", "test001", "5", "09:00-12:00");

        // Verify the result is a valid lab_appointment_id
        assertTrue(result.matches("lapp\\d{3}"), "Result should be a valid lab_appointment_id");

        // Query the database to check the appointment date
        ResultSet appointmentRs = stmt
                .executeQuery("SELECT date FROM lab_appointment WHERE lab_appointment_id = '" + result + "'");
        assertTrue(appointmentRs.next(), "Lab appointment record should exist");

        // Verify the appointment date is correctly calculated
        Calendar cal = Calendar.getInstance();
        int currentDay = cal.get(Calendar.DAY_OF_WEEK);
        int daysToAdd = (5 >= currentDay) ? (5 - currentDay) : (7 - currentDay + 5);
        cal.add(Calendar.DATE, daysToAdd);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String expectedDate = sdf.format(cal.getTime());

        assertTrue(appointmentRs.getString("date").startsWith(expectedDate),
                "Appointment date should be correctly calculated for Friday");
    }

    /*
     * RE_MLA_06
     * Objective: Verify that makeLabAppointment correctly creates a new tmp_bill
     * when one doesn't exist
     * Input: Same as successful case but ensure no existing tmp_bill
     * Expected output: Returns a new lab_appointment_id
     * Expected database state: New tmp_bill created with correct laboratory fee
     */
    @Test
    public void testMakeLabAppointment_NewTmpBill() throws SQLException {
        // Set up test data
        Statement stmt = connection.createStatement();

        // Insert dummy data for lab test
        stmt.executeUpdate("INSERT INTO lab_test (test_id, test_name, test_fee) " +
                "VALUES ('test001', 'Blood Test', '1000')");

        stmt.executeUpdate(
                "INSERT INTO lab_appointment_timetable (app_test_id, app_day, time_slot, current_week_appointments) "
                        + "VALUES ('test001', '2', '09:00-12:00', 0)");

        // Ensure no existing tmp_bill
        stmt.executeUpdate("DELETE FROM tmp_bill WHERE patient_id = 'hms0001pa'");

        // Call the method under test
        String result = receptionistInstance.makeLabAppointment("hms0001pa", "22387", "test001", "2", "09:00-12:00");

        // Verify the result is a valid lab_appointment_id
        assertTrue(result.matches("lapp\\d{3}"), "Result should be a valid lab_appointment_id");

        // Verify new tmp_bill was created
        ResultSet billRs = stmt.executeQuery(
                "SELECT * FROM tmp_bill WHERE patient_id = 'hms0001pa'");
        assertTrue(billRs.next(), "New temporary bill should be created");
        assertEquals("1000", billRs.getString("laboratory_fee").trim());
        assertTrue(billRs.getString("tmp_bill_id").matches("hms\\d{4}tb"),
                "tmp_bill_id should have correct format");
    }

    /*
     * RE_MLA_07
     * Objective: Verify that makeLabAppointment correctly handles the first
     * tmp_bill
     * creation when no tmp_bills exist in the system
     * Input: Same as successful case but with empty tmp_bill table
     * Expected output: Returns a new lab_appointment_id
     * Expected database state: First tmp_bill created with ID hms0001tb
     */
    @Test
    public void testMakeLabAppointment_FirstTmpBill() throws SQLException {
        // Set up test data
        Statement stmt = connection.createStatement();

        // Clear tmp_bill table
        stmt.executeUpdate("DELETE FROM tmp_bill");

        // Insert dummy data for lab test
        stmt.executeUpdate("INSERT INTO lab_test (test_id, test_name, test_fee) " +
                "VALUES ('test001', 'Blood Test', '1000')");

        stmt.executeUpdate(
                "INSERT INTO lab_appointment_timetable (app_test_id, app_day, time_slot, current_week_appointments) "
                        + "VALUES ('test001', '2', '09:00-12:00', 0)");

        // Call the method under test
        String result = receptionistInstance.makeLabAppointment("hms0001pa", "22387", "test001", "2", "09:00-12:00");

        // Verify the result is a valid lab_appointment_id
        assertTrue(result.matches("lapp\\d{3}"), "Result should be a valid lab_appointment_id");

        // Verify first tmp_bill was created with correct ID
        ResultSet billRs = stmt.executeQuery(
                "SELECT * FROM tmp_bill WHERE patient_id = 'hms0001pa'");
        assertTrue(billRs.next(), "First temporary bill should be created");
        assertEquals("hms0001tb", billRs.getString("tmp_bill_id").trim(),
                "First tmp_bill should have ID hms0001tb");
        assertEquals("1000", billRs.getString("laboratory_fee").trim());
    }

    /*
     * RE_MLA_08
     * Objective: Verify that makeLabAppointment handles the case where the
     * retrieved
     * tmp_bill_id length is <= 3 (e.g., "hm")
     * Input: Same as successful case
     * Mock behavior:
     * - DatabaseOperator returns valid lab_appointment_id
     * - Throws exception for existing tmp_bill query
     * - Returns short tmp_bill_id for max tmp_bill query
     * Expected output: Returns "false"
     * Expected database state: No changes to database
     */
    @Test
    public void testMakeLabAppointment_ShortBillID() throws SQLException, ClassNotFoundException {
        // Manually set the dbOperator field in Receptionist to use mock
        receptionistInstance.dbOperator = dbOperator;

        // Mock data for lab appointment ID query
        ArrayList<ArrayList<String>> appointmentIdResult = new ArrayList<>();
        ArrayList<String> appointmentHeaders = new ArrayList<>();
        appointmentHeaders.add("lab_appointment_id");
        ArrayList<String> appointmentData = new ArrayList<>();
        appointmentData.add("lapp010");
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

        // Mock data for lab test fee query
        ArrayList<ArrayList<String>> testFeeResult = new ArrayList<>();
        ArrayList<String> testFeeHeaders = new ArrayList<>();
        testFeeHeaders.add("test_fee");
        ArrayList<String> testFeeData = new ArrayList<>();
        testFeeData.add("1000");
        testFeeResult.add(testFeeHeaders);
        testFeeResult.add(testFeeData);

        // Setup mock behavior for specific SQL queries
        // 1. Mock for max lab_appointment_id query
        when(dbOperator.customSelection(
                "SELECT lab_appointment_id FROM lab_appointment WHERE lab_appointment_id = (SELECT MAX(lab_appointment_id) FROM lab_appointment);"))
                .thenReturn(appointmentIdResult);

        // 2. Mock for existing tmp_bill query to throw exception
        when(dbOperator.customSelection(
                "SELECT tmp_bill_id FROM tmp_bill WHERE patient_id = 'hms0001pa';"))
                .thenThrow(new SQLException("Error processing short billID"));

        // 3. Mock for max tmp_bill_id query
        when(dbOperator.customSelection(
                "SELECT tmp_bill_id FROM tmp_bill WHERE tmp_bill_id = (SELECT MAX(tmp_bill_id) FROM tmp_bill);"))
                .thenReturn(maxBillResult);

        // 4. Mock for lab test fee query
        when(dbOperator.customSelection(
                "SELECT test_fee FROM lab_test WHERE test_id = 'test001';"))
                .thenReturn(testFeeResult);

        // Mock all database insertions to return true
        when(dbOperator.customInsertion(anyString())).thenReturn(true);

        // Call the method under test
        String result = receptionistInstance.makeLabAppointment("hms0001pa", "22387", "test001", "2", "09:00-12:00");

        // Verify the result indicates failure
        assertEquals("false", result, "Method should return 'false' when billID length is <= 3");

        // Verify no records were added to the database
        Statement stmt = connection.createStatement();

        // Check no lab appointment was created
        ResultSet rs = stmt
                .executeQuery("SELECT COUNT(*) as count FROM lab_appointment WHERE patient_id = 'hms0001pa'");
        rs.next();
        assertEquals(0, rs.getInt("count"), "No lab appointment record should be added when billID is invalid");

        // Check lab_appointment_timetable was not updated
        ResultSet availabilityRs = stmt.executeQuery(
                "SELECT current_week_appointments FROM lab_appointment_timetable " +
                        "WHERE app_test_id = 'test001' AND app_day = '2' AND time_slot = '09:00-12:00'");
        if (availabilityRs.next()) {
            assertEquals(0, availabilityRs.getInt("current_week_appointments"),
                    "Lab appointment timetable should not be updated when billID is invalid");
        }

        // Check no tmp_bill was created
        ResultSet billRs = stmt.executeQuery("SELECT COUNT(*) as count FROM tmp_bill WHERE patient_id = 'hms0001pa'");
        billRs.next();
        assertEquals(0, billRs.getInt("count"), "No temporary bill should be created when billID is invalid");
    }
    
        /*
     * RE_MLA_09
     * Purpose: Verify that the tmp_bill ID generation block executes without throwing an exception when creating a new tmp_bill, using real database operations
     * 
     * Test Data Setup:
     * - Patient ID: "hms0001pa"
     * - Doctor ID: "22387"
     * - Test ID: "test001"
     * - Day: "2" (Tuesday, current week)
     * - Time Slot: "09:00-12:00"
     * - Database records:
     *   - Existing lab_appointment: "lapp010"
     *   - Lab test: "test001" with fee "1000"
     *   - Lab appointment timetable for "test001" on day 2, time slot "09:00-12:00"
     *   - Existing tmp_bill: "hms0005tb" for a different patient
     *   - No tmp_bill for patient "hms0001pa"
     * 
     * Execution:
     * - Inserts necessary records into lab_appointment, lab_test, lab_appointment_timetable, and tmp_bill tables
     * - Calls makeLabAppointment with the test data
     * - Queries the database to verify the new tmp_bill creation
     * 
     * Expected Results:
     * 1. Returns new lab_appointment_id in format "lappXXX"
     * 2. The tmp_bill ID generation block executes without throwing an exception
     * 3. Creates new tmp_bill with tmp_bill_id "hms0006tb" and laboratory fee 1000
     * 
     * Tests Business Rules:
     * - Correct generation of new tmp_bill_id based on max existing ID
     * - Successful database operations within the tmp_bill ID generation block
     */
    @Test
    public void testMakeLabAppointment_TmpBillIdGenerationSuccess() throws SQLException {
        // Set up test data
        Statement stmt = connection.createStatement();

        // Insert lab test data
        stmt.executeUpdate("INSERT INTO lab_test (test_id, test_name, test_fee) " +
                "VALUES ('test001', 'Blood Test', '1000')");
        
        // Insert prerequisite data for lab_appointment (to generate next lab_appointment_id)
        stmt.executeUpdate(
                "INSERT INTO lab_appointment (lab_appointment_id, test_id, patient_id, doctor_id, date, cancelled) " +
                        "VALUES ('lapp010', 'test001', 'hms0002pa', '22387', '2024-03-19 08:00:00', false)");

        // Insert lab appointment timetable data
        stmt.executeUpdate(
                "INSERT INTO lab_appointment_timetable (app_test_id, app_day, time_slot, current_week_appointments) " +
                        "VALUES ('test001', '2', '09:00-12:00', 0)");

        // Insert an existing tmp_bill for a different patient to ensure max tmp_bill_id query returns a value
        stmt.executeUpdate("INSERT INTO tmp_bill (tmp_bill_id, patient_id, laboratory_fee) " +
                "VALUES ('hms0005tb', 'hms0002pa', '0')");

        // Ensure no tmp_bill exists for the test patient to trigger the block
        stmt.executeUpdate("DELETE FROM tmp_bill WHERE patient_id = 'hms0001pa'");

        // Call the method under test
        String result = receptionistInstance.makeLabAppointment("hms0001pa", "22387", "test001", "2", "09:00-12:00");

        // Verify the result is a valid lab_appointment_id
        assertTrue(result.matches("lapp\\d{3}"), "Result should be a valid lab_appointment_id");

        // Verify the new tmp_bill was created with the correct tmp_bill_id and laboratory fee
        ResultSet billRs = stmt.executeQuery(
                "SELECT tmp_bill_id, laboratory_fee FROM tmp_bill WHERE patient_id = 'hms0001pa'");
        assertTrue(billRs.next(), "New temporary bill record should exist");
        assertEquals("hms0006tb", billRs.getString("tmp_bill_id"),
                "New tmp_bill_id should be generated as hms0006tb");
        assertEquals("1000", billRs.getString("laboratory_fee"), "Laboratory fee should be updated to '1000'");

        // Verify the lab_appointment was created
        ResultSet appointmentRs = stmt.executeQuery(
                "SELECT * FROM lab_appointment WHERE lab_appointment_id = '" + result + "'");
        assertTrue(appointmentRs.next(), "Lab appointment record should exist");
        assertEquals("hms0001pa", appointmentRs.getString("patient_id"));
        assertEquals("22387", appointmentRs.getString("doctor_id"));
        assertEquals("test001", appointmentRs.getString("test_id"));
        assertFalse(appointmentRs.getBoolean("cancelled"));

        // Verify lab_appointment_timetable was updated
        ResultSet availabilityRs = stmt.executeQuery(
                "SELECT current_week_appointments FROM lab_appointment_timetable " +
                        "WHERE app_test_id = 'test001' AND app_day = '2' AND time_slot = '09:00-12:00'");
        assertTrue(availabilityRs.next(), "Lab appointment timetable record should exist");
        assertEquals(1, availabilityRs.getInt("current_week_appointments"),
                "Current week appointments should be incremented");
    }
}
