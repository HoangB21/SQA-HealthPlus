package Pharmacist;

import com.hms.hms_test_2.DatabaseOperator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.SQLException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

public class PharmacistPharmacyHistoryTest {

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
        pharmacistInstance = new Pharmacist("hms0016u", dbOperator);
    }

    @AfterEach
    public void tearDown() throws Exception {
        closeable.close();
    }

    /* PH_HIS_01
    Objective: Verify that the getpharmacyHistory method correctly retrieves pharmacy history
               for the specified number of rows.
    Input: rows = 5
           Pre-test state: Mocked DatabaseOperator returns predefined pharmacy history data.
    Expected output: An ArrayList containing column names and pharmacy history data.
    Expected change: No database interaction.
     */
    @Test
    public void testGetPharmacyHistory_SuccessfulRetrieval() throws SQLException, ClassNotFoundException {
        // Prepare mock data
        ArrayList<ArrayList<String>> mockResult = new ArrayList<>();
        ArrayList<String> columns = new ArrayList<>();
        columns.add("date");
        columns.add("no_of_drugs");
        columns.add("excluded");
        columns.add("bill_id");
        columns.add("pharmacy_fee");
        columns.add("drugs_dose");
        columns.add("patient_id");
        columns.add("first_name");
        columns.add("last_name");
        mockResult.add(columns);
        ArrayList<String> data = new ArrayList<>();
        data.add("2025-05-15");
        data.add("3");
        data.add("None");
        data.add("BILL001");
        data.add("50.00");
        data.add("Paracetamol 500mg, twice daily");
        data.add("PAT001");
        data.add("John");
        data.add("Doe");
        mockResult.add(data);

        // Mock behavior
        when(dbOperator.customSelection(anyString())).thenReturn(mockResult);

        // Call the method
        ArrayList<ArrayList<String>> result = pharmacistInstance.getpharmacyHistory(5);

        // Verify results
        assertNotNull(result, "The result should not be null when data is retrieved");
        assertEquals(2, result.size(), "The result should contain two rows (columns and data)");
        assertEquals(9, result.get(0).size(), "The column names should include 9 fields");
        assertEquals("date", result.get(0).get(0), "The first column name should be date");
        assertEquals("2025-05-15", result.get(1).get(0), "The date should match");
        assertEquals("PAT001", result.get(1).get(6), "The patient_id should match");
    }

    /* PH_HIS_02
    Objective: Verify that the getpharmacyHistory method handles an SQLException during database operations
               and returns null.
    Input: rows = 5
           Pre-test state: Mocked SQLException.
           Mock behavior: dbOperator.customSelection throws an SQLException.
    Expected output: null
    Expected change: No database interaction.
     */
    @Test
    public void testGetPharmacyHistory_ThrowsSQLException() throws SQLException, ClassNotFoundException {
        // Mock SQLException
        when(dbOperator.customSelection(anyString())).thenThrow(new SQLException("Database error"));

        // Call the method
        ArrayList<ArrayList<String>> result = pharmacistInstance.getpharmacyHistory(5);

        // Verify results
        assertNull(result, "The result should be null when an SQLException is thrown");
    }

    /* PH_HIS_03
    Objective: Verify that the getpharmacyHistory method handles a case where no pharmacy history is returned
               for the specified number of rows.
    Input: rows = 5
           Pre-test state: Mocked result with only column names (no data rows).
    Expected output: An ArrayList containing only column names.
    Expected change: No database interaction.
     */
    @Test
    public void testGetPharmacyHistory_NoData() throws SQLException, ClassNotFoundException {
        // Mock result with only column names
        ArrayList<ArrayList<String>> mockResult = new ArrayList<>();
        ArrayList<String> columns = new ArrayList<>();
        columns.add("date");
        columns.add("no_of_drugs");
        columns.add("excluded");
        columns.add("bill_id");
        columns.add("pharmacy_fee");
        columns.add("drugs_dose");
        columns.add("patient_id");
        columns.add("first_name");
        columns.add("last_name");
        mockResult.add(columns);

        // Mock behavior
        when(dbOperator.customSelection(anyString())).thenReturn(mockResult);

        // Call the method
        ArrayList<ArrayList<String>> result = pharmacistInstance.getpharmacyHistory(5);

        // Verify results
        assertNotNull(result, "The result should not be null when no data rows are returned");
        assertEquals(1, result.size(), "The result should contain one row (columns only)");
        assertEquals(9, result.get(0).size(), "The column names should include 9 fields");
        assertEquals("date", result.get(0).get(0), "The first column name should be date");
    }
}