package Cashier;

import com.hms.hms_test_2.DatabaseOperator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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

        Statement stmt = connection.createStatement();
        // Chèn person trước để thỏa mãn sys_user.person_id (nếu có)
        stmt.executeUpdate("INSERT INTO person (person_id, user_id, nic, gender, date_of_birth, address, mobile, " +
                "first_name, last_name, email, nationality, religion) " +
                "VALUES ('p020', 'hms020', '872984565V', 'M', '1987-10-24', '123/f Yanthampalawa Kurunegala', " +
                "'0713457779', 'Lakshitha', 'Rangana', 'lakshithaasd@gmail.com', 'Sri Lankan', 'Buddhism') " +
                "ON DUPLICATE KEY UPDATE user_id = 'hms020', nic = '872984565V', gender = 'M', " +
                "date_of_birth = '1987-10-24', address = '123/f Yanthampalawa Kurunegala', mobile = '0713457779', " +
                "first_name = 'Lakshitha', last_name = 'Rangana', email = 'lakshithaasd@gmail.com', " +
                "nationality = 'Sri Lankan', religion = 'Buddhism'");
        // Chèn sys_user sau
        stmt.executeUpdate("INSERT INTO sys_user (user_id, user_name, user_type, password, profile_pic, person_id) " +
                "VALUES ('hms020', 'user020', 'cashier', '1234', 'user020ProfPic.png', 'p020') " +
                "ON DUPLICATE KEY UPDATE user_name = 'user020', user_type = 'cashier', " +
                "password = '1234', profile_pic = 'user020ProfPic.png', person_id = 'p020'");
        stmt.close();

        cashierInstance = new Cashier("user020");
        cashierInstance.dbOperator = new DatabaseOperator() {
            @Override
            public boolean customInsertion(String query) throws SQLException, ClassNotFoundException {
                Statement stmt = connection.createStatement();
                int rowsAffected = stmt.executeUpdate(query);
                stmt.close();
                return rowsAffected > 0;
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
    public void testUpdateAccountInfo_SuccessfulUpdateMultipleFields() throws SQLException {
        String info = "user_name user021#password newpass123";
        boolean result = cashierInstance.updateAccountInfo(info);

        assertTrue(result);

        Statement verifyStmt = connection.createStatement();
        ResultSet rs = verifyStmt.executeQuery("SELECT * FROM sys_user WHERE user_id = 'hms020'");
        assertTrue(rs.next());
        assertEquals("user021", rs.getString("user_name"));
        assertEquals("newpass123", rs.getString("password"));
        rs.close();
        verifyStmt.close();
    }

    @Test
    public void testUpdateAccountInfo_SuccessfulUpdateSingleField() throws SQLException {
        String info = "user_name user021";
        boolean result = cashierInstance.updateAccountInfo(info);

        assertTrue(result);

        Statement verifyStmt = connection.createStatement();
        ResultSet rs = verifyStmt.executeQuery("SELECT * FROM sys_user WHERE user_id = 'hms020'");
        assertTrue(rs.next());
        assertEquals("user021", rs.getString("user_name"));
        assertEquals("1234", rs.getString("password"));
        rs.close();
        verifyStmt.close();
    }

    @Test
    public void testUpdateAccountInfo_EmptyInfo() throws SQLException {
        String info = "";
        boolean result = cashierInstance.updateAccountInfo(info);

        assertFalse(result);

        Statement verifyStmt = connection.createStatement();
        ResultSet rs = verifyStmt.executeQuery("SELECT * FROM sys_user WHERE user_id = 'hms020'");
        assertTrue(rs.next());
        assertEquals("user020", rs.getString("user_name"));
        rs.close();
        verifyStmt.close();
    }

    @Test
    public void testUpdateAccountInfo_NullInfo() throws SQLException {
        boolean result = cashierInstance.updateAccountInfo(null);

        assertFalse(result);

        Statement verifyStmt = connection.createStatement();
        ResultSet rs = verifyStmt.executeQuery("SELECT * FROM sys_user WHERE user_id = 'hms020'");
        assertTrue(rs.next());
        assertEquals("user020", rs.getString("user_name"));
        rs.close();
        verifyStmt.close();
    }

    @Test
    public void testUpdateAccountInfo_MalformedInfo() throws SQLException {
        String info = "user_name#password newpass123";
        boolean result = cashierInstance.updateAccountInfo(info);

        assertFalse(result);

        Statement verifyStmt = connection.createStatement();
        ResultSet rs = verifyStmt.executeQuery("SELECT * FROM sys_user WHERE user_id = 'hms020'");
        assertTrue(rs.next());
        assertEquals("user020", rs.getString("user_name"));
        rs.close();
        verifyStmt.close();
    }

    @Test
    public void testUpdateAccountInfo_InvalidColumn() throws SQLException {
        String info = "invalid_column Test";
        boolean result = cashierInstance.updateAccountInfo(info);

        assertFalse(result);

        Statement verifyStmt = connection.createStatement();
        ResultSet rs = verifyStmt.executeQuery("SELECT * FROM sys_user WHERE user_id = 'hms020'");
        assertTrue(rs.next());
        assertEquals("user020", rs.getString("user_name"));
        rs.close();
        verifyStmt.close();
    }

    @Test
    public void testUpdateAccountInfo_MaxLengthField() throws SQLException {
        String info = "user_name VeryLongUserNameExceedingLimit";
        boolean result = cashierInstance.updateAccountInfo(info);

        assertFalse(result); // Giả định user_name có giới hạn độ dài

        Statement verifyStmt = connection.createStatement();
        ResultSet rs = verifyStmt.executeQuery("SELECT * FROM sys_user WHERE user_id = 'hms020'");
        assertTrue(rs.next());
        assertEquals("user020", rs.getString("user_name"));
        rs.close();
        verifyStmt.close();
    }
}