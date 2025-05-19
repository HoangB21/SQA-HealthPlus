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

public class CashierRemoveFromTempBillIntegrationTest {

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
                // Mock hành vi xóa thành công cho patient_id hợp lệ
                if (query.contains("DELETE FROM tmp_bill") && query.contains("hms0001pa")) {
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
                    row.add("hms020");
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
    public void testRemoveFromTempBill_NoRecord() {
        boolean result = cashierInstance.removeFromTempBill("invalid");

        assertFalse(result, "Remove should fail for invalid patient ID");
    }

    @Test
    public void testRemoveFromTempBill_NullPatientId() {
        boolean result = cashierInstance.removeFromTempBill(null);

        assertFalse(result, "Remove should fail for null patient ID");
    }

    @Test
    public void testRemoveFromTempBill_EmptyPatientId() {
        boolean result = cashierInstance.removeFromTempBill("");

        assertFalse(result, "Remove should fail for empty patient ID");
    }

    @Test
    public void testRemoveFromTempBill_DatabaseError() {
        cashierInstance.dbOperator = new DatabaseOperator() {
            @Override
            public boolean customInsertion(String query) throws SQLException, ClassNotFoundException {
                throw new SQLException("Database error");
            }

            @Override
            public ArrayList<ArrayList<String>> showTableData(String table, String columns, String condition)
                    throws SQLException, ClassNotFoundException {
                ArrayList<ArrayList<String>> result = new ArrayList<>();
                ArrayList<String> columnNames = new ArrayList<>(Arrays.asList(columns.split(",")));
                result.add(columnNames);
                if (table.equals("sys_user") && columns.equals("user_id,user_type") && condition.contains("user_name = 'user020'")) {
                    ArrayList<String> row = new ArrayList<>();
                    row.add("hms020");
                    row.add("cashier");
                    result.add(row);
                }
                return result;
            }
        };

        boolean result = cashierInstance.removeFromTempBill("hms0001pa");

        assertFalse(result, "Remove should fail on database error");
    }
}
