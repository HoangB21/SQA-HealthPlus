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

public class PharmacistDrugAmountsTest {

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
        userData.add("hms016");
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

        pharmacistInstance = new Pharmacist("hms016", dbOperator);
    }

    @AfterEach
    public void tearDown() throws Exception {
        closeable.close();
    }

    /* PH_DRUG_AMOUNTS_01
    Objective: Verify that the getDrugAmounts method correctly retrieves remaining quantities for a given brandID.
    Input: brandID = "br0001"
           Pre-test state: Mocked DatabaseOperator returns predefined stock data.
    Expected output: An ArrayList containing column names and remaining quantities.
    Expected change: No database interaction.
     */
    @Test
    public void testGetDrugAmounts_SuccessfulRetrieval() throws SQLException, ClassNotFoundException {
        ArrayList<ArrayList<String>> mockResult = new ArrayList<>();
        ArrayList<String> columns = new ArrayList<>();
        columns.add("remaining_quantity");
        mockResult.add(columns);
        ArrayList<String> data = new ArrayList<>();
        data.add("450");
        mockResult.add(data);

        when(dbOperator.customSelection(anyString())).thenReturn(mockResult);

        ArrayList<ArrayList<String>> result = pharmacistInstance.getDrugAmounts("br0001");

        assertNotNull(result, "The result should not be null when data is retrieved");
        assertEquals(2, result.size(), "The result should contain two rows (columns and data)");
        assertEquals("450", result.get(1).get(0), "The remaining quantity should match");
    }

    /* PH_DRUG_AMOUNTS_02
    Objective: Verify that the getDrugAmounts method handles an SQLException and returns null.
    Input: brandID = "br0001"
           Pre-test state: Mocked SQLException.
           Mock behavior: dbOperator.customSelection throws an SQLException.
    Expected output: null
    Expected change: No database interaction.
     */
    @Test
    public void testGetDrugAmounts_ThrowsSQLException() throws SQLException, ClassNotFoundException {
        when(dbOperator.customSelection(anyString())).thenThrow(new SQLException("Database error"));

        ArrayList<ArrayList<String>> result = pharmacistInstance.getDrugAmounts("br0001");

        assertNull(result, "The result should be null when an SQLException is thrown");
    }

    /* PH_DRUG_AMOUNTS_03
    Objective: Verify that the getDrugAmounts method handles a case where no stock data is returned.
    Input: brandID = "br0001"
           Pre-test state: Mocked result with only column names (no data rows).
    Expected output: An ArrayList containing only column names.
    Expected change: No database interaction.
     */
    @Test
    public void testGetDrugAmounts_NoData() throws SQLException, ClassNotFoundException {
        ArrayList<ArrayList<String>> mockResult = new ArrayList<>();
        ArrayList<String> columns = new ArrayList<>();
        columns.add("remaining_quantity");
        mockResult.add(columns);

        when(dbOperator.customSelection(anyString())).thenReturn(mockResult);

        ArrayList<ArrayList<String>> result = pharmacistInstance.getDrugAmounts("br0001");

        assertNotNull(result, "The result should not be null when no data rows are returned");
        assertEquals(1, result.size(), "The result should contain one row (columns only)");
        assertEquals("remaining_quantity", result.get(0).get(0), "The column name should be remaining_quantity");
    }
}