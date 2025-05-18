package Pharmacist;

import com.hms.hms_test_2.DatabaseOperator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

public class PharmacistProfileTest {

    private Pharmacist pharmacistInstance;

    @Mock
    private DatabaseOperator dbOperator;

    private AutoCloseable closeable;

    @BeforeEach
    public void setUp() throws SQLException, ClassNotFoundException {
        // Initialize Mockito annotations
        closeable = MockitoAnnotations.openMocks(this);

        // Mock connectAndUseDatabase
        doNothing().when(dbOperator).connectAndUseDatabase();

        // Mock showTableData for User constructor
        ArrayList<ArrayList<String>> mockUserData = new ArrayList<>();
        ArrayList<String> userColumns = new ArrayList<>();
        userColumns.add("user_id");
        userColumns.add("user_type");
        mockUserData.add(userColumns);
        ArrayList<String> userData = new ArrayList<>();
        userData.add("hms0016u");
        userData.add("pharmacist");
        mockUserData.add(userData);

        // Mock showTableData for Pharmacist constructor
        ArrayList<ArrayList<String>> mockPharmacistData = new ArrayList<>();
        ArrayList<String> pharmacistColumns = new ArrayList<>();
        pharmacistColumns.add("pharmacist_id");
        mockPharmacistData.add(pharmacistColumns);
        ArrayList<String> pharmacistData = new ArrayList<>();
        pharmacistData.add("PH001");
        mockPharmacistData.add(pharmacistData);

        // Mock behavior
        when(dbOperator.showTableData(eq("sys_user"), eq("user_id,user_type"), anyString()))
                .thenReturn(mockUserData);
        when(dbOperator.showTableData(eq("pharmacist"), eq("pharmacist_id"), anyString()))
                .thenReturn(mockPharmacistData);

        // Initialize Pharmacist with mocked dbOperator
        pharmacistInstance = new Pharmacist("user016", dbOperator);
    }

    @AfterEach
    public void tearDown() throws Exception {
        closeable.close();
    }

    /* PH_PR_01
    Objective: Verify that the getProfileInfo method correctly retrieves profile information from the database
               when provided with a valid userID and returns a HashMap with the expected key-value pairs.
    Input: userID = "hms0016u"
           Pre-test state: Mocked DatabaseOperator returns predefined data.
    Expected output: A HashMap containing key-value pairs for columns from sys_user, person, and pharmacist tables.
    Expected change: No database interaction.
     */
    @Test
    public void testGetProfileInfo_SuccessfulRetrieval() throws SQLException, ClassNotFoundException {
        // Prepare mock data for getProfileInfo
        ArrayList<ArrayList<String>> mockResult = new ArrayList<>();
        ArrayList<String> columns = new ArrayList<>();
        columns.add("user_id");
        columns.add("first_name");
        columns.add("last_name");
        columns.add("education");
        columns.add("person_id");
        columns.add("nic");
        columns.add("gender");
        mockResult.add(columns);
        ArrayList<String> data = new ArrayList<>();
        data.add("hms0016u");
        data.add("hms00096");
        data.add("user016");
        data.add("Master Pharmaceutical Sciences");
        data.add("P016");
        data.add("123456");
        data.add("M");
        mockResult.add(data);

        // Mock behavior for getProfileInfo
        when(dbOperator.customSelection(anyString())).thenReturn(mockResult);

        // Call the method
        HashMap<String, String> result = pharmacistInstance.getProfileInfo();

        // Verify results
        assertFalse(result.isEmpty(), "The HashMap should not be empty when data is retrieved");
        assertEquals("hms0016u", result.get("user_id"), "user_id should be hms0016u");
        assertEquals("hms00096", result.get("first_name"), "first_name should be hms00096");
        assertEquals("user016", result.get("last_name"), "last_name should be user016");
        assertEquals("Master Pharmaceutical Sciences", result.get("education"), "education should be Master Pharmaceutical Sciences");
        assertEquals("P016", result.get("person_id"), "person_id should be P016");
        assertEquals("123456", result.get("nic"), "nic should be 123456");
        assertEquals("M", result.get("gender"), "gender should be M");
    }

    /* PH_PR_02
    Objective: Verify that the getProfileInfo method handles an SQLException during database operations
               and returns an empty HashMap.
    Input: userID = "hms0016u"
           Pre-test state: Mocked SQLException.
           Mock behavior: dbOperator.customSelection throws an SQLException.
    Expected output: An empty HashMap.
    Expected change: No database interaction.
     */
    @Test
    public void testGetProfileInfo_ThrowsSQLException() throws SQLException, ClassNotFoundException {
        // Mock SQLException
        when(dbOperator.customSelection(anyString())).thenThrow(new SQLException("Database error"));

        HashMap<String, String> result = pharmacistInstance.getProfileInfo();

        assertTrue(result.isEmpty(), "The HashMap should be empty when an SQLException is thrown");
    }

    /* PH_PR_03
    Objective: Verify that the getProfileInfo method handles a case where no data is returned by the database
               and returns an empty HashMap.
    Input: userID = "hms0016u"
           Pre-test state: Mocked empty result.
           Mock behavior: dbOperator.customSelection returns an empty ArrayList.
    Expected output: An empty HashMap.
    Expected change: No database interaction.
     */
    @Test
    public void testGetProfileInfo_NoData() throws SQLException, ClassNotFoundException {
        // Mock empty result
        when(dbOperator.customSelection(anyString())).thenReturn(new ArrayList<>());

        HashMap<String, String> result = pharmacistInstance.getProfileInfo();

        assertTrue(result.isEmpty(), "The HashMap should be empty when no data is returned");
    }
}