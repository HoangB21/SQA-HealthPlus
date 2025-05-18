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

public class PharmacistAddNewSupplierIntegrationTest {

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

    /* PH_ADD_NEW_SUPPLIER_INT_01
    Objective: Verify that the addNewSupplier method correctly inserts a new supplier and returns the generated supplier_id.
    Input: suppName = "PharmaCorp"
           Pre-test state: Database contains a supplier with supplier_id = "sup0009".
    Expected output: The generated supplier_id ("sup0010"), and the new supplier is present in the database.
    Expected change: Database is rolled back after test.
     */
    @Test
    public void testAddNewSupplier_SuccessfulInsertion() throws SQLException {
        Statement stmt = connection.createStatement();
        stmt.executeUpdate("INSERT INTO suppliers (supplier_id, supplier_name) " +
                "VALUES ('sup0009', 'MediCorp') " +
                "ON DUPLICATE KEY UPDATE supplier_name = 'MediCorp'");
        stmt.close();

        String result = pharmacistInstance.addNewSupplier("PharmaCorp");

        assertEquals("sup0010", result, "The method should return the generated supplier_id 'sup0010'");

        Statement verifyStmt = connection.createStatement();
        ResultSet rs = verifyStmt.executeQuery("SELECT * FROM suppliers WHERE supplier_id = 'sup0010'");
        assertTrue(rs.next(), "The new supplier should be present in the database");
        assertEquals("PharmaCorp", rs.getString("supplier_name"), "The supplier_name should match");
        rs.close();
        verifyStmt.close();
    }

    /* PH_ADD_NEW_SUPPLIER_INT_02
    Objective: Verify that the addNewSupplier method returns an empty string when no max supplier_id exists.
    Input: suppName = "PharmaCorp"
           Pre-test state: Database is empty (no supplier_id to generate).
    Expected output: Empty string, and no new supplier is added.
    Expected change: Database is rolled back after test.
     */
    @Test
    public void testAddNewSupplier_NoMaxSupplierID() throws SQLException {
        String result = pharmacistInstance.addNewSupplier("PharmaCorp");

        assertEquals("", result, "The method should return an empty string when no max supplier_id exists");

        Statement verifyStmt = connection.createStatement();
        ResultSet rs = verifyStmt.executeQuery("SELECT * FROM suppliers WHERE supplier_name = 'PharmaCorp'");
        assertFalse(rs.next(), "No new supplier should be added to the database");
        rs.close();
        verifyStmt.close();
    }

    /* PH_ADD_NEW_SUPPLIER_INT_03
    Objective: Verify that the addNewSupplier method handles invalid suppName input and returns the generated supplier_id.
    Input: suppName = ""
           Pre-test state: Database contains a supplier with supplier_id = "sup0009".
    Expected output: The generated supplier_id ("sup0010"), and the new supplier with empty name is added.
    Expected change: Database is rolled back after test.
     */
    @Test
    public void testAddNewSupplier_InvalidSuppName() throws SQLException {
        Statement stmt = connection.createStatement();
        stmt.executeUpdate("INSERT INTO suppliers (supplier_id, supplier_name) " +
                "VALUES ('sup0009', 'MediCorp') " +
                "ON DUPLICATE KEY UPDATE supplier_name = 'MediCorp'");
        stmt.close();

        String result = pharmacistInstance.addNewSupplier("");

        assertEquals("sup0010", result, "The method should return the generated supplier_id 'sup0010'");

        Statement verifyStmt = connection.createStatement();
        ResultSet rs = verifyStmt.executeQuery("SELECT * FROM suppliers WHERE supplier_id = 'sup0010'");
        assertTrue(rs.next(), "The new supplier should be present in the database");
        assertEquals("", rs.getString("supplier_name"), "The supplier_name should be empty");
        rs.close();
        verifyStmt.close();
    }
}