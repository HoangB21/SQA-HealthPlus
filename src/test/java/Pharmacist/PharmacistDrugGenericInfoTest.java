package Pharmacist;

import com.hms.hms_test_2.DatabaseOperator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

public class PharmacistDrugGenericInfoTest {

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

    /* PH_DRUG_GENERIC_INFO_01
    Objective: Verify that the getDrugGenericInfo method correctly retrieves brand names and their generic names.
    Input: None
           Pre-test state: Mocked DatabaseOperator returns predefined brand data.
    Expected output: A HashMap mapping brand names to generic names.
    Expected change: No database interaction.
     */
    @Test
    public void testGetDrugGenericInfo_SuccessfulRetrieval() throws SQLException, ClassNotFoundException {
        ArrayList<ArrayList<String>> mockResult = new ArrayList<>();
        ArrayList<String> columns = new ArrayList<>();
        columns.add("brand_name");
        columns.add("generic_name");
        mockResult.add(columns);
        ArrayList<String> data = new ArrayList<>();
        data.add("Panadol");
        data.add("Acetaminophen");
        mockResult.add(data);

        when(dbOperator.customSelection(anyString())).thenReturn(mockResult);

        HashMap<String, String> result = pharmacistInstance.getDrugGenericInfo();

        assertNotNull(result, "The result should not be null when data is retrieved");
        assertEquals(1, result.size(), "The result should contain one brand");
        assertEquals("Acetaminophen", result.get("Panadol"), "The generic name should match");
    }

    /* PH_DRUG_GENERIC_INFO_02
    Objective: Verify that the getDrugGenericInfo method handles an SQLException and returns an empty HashMap.
    Input: None
           Pre-test state: Mocked SQLException.
           Mock behavior: dbOperator.customSelection throws an SQLException.
    Expected output: An empty HashMap
    Expected change: No database interaction.
     */
    @Test
    public void testGetDrugGenericInfo_ThrowsSQLException() throws SQLException, ClassNotFoundException {
        when(dbOperator.customSelection(anyString())).thenThrow(new SQLException("Database error"));

        HashMap<String, String> result = pharmacistInstance.getDrugGenericInfo();

        assertNotNull(result, "The result should not be null when an SQLException is thrown");
        assertTrue(result.isEmpty(), "The result should be an empty HashMap when an SQLException is thrown");
    }

    /* PH_DRUG_GENERIC_INFO_03
    Objective: Verify that the getDrugGenericInfo method handles a case where no brand data is returned.
    Input: None
           Pre-test state: Mocked result with only column names (no data rows).
    Expected output: An empty HashMap
    Expected change: No database interaction.
     */
    @Test
    public void testGetDrugGenericInfo_NoData() throws SQLException, ClassNotFoundException {
        ArrayList<ArrayList<String>> mockResult = new ArrayList<>();
        ArrayList<String> columns = new ArrayList<>();
        columns.add("brand_name");
        columns.add("generic_name");
        mockResult.add(columns);

        when(dbOperator.customSelection(anyString())).thenReturn(mockResult);

        HashMap<String, String> result = pharmacistInstance.getDrugGenericInfo();

        assertNotNull(result, "The result should not be null when no data rows are returned");
        assertTrue(result.isEmpty(), "The result should be an empty HashMap when no data rows are returned");
    }
}