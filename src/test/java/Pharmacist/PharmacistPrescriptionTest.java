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

public class PharmacistPrescriptionTest {

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

    /* PH_RX_01
    Objective: Verify that the getPrescriptionInfo method correctly retrieves prescription information
               for a given patientID with the latest date.
    Input: patientID = "PAT001"
           Pre-test state: Mocked DatabaseOperator returns predefined data with drugs_dose.
    Expected output: An ArrayList containing column names and prescription data.
    Expected change: No database interaction.
     */
    @Test
    public void testGetPrescriptionInfo_SuccessfulRetrieval() throws SQLException, ClassNotFoundException {
        // Prepare mock data for getPrescriptionInfo
        ArrayList<ArrayList<String>> mockResult = new ArrayList<>();
        ArrayList<String> columns = new ArrayList<>();
        columns.add("drugs_dose");
        mockResult.add(columns);
        ArrayList<String> data = new ArrayList<>();
        data.add("Paracetamol 500mg, twice daily");
        mockResult.add(data);

        // Mock behavior for getPrescriptionInfo
        when(dbOperator.customSelection(anyString())).thenReturn(mockResult);

        // Call the method
        ArrayList<ArrayList<String>> result = pharmacistInstance.getPrescriptionInfo("PAT001");

        // Verify results
        assertNotNull(result, "The result should not be null when data is retrieved");
        assertEquals(2, result.size(), "The result should contain two rows (columns and data)");
        assertEquals("drugs_dose", result.get(0).get(0), "The column name should be drugs_dose");
        assertEquals("Paracetamol 500mg, twice daily", result.get(1).get(0), "The drugs_dose should match");
    }

    /* PH_RX_02
    Objective: Verify that the getPrescriptionInfo method handles an SQLException during database operations
               and returns null.
    Input: patientID = "PAT001"
           Pre-test state: Mocked SQLException.
           Mock behavior: dbOperator.customSelection throws an SQLException.
    Expected output: null
    Expected change: No database interaction.
     */
    @Test
    public void testGetPrescriptionInfo_ThrowsSQLException() throws SQLException, ClassNotFoundException {
        // Mock SQLException
        when(dbOperator.customSelection(anyString())).thenThrow(new SQLException("Database error"));

        // Call the method
        ArrayList<ArrayList<String>> result = pharmacistInstance.getPrescriptionInfo("PAT001");

        // Verify results
        assertNull(result, "The result should be null when an SQLException is thrown");
    }

    /* PH_RX_03
    Objective: Verify that the getPrescriptionInfo method handles a case where no prescription data is returned
               for the given patientID and latest date.
    Input: patientID = "PAT001"
           Pre-test state: Mocked result with only column names (no data rows).
    Expected output: An ArrayList containing only column names.
    Expected change: No database interaction.
     */
    @Test
    public void testGetPrescriptionInfo_NoData() throws SQLException, ClassNotFoundException {
        // Mock result with only column names (no data rows)
        ArrayList<ArrayList<String>> mockResult = new ArrayList<>();
        ArrayList<String> columns = new ArrayList<>();
        columns.add("drugs_dose");
        mockResult.add(columns);

        // Mock behavior for getPrescriptionInfo
        when(dbOperator.customSelection(anyString())).thenReturn(mockResult);

        // Call the method
        ArrayList<ArrayList<String>> result = pharmacistInstance.getPrescriptionInfo("PAT001");

        // Verify results
        assertNotNull(result, "The result should not be null when no data rows are returned");
        assertEquals(1, result.size(), "The result should contain one row (columns only)");
        assertEquals("drugs_dose", result.get(0).get(0), "The column name should be drugs_dose");
    }
}