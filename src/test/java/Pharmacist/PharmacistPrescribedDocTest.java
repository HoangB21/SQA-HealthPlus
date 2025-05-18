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

public class PharmacistPrescribedDocTest {

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

    /* PH_DOC_01
    Objective: Verify that the getPrescribedDoc method correctly retrieves the consultant ID
               for a given patientID with the latest date.
    Input: patientID = "PAT001"
           Pre-test state: Mocked DatabaseOperator returns predefined data with consultant_id.
    Expected output: An ArrayList containing column names and consultant ID data.
    Expected change: No database interaction.
     */
    @Test
    public void testGetPrescribedDoc_SuccessfulRetrieval() throws SQLException, ClassNotFoundException {
        // Prepare mock data
        ArrayList<ArrayList<String>> mockResult = new ArrayList<>();
        ArrayList<String> columns = new ArrayList<>();
        columns.add("consultant_id");
        mockResult.add(columns);
        ArrayList<String> data = new ArrayList<>();
        data.add("DOC001");
        mockResult.add(data);

        // Mock behavior
        when(dbOperator.customSelection(anyString())).thenReturn(mockResult);

        // Call the method
        ArrayList<ArrayList<String>> result = pharmacistInstance.getPrescribedDoc("PAT001");

        // Verify results
        assertNotNull(result, "The result should not be null when data is retrieved");
        assertEquals(2, result.size(), "The result should contain two rows (columns and data)");
        assertEquals("consultant_id", result.get(0).get(0), "The column name should be consultant_id");
        assertEquals("DOC001", result.get(1).get(0), "The consultant_id should match");
    }

    /* PH_DOC_02
    Objective: Verify that the getPrescribedDoc method handles an SQLException during database operations
               and returns null.
    Input: patientID = "PAT001"
           Pre-test state: Mocked SQLException.
           Mock behavior: dbOperator.customSelection throws an SQLException.
    Expected output: null
    Expected change: No database interaction.
     */
    @Test
    public void testGetPrescribedDoc_ThrowsSQLException() throws SQLException, ClassNotFoundException {
        // Mock SQLException
        when(dbOperator.customSelection(anyString())).thenThrow(new SQLException("Database error"));

        // Call the method
        ArrayList<ArrayList<String>> result = pharmacistInstance.getPrescribedDoc("PAT001");

        // Verify results
        assertNull(result, "The result should be null when an SQLException is thrown");
    }

    /* PH_DOC_03
    Objective: Verify that the getPrescribedDoc method handles a case where no consultant ID is returned
               for the given patientID and latest date.
    Input: patientID = "PAT001"
           Pre-test state: Mocked result with only column names (no data rows).
    Expected output: An ArrayList containing only column names.
    Expected change: No database interaction.
     */
    @Test
    public void testGetPrescribedDoc_NoData() throws SQLException, ClassNotFoundException {
        // Mock result with only column names
        ArrayList<ArrayList<String>> mockResult = new ArrayList<>();
        ArrayList<String> columns = new ArrayList<>();
        columns.add("consultant_id");
        mockResult.add(columns);

        // Mock behavior
        when(dbOperator.customSelection(anyString())).thenReturn(mockResult);

        // Call the method
        ArrayList<ArrayList<String>> result = pharmacistInstance.getPrescribedDoc("PAT001");

        // Verify results
        assertNotNull(result, "The result should not be null when no data rows are returned");
        assertEquals(1, result.size(), "The result should contain one row (columns only)");
        assertEquals("consultant_id", result.get(0).get(0), "The column name should be consultant_id");
    }
}