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

public class PharmacistAllNamesTest {

    private Pharmacist pharmacistInstance;

    @Mock
    private DatabaseOperator dbOperator;

    private AutoCloseable closeable;

    @BeforeEach
    public void setUp() throws SQLException, ClassNotFoundException {
        closeable = MockitoAnnotations.openMocks(this);
        doNothing().when(dbOperator).connectAndUseDatabase();

        ArrayList<ArrayList<String>> mockUserData = new ArrayList<>();
        ArrayList<String> userColumns = new ArrayList<>();
        userColumns.add("user_id");
        userColumns.add("user_type");
        mockUserData.add(userColumns);
        ArrayList<String> userData = new ArrayList<>();
        userData.add("hms0016u");
        userData.add("pharmacist");
        mockUserData.add(userData);

        ArrayList<ArrayList<String>> mockPharmacistData = new ArrayList<>();
        ArrayList<String> pharmacistColumns = new ArrayList<>();
        pharmacistColumns.add("pharmacist_id");
        mockPharmacistData.add(pharmacistColumns);
        ArrayList<String> pharmacistData = new ArrayList<>();
        pharmacistData.add("PH001");
        mockPharmacistData.add(pharmacistData);

        when(dbOperator.showTableData(eq("sys_user"), eq("user_id,user_type"), anyString()))
                .thenReturn(mockUserData);
        when(dbOperator.showTableData(eq("pharmacist"), eq("pharmacist_id"), anyString()))
                .thenReturn(mockPharmacistData);

        pharmacistInstance = new Pharmacist("hms0016u", dbOperator);
    }

    @AfterEach
    public void tearDown() throws Exception {
        closeable.close();
    }

    /* PH_NAMES_01
    Objective: Verify that the getAllNames method correctly retrieves patient names and details.
    Input: None
           Pre-test state: Mocked DatabaseOperator returns predefined patient data.
    Expected output: An ArrayList containing column names and patient data.
    Expected change: No database interaction.
     */
    @Test
    public void testGetAllNames_SuccessfulRetrieval() throws SQLException, ClassNotFoundException {
        ArrayList<ArrayList<String>> mockResult = new ArrayList<>();
        ArrayList<String> columns = new ArrayList<>();
        columns.add("patient_id");
        columns.add("first_name");
        columns.add("last_name");
        columns.add("date_of_birth");
        mockResult.add(columns);
        ArrayList<String> data = new ArrayList<>();
        data.add("PAT001");
        data.add("John");
        data.add("Doe");
        data.add("1990-01-01");
        mockResult.add(data);

        when(dbOperator.customSelection(anyString())).thenReturn(mockResult);

        ArrayList<ArrayList<String>> result = pharmacistInstance.getAllNames();

        assertNotNull(result, "The result should not be null when data is retrieved");
        assertEquals(2, result.size(), "The result should contain two rows (columns and data)");
        assertEquals("patient_id", result.get(0).get(0), "The first column name should be patient_id");
        assertEquals("PAT001", result.get(1).get(0), "The patient_id should match");
        assertEquals("John", result.get(1).get(1), "The first_name should match");
    }

    /* PH_NAMES_02
    Objective: Verify that the getAllNames method handles an SQLException during database operations
               and returns null.
    Input: None
           Pre-test state: Mocked SQLException.
           Mock behavior: dbOperator.customSelection throws an SQLException.
    Expected output: null
    Expected change: No database interaction.
     */
    @Test
    public void testGetAllNames_ThrowsSQLException() throws SQLException, ClassNotFoundException {
        when(dbOperator.customSelection(anyString())).thenThrow(new SQLException("Database error"));

        ArrayList<ArrayList<String>> result = pharmacistInstance.getAllNames();

        assertNull(result, "The result should be null when an SQLException is thrown");
    }

    /* PH_NAMES_03
    Objective: Verify that the getAllNames method handles a case where no patient names are returned.
    Input: None
           Pre-test state: Mocked result with only column names (no data rows).
    Expected output: An ArrayList containing only column names.
    Expected change: No database interaction.
     */
    @Test
    public void testGetAllNames_NoData() throws SQLException, ClassNotFoundException {
        ArrayList<ArrayList<String>> mockResult = new ArrayList<>();
        ArrayList<String> columns = new ArrayList<>();
        columns.add("patient_id");
        columns.add("first_name");
        columns.add("last_name");
        columns.add("date_of_birth");
        mockResult.add(columns);

        when(dbOperator.customSelection(anyString())).thenReturn(mockResult);

        ArrayList<ArrayList<String>> result = pharmacistInstance.getAllNames();

        assertNotNull(result, "The result should not be null when no data rows are returned");
        assertEquals(1, result.size(), "The result should contain one row (columns only)");
        assertEquals("patient_id", result.get(0).get(0), "The first column name should be patient_id");
    }
}