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

public class CashierGetWaitingRefundsIntegrationTest {

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
            public ArrayList<ArrayList<String>> customSelection(String query) throws SQLException, ClassNotFoundException {
                ArrayList<ArrayList<String>> result = new ArrayList<>();
                ArrayList<String> columns = new ArrayList<>(Arrays.asList("refund_id", "bill_id", "payment_type", "amount", "reason", "date"));
                result.add(columns);
                if (query.toLowerCase().contains("select * from refund")) {
                    ArrayList<String> dataRow1 = new ArrayList<>(Arrays.asList("r0001", "hms0007b", "cash", "100", "overpayment", "2016-08-30 14:30:00"));
                    ArrayList<String> dataRow2 = new ArrayList<>(Arrays.asList("r0002", "hms0007b", "card", "200", "return", "2016-08-31 10:00:00"));
                    result.add(dataRow1);
                    result.add(dataRow2);
                }
                return result;
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
    public void testGetWaitingRefunds_SuccessfulRetrieval() throws SQLException {
        ArrayList<ArrayList<String>> result = cashierInstance.getWaitingRefunds();

        assertNotNull(result, "Result should not be null");
        assertEquals(3, result.size(), "Result should contain 3 rows (1 header + 2 data)");
        assertEquals("refund_id", result.get(0).get(0), "First column should be refund_id");
        assertEquals("r0001", result.get(1).get(0), "First refund ID should match");
        assertEquals("hms0007b", result.get(1).get(1), "First bill ID should match");
        assertEquals("r0002", result.get(2).get(0), "Second refund ID should match");
    }

    @Test
    public void testGetWaitingRefunds_NoData() throws SQLException {
        cashierInstance.dbOperator = new DatabaseOperator() {
            @Override
            public ArrayList<ArrayList<String>> customSelection(String query) throws SQLException, ClassNotFoundException {
                ArrayList<ArrayList<String>> result = new ArrayList<>();
                ArrayList<String> columns = new ArrayList<>(Arrays.asList("refund_id", "bill_id", "payment_type", "amount", "reason", "date"));
                result.add(columns);
                return result;
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

        ArrayList<ArrayList<String>> result = cashierInstance.getWaitingRefunds();

        assertNotNull(result, "Result should not be null");
        assertEquals(1, result.size(), "Result should contain only header row");
        assertEquals("refund_id", result.get(0).get(0), "First column should be refund_id");
    }

    @Test
    public void testGetWaitingRefunds_DatabaseError() throws SQLException {
        cashierInstance.dbOperator = new DatabaseOperator() {
            @Override
            public ArrayList<ArrayList<String>> customSelection(String query) throws SQLException, ClassNotFoundException {
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
                    row.add("hms0020u");
                    row.add("cashier");
                    result.add(row);
                }
                return result;
            }
        };

        ArrayList<ArrayList<String>> result = cashierInstance.getWaitingRefunds();

        assertNotNull(result, "Result should not be null");
        assertTrue(result.isEmpty(), "Result should be empty on database error");
    }
}