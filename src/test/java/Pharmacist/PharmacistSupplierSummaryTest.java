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

public class PharmacistSupplierSummaryTest {

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

    /* PH_SUPPLIER_SUMMARY_01
    Objective: Verify that the getSupplierSummary method correctly retrieves supplier summary with total stock.
    Input: None
           Pre-test state: Mocked DatabaseOperator returns predefined stock data.
    Expected output: An ArrayList containing supplier IDs and total stock quantities.
    Expected change: No database interaction.
     */
    @Test
    public void testGetSupplierSummary_SuccessfulRetrieval() throws SQLException, ClassNotFoundException {
        ArrayList<ArrayList<String>> mockResult = new ArrayList<>();
        ArrayList<String> columns = new ArrayList<>();
        columns.add("remaining_quantity");
        columns.add("supplier_id");
        mockResult.add(columns);
        ArrayList<String> data1 = new ArrayList<>();
        data1.add("100");
        data1.add("sup0001");
        mockResult.add(data1);
        ArrayList<String> data2 = new ArrayList<>();
        data2.add("50");
        data2.add("sup0001");
        mockResult.add(data2);
        ArrayList<String> data3 = new ArrayList<>();
        data3.add("200");
        data3.add("sup0002");
        mockResult.add(data3);

        when(dbOperator.customSelection(anyString())).thenReturn(mockResult);

        ArrayList<ArrayList<String>> result = pharmacistInstance.getSupplierSummary();

        assertNotNull(result, "The result should not be null when data is retrieved");
        assertEquals(2, result.size(), "The result should contain two rows (suppliers and stock)");
        assertEquals("sup0001", result.get(0).get(0), "The first supplier_id should match");
        assertEquals("sup0002", result.get(0).get(1), "The second supplier_id should match");
        assertEquals("150", result.get(1).get(0), "The total stock for sup0001 should be 150");
        assertEquals("200", result.get(1).get(1), "The total stock for sup0002 should be 200");
    }

    /* PH_SUPPLIER_SUMMARY_02
    Objective: Verify that the getSupplierSummary method handles an SQLException and returns an empty ArrayList.
    Input: None
           Pre-test state: Mocked SQLException.
           Mock behavior: dbOperator.customSelection throws an SQLException.
    Expected output: An empty ArrayList
    Expected change: No database interaction.
     */
    @Test
    public void testGetSupplierSummary_ThrowsSQLException() throws SQLException, ClassNotFoundException {
        when(dbOperator.customSelection(anyString())).thenThrow(new SQLException("Database error"));

        ArrayList<ArrayList<String>> result = pharmacistInstance.getSupplierSummary();

        assertNotNull(result, "The result should not be null when an SQLException is thrown");
        assertTrue(result.isEmpty(), "The result should be an empty ArrayList when an SQLException is thrown");
    }

    /* PH_SUPPLIER_SUMMARY_03
    Objective: Verify that the getSupplierSummary method handles a case where no stock data is returned.
    Input: None
           Pre-test state: Mocked result with only column names (no data rows).
    Expected output: An empty ArrayList
    Expected change: No database interaction.
     */
    @Test
    public void testGetSupplierSummary_NoData() throws SQLException, ClassNotFoundException {
        ArrayList<ArrayList<String>> mockResult = new ArrayList<>();
        ArrayList<String> columns = new ArrayList<>();
        columns.add("remaining_quantity");
        columns.add("supplier_id");
        mockResult.add(columns);

        when(dbOperator.customSelection(anyString())).thenReturn(mockResult);

        ArrayList<ArrayList<String>> result = pharmacistInstance.getSupplierSummary();

        assertNotNull(result, "The result should not be null when no data rows are returned");
        assertTrue(result.isEmpty(), "The result should be an empty ArrayList when no data rows are returned");
    }
}