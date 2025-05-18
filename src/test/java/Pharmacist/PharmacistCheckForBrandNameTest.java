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

public class PharmacistCheckForBrandNameTest {

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

        pharmacistInstance = new Pharmacist("user016", dbOperator);
    }

    @AfterEach
    public void tearDown() throws Exception {
        closeable.close();
    }

    /* PH_CHECK_BRAND_NAME_01
    Objective: Verify that the checkForBrandName method correctly retrieves the brand_id for a given brand name.
    Input: brandName = "Panadol"
           Pre-test state: Mocked DatabaseOperator returns predefined brand data.
    Expected output: The brand_id of the matching brand.
    Expected change: No database interaction.
     */
    @Test
    public void testCheckForBrandName_SuccessfulRetrieval() throws SQLException, ClassNotFoundException {
        ArrayList<ArrayList<String>> mockResult = new ArrayList<>();
        ArrayList<String> columns = new ArrayList<>();
        columns.add("brand_id");
        mockResult.add(columns);
        ArrayList<String> data = new ArrayList<>();
        data.add("br0001");
        mockResult.add(data);

        when(dbOperator.customSelection(anyString())).thenReturn(mockResult);

        String result = pharmacistInstance.checkForBrandName("Panadol");

        assertEquals("br0001", result, "The brand_id should match the brand name");
    }

    /* PH_CHECK_BRAND_NAME_02
    Objective: Verify that the checkForBrandName method returns "0" when no matching brand is found.
    Input: brandName = "NonExistentBrand"
           Pre-test state: Mocked DatabaseOperator returns only column names (no data rows).
    Expected output: "0"
    Expected change: No database interaction.
     */
    @Test
    public void testCheckForBrandName_NoMatchingBrand() throws SQLException, ClassNotFoundException {
        ArrayList<ArrayList<String>> mockResult = new ArrayList<>();
        ArrayList<String> columns = new ArrayList<>();
        columns.add("brand_id");
        mockResult.add(columns);

        when(dbOperator.customSelection(anyString())).thenReturn(mockResult);

        String result = pharmacistInstance.checkForBrandName("NonExistentBrand");

        assertEquals("0", result, "The method should return '0' when no matching brand is found");
    }

    /* PH_CHECK_BRAND_NAME_03
    Objective: Verify that the checkForBrandName method returns "0" when an SQLException occurs.
    Input: brandName = "Panadol"
           Pre-test state: Mocked DatabaseOperator throws an SQLException.
    Expected output: "0"
    Expected change: No database interaction.
     */
    @Test
    public void testCheckForBrandName_ThrowsSQLException() throws SQLException, ClassNotFoundException {
        when(dbOperator.customSelection(anyString())).thenThrow(new SQLException("Database error"));

        String result = pharmacistInstance.checkForBrandName("Panadol");

        assertEquals("0", result, "The method should return '0' when an SQLException occurs");
    }
}