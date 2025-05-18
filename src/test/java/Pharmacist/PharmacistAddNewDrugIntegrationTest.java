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

public class PharmacistAddNewDrugIntegrationTest {

    private Pharmacist pharmacistInstance;
    private Connection connection;

    @BeforeEach
    public void setUp() throws SQLException {
        connection = DriverManager.getConnection(
                "jdbc:mysql://127.0.0.1:3306/test_HMS2?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                "root", "hieu");
        connection.setAutoCommit(false);

        // Thêm dữ liệu mẫu vào sys_user

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

    /* PH_ADD_DRUG_INT_01
    Objective: Verify that the addNewDrug method correctly inserts a new drug into the database.
    Input: drugInfo = "drug_name nafcillin,dangerous_drug 1"
           Pre-test state: Database contains a drug with drug_id = "d0006".
    Expected output: true, and the new drug is present in the database.
    Expected change: Database is rolled back after test.
     */
    @Test
    public void testAddNewDrug_SuccessfulInsertion() throws SQLException {
        Statement stmt = connection.createStatement();
        stmt.executeUpdate("INSERT INTO drug (drug_id, drug_name, dangerous_drug) " +
                "VALUES ('d0006', 'Aspirin', 1)");
        stmt.close();

        String drugInfo = "drug_name nafcillin,dangerous_drug 1";
        boolean result = pharmacistInstance.addNewDrug(drugInfo);

        assertTrue(result, "The method should return true when the drug is added successfully");

        Statement verifyStmt = connection.createStatement();
        ResultSet rs = verifyStmt.executeQuery("SELECT * FROM drug WHERE drug_id = 'd0007'");
        assertTrue(rs.next(), "The new drug should be present in the database");
        assertEquals("nafcillin", rs.getString("drug_name"), "The drug_name should match");
        assertEquals(1, rs.getInt("dangerous_drug"), "The dangerous_drug should match");
        rs.close();
        verifyStmt.close();
    }

    /* PH_ADD_DRUG_INT_02
    Objective: Verify that the addNewDrug method fails when no max drug_id exists
               and returns false.
    Input: drugInfo = "drug_name nafcillin,dangerous_drug 1"
           Pre-test state: Database is empty (no drug_id to generate).
    Expected output: false, and no new drug is added.
    Expected change: Database is rolled back after test.
     */
    @Test
    public void testAddNewDrug_NoMaxDrugID() throws SQLException {
        String drugInfo = "drug_name nafcillin,dangerous_drug 1";
        boolean result = pharmacistInstance.addNewDrug(drugInfo);

        assertFalse(result, "The method should return false when no max drug_id exists");

        Statement verifyStmt = connection.createStatement();
        ResultSet rs = verifyStmt.executeQuery("SELECT * FROM drug WHERE drug_id = 'd0006'");
        assertFalse(rs.next(), "No new drug should be added to the database");
        rs.close();
        verifyStmt.close();
    }

    /* PH_ADD_DRUG_INT_03
    Objective: Verify that the addNewDrug method handles invalid drugInfo input
               and returns false.
    Input: drugInfo = "" (empty string)
           Pre-test state: Database contains a drug with drug_id = "d0006".
    Expected output: false, and no new drug is added.
    Expected change: Database is rolled back after test.
     */
    @Test
    public void testAddNewDrug_InvalidInput() throws SQLException {
        Statement stmt = connection.createStatement();
        stmt.executeUpdate("INSERT INTO drug (drug_id, drug_name, dangerous_drug) " +
                "VALUES ('d0006', 'Aspirin', 1)");
        stmt.close();

        String drugInfo = "";
        boolean result = pharmacistInstance.addNewDrug(drugInfo);

        assertFalse(result, "The method should return false when drugInfo is invalid");

        Statement verifyStmt = connection.createStatement();
        ResultSet rs = verifyStmt.executeQuery("SELECT * FROM drug WHERE drug_id = 'd0007'");
        assertFalse(rs.next(), "No new drug should be added to the database");
        rs.close();
        verifyStmt.close();
    }
}