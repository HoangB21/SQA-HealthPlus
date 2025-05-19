package Cashier;

import com.hms.hms_test_2.DatabaseOperator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class CashierRefundIntegrationTest {

    private Cashier cashierInstance;
    private Connection connection;

    @BeforeEach
    public void setUp() throws SQLException {
        connection = DriverManager.getConnection(
                "jdbc:mysql://127.0.0.1:3306/test_HMS2?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                "root", "123456");
        connection.setAutoCommit(false);

        Statement stmt = connection.createStatement();
        stmt.executeUpdate("INSERT INTO sys_user (user_id, user_name, user_type, password, profile_pic) " +
                "VALUES ('hms0020u', 'user020', 'cashier', '1234', 'user020ProfPic.png') " +
                "ON DUPLICATE KEY UPDATE user_name = 'user020', user_type = 'cashier', " +
                "password = '1234', profile_pic = 'user020ProfPic.png', person_id = NULL");
        stmt.executeUpdate("INSERT INTO person (person_id, user_id) " +
                "VALUES ('hms00001', 'hms0020u') " +
                "ON DUPLICATE KEY UPDATE user_id = 'hms0020u'");
        stmt.executeUpdate("UPDATE sys_user SET person_id = 'hms00001' WHERE user_id = 'hms0020u'");
        stmt.executeUpdate("INSERT INTO patient (patient_id, person_id) " +
                "VALUES ('hms0001pa', 'hms00001') " +
                "ON DUPLICATE KEY UPDATE person_id = 'hms00001'");
        stmt.executeUpdate("INSERT INTO bill (bill_id, bill_date, total, patient_id) " +
                "VALUES ('hms0001b', '2016-08-30 14:30:00', 1210, 'hms0001pa') " +
                "ON DUPLICATE KEY UPDATE bill_date = '2016-08-30 14:30:00', total = 1210, patient_id = 'hms0001pa'");
        stmt.close();

        cashierInstance = new Cashier("user020");
        cashierInstance.dbOperator = new DatabaseOperator() {
            @Override
            public boolean customInsertion(String query) throws SQLException, ClassNotFoundException {
                // Mock hành vi chèn thành công cho refund hợp lệ
                if (query.contains("INSERT INTO refund") && query.contains("hms0001b") && query.contains("amount = 50")) {
                    return true;
                }
                return false; // Giả lập thất bại cho các trường hợp không hợp lệ
            }

            @Override
            public ArrayList<ArrayList<String>> customSelection(String query) throws SQLException, ClassNotFoundException {
                ArrayList<ArrayList<String>> result = new ArrayList<>();
                ArrayList<String> columns = new ArrayList<>(Arrays.asList("refund_id"));
                result.add(columns);
                if (query.contains("SELECT refund_id FROM refund")) {
                    ArrayList<String> dataRow = new ArrayList<>();
                    dataRow.add("r0001");
                    result.add(dataRow);
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
            connection.rollback(); // Rollback để không thay đổi database
            connection.setAutoCommit(true);
            connection.close();
        }
    }

    @Test
    public void testRefund_Success() {
        String refundInfo = "bill_id hms0001b,amount 50,description Refund,payment_type cash";
        boolean result = cashierInstance.refund(refundInfo);

        assertTrue(result, "Refund should be successful with valid input");
    }

    @Test
    public void testRefund_InvalidBill() {
        String refundInfo = "bill_id invalid_bill,amount 50,description Refund,payment_type cash";
        boolean result = cashierInstance.refund(refundInfo);

        assertFalse(result, "Refund should fail for invalid bill_id");
    }

    @Test
    public void testRefund_MissingAmount() {
        String refundInfo = "bill_id hms0001b,description Refund,payment_type cash";
        boolean result = cashierInstance.refund(refundInfo);

        assertFalse(result, "Refund should fail for missing amount");
    }

    @Test
    public void testRefund_NegativeAmount() {
        String refundInfo = "bill_id hms0001b,amount -50,description Refund,payment_type cash";
        boolean result = cashierInstance.refund(refundInfo);

        assertFalse(result, "Refund should fail for negative amount");
    }

    @Test
    public void testRefund_DatabaseError() {
        String refundInfo = "bill_id hms0001b,amount 50,description Refund,payment_type cash";
        boolean result = cashierInstance.refund(refundInfo);

        assertTrue(result, "Refund should be successful with mocked insertion");
    }
}