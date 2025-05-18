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

public class PharmacistStockInfoTest {

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

    /* PH_STOCK_INFO_01
    Objective: Verify that the getStockInfo method correctly retrieves stock information for a given supplierID.
    Input: supplierID = "sup0001"
           Pre-test state: Mocked DatabaseOperator returns predefined stock data.
    Expected output: An ArrayList containing column names and stock data.
    Expected change: No database interaction.
     */
    @Test
    public void testGetStockInfo_SuccessfulRetrieval() throws SQLException, ClassNotFoundException {
        ArrayList<ArrayList<String>> mockResult = new ArrayList<>();
        ArrayList<String> columns = new ArrayList<>();
        columns.add("stock_id");
        columns.add("stock");
        columns.add("remaining_quantity");
        columns.add("date");
        columns.add("manufac_date");
        columns.add("exp_date");
        columns.add("drug_name");
        columns.add("drug_unit");
        mockResult.add(columns);
        ArrayList<String> data = new ArrayList<>();
        data.add("stk0001");
        data.add("500");
        data.add("450");
        data.add("2016-08-20");
        data.add("2016-08-10");
        data.add("2017-09-01");
        data.add("Paracetamol");
        data.add("Tablet");
        mockResult.add(data);

        when(dbOperator.customSelection(anyString())).thenReturn(mockResult);

        ArrayList<ArrayList<String>> result = pharmacistInstance.getStockInfo("sup0001");

        assertNotNull(result, "The result should not be null when data is retrieved");
        assertEquals(2, result.size(), "The result should contain two rows (columns and data)");
        assertEquals("stk0001", result.get(1).get(0), "The stock_id should match");
        assertEquals("Paracetamol", result.get(1).get(6), "The drug_name should match");
    }

    /* PH_STOCK_INFO_02
    Objective: Verify that the getStockInfo method handles an SQLException and returns null.
    Input: supplierID = "sup0001"
           Pre-test state: Mocked SQLException.
           Mock behavior: dbOperator.customSelection throws an SQLException.
    Expected output: null
    Expected change: No database interaction.
     */
    @Test
    public void testGetStockInfo_ThrowsSQLException() throws SQLException, ClassNotFoundException {
        when(dbOperator.customSelection(anyString())).thenThrow(new SQLException("Database error"));

        ArrayList<ArrayList<String>> result = pharmacistInstance.getStockInfo("sup0001");

        assertNull(result, "The result should be null when an SQLException is thrown");
    }

    /* PH_STOCK_INFO_03
    Objective: Verify that the getStockInfo method handles a case where no stock data is returned.
    Input: supplierID = "sup0001"
           Pre-test state: Mocked result with only column names (no data rows).
    Expected output: An ArrayList containing only column names.
    Expected change: No database interaction.
     */
    @Test
    public void testGetStockInfo_NoData() throws SQLException, ClassNotFoundException {
        ArrayList<ArrayList<String>> mockResult = new ArrayList<>();
        ArrayList<String> columns = new ArrayList<>();
        columns.add("stock_id");
        columns.add("stock");
        columns.add("remaining_quantity");
        columns.add("date");
        columns.add("manufac_date");
        columns.add("exp_date");
        columns.add("drug_name");
        columns.add("drug_unit");
        mockResult.add(columns);

        when(dbOperator.customSelection(anyString())).thenReturn(mockResult);

        ArrayList<ArrayList<String>> result = pharmacistInstance.getStockInfo("sup0001");

        assertNotNull(result, "The result should not be null when no data rows are returned");
        assertEquals(1, result.size(), "The result should contain one row (columns only)");
        assertEquals("stock_id", result.get(0).get(0), "The first column name should be stock_id");
    }
}