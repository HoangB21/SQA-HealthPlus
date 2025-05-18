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

public class PharmacistStockSummary2Test {

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

    /* PH_STOCK_SUMMARY2_01
    Objective: Verify that the getStockSummary2 method correctly retrieves brand summary with total amount and suppliers.
    Input: None
           Pre-test state: Mocked DatabaseOperator returns predefined brand and stock data.
    Expected output: An ArrayList containing column names, brand data, total amount, and supplier count.
    Expected change: No database interaction.
     */
    @Test
    public void testGetStockSummary2_SuccessfulRetrieval() throws SQLException, ClassNotFoundException {
        ArrayList<ArrayList<String>> mockBrandData = new ArrayList<>();
        ArrayList<String> brandColumns = new ArrayList<>();
        brandColumns.add("brand_id");
        brandColumns.add("brand_name");
        brandColumns.add("drug_type");
        brandColumns.add("drug_unit");
        brandColumns.add("unit_price");
        mockBrandData.add(brandColumns);
        ArrayList<String> brandData = new ArrayList<>();
        brandData.add("br0001");
        brandData.add("Panadol");
        brandData.add("Analgesic");
        brandData.add("Tablet");
        brandData.add("0.50");
        mockBrandData.add(brandData);

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

        when(dbOperator.customSelection("SELECT brand_id, brand_name, drug_type,drug_unit, unit_price FROM drug_brand_names;")).thenReturn(mockBrandData);
        when(dbOperator.customSelection(contains("WHERE brand_id = 'br0001'"))).thenReturn(mockStockData);

        ArrayList<ArrayList<String>> result = pharmacistInstance.getStockSummary2();

        assertNotNull(result, "The result should not be null when data is retrieved");
        assertEquals(2, result.size(), "The result should contain two rows (columns and data)");
        assertEquals("amount", result.get(0).get(5), "The column should include amount");
        assertEquals("suppliers", result.get(0).get(6), "The column should include suppliers");
        assertEquals("150", result.get(1).get(5), "The total amount should be 150");
        assertEquals("2", result.get(1).get(6), "The supplier count should be 2");
    }

    /* PH_STOCK_SUMMARY2_02
    Objective: Verify that the getStockSummary2 method handles an SQLException and returns null.
    Input: None
           Pre-test state: Mocked SQLException.
           Mock behavior: dbOperator.customSelection throws an SQLException.
    Expected output: null
    Expected change: No database interaction.
     */
    @Test
    public void testGetStockSummary2_ThrowsSQLException() throws SQLException, ClassNotFoundException {
        when(dbOperator.customSelection(anyString())).thenThrow(new SQLException("Database error"));

        ArrayList<ArrayList<String>> result = pharmacistInstance.getStockSummary2();

        assertNull(result, "The result should be null when an SQLException is thrown");
    }

    /* PH_STOCK_SUMMARY2_03
    Objective: Verify that the getStockSummary2 method handles a case where no brand data is returned.
    Input: None
           Pre-test state: Mocked result with only column names (no data rows).
    Expected output: An ArrayList containing only column names.
    Expected change: No database interaction.
     */
    @Test
    public void testGetStockSummary2_NoData() throws SQLException, ClassNotFoundException {
        ArrayList<ArrayList<String>> mockResult = new ArrayList<>();
        ArrayList<String> columns = new ArrayList<>();
        columns.add("brand_id");
        columns.add("brand_name");
        columns.add("drug_type");
        columns.add("drug_unit");
        columns.add("unit_price");
        mockResult.add(columns);

        when(dbOperator.customSelection("SELECT brand_id, brand_name, drug_type,drug_unit, unit_price FROM drug_brand_names;")).thenReturn(mockResult);

        ArrayList<ArrayList<String>> result = pharmacistInstance.getStockSummary2();

        assertNotNull(result, "The result should not be null when no data rows are returned");
        assertEquals(1, result.size(), "The result should contain one row (columns only)");
        assertEquals("amount", result.get(0).get(5), "The column should include amount");
    }
}