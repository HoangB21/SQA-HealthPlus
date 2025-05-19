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

public class CashierMakeRefundIntegrationTest {

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
                // Mock hành vi xóa thành công cho refund hợp lệ
                if (query.contains("DELETE FROM refund") && query.contains("r0001")) {
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
    public void testMakeRefund_Success() {
        boolean result = cashierInstance.makeRefund("r0001");

        assertTrue(result, "Refund should be successful for valid refund ID");
    }

    @Test
    public void testMakeRefund_InvalidRefundId() {
        boolean result = cashierInstance.makeRefund("invalid_id");

        assertFalse(result, "Refund should fail for invalid refund ID");
    }

    @Test
    public void testMakeRefund_NullId() {
        boolean result = cashierInstance.makeRefund(null);

        assertFalse(result, "Refund should fail for null refund ID");
    }

    @Test
    public void testMakeRefund_EmptyId() {
        boolean result = cashierInstance.makeRefund("");

        assertFalse(result, "Refund should fail for empty refund ID");
    }
}
