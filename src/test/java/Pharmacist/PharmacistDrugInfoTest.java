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

public class PharmacistDrugInfoTest {

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

    /* PH_DRUG_01
    Objective: Verify that the getDrugInfo method correctly retrieves drug information and total quantity
               for a given drugID.
    Input: drugID = "DRUG001"
           Pre-test state: Mocked DatabaseOperator returns predefined drug and stock data.
    Expected output: An ArrayList containing column names, drug data, and total quantity.
    Expected change: No database interaction.
     */
    @Test
    public void testGetDrugInfo_SuccessfulRetrieval() throws SQLException, ClassNotFoundException {
        ArrayList<ArrayList<String>> mockDrugData = new ArrayList<>();
        ArrayList<String> columns = new ArrayList<>();
        columns.add("drug_name");
        columns.add("drug_type");
        columns.add("drug_unit");
        columns.add("unit_price");
        columns.add("stock_id");
        columns.add("manufac_name");
        columns.add("manufac_date");
        columns.add("exp_date");
        columns.add("supplier_id");
        mockDrugData.add(columns);
        ArrayList<String> data = new ArrayList<>();
        data.add("Paracetamol");
        data.add("Analgesic");
        data.add("Tablet");
        data.add("0.50");
        data.add("STOCK001");
        data.add("PharmaCorp");
        data.add("2025-01-01");
        data.add("2026-01-01");
        data.add("SUP001");
        mockDrugData.add(data);

        ArrayList<ArrayList<String>> mockStockData = new ArrayList<>();
        ArrayList<String> stockData1 = new ArrayList<>();
        stockData1.add("100");
        mockStockData.add(stockData1);
        ArrayList<String> stockData2 = new ArrayList<>();
        stockData2.add("50");
        mockStockData.add(stockData2);

        when(dbOperator.customSelection(anyString())).thenReturn(mockDrugData);
        when(dbOperator.showTableData(eq("pharmacy_stock"), eq("remaining_quantity"), anyString()))
                .thenReturn(mockStockData);

        ArrayList<ArrayList<String>> result = pharmacistInstance.getDrugInfo("DRUG001");

        assertNotNull(result, "The result should not be null when data is retrieved");
        assertEquals(3, result.size(), "The result should contain three rows (columns, data, total)");
        assertEquals("drug_name", result.get(0).get(0), "The first column name should be drug_name");
        assertEquals("Paracetamol", result.get(1).get(0), "The drug_name should match");
        assertEquals("150", result.get(2).get(0), "The total quantity should be 150");
    }

    /* PH_DRUG_02
    Objective: Verify that the getDrugInfo method handles an SQLException during database operations
               and returns null.
    Input: drugID = "DRUG001"
           Pre-test state: Mocked SQLException.
           Mock behavior: dbOperator.customSelection throws an SQLException.
    Expected output: null
    Expected change: No database interaction.
     */
    @Test
    public void testGetDrugInfo_ThrowsSQLException() throws SQLException, ClassNotFoundException {
        when(dbOperator.customSelection(anyString())).thenThrow(new SQLException("Database error"));

        ArrayList<ArrayList<String>> result = pharmacistInstance.getDrugInfo("DRUG001");

        assertNull(result, "The result should be null when an SQLException is thrown");
    }

    /* PH_DRUG_03
    Objective: Verify that the getDrugInfo method handles a case where no drug information is returned
               for the given drugID.
    Input: drugID = "DRUG001"
           Pre-test state: Mocked result with only column names and zero total quantity.
    Expected output: An ArrayList containing column names and total quantity (0).
    Expected change: No database interaction.
     */
    @Test
    public void testGetDrugInfo_NoData() throws SQLException, ClassNotFoundException {
        ArrayList<ArrayList<String>> mockResult = new ArrayList<>();
        ArrayList<String> columns = new ArrayList<>();
        columns.add("drug_name");
        columns.add("drug_type");
        columns.add("drug_unit");
        columns.add("unit_price");
        columns.add("stock_id");
        columns.add("manufac_name");
        columns.add("manufac_date");
        columns.add("exp_date");
        columns.add("supplier_id");
        mockResult.add(columns);

        ArrayList<ArrayList<String>> mockStockData = new ArrayList<>();

        when(dbOperator.customSelection(anyString())).thenReturn(mockResult);
        when(dbOperator.showTableData(eq("pharmacy_stock"), eq("remaining_quantity"), anyString()))
                .thenReturn(mockStockData);

        ArrayList<ArrayList<String>> result = pharmacistInstance.getDrugInfo("DRUG001");

        assertNotNull(result, "The result should not be null when no data rows are returned");
        assertEquals(2, result.size(), "The result should contain two rows (columns and total)");
        assertEquals("drug_name", result.get(0).get(0), "The first column name should be drug_name");
        assertEquals("0", result.get(1).get(0), "The total quantity should be 0");
    }
}