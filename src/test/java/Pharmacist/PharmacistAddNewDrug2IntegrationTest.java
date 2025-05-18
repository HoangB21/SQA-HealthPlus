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

public class PharmacistAddNewDrug2IntegrationTest {

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

    /* PH_ADD_NEW_DRUG2_INT_01
    Objective: Verify that the addNewDrug2 method correctly inserts a new drug and returns the generated drug_id.
    Input: genName = "Nafcillin"
           Pre-test state: Database contains a drug with drug_id = "d0006".
    Expected output: The generated drug_id ("d0007"), and the new drug is present in the database.
    Expected change: Database is rolled back after test.
     */
    @Test
    public void testAddNewDrug2_SuccessfulInsertion() throws SQLException {
        Statement stmt = connection.createStatement();
        stmt.executeUpdate("INSERT INTO drug (drug_id, drug_name, dangerous_drug) " +
                "VALUES ('d0006', 'Aspirin', 1) " +
                "ON DUPLICATE KEY UPDATE drug_name = 'Aspirin', dangerous_drug = 1");
        stmt.close();

        String result = pharmacistInstance.addNewDrug2("Nafcillin");

        assertEquals("d0007", result, "The method should return the generated drug_id 'd0007'");

        Statement verifyStmt = connection.createStatement();
        ResultSet rs = verifyStmt.executeQuery("SELECT * FROM drug WHERE drug_id = 'd0007'");
        assertTrue(rs.next(), "The new drug should be present in the database");
        assertEquals("Nafcillin", rs.getString("drug_name"), "The drug_name should match");
        assertEquals(0, rs.getInt("dangerous_drug"), "The dangerous_drug should be 0");
        rs.close();
        verifyStmt.close();
    }

    /* PH_ADD_NEW_DRUG2_INT_02
    Objective: Verify that the addNewDrug2 method returns an empty string when no max drug_id exists.
    Input: genName = "Nafcillin"
           Pre-test state: Database is empty (no drug_id to generate).
    Expected output: Empty string, and no new drug is added.
    Expected change: Database is rolled back after test.
     */
    @Test
    public void testAddNewDrug2_NoMaxDrugID() throws SQLException {
        String result = pharmacistInstance.addNewDrug2("Nafcillin");

        assertEquals("", result, "The method should return an empty string when no max drug_id exists");

        Statement verifyStmt = connection.createStatement();
        ResultSet rs = verifyStmt.executeQuery("SELECT * FROM drug WHERE drug_name = 'Nafcillin'");
        assertFalse(rs.next(), "No new drug should be added to the database");
        rs.close();
        verifyStmt.close();
    }

    /* PH_ADD_NEW_DRUG2_INT_03
    Objective: Verify that the addNewDrug2 method handles invalid genName input and returns the generated drug_id.
    Input: genName = "" (empty string)
           Pre-test state: Database contains a drug with drug_id = "d0006".
    Expected output: The generated drug_id ("d0007"), and the new drug with empty name is added.
    Expected change: Database is rolled back after test.
     */
    @Test
    public void testAddNewDrug2_InvalidGenName() throws SQLException {
        Statement stmt = connection.createStatement();
        stmt.executeUpdate("INSERT INTO drug (drug_id, drug_name, dangerous_drug) " +
                "VALUES ('d0006', 'Aspirin', 1) " +
                "ON DUPLICATE KEY UPDATE drug_name = 'Aspirin', dangerous_drug = 1");
        stmt.close();

        String result = pharmacistInstance.addNewDrug2("");

        assertEquals("d0007", result, "The method should return the generated drug_id 'd0007'");

        Statement verifyStmt = connection.createStatement();
        ResultSet rs = verifyStmt.executeQuery("SELECT * FROM drug WHERE drug_id = 'd0007'");
        assertTrue(rs.next(), "The new drug should be present in the database");
        assertEquals("", rs.getString("drug_name"), "The drug_name should be empty");
        assertEquals(0, rs.getInt("dangerous_drug"), "The dangerous_drug should be 0");
        rs.close();
        verifyStmt.close();
    }
}