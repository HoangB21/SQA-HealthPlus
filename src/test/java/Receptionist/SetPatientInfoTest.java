/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.ArrayList;

/**
 * Test class for setPatientInfo method in Receptionist class.
 * 
 * Method Purpose:
 * - Creates new patient records in the system
 * - Generates unique identifiers for person and patient records
 * - Stores patient's personal and contact information
 * 
 * Method Parameters:
 * - patientInfo: String containing patient details in format:
 *   "nic VALUE,gender VALUE,date_of_birth YYYYMMDD,address VALUE,
 *    mobile VALUE,first_name VALUE,last_name VALUE,email VALUE"
 * 
 * Method Return Format:
 * - Success: Returns new person_id (format: "hmsXXXXX" where X is a digit)
 * - Failure: Returns "false"
 * 
 * Business Rules:
 * 1. Each patient must have unique identifiers:
 *    - person_id format: "hmsXXXXX"
 *    - patient_id format: "hmsXXXXpa"
 * 2. Required fields: NIC, gender, date_of_birth, first_name, last_name
 * 3. Optional fields: address, mobile, email
 * 4. Date format: YYYYMMDD for input, stored as YYYY-MM-DD
 * 5. Gender must be 'f' or 'm'
 * 6. NIC must be unique in the system
 */
public class SetPatientInfoTest {

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
     * RE_SP_01
     * Purpose: Verify successful creation of a new patient record with valid input data
     * 
     * Test Data Setup:
     * - Patient Info: 
     *   * NIC: "652489567V"
     *   * Gender: "f"
     *   * Date of Birth: "19950203"
     *   * Address: "145|town1|Street1"
     *   * Mobile: "0775123465"
     *   * First Name: "heshan"
     *   * Last Name: "eranga"
     *   * Email: "erangamx@gmail.com"
     * - Existing records:
     *   * Person with ID "hms00136" and NIC "652489765V"
     *   * Patient with ID "hms0036pa" linked to person "hms00136"
     * 
     * Expected Results:
     * 1. Returns new person_id in format "hmsXXXXX"
     * 2. Creates new person record with:
     *    - All provided personal information
     *    - Correctly formatted date (1995-02-03)
     *    - Valid gender value
     * 3. Creates new patient record with:
     *    - ID in format "hmsXXXXpa"
     *    - Correct link to new person_id
     * 
     * Tests Business Rules:
     * - Person and patient ID generation formats
     * - Required field validation
     * - Date format conversion
     * - Gender value validation
     * - Record linking between person and patient tables
     */
    @Test
    public void testSetPatientInfo_SuccessfulInsertion() throws SQLException {
        // Set up test data
        Statement stmt = connection.createStatement();

        // Insert dummy data to simulate existing records
        stmt.executeUpdate("INSERT INTO person (person_id, first_name, last_name, nic) " +
                "VALUES ('hms00136', 'John', 'Doe', '652489765V')");
        stmt.executeUpdate("INSERT INTO patient (patient_id, person_id) " +
                "VALUES ('hms0036pa', 'hms00136')");

        // Input for setPatientInfo method
        String patientInfo = "nic 652489567V,gender f,date_of_birth 19950203,address 145|town1|Street1," +
                "mobile 0775123465,first_name heshan,last_name eranga,email erangamx@gmail.com";

        // Call the method under test
        String result = receptionistInstance.setPatientInfo(patientInfo);

        // Verify the result is a valid person_id
        assertTrue(result.matches("hms\\d{5}"), "Result should be a valid person_id");

        // Query the database to check the new records
        ResultSet personRs = stmt.executeQuery("SELECT * FROM person WHERE person_id = '" + result + "'");

        // Verify person record was created correctly
        assertTrue(personRs.next(), "Person record should exist");
        assertEquals("heshan", personRs.getString("first_name"));
        assertEquals("eranga", personRs.getString("last_name"));
        assertEquals("652489567V", personRs.getString("nic"));
        assertEquals("f", personRs.getString("gender"));
        assertEquals("1995-02-03", personRs.getString("date_of_birth"));
        assertEquals("145|town1|Street1", personRs.getString("address"));
        assertEquals("0775123465", personRs.getString("mobile"));
        assertEquals("erangamx@gmail.com", personRs.getString("email"));

        // Verify patient record was created and linked correctly
        ResultSet patientRs = stmt.executeQuery("SELECT * FROM patient WHERE person_id = '" + result + "'");
        assertTrue(patientRs.next(), "Patient record should exist");
        assertTrue(patientRs.getString("patient_id").matches("hms\\d{4}pa"), "Patient ID should match expected format");
    }

    /*
     * RE_SP_02
     * Purpose: Verify error handling when database operations fail
     * 
     * Test Data Setup:
     * - Patient Info:
     *   * NIC: "199532648675"
     *   * Gender: "f"
     *   * Date of Birth: "19950203"
     *   * Address: "145|town1|Street1"
     *   * Mobile: "0775123465"
     *   * First Name: "heshan"
     *   * Last Name: "eranga"
     *   * Email: "erangamx@gmail.com"
     * - Mock DatabaseOperator to throw SQLException for all operations
     * 
     * Expected Results:
     * 1. Returns "false" to indicate failure
     * 2. No person record created
     * 3. No patient record created
     * 4. Database remains in original state
     * 
     * Tests Error Handling:
     * - Database operation failures
     * - Proper error state return value
     * - Data integrity preservation
     * - Transaction rollback functionality
     */
    @Test
    public void testSetPatientInfo_DatabaseError() throws SQLException, ClassNotFoundException {
        // Manually set the dbOperator field in Receptionist to use mock
        receptionistInstance.dbOperator = dbOperator;

        // Mock the customSelection method to throw SQLException
        when(dbOperator.customSelection(anyString())).thenThrow(new SQLException("Database error"));

        // Input for setPatientInfo method
        String patientInfo = "nic 199532648675,gender f,date_of_birth 19950203,address 145|town1|Street1," +
                "mobile 0775123465,first_name heshan,last_name eranga,email erangamx@gmail.com";

        // Call the method under test
        String result = receptionistInstance.setPatientInfo(patientInfo);

        // Verify the result indicates failure
        assertEquals("false", result, "Method should return 'false' when database error occurs");

        // Verify no records were added to the database
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM person WHERE nic = '199532648675'");
        rs.next();
        assertEquals(0, rs.getInt("count"), "No person record should be added");
    }

    /*
     * RE_SP_03
     * Purpose: Verify handling of incomplete or malformed input data
     * 
     * Test Data Setup:
     * - Patient Info with missing required fields:
     *   * Gender: "f"
     *   * Address: "145|town1|Street1"
     *   * Mobile: "0775123465"
     *   * Missing: NIC, date_of_birth, first_name, last_name
     * 
     * Expected Results:
     * 1. Returns "false" to indicate failure
     * 2. No person record created
     * 3. No patient record created
     * 4. Database remains unchanged
     * 
     * Tests Business Rules:
     * - Required field validation:
     *   * NIC is required
     *   * Date of birth is required
     *   * First and last names are required
     * - Data integrity protection
     * - Input validation before database operations
     */
    @Test
    public void testSetPatientInfo_MalformedInput() throws SQLException {
        // Input with missing required fields
        String patientInfo = "gender f,address 145|town1|Street1,mobile 0775123abc";

        // Call the method under test
        String result = receptionistInstance.setPatientInfo(patientInfo);

        // Verify the result indicates failure
        assertEquals("false", result, "Method should return 'false' for malformed input");

        // Verify no records were added to the database
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM person WHERE mobile = '0775123465'");
        rs.next();
        assertEquals(0, rs.getInt("count"), "No person record should be added");
    }

    /*
     * RE_SP_04
     * Purpose: Verify handling of invalid patient ID format
     * 
     * Test Data Setup:
     * - Patient Info:
     *   * NIC: "199532648675"
     *   * Gender: "f"
     *   * Date of Birth: "19950203"
     *   * Address: "145|town1|Street1"
     *   * Mobile: "0775123465"
     *   * First Name: "heshan"
     *   * Last Name: "eranga"
     *   * Email: "erangamx@gmail.com"
     * - Mock DatabaseOperator to return invalid patient ID:
     *   * Headers: ["patient_id"]
     *   * Data: ["hms"] (length <= 3)
     * 
     * Expected Results:
     * 1. Returns "false" to indicate failure
     * 2. No person record created
     * 3. No patient record created
     * 4. Database remains unchanged
     * 
     * Tests Business Rules:
     * - Patient ID format validation (must be longer than 3 characters)
     * - No database changes on validation failure
     * - Proper error handling for invalid ID format
     * - Data integrity preservation
     */
    @Test
    public void testSetPatientInfo_ShortPatientID() throws SQLException, ClassNotFoundException {
        // Manually set the dbOperator field in Receptionist to use mock
        receptionistInstance.dbOperator = dbOperator;

        // Create mock data with short patientID
        ArrayList<ArrayList<String>> mockResult = new ArrayList<>();
        ArrayList<String> headers = new ArrayList<>();
        headers.add("patient_id");
        ArrayList<String> data = new ArrayList<>();
        data.add("hms"); // patientID with length <= 3
        mockResult.add(headers);
        mockResult.add(data);

        // Mock the customSelection method to return the short patientID
        when(dbOperator.customSelection(anyString())).thenReturn(mockResult);

        // Input for setPatientInfo method
        String patientInfo = "nic 199532648675,gender f,date_of_birth 19950203,address 145|town1|Street1," +
                "mobile 0775123465,first_name heshan,last_name eranga,email erangamx@gmail.com";

        // Call the method under test
        String result = receptionistInstance.setPatientInfo(patientInfo);

        // Verify the result indicates failure
        assertEquals("false", result, "Method should return 'false' when patientID length is <= 3");

        // Verify no records were added to the database
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM person WHERE nic = '199532648675'");
        rs.next();
        assertEquals(0, rs.getInt("count"), "No person record should be added when patientID is invalid");
    }
}
