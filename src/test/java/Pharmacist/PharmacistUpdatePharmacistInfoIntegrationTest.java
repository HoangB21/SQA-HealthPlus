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

public class PharmacistUpdatePharmacistInfoIntegrationTest {

    private Pharmacist pharmacistInstance;
    private Connection connection;

    @BeforeEach
    public void setUp() throws SQLException {
        connection = DriverManager.getConnection(
                "jdbc:mysql://127.0.0.1:3306/test_HMS2?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                "root", "hieu");
        connection.setAutoCommit(false);

        // Thêm hoặc cập nhật dữ liệu mẫu vào sys_user và pharmacist
        Statement stmt = connection.createStatement();
        stmt.executeUpdate("INSERT INTO sys_user (user_id, user_name, user_type) " +
                "VALUES ('hms016', 'user016', 'pharmacist') " +
                "ON DUPLICATE KEY UPDATE user_name = 'user016', user_type = 'pharmacist'");
        stmt.executeUpdate("INSERT INTO pharmacist (pharmacist_id, user_id, education, training) " +
                "VALUES ('PH001', 'hms016', 'BPharm', 'Basic Training') " +
                "ON DUPLICATE KEY UPDATE user_id = 'hms016', education = 'BPharm', training = 'Basic Training'");
        stmt.close();

        pharmacistInstance = new Pharmacist("user016");
        pharmacistInstance.pharmacistID = "PH001"; // Giả định pharmacistID được thiết lập
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

    /* PH_UPDATE_PHARMACIST_INFO_INT_01
    Objective: Verify that the updatePharmacistInfo method correctly updates the pharmacist table and returns true.
    Input: info = "education MPharm#training Advanced Training"
           Pre-test state: Database contains a pharmacist with pharmacist_id = "PH001".
    Expected output: true, and the pharmacist record is updated with new education and training.
    Expected change: Database is rolled back after test.
     */
    @Test
    public void testUpdatePharmacistInfo_SuccessfulUpdate() throws SQLException {
        String info = "education MPharm#training Advanced Training";
        boolean result = pharmacistInstance.updatePharmacistInfo(info);

        assertTrue(result, "The method should return true when the pharmacist info is updated successfully");

        Statement verifyStmt = connection.createStatement();
        ResultSet rs = verifyStmt.executeQuery("SELECT education, training FROM pharmacist WHERE pharmacist_id = 'PH001'");
        assertTrue(rs.next(), "The pharmacist record should be present in the database");
        assertEquals("MPharm", rs.getString("education"), "The education should be updated");
        assertEquals("Advanced Training", rs.getString("training"), "The training should be updated");
        rs.close();
        verifyStmt.close();
    }

    /* PH_UPDATE_PHARMACIST_INFO_INT_02
    Objective: Verify that the updatePharmacistInfo method returns false when an SQLException occurs (e.g., invalid column).
    Input: info = "invalid_column Value"
           Pre-test state: Database contains a pharmacist with pharmacist_id = "PH001".
    Expected output: false, and no changes are made to the pharmacist record.
    Expected change: Database is rolled back after test.
     */
    @Test
    public void testUpdatePharmacistInfo_ThrowsSQLException() throws SQLException {
        String info = "invalid_col@#$@umn Value";
        boolean result = pharmacistInstance.updatePharmacistInfo(info);

        assertFalse(result, "The method should return false when an SQLException occurs");

        Statement verifyStmt = connection.createStatement();
        ResultSet rs = verifyStmt.executeQuery("SELECT education, training FROM pharmacist WHERE pharmacist_id = 'PH001'");
        assertTrue(rs.next(), "The pharmacist record should be present in the database");
        assertEquals("BPharm", rs.getString("education"), "The education should remain unchanged");
        assertEquals("Basic Training", rs.getString("training"), "The training should remain unchanged");
        rs.close();
        verifyStmt.close();
    }

    /* PH_UPDATE_PHARMACIST_INFO_INT_03
    Objective: Verify that the updatePharmacistInfo method handles invalid info input and returns false.
    Input: info = "" (empty string)
           Pre-test state: Database contains a pharmacist with pharmacist_id = "PH001".
    Expected output: false, and no changes are made to the pharmacist record.
    Expected change: Database is rolled back after test.
     */
    @Test
    public void testUpdatePharmacistInfo_InvalidInfo() throws SQLException {
        String info = "";
        boolean result = pharmacistInstance.updatePharmacistInfo(info);

        assertFalse(result, "The method should return false when the info input is invalid");

        Statement verifyStmt = connection.createStatement();
        ResultSet rs = verifyStmt.executeQuery("SELECT education, training FROM pharmacist WHERE pharmacist_id = 'PH001'");
        assertTrue(rs.next(), "The pharmacist record should be present in the database");
        assertEquals("BPharm", rs.getString("education"), "The education should remain unchanged");
        assertEquals("Basic Training", rs.getString("training"), "The training should remain unchanged");
        rs.close();
        verifyStmt.close();
    }
}