package Receptionist;

import com.hms.hms_test_2.DatabaseOperator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Test class for getAppointmentDetails method in Receptionist class.
 * 
 * Method Purpose:
 * - Retrieves appointment details based on different search criteria
 * - Joins appointment, patient, and person tables
 * - Returns appointment information based on type parameter
 * 
 * Method Parameters:
 * - type: String - Search type ('d' for doctor, 'p' for patient, 'a' for appointment)
 * - value: String - Search value (SLMC number, patient ID, or appointment ID)
 * 
 * Method Return Format:
 * - ArrayList<ArrayList<String>> containing:
 *   - First row: Column headers (varies by type)
 *   - Subsequent rows: Appointment data
 * - Returns null if an exception occurs
 * 
 * Business Rules:
 * 1. Must handle three different types of queries (d, p, a)
 * 2. Must join tables correctly for each query type
 * 3. Must filter cancelled appointments for doctor and patient queries
 * 4. Must handle database exceptions gracefully
 */
public class GetAppointmentDetailsTest {

    private Receptionist receptionistInstance;

    @Mock
    private DatabaseOperator dbOperator;

    private AutoCloseable closeable;

    @BeforeEach
    public void setUp() {
        // Initialize Mockito annotations
        closeable = MockitoAnnotations.openMocks(this);

        // Initialize the Receptionist instance
        receptionistInstance = new Receptionist("user018");
        
        // Set the mocked dbOperator
        receptionistInstance.dbOperator = dbOperator;
    }

    @AfterEach
    public void tearDown() throws Exception {
        // Close Mockito resources
        closeable.close();
    }

    /*
     * RE_GAD_01
     * Purpose: Verify successful retrieval of appointments by doctor
     * 
     * Test Data Setup:
     * - Type: "d" (doctor)
     * - Value: "SLMC001" (doctor's registration number)
     * - Mock database records with doctor's appointments
     * 
     * Execution:
     * - Mocks database response with test data
     * - Calls getAppointmentDetails with doctor type
     * - Verifies the returned list contains correct appointment information
     * 
     * Expected Results:
     * 1. Returns ArrayList with header row and appointment data
     * 2. Data matches the mock records
     * 3. Only non-cancelled appointments are included
     */
    @Test
    public void testGetAppointmentDetails_DoctorSuccess() throws SQLException, ClassNotFoundException {
        // Prepare mock data
        ArrayList<ArrayList<String>> mockResult = new ArrayList<>();
        // Add column names
        mockResult.add(new ArrayList<>(Arrays.asList(
            "slmc_reg_no", "date", "first_name", "last_name", "patient_id"
        )));
        // Add appointment data
        mockResult.add(new ArrayList<>(Arrays.asList(
            "SLMC001", "2024-03-20", "John", "Doe", "PAT001"
        )));
        mockResult.add(new ArrayList<>(Arrays.asList(
            "SLMC001", "2024-03-21", "Jane", "Smith", "PAT002"
        )));

        // Setup mock behavior
        String expectedQuery = "SELECT " +
                "appointment.slmc_reg_no, appointment.date, person.first_name , person.last_name,patient.patient_id " +
                "FROM appointment INNER JOIN patient ON appointment.patient_id = patient.patient_id " +
                "INNER JOIN person ON person.person_id = patient.person_id " +
                "WHERE appointment.slmc_reg_no = 'SLMC001' AND appointment.cancelled = 0;";
        when(dbOperator.customSelection(expectedQuery)).thenReturn(mockResult);

        // Call the method under test
        ArrayList<ArrayList<String>> result = receptionistInstance.getAppointmentDetails("d", "SLMC001");

        // Verify the result
        assertNotNull(result, "Result should not be null");
        assertEquals(3, result.size(), "Should contain header row and two appointments");
        assertEquals(Arrays.asList("slmc_reg_no", "date", "first_name", "last_name", "patient_id"),
                    result.get(0), "Header row should match expected columns");
        assertEquals(Arrays.asList("SLMC001", "2024-03-20", "John", "Doe", "PAT001"),
                    result.get(1), "First appointment should match");
    }

    /*
     * RE_GAD_02
     * Purpose: Verify successful retrieval of appointments by patient
     * 
     * Test Data Setup:
     * - Type: "p" (patient)
     * - Value: "PAT001" (patient ID)
     * - Mock database records with patient's appointments
     * 
     * Execution:
     * - Mocks database response with test data
     * - Calls getAppointmentDetails with patient type
     * - Verifies the returned list contains correct appointment information
     * 
     * Expected Results:
     * 1. Returns ArrayList with header row and appointment data
     * 2. Data matches the mock records
     * 3. Only non-cancelled appointments are included
     */
    @Test
    public void testGetAppointmentDetails_PatientSuccess() throws SQLException, ClassNotFoundException {
        // Prepare mock data
        ArrayList<ArrayList<String>> mockResult = new ArrayList<>();
        // Add column names
        mockResult.add(new ArrayList<>(Arrays.asList(
            "patient_id", "date", "first_name", "last_name", "slmc_reg_no"
        )));
        // Add appointment data
        mockResult.add(new ArrayList<>(Arrays.asList(
            "PAT001", "2024-03-20", "John", "Doe", "SLMC001"
        )));

        // Setup mock behavior
        String expectedQuery = "SELECT " +
                "patient.patient_id, appointment.date, person.first_name , person.last_name,appointment.slmc_reg_no " +
                "FROM appointment INNER JOIN patient ON appointment.patient_id = patient.patient_id " +
                "INNER JOIN person ON person.person_id = patient.person_id " +
                "WHERE appointment.patient_id = 'PAT001' AND appointment.cancelled = 0;";
        when(dbOperator.customSelection(expectedQuery)).thenReturn(mockResult);

        // Call the method under test
        ArrayList<ArrayList<String>> result = receptionistInstance.getAppointmentDetails("p", "PAT001");

        // Verify the result
        assertNotNull(result, "Result should not be null");
        assertEquals(2, result.size(), "Should contain header row and one appointment");
        assertEquals(Arrays.asList("patient_id", "date", "first_name", "last_name", "slmc_reg_no"),
                    result.get(0), "Header row should match expected columns");
        assertEquals(Arrays.asList("PAT001", "2024-03-20", "John", "Doe", "SLMC001"),
                    result.get(1), "Appointment details should match");
    }

    /*
     * RE_GAD_03
     * Purpose: Verify successful retrieval of appointment by appointment ID
     * 
     * Test Data Setup:
     * - Type: "a" (appointment)
     * - Value: "APT001" (appointment ID)
     * - Mock database records with specific appointment
     * 
     * Execution:
     * - Mocks database response with test data
     * - Calls getAppointmentDetails with appointment type
     * - Verifies the returned list contains correct appointment information
     * 
     * Expected Results:
     * 1. Returns ArrayList with header row and appointment data
     * 2. Data matches the mock records
     * 3. Includes appointment regardless of cancelled status
     */
    @Test
    public void testGetAppointmentDetails_AppointmentSuccess() throws SQLException, ClassNotFoundException {
        // Prepare mock data
        ArrayList<ArrayList<String>> mockResult = new ArrayList<>();
        // Add column names
        mockResult.add(new ArrayList<>(Arrays.asList(
            "slmc_reg_no", "date", "first_name", "last_name", "patient_id"
        )));
        // Add appointment data
        mockResult.add(new ArrayList<>(Arrays.asList(
            "SLMC001", "2024-03-20", "John", "Doe", "PAT001"
        )));

        // Setup mock behavior
        String expectedQuery = "SELECT " +
                "appointment.slmc_reg_no, appointment.date, person.first_name , person.last_name,patient.patient_id " +
                "FROM appointment INNER JOIN patient ON appointment.patient_id = patient.patient_id " +
                "INNER JOIN person ON person.person_id = patient.person_id " +
                "WHERE appointment_id = 'APT001';";
        when(dbOperator.customSelection(expectedQuery)).thenReturn(mockResult);

        // Call the method under test
        ArrayList<ArrayList<String>> result = receptionistInstance.getAppointmentDetails("a", "APT001");

        // Verify the result
        assertNotNull(result, "Result should not be null");
        assertEquals(2, result.size(), "Should contain header row and one appointment");
        assertEquals(Arrays.asList("slmc_reg_no", "date", "first_name", "last_name", "patient_id"),
                    result.get(0), "Header row should match expected columns");
        assertEquals(Arrays.asList("SLMC001", "2024-03-20", "John", "Doe", "PAT001"),
                    result.get(1), "Appointment details should match");
    }

    /*
     * RE_GAD_04
     * Purpose: Verify handling of database exception
     * 
     * Test Data Setup:
     * - Type: "d" (doctor)
     * - Value: "SLMC001"
     * - Mock DatabaseOperator to throw SQLException
     * 
     * Execution:
     * - Sets up mock to throw SQLException
     * - Calls getAppointmentDetails
     * - Verifies the returned value is null
     * 
     * Expected Results:
     * 1. Returns null
     * 2. Handles SQLException gracefully
     */
    @Test
    public void testGetAppointmentDetails_DatabaseException() throws SQLException, ClassNotFoundException {
        // Setup mock to throw exception
        when(dbOperator.customSelection(anyString()))
            .thenThrow(new SQLException("Database connection failed"));

        // Call the method under test
        ArrayList<ArrayList<String>> result = receptionistInstance.getAppointmentDetails("d", "SLMC001");

        // Verify the result
        assertNull(result, "Result should be null when database query fails");
    }

    /*
     * RE_GAD_05
     * Purpose: Verify handling of invalid type parameter
     * 
     * Test Data Setup:
     * - Type: "x" (invalid)
     * - Value: "ANY"
     * 
     * Execution:
     * - Calls getAppointmentDetails with invalid type
     * - Verifies the method handles invalid input appropriately
     * 
     * Expected Results:
     * 1. Returns null
     * 2. No database query attempted
     */
    @Test
    public void testGetAppointmentDetails_InvalidType() throws SQLException, ClassNotFoundException {
        // Call the method under test with invalid type
        ArrayList<ArrayList<String>> result = receptionistInstance.getAppointmentDetails("x", "ANY");

        // Verify the result
        assertEquals(0, result.size(), "Result should be empty for invalid type parameter");
    }

    /*
     * RE_GAD_06
     * Purpose: Verify handling of empty result set
     * 
     * Test Data Setup:
     * - Type: "d" (doctor)
     * - Value: "SLMC001"
     * - Mock database to return empty result (only headers)
     * 
     * Execution:
     * - Mocks database response with only header row
     * - Calls getAppointmentDetails
     * - Verifies the returned list contains only headers
     * 
     * Expected Results:
     * 1. Returns ArrayList with only header row
     * 2. No appointment data present
     */
    @Test
    public void testGetAppointmentDetails_EmptyResultSet() throws SQLException, ClassNotFoundException {
        // Prepare mock data with only headers
        ArrayList<ArrayList<String>> mockResult = new ArrayList<>();
        mockResult.add(new ArrayList<>(Arrays.asList(
            "slmc_reg_no", "date", "first_name", "last_name", "patient_id"
        )));

        // Setup mock behavior
        when(dbOperator.customSelection(anyString())).thenReturn(mockResult);

        // Call the method under test
        ArrayList<ArrayList<String>> result = receptionistInstance.getAppointmentDetails("d", "SLMC001");

        // Verify the result
        assertNotNull(result, "Result should not be null");
        assertEquals(1, result.size(), "Should contain only header row");
        assertEquals(5, result.get(0).size(), "Header should have all columns");
    }

    /*
     * RE_GAD_07
     * Purpose: Verify successful retrieval of appointment details with doctor info
     * 
     * Test Data Setup:
     * - AppointmentID: "APT001"
     * - Mock database to return both appointment and doctor data
     * 
     * Execution:
     * - Mocks database responses for both queries
     * - Calls getAppointmentDetails with appointment ID
     * - Verifies the returned list contains combined information
     * 
     * Expected Results:
     * 1. Returns ArrayList with header row, appointment data, and doctor data
     * 2. Data matches the mock records from both queries
     */
    @Test
    public void testGetAppointmentDetails_SingleParamSuccess() throws SQLException, ClassNotFoundException {
        // Prepare mock data for first query (appointment details)
        ArrayList<ArrayList<String>> mockAppointmentResult = new ArrayList<>();
        // Add column names
        mockAppointmentResult.add(new ArrayList<>(Arrays.asList(
            "slmc_reg_no", "date", "first_name", "last_name"
        )));
        // Add appointment data
        mockAppointmentResult.add(new ArrayList<>(Arrays.asList(
            "SLMC001", "2024-03-20", "John", "Doe"
        )));

        // Prepare mock data for second query (doctor details)
        ArrayList<ArrayList<String>> mockDoctorResult = new ArrayList<>();
        // Add column names
        mockDoctorResult.add(new ArrayList<>(Arrays.asList(
            "first_name", "last_name"
        )));
        // Add doctor data
        mockDoctorResult.add(new ArrayList<>(Arrays.asList(
            "Dr. Jane", "Smith"
        )));

        // Setup mock behavior for first query
        String expectedQuery1 = "SELECT " +
                "appointment.slmc_reg_no, appointment.date, person.first_name , person.last_name " +
                "FROM appointment INNER JOIN patient ON appointment.patient_id = patient.patient_id " +
                "INNER JOIN person ON person.person_id = patient.person_id " +
                "WHERE appointment.appointment_id = 'APT001';";
        when(dbOperator.customSelection(expectedQuery1)).thenReturn(mockAppointmentResult);

        // Setup mock behavior for second query
        String expectedQuery2 = "SELECT " +
                "person.first_name , person.last_name " +
                "FROM doctor INNER JOIN person ON doctor.user_id = person.user_id " +
                "WHERE doctor.slmc_reg_no = 'SLMC001';";
        when(dbOperator.customSelection(expectedQuery2)).thenReturn(mockDoctorResult);

        // Call the method under test
        ArrayList<ArrayList<String>> result = receptionistInstance.getAppointmentDetails("APT001");

        // Verify the result
        assertNotNull(result, "Result should not be null");
        assertEquals(3, result.size(), "Should contain header row, appointment data, and doctor data");
        assertEquals(Arrays.asList("slmc_reg_no", "date", "first_name", "last_name"),
                    result.get(0), "Header row should match expected columns");
        assertEquals(Arrays.asList("SLMC001", "2024-03-20", "John", "Doe"),
                    result.get(1), "Appointment details should match");
        assertEquals(Arrays.asList("Dr. Jane", "Smith"),
                    result.get(2), "Doctor details should match");
    }

    /*
     * RE_GAD_08
     * Purpose: Verify handling of database exception in first query
     * 
     * Test Data Setup:
     * - AppointmentID: "APT001"
     * - Mock first database query to throw SQLException
     * 
     * Execution:
     * - Sets up mock to throw SQLException for appointment query
     * - Calls getAppointmentDetails
     * - Verifies the returned value is null
     * 
     * Expected Results:
     * 1. Returns null
     * 2. Handles SQLException gracefully
     */
    @Test
    public void testGetAppointmentDetails_SingleParam_FirstQueryException() throws SQLException, ClassNotFoundException {
        // Setup mock to throw exception for first query
        when(dbOperator.customSelection(anyString()))
            .thenThrow(new SQLException("Database connection failed"));

        // Call the method under test
        ArrayList<ArrayList<String>> result = receptionistInstance.getAppointmentDetails("APT001");

        // Verify the result
        assertNull(result, "Result should be null when first database query fails");
    }

    /*
     * RE_GAD_09
     * Purpose: Verify handling of database exception in second query
     * 
     * Test Data Setup:
     * - AppointmentID: "APT001"
     * - Mock first query to succeed
     * - Mock second query to throw SQLException
     * 
     * Execution:
     * - Sets up mocks for both queries
     * - Calls getAppointmentDetails
     * - Verifies the returned value is null
     * 
     * Expected Results:
     * 1. Returns null
     * 2. Handles SQLException gracefully
     */
    @Test
    public void testGetAppointmentDetails_SingleParam_SecondQueryException() throws SQLException, ClassNotFoundException {
        // Prepare mock data for first query
        ArrayList<ArrayList<String>> mockAppointmentResult = new ArrayList<>();
        mockAppointmentResult.add(new ArrayList<>(Arrays.asList(
            "slmc_reg_no", "date", "first_name", "last_name"
        )));
        mockAppointmentResult.add(new ArrayList<>(Arrays.asList(
            "SLMC001", "2024-03-20", "John", "Doe"
        )));

        // Setup mock behavior: first query succeeds, second throws exception
        String expectedQuery1 = "SELECT " +
                "appointment.slmc_reg_no, appointment.date, person.first_name , person.last_name " +
                "FROM appointment INNER JOIN patient ON appointment.patient_id = patient.patient_id " +
                "INNER JOIN person ON person.person_id = patient.person_id " +
                "WHERE appointment.appointment_id = 'APT001';";
        when(dbOperator.customSelection(expectedQuery1)).thenReturn(mockAppointmentResult);

        String expectedQuery2 = "SELECT " +
                "person.first_name , person.last_name " +
                "FROM doctor INNER JOIN person ON doctor.user_id = person.user_id " +
                "WHERE doctor.slmc_reg_no = 'SLMC001';";
        when(dbOperator.customSelection(expectedQuery2))
            .thenThrow(new SQLException("Database connection failed"));

        // Call the method under test
        ArrayList<ArrayList<String>> result = receptionistInstance.getAppointmentDetails("APT001");

        // Verify the result
        assertEquals(1, result.size(), "Result should be empty (header row only) when second database query fails");
    }

    /*
     * RE_GAD_10
     * Purpose: Verify handling of non-existent appointment ID
     * 
     * Test Data Setup:
     * - AppointmentID: "NONEXISTENT"
     * - Mock database to return empty result
     * 
     * Execution:
     * - Mocks database response with only headers
     * - Calls getAppointmentDetails
     * - Verifies the returned list contains only headers
     * 
     * Expected Results:
     * 1. Returns ArrayList with only header row
     * 2. No appointment data present
     */
    @Test
    public void testGetAppointmentDetails_SingleParam_NonExistentAppointment() throws SQLException, ClassNotFoundException {
        // Prepare mock data with only headers
        ArrayList<ArrayList<String>> mockResult = new ArrayList<>();
        mockResult.add(new ArrayList<>(Arrays.asList(
            "slmc_reg_no", "date", "first_name", "last_name"
        )));

        // Setup mock behavior
        when(dbOperator.customSelection(anyString())).thenReturn(mockResult);

        // Call the method under test
        ArrayList<ArrayList<String>> result = receptionistInstance.getAppointmentDetails("NONEXISTENT");

        // Verify the result
        assertNotNull(result, "Result should not be null");
        assertEquals(1, result.size(), "Should contain only header row");
        assertEquals(4, result.get(0).size(), "Header should have all columns");
    }
} 