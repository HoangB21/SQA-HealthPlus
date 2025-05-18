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

public class PharmacistStockSummaryTest {

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

    /* PH_STOCK_SUMMARY_01
    Objective: Verify that the getStockSummary method correctly retrieves drug summary with total amount and suppliers.
    Input: None
           Pre-test state: Mocked DatabaseOperator returns predefined drug and stock data.
    Expected output: An ArrayList containing column names, drug data, total amount, and supplier count.
    Expected change: No database interaction.
     */
    @Test
    public void testGetStockSummary_SuccessfulRetrieval() throws SQLException, ClassNotFoundException {
        ArrayList<ArrayList<String>> mockDrugData = new ArrayList<>();
        ArrayList<String> drugColumns = new ArrayList<>();
        drugColumns.add("drug_id");
        drugColumns.add("drug_name");
        drugColumns.add("dangerous_drug");
        mockDrugData.add(drugColumns);
        ArrayList<String> drugData = new ArrayList<>();
        drugData.add("DRUG001");
        drugData.add("Paracetamol");
        drugData.add("1");
        mockDrugData.add(drugData);

        ArrayList<ArrayList<String>> mockStockData = new ArrayList<>();
        ArrayList<String> stockColumns = new ArrayList<>();
        stockColumns.add("remaining_quantity");
        stockColumns.add("supplier_id");
        mockStockData.add(stockColumns);
        ArrayList<String> stockData1 = new ArrayList<>();
        stockData1.add("100");
        stockData1.add("sup0001");
        mockStockData.add(stockData1);
        ArrayList<String> stockData2 = new ArrayList<>();
        stockData2.add("50");
        stockData2.add("sup0002");
        mockStockData.add(stockData2);

        when(dbOperator.customSelection("SELECT * FROM drug;")).thenReturn(mockDrugData);
        when(dbOperator.customSelection(contains("WHERE drug_id = 'DRUG001'"))).thenReturn(mockStockData);

        ArrayList<ArrayList<String>> result = pharmacistInstance.getStockSummary();

        assertNotNull(result, "The result should not be null when data is retrieved");
        assertEquals(2, result.size(), "The result should contain two rows (columns and data)");
        assertEquals("amount", result.get(0).get(3), "The column should include amount");
        assertEquals("suppliers", result.get(0).get(4), "The column should include suppliers");
        assertEquals("150", result.get(1).get(3), "The total amount should be 150");
        assertEquals("2", result.get(1).get(4), "The supplier count should be 2");
    }

    /* PH_STOCK_SUMMARY_02
    Objective: Verify that the getStockSummary method handles an SQLException and returns null.
    Input: None
           Pre-test state: Mocked SQLException.
           Mock behavior: dbOperator.customSelection throws an SQLException.
    Expected output: null
    Expected change: No database interaction.
     */
    @Test
    public void testGetStockSummary_ThrowsSQLException() throws SQLException, ClassNotFoundException {
        when(dbOperator.customSelection(anyString())).thenThrow(new SQLException("Database error"));

        ArrayList<ArrayList<String>> result = pharmacistInstance.getStockSummary();

        assertNull(result, "The result should be null when an SQLException is thrown");
    }

    /* PH_STOCK_SUMMARY_03
    Objective: Verify that the getStockSummary method handles a case where no drug data is returned.
    Input: None
           Pre-test state: Mocked result with only column names (no data rows).
    Expected output: An ArrayList containing only column names.
    Expected change: No database interaction.
     */
    @Test
    public void testGetStockSummary_NoData() throws SQLException, ClassNotFoundException {
        ArrayList<ArrayList<String>> mockResult = new ArrayList<>();
        ArrayList<String> columns = new ArrayList<>();
        columns.add("drug_id");
        columns.add("drug_name");
        columns.add("dangerous_drug");
        mockResult.add(columns);

        when(dbOperator.customSelection("SELECT * FROM drug;")).thenReturn(mockResult);

        ArrayList<ArrayList<String>> result = pharmacistInstance.getStockSummary();

        assertNotNull(result, "The result should not be null when no data rows are returned");
        assertEquals(1, result.size(), "The result should contain one row (columns only)");
        assertEquals("amount", result.get(0).get(3), "The column should include amount");
    }
}