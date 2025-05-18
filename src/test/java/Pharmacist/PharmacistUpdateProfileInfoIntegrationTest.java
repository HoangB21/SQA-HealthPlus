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

public class PharmacistUpdateProfileInfoIntegrationTest {

    private Pharmacist pharmacistInstance;
    private Connection connection;

    @BeforeEach
    public void setUp() throws SQLException {
        connection = DriverManager.getConnection(
                "jdbc:mysql://127.0.0.1:3306/test_HMS2?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                "root", "hieu");
        connection.setAutoCommit(false);

        // Thêm hoặc cập nhật dữ liệu mẫu vào sys_user và person
        Statement stmt = connection.createStatement();
        stmt.executeUpdate("INSERT INTO person (person_id, first_name, last_name) " +
                "VALUES ('p001', 'OldFirst', 'OldLast') " +
                "ON DUPLICATE KEY UPDATE first_name = 'OldFirst', last_name = 'OldLast'");
        stmt.executeUpdate("INSERT INTO sys_user (user_id, user_name, user_type, person_id) " +
                "VALUES ('hms016', 'pharmacist001', 'pharmacist', 'p001') " +
                "ON DUPLICATE KEY UPDATE user_name = 'pharmacist001', user_type = 'pharmacist', person_id = 'p001'");
        stmt.close();

        pharmacistInstance = new Pharmacist("user016");
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

    /* PH_UPDATE_PROFILE_INFO_INT_01
    Objective: Verify that the updateProfileInfo method correctly updates the person table and returns true.
    Input: info = "first_name John#last_name Doe"
           Pre-test state: Database contains a person with person_id = "p001" and sys_user with user_name = "pharmacist001".
    Expected output: true, and the person record is updated with new first_name and last_name.
    Expected change: Database is rolled back after test.
     */
    @Test
    public void testUpdateProfileInfo_SuccessfulUpdate() throws SQLException {
        String info = "first_name John#last_name Doe";
        boolean result = pharmacistInstance.updateProfileInfo(info);

        assertTrue(result, "The method should return true when the profile is updated successfully");

        Statement verifyStmt = connection.createStatement();
        ResultSet rs = verifyStmt.executeQuery("SELECT first_name, last_name FROM person WHERE person_id = 'p001'");
        assertTrue(rs.next(), "The person record should be present in the database");
        assertEquals("John", rs.getString("first_name"), "The first_name should be updated");
        assertEquals("Doe", rs.getString("last_name"), "The last_name should be updated");
        rs.close();
        verifyStmt.close();
    }

    /* PH_UPDATE_PROFILE_INFO_INT_02
    Objective: Verify that the updateProfileInfo method returns false when an SQLException occurs (e.g., invalid column).
    Input: info = "invalid_column Value"
           Pre-test state: Database contains a person with person_id = "p001" and sys_user with user_name = "pharmacist001".
    Expected output: false, and no changes are made to the person record.
    Expected change: Database is rolled back after test.
     */
    @Test
    public void testUpdateProfileInfo_ThrowsSQLException() throws SQLException {
        String info = "invalid_column Value";
        boolean result = pharmacistInstance.updateProfileInfo(info);

        assertFalse(result, "The method should return false when an SQLException occurs");

        Statement verifyStmt = connection.createStatement();
        ResultSet rs = verifyStmt.executeQuery("SELECT first_name, last_name FROM person WHERE person_id = 'p001'");
        assertTrue(rs.next(), "The person record should be present in the database");
        assertEquals("OldFirst", rs.getString("first_name"), "The first_name should remain unchanged");
        assertEquals("OldLast", rs.getString("last_name"), "The last_name should remain unchanged");
        rs.close();
        verifyStmt.close();
    }

    /* PH_UPDATE_PROFILE_INFO_INT_03
    Objective: Verify that the updateProfileInfo method handles invalid info input and returns false.
    Input: info = "" (empty string)
           Pre-test state: Database contains a person with person_id = "p001" and sys_user with user_name = "pharmacist001".
    Expected output: false, and no changes are made to the person record.
    Expected change: Database is rolled back after test.
     */
    @Test
    public void testUpdateProfileInfo_InvalidInfo() throws SQLException {
        String info = "";
        boolean result = pharmacistInstance.updateProfileInfo(info);

        assertFalse(result, "The method should return false when the info input is invalid");

        Statement verifyStmt = connection.createStatement();
        ResultSet rs = verifyStmt.executeQuery("SELECT first_name, last_name FROM person WHERE person_id = 'p001'");
        assertTrue(rs.next(), "The person record should be present in the database");
        assertEquals("OldFirst", rs.getString("first_name"), "The first_name should remain unchanged");
        assertEquals("OldLast", rs.getString("last_name"), "The last_name should remain unchanged");
        rs.close();
        verifyStmt.close();
    }
}