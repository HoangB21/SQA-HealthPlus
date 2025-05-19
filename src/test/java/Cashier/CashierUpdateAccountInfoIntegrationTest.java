package Cashier;

import com.hms.hms_test_2.DatabaseOperator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class CashierUpdateAccountInfoIntegrationTest {

    private Cashier cashierInstance;
    private Connection connection;

    @BeforeEach
    public void setUp() throws SQLException {
        connection = DriverManager.getConnection(
                "jdbc:mysql://127.0.0.1:3306/test_HMS2?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                "root", "123456");
        connection.setAutoCommit(false);

        cashierInstance = new Cashier("user020");
        cashierInstance.dbOperator = new DatabaseOperator() {
            @Override
            public boolean customInsertion(String query) throws SQLException, ClassNotFoundException {
                // Mock hành vi cập nhật thành công cho các truy vấn hợp lệ
                if (query.contains("UPDATE sys_user SET") && query.contains("user_name")) {
                    return true;
                }
                return false;
            }

            @Override
            public ArrayList<ArrayList<String>> showTableData(String table, String columns, String condition)
                    throws SQLException, ClassNotFoundException {
                ArrayList<ArrayList<String>> result = new ArrayList<>();
                ArrayList<String> columnNames = new ArrayList<>(Arrays.asList(columns.split(",")));
                result.add(columnNames);
                if (table.equals("sys_user") && columns.equals("user_id,user_type") && condition.contains("user_name = 'user020'")) {
                    ArrayList<String> row = new ArrayList<>();
                    row.add("hms0020u");
                    row.add("cashier");
                    result.add(row);
                }
                return result;
            }
        };
    }

    @AfterEach
    public void tearDown() throws SQLException {
        if (connection != null) {
            connection.rollback();
            connection.setAutoCommit(true);
            connection.close();
        }
    }

    @Test
    public void testUpdateAccountInfo_ValidUserName() {
        boolean result = cashierInstance.updateAccountInfo("user_name#new_user");

        assertTrue(result, "Update should succeed for valid user_name");
    }

    @Test
    public void testUpdateAccountInfo_ValidPassword() {
        boolean result = cashierInstance.updateAccountInfo("password#new_pass");

        assertTrue(result, "Update should succeed for valid password");
    }

    @Test
    public void testUpdateAccountInfo_EmptyInfo() {
        boolean result = cashierInstance.updateAccountInfo("");

        assertFalse(result, "Update should fail for empty info");
    }

    @Test
    public void testUpdateAccountInfo_NullInfo() {
        boolean result = cashierInstance.updateAccountInfo(null);

        assertFalse(result, "Update should fail for null info");
    }
}