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

public class PharmacistCheckForGenNameIntegrationTest {

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
                "VALUES ('hms016', 'pharmacist001', 'pharmacist') " +
                "ON DUPLICATE KEY UPDATE user_name = 'pharmacist001', user_type = 'pharmacist'");
        stmt.close();

        pharmacistInstance = new Pharmacist("user016");
        pharmacistInstance.dbOperator = new DatabaseOperator() {
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
            public boolean customInsertion(String query) throws SQLException, ClassNotFoundException {
                Statement stmt = connection.createStatement();
                int rowsAffected = stmt.executeUpdate(query);
                stmt.close();
                return rowsAffected > 0;
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

    /* PH_CHECK_GEN_NAME_INT_01
    Objective: Verify that the checkForGenName method correctly retrieves the drug_id for a given generic name.
    Input: genName = "Paracetamol"
           Pre-test state: Database contains a drug with drug_name = "Paracetamol".
    Expected output: The drug_id of the matching drug.
    Expected change: Database is rolled back after test.
     */
    @Test
    public void testCheckForGenName_SuccessfulRetrieval() throws SQLException {
        Statement stmt = connection.createStatement();
        stmt.executeUpdate("INSERT INTO drug (drug_id, drug_name, dangerous_drug) " +
                "VALUES ('d0001', 'Paracetamol', 1) " +
                "ON DUPLICATE KEY UPDATE drug_name = 'Paracetamol', dangerous_drug = 1");
        stmt.close();

        String result = pharmacistInstance.checkForGenName("Paracetamol");

        assertEquals("d0001", result, "The drug_id should match the generic name");
    }

    /* PH_CHECK_GEN_NAME_INT_02
    Objective: Verify that the checkForGenName method returns "0" when the generic name is not found.
    Input: genName = "NonExistentDrug"
           Pre-test state: Database is empty or has no matching drug.
    Expected output: "0"
    Expected change: Database is rolled back after test.
     */
    @Test
    public void testCheckForGenName_NoMatchingDrug() throws SQLException {
        String result = pharmacistInstance.checkForGenName("NonExistentDrug");

        assertEquals("0", result, "The method should return '0' when no matching drug is found");
    }

    /* PH_CHECK_GEN_NAME_INT_03
    Objective: Verify that the checkForGenName method returns "0" when an SQLException occurs.
    Input: genName = "Paracetamol"
           Pre-test state: Database contains an invalid table or query causes SQLException.
    Expected output: "0"
    Expected change: Database is rolled back after test.
     */
    @Test
    public void testCheckForGenName_NoData() throws SQLException {
        Statement stmt = connection.createStatement();
        stmt.executeUpdate("INSERT INTO drug (drug_id, drug_name, dangerous_drug) " +
                "VALUES ('d0001', 'Aspirin', 1) " +
                "ON DUPLICATE KEY UPDATE drug_name = 'Aspirin', dangerous_drug = 1");
        stmt.close();

        String result = pharmacistInstance.checkForGenName("Paracetamol");

        assertEquals("0", result, "The method should return '0' when no data rows are returned for the generic name");
    }
}