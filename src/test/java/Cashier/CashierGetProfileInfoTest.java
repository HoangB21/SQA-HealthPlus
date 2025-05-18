package Cashier;

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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class CashierGetProfileInfoTest {

    private Cashier cashierInstance;

    @Mock
    private DatabaseOperator dbOperator;

    private AutoCloseable closeable;

    @BeforeEach
    public void setUp() throws SQLException, ClassNotFoundException {
        closeable = MockitoAnnotations.openMocks(this);
        doNothing().when(dbOperator).connectAndUseDatabase();

        // Mock dữ liệu sys_user và cashier để khởi tạo Cashier
        ArrayList<ArrayList<String>> mockUserData = new ArrayList<>();
        ArrayList<String> userColumns = new ArrayList<>();
        userColumns.add("user_id");
        userColumns.add("user_name");
        userColumns.add("user_type");
        mockUserData.add(userColumns);
        ArrayList<String> userData = new ArrayList<>();
        userData.add("hms020");
        userData.add("user020");
        userData.add("cashier");
        mockUserData.add(userData);

        ArrayList<ArrayList<String>> mockCashierData = new ArrayList<>();
        ArrayList<String> cashierColumns = new ArrayList<>();
        cashierColumns.add("cashier_id");
        mockCashierData.add(cashierColumns);
        ArrayList<String> cashierData = new ArrayList<>();
        cashierData.add("CA001");
        mockCashierData.add(cashierData);

        when(dbOperator.showTableData(eq("sys_user"), eq("user_id,user_name,user_type"), anyString()))
                .thenReturn(mockUserData);
        when(dbOperator.showTableData(eq("cashier"), eq("cashier_id"), anyString()))
                .thenReturn(mockCashierData);

        cashierInstance = new Cashier("hms020");
        cashierInstance.dbOperator = dbOperator;
    }

    @AfterEach
    public void tearDown() throws Exception {
        closeable.close();
    }

    /* CA_GET_PROFILE_INFO_01
    Objective: Verify that the getProfileInfo method correctly retrieves profile information from mocked database.
    Input: None (uses userID = "hms020")
           Pre-test state: Mocked DatabaseOperator returns predefined person and sys_user data.
    Expected output: HashMap containing profile information (e.g., first_name, last_name, user_name).
    Expected change: No database interaction.
     */
    @Test
    public void testGetProfileInfo_SuccessfulRetrieval() throws SQLException, ClassNotFoundException {
        ArrayList<ArrayList<String>> mockResult = new ArrayList<>();
        ArrayList<String> columns = new ArrayList<>();
        columns.add("person_id");
        columns.add("user_id");
        columns.add("first_name");
        columns.add("last_name");
        columns.add("address");
        columns.add("phone");
        columns.add("user_id");
        columns.add("user_name");
        columns.add("user_type");
        mockResult.add(columns);
        ArrayList<String> data = new ArrayList<>();
        data.add("p020");
        data.add("hms020");
        data.add("John");
        data.add("Doe");
        data.add("123 Main St");
        data.add("1234567890");
        data.add("hms020");
        data.add("user020");
        data.add("cashier");
        mockResult.add(data);

        when(dbOperator.customSelection(anyString())).thenReturn(mockResult);

        HashMap<String, String> result = cashierInstance.getProfileInfo();

        assertNotNull(result, "The result HashMap should not be null");
        assertFalse(result.isEmpty(), "The result HashMap should not be empty");
        assertEquals("John", result.get("first_name"), "The first_name should match");
        assertEquals("Doe", result.get("last_name"), "The last_name should match");
        assertEquals("user020", result.get("user_name"), "The user_name should match");
        assertEquals("cashier", result.get("user_type"), "The user_type should match");
        assertEquals("123 Main St", result.get("address"), "The address should match");
        assertEquals("1234567890", result.get("phone"), "The phone should match");
    }

    /* CA_GET_PROFILE_INFO_02
    Objective: Verify that the getProfileInfo method returns an empty HashMap when no data is found.
    Input: None (uses userID = "hms020")
           Pre-test state: Mocked DatabaseOperator returns only column names (no data rows).
    Expected output: Empty HashMap.
    Expected change: No database interaction.
     */
    @Test
    public void testGetProfileInfo_NoData() throws SQLException, ClassNotFoundException {
        ArrayList<ArrayList<String>> mockResult = new ArrayList<>();
        ArrayList<String> columns = new ArrayList<>();
        columns.add("person_id");
        columns.add("user_id");
        columns.add("first_name");
        columns.add("last_name");
        columns.add("address");
        columns.add("phone");
        columns.add("user_id");
        columns.add("user_name");
        columns.add("user_type");
        mockResult.add(columns);

        when(dbOperator.customSelection(anyString())).thenReturn(mockResult);

        HashMap<String, String> result = cashierInstance.getProfileInfo();

        assertNotNull(result, "The result HashMap should not be null");
        assertTrue(result.isEmpty(), "The result HashMap should be empty when no data is found");
    }

    /* CA_GET_PROFILE_INFO_03
    Objective: Verify that the getProfileInfo method returns an empty HashMap when an SQLException occurs.
    Input: None (uses userID = "hms020")
           Pre-test state: Mocked DatabaseOperator throws an SQLException.
    Expected output: Empty HashMap.
    Expected change: No database interaction.
     */
    @Test
    public void testGetProfileInfo_ThrowsSQLException() throws SQLException, ClassNotFoundException {
        when(dbOperator.customSelection(anyString())).thenThrow(new SQLException("Database error"));

        HashMap<String, String> result = cashierInstance.getProfileInfo();

        assertNotNull(result, "The result HashMap should not be null");
        assertTrue(result.isEmpty(), "The result HashMap should be empty when an SQLException occurs");
    }
}