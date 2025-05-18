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

public class PharmacistUpdateStockIntegrationTest {

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
        // Thêm dữ liệu mẫu vào drug để thỏa mãn khóa ngoại
        stmt.executeUpdate("INSERT INTO drug (drug_id, drug_name, dangerous_drug) " +
                "VALUES ('DRUG001', 'Paracetamol', 1) " +
                "ON DUPLICATE KEY UPDATE drug_name = 'Paracetamol', dangerous_drug = 1");
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

    /* PH_UPDATE_STOCK_INT_01
    Objective: Verify that the updateStock method correctly inserts a new stock and returns true.
    Input: drugID = "DRUG001", brandID = "br0001", stock = "500", manuDate = "2016-08-10", expDate = "2017-09-01", suppID = "sup0001", date = "2016-08-20"
           Pre-test state: Database contains a stock with stock_id = "stk0001", a drug with drug_id = "DRUG001", a brand with brand_id = "br0001", and a supplier with supplier_id = "sup0001".
    Expected output: true, and the new stock is present in the database with stock_id = "stk0002".
    Expected change: Database is rolled back after test.
     */
    @Test
    public void testUpdateStock_SuccessfulInsertion() throws SQLException {
        Statement stmt = connection.createStatement();
        stmt.executeUpdate("INSERT INTO drug_brand_names (brand_id, brand_name, generic_name, drug_type, drug_unit, unit_price) " +
                "VALUES ('br0001', 'Panadol', 'Acetaminophen', 'Analgesic', 'Tablet', '0.50') " +
                "ON DUPLICATE KEY UPDATE brand_name = 'Panadol', generic_name = 'Acetaminophen', drug_type = 'Analgesic', drug_unit = 'Tablet', unit_price = '0.50'");
        stmt.executeUpdate("INSERT INTO suppliers (supplier_id, supplier_name) " +
                "VALUES ('sup0001', 'MediCorp') " +
                "ON DUPLICATE KEY UPDATE supplier_name = 'MediCorp'");
        stmt.executeUpdate("INSERT INTO pharmacy_stock (stock_id, drug_id, brand_id, stock, remaining_quantity, manufac_date, exp_date, supplier_id, date) " +
                "VALUES ('stk0001', 'DRUG001', 'br0001', 500, 450, '2016-08-10', '2017-09-01', 'sup0001', '2016-08-20') " +
                "ON DUPLICATE KEY UPDATE drug_id = 'DRUG001', brand_id = 'br0001', stock = 500, remaining_quantity = 450, " +
                "manufac_date = '2016-08-10', exp_date = '2017-09-01', supplier_id = 'sup0001', date = '2016-08-20'");
        stmt.close();

        boolean result = pharmacistInstance.updateStock("DRUG001", "br0001", "500", "2016-08-10", "2017-09-01", "sup0001", "2016-08-20");

        assertTrue(result, "The method should return true when the stock is added successfully");

        Statement verifyStmt = connection.createStatement();
        ResultSet rs = verifyStmt.executeQuery("SELECT * FROM pharmacy_stock WHERE stock_id = 'stk0002'");
        assertTrue(rs.next(), "The new stock should be present in the database");
        assertEquals("DRUG001", rs.getString("drug_id"), "The drug_id should match");
        assertEquals("br0001", rs.getString("brand_id"), "The brand_id should match");
        assertEquals(500, rs.getInt("stock"), "The stock should match");
        assertEquals(500, rs.getInt("remaining_quantity"), "The remaining_quantity should match");
        assertEquals("2016-08-10", rs.getString("manufac_date"), "The manufac_date should match");
        assertEquals("2017-09-01", rs.getString("exp_date"), "The exp_date should match");
        assertEquals("sup0001", rs.getString("supplier_id"), "The supplier_id should match");
        assertEquals("2016-08-20", rs.getString("date"), "The date should match");
        rs.close();
        verifyStmt.close();
    }

    /* PH_UPDATE_STOCK_INT_02
    Objective: Verify that the updateStock method returns true when no max stock_id exists.
    Input: drugID = "DRUG001", brandID = "br0001", stock = "500", manuDate = "2016-08-10", expDate = "2017-09-01", suppID = "sup0001", date = "2016-08-20"
           Pre-test state: Database is empty (no stock_id to generate).
    Expected output: true, and no new stock is added due to invalid stock_id.
    Expected change: Database is rolled back after test.
     */
    @Test
    public void testUpdateStock_NoMaxStockID() throws SQLException {
        Statement stmt = connection.createStatement();
        stmt.executeUpdate("INSERT INTO drug_brand_names (brand_id, brand_name, generic_name, drug_type, drug_unit, unit_price) " +
                "VALUES ('br0001', 'Panadol', 'Acetaminophen', 'Analgesic', 'Tablet', '0.50') " +
                "ON DUPLICATE KEY UPDATE brand_name = 'Panadol', generic_name = 'Acetaminophen', drug_type = 'Analgesic', drug_unit = 'Tablet', unit_price = '0.50'");
        stmt.executeUpdate("INSERT INTO suppliers (supplier_id, supplier_name) " +
                "VALUES ('sup0001', 'MediCorp') " +
                "ON DUPLICATE KEY UPDATE supplier_name = 'MediCorp'");
        stmt.close();

        boolean result = pharmacistInstance.updateStock("DRUG001", "br0001", "500", "2016-08-10", "2017-09-01", "sup0001", "2016-08-20");

        assertTrue(result, "The method should return true when no max stock_id exists (current behavior)");

        Statement verifyStmt = connection.createStatement();
        ResultSet rs = verifyStmt.executeQuery("SELECT * FROM pharmacy_stock WHERE stock_id = ''");
        assertFalse(rs.next(), "No new stock should be added to the database due to invalid stock_id");
        rs.close();
        verifyStmt.close();
    }

    /* PH_UPDATE_STOCK_INT_03
    Objective: Verify that the updateStock method returns false when the drug_id is invalid.
    Input: drugID = "INVALID", brandID = "br0001", stock = "500", manuDate = "2016-08-10", expDate = "2017-09-01", suppID = "sup0001", date = "2016-08-20"
           Pre-test state: Database contains a stock with stock_id = "stk0001", a brand with brand_id = "br0001", and a supplier with supplier_id = "sup0001".
    Expected output: false, and no new stock is added due to foreign key constraint violation.
    Expected change: Database is rolled back after test.
     */
    @Test
    public void testUpdateStock_InvalidDrugID() throws SQLException {
        Statement stmt = connection.createStatement();
        stmt.executeUpdate("INSERT INTO drug_brand_names (brand_id, brand_name, generic_name, drug_type, drug_unit, unit_price) " +
                "VALUES ('br0001', 'Panadol', 'Acetaminophen', 'Analgesic', 'Tablet', '0.50') " +
                "ON DUPLICATE KEY UPDATE brand_name = 'Panadol', generic_name = 'Acetaminophen', drug_type = 'Analgesic', drug_unit = 'Tablet', unit_price = '0.50'");
        stmt.executeUpdate("INSERT INTO suppliers (supplier_id, supplier_name) " +
                "VALUES ('sup0001', 'MediCorp') " +
                "ON DUPLICATE KEY UPDATE supplier_name = 'MediCorp'");
        stmt.executeUpdate("INSERT INTO pharmacy_stock (stock_id, drug_id, brand_id, stock, remaining_quantity, manufac_date, exp_date, supplier_id, date) " +
                "VALUES ('stk0001', 'DRUG001', 'br0001', 500, 450, '2016-08-10', '2017-09-01', 'sup0001', '2016-08-20') " +
                "ON DUPLICATE KEY UPDATE drug_id = 'DRUG001', brand_id = 'br0001', stock = 500, remaining_quantity = 450, " +
                "manufac_date = '2016-08-10', exp_date = '2017-09-01', supplier_id = 'sup0001', date = '2016-08-20'");
        stmt.close();

        boolean result = pharmacistInstance.updateStock("INVALID", "br0001", "500", "2016-08-10", "2017-09-01", "sup0001", "2016-08-20");

        assertFalse(result, "The method should return false when the drug_id is invalid");

        Statement verifyStmt = connection.createStatement();
        ResultSet rs = verifyStmt.executeQuery("SELECT * FROM pharmacy_stock WHERE stock_id = 'stk0002'");
        assertFalse(rs.next(), "No new stock should be added to the database");
        rs.close();
        verifyStmt.close();
    }
}