package Pharmacist;

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

import static org.junit.jupiter.api.Assertions.*;

public class PharmacistUpdateAccountInfoIntegrationTest {

    private Pharmacist pharmacistInstance;
    private Connection connection;

    @BeforeEach
    public void setUp() throws SQLException {
        connection = DriverManager.getConnection(
                "jdbc:mysql://127.0.0.1:3306/test_HMS2?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                "root", "hieu");
        connection.setAutoCommit(false);

        // Thêm hoặc cập nhật dữ liệu mẫu vào sys_user
        Statement stmt = connection.createStatement();
        stmt.executeUpdate("INSERT INTO sys_user (user_id, user_name, user_type) " +
                "VALUES ('hms016', 'user016', 'pharmacist') " +
                "ON DUPLICATE KEY UPDATE user_name = 'user016', user_type = 'pharmacist'");
        stmt.close();

        pharmacistInstance = new Pharmacist("user016");
        pharmacistInstance.userID = "hms016"; // Giả định userID được thiết lập
        pharmacistInstance.dbOperator = new DatabaseOperator() {
            @Override
            public boolean customInsertion(String query) throws SQLException, ClassNotFoundException {
                Statement stmt = connection.createStatement();
                int rowsAffected = stmt.executeUpdate(query);
                stmt.close();
                return rowsAffected > 0;
            }

            @Override
            public ArrayList<ArrayList<String>> customSelection(String query) throws SQLException, ClassNotFoundException {
                ArrayList<ArrayList<String>> result = new ArrayList<>();
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(query);
                ArrayList<String> columns = new ArrayList<>();
                for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                    columns.add(rs.getMetaData().getColumnName(i));
                }
                result.add(columns);
                if (rs.next()) {
                    ArrayList<String> dataRow = new ArrayList<>();
                    for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                        dataRow.add(rs.getString(i));
                    }
                    result.add(dataRow);
                }
                rs.close();
                stmt.close();
                return result;
            }

            @Override
            public ArrayList<ArrayList<String>> showTableData(String table, String columns, String condition)
                    throws SQLException, ClassNotFoundException {
                String query = "SELECT " + columns + " FROM " + table + " WHERE " + condition;
                return customSelection(query);
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

    /* PH_UPDATE_ACCOUNT_INFO_INT_01
    Objective: Verify that the updateAccountInfo method correctly updates the sys_user table and returns true.
    Input: info = "user_name new_username"
           Pre-test state: Database contains a sys_user with user_id = "hms016".
    Expected output: true, and the sys_user record is updated with new user_name.
    Expected change: Database is rolled back after test.
     */
    @Test
    public void testUpdateAccountInfo_SuccessfulUpdate() throws SQLException {
        String info = "user_name new_username";
        boolean result = pharmacistInstance.updateAccountInfo(info);

        assertTrue(result, "The method should return true when the account info is updated successfully");

        Statement verifyStmt = connection.createStatement();
        ResultSet rs = verifyStmt.executeQuery("SELECT user_name FROM sys_user WHERE user_id = 'hms016'");
        assertTrue(rs.next(), "The sys_user record should be present in the database");
        assertEquals("new_username", rs.getString("user_name"), "The user_name should be updated");
        rs.close();
        verifyStmt.close();
    }

    /* PH_UPDATE_ACCOUNT_INFO_INT_02
    Objective: Verify that the updateAccountInfo method returns false when an SQLException occurs (e.g., invalid column).
    Input: info = "invalid_column Value"
           Pre-test state: Database contains a sys_user with user_id = "hms016".
    Expected output: false, and no changes are made to the sys_user record.
    Expected change: Database is rolled back after test.
     */
    @Test
    public void testUpdateAccountInfo_ThrowsSQLException() throws SQLException {
        String info = "i#$%ue";
        boolean result = pharmacistInstance.updateAccountInfo(info);

        assertFalse(result, "The method should return false when an SQLException occurs");

        Statement verifyStmt = connection.createStatement();
        ResultSet rs = verifyStmt.executeQuery("SELECT user_name FROM sys_user WHERE user_id = 'hms016'");
        assertTrue(rs.next(), "The sys_user record should be present in the database");
        assertEquals("user016", rs.getString("user_name"), "The user_name should remain unchanged");
        rs.close();
        verifyStmt.close();
    }

    /* PH_UPDATE_ACCOUNT_INFO_INT_03
    Objective: Verify that the updateAccountInfo method handles invalid info input and returns false.
    Input: info = "" (empty string)
           Pre-test state: Database contains a sys_user with user_id = "hms016".
    Expected output: false, and no changes are made to the sys_user record.
    Expected change: Database is rolled back after test.
     */
    @Test
    public void testUpdateAccountInfo_InvalidInfo() throws SQLException {
        String info = "";
        boolean result = pharmacistInstance.updateAccountInfo(info);

        assertFalse(result, "The method should return false when the info input is invalid");

        Statement verifyStmt = connection.createStatement();
        ResultSet rs = verifyStmt.executeQuery("SELECT user_name FROM sys_user WHERE user_id = 'hms016'");
        assertTrue(rs.next(), "The sys_user record should be present in the database");
        assertEquals("user016", rs.getString("user_name"), "The user_name should remain unchanged");
        rs.close();
        verifyStmt.close();
    }
}