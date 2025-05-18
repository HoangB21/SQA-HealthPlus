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

public class PharmacistAddNewBrandIntegrationTest {

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

    /* PH_ADD_NEW_BRAND_INT_01
    Objective: Verify that the addNewBrand method correctly inserts a new brand and returns the generated brand_id.
    Input: brandName = "Panadol", genName = "Acetaminophen", type = "Analgesic", unit = "Tablet", price = "0.50"
           Pre-test state: Database contains a brand with brand_id = "br0017".
    Expected output: The generated brand_id ("br0018"), and the new brand is present in the database.
    Expected change: Database is rolled back after test.
     */
    @Test
    public void testAddNewBrand_SuccessfulInsertion() throws SQLException {
        Statement stmt = connection.createStatement();
        stmt.executeUpdate("INSERT INTO drug_brand_names (brand_id, brand_name, generic_name, drug_type, drug_unit, unit_price) " +
                "VALUES ('br0017', 'AspirinBrand', 'Aspirin', 'Analgesic', 'Tablet', '0.30') " +
                "ON DUPLICATE KEY UPDATE brand_name = 'AspirinBrand', generic_name = 'Aspirin', drug_type = 'Analgesic', drug_unit = 'Tablet', unit_price = '0.30'");
        stmt.close();

        String result = pharmacistInstance.addNewBrand("Panadol", "Acetaminophen", "Analgesic", "Tablet", "0.50");

        assertEquals("br0018", result, "The method should return the generated brand_id 'br0018'");

        Statement verifyStmt = connection.createStatement();
        ResultSet rs = verifyStmt.executeQuery("SELECT * FROM drug_brand_names WHERE brand_id = 'br0018'");
        assertTrue(rs.next(), "The new brand should be present in the database");
        assertEquals("Panadol", rs.getString("brand_name"), "The brand_name should match");
        assertEquals("Acetaminophen", rs.getString("generic_name"), "The generic_name should match");
        assertEquals("Analgesic", rs.getString("drug_type"), "The drug_type should match");
        assertEquals("Tablet", rs.getString("drug_unit"), "The drug_unit should match");
        assertEquals("0.50", rs.getString("unit_price"), "The unit_price should match");
        rs.close();
        verifyStmt.close();
    }

    /* PH_ADD_NEW_BRAND_INT_02
    Objective: Verify that the addNewBrand method returns an empty string when no max brand_id exists.
    Input: brandName = "Panadol", genName = "Acetaminophen", type = "Analgesic", unit = "Tablet", price = "0.50"
           Pre-test state: Database is empty (no brand_id to generate).
    Expected output: Empty string, and no new brand is added.
    Expected change: Database is rolled back after test.
     */
    @Test
    public void testAddNewBrand_NoMaxBrandID() throws SQLException {
        String result = pharmacistInstance.addNewBrand("Panadol", "Acetaminophen", "Analgesic", "Tablet", "0.50");

        assertEquals("", result, "The method should return an empty string when no max brand_id exists");

        Statement verifyStmt = connection.createStatement();
        ResultSet rs = verifyStmt.executeQuery("SELECT * FROM drug_brand_names WHERE brand_name = 'Panadol'");
        assertFalse(rs.next(), "No new brand should be added to the database");
        rs.close();
        verifyStmt.close();
    }

    /* PH_ADD_NEW_BRAND_INT_03
    Objective: Verify that the addNewBrand method handles invalid brandName input and returns the generated brand_id.
    Input: brandName = "", genName = "Acetaminophen", type = "Analgesic", unit = "Tablet", price = "0.50"
           Pre-test state: Database contains a brand with brand_id = "br0018".
    Expected output: The generated brand_id ("br0018"), and the new brand with empty name is added.
    Expected change: Database is rolled back after test.
     */
    @Test
    public void testAddNewBrand_InvalidBrandName() throws SQLException {
        Statement stmt = connection.createStatement();
        stmt.executeUpdate("INSERT INTO drug_brand_names (brand_id, brand_name, generic_name, drug_type, drug_unit, unit_price) " +
                "VALUES ('br0017', 'AspirinBrand', 'Aspirin', 'Analgesic', 'Tablet', '0.30') " +
                "ON DUPLICATE KEY UPDATE brand_name = 'AspirinBrand', generic_name = 'Aspirin', drug_type = 'Analgesic', drug_unit = 'Tablet', unit_price = '0.30'");
        stmt.close();

        String result = pharmacistInstance.addNewBrand("", "Acetaminophen", "Analgesic", "Tablet", "0.50");

        assertEquals("br0018", result, "The method should return the generated brand_id 'br0018'");

        Statement verifyStmt = connection.createStatement();
        ResultSet rs = verifyStmt.executeQuery("SELECT * FROM drug_brand_names WHERE brand_id = 'br0018'");
        assertTrue(rs.next(), "The new brand should be present in the database");
        assertEquals("", rs.getString("brand_name"), "The brand_name should be empty");
        assertEquals("Acetaminophen", rs.getString("generic_name"), "The generic_name should match");
        rs.close();
        verifyStmt.close();
    }
}