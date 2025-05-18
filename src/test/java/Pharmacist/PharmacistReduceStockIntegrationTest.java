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

public class PharmacistReduceStockIntegrationTest {

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

    /* PH_REDUCE_STOCK_INT_01
    Objective: Verify that the reduceStock method correctly reduces the remaining quantity in the database.
    Input: qt = 50, stkID = "stk0001"
           Pre-test state: Database contains a stock with stock_id = "stk0001" and a drug with drug_id = "DRUG001".
    Expected output: true, and the remaining_quantity is reduced by 50.
    Expected change: Database is rolled back after test.
     */
    @Test
    public void testReduceStock_SuccessfulUpdate() throws SQLException {
        Statement stmt = connection.createStatement();
        stmt.executeUpdate("INSERT INTO drug (drug_id, drug_name, dangerous_drug) " +
                "VALUES ('DRUG001', 'Paracetamol', 1) " +
                "ON DUPLICATE KEY UPDATE drug_name = 'Paracetamol', dangerous_drug = 1");
        stmt.executeUpdate("INSERT INTO pharmacy_stock (stock_id, drug_id, brand_id, stock, remaining_quantity, manufac_date, exp_date, supplier_id, date) " +
                "VALUES ('stk0001', 'DRUG001', 'br0001', 500, 450, '2016-08-10', '2017-09-01', 'sup0001', '2016-08-20') " +
                "ON DUPLICATE KEY UPDATE drug_id = 'DRUG001', brand_id = 'br0001', stock = 500, remaining_quantity = 450, " +
                "manufac_date = '2016-08-10', exp_date = '2017-09-01', supplier_id = 'sup0001', date = '2016-08-20'");
        stmt.close();

        boolean result = pharmacistInstance.reduceStock(50, "stk0001");

        assertTrue(result, "The method should return true when the stock is reduced successfully");

        Statement verifyStmt = connection.createStatement();
        ResultSet rs = verifyStmt.executeQuery("SELECT remaining_quantity FROM pharmacy_stock WHERE stock_id = 'stk0001'");
        assertTrue(rs.next(), "The stock should be present in the database");
        assertEquals(400, rs.getInt("remaining_quantity"), "The remaining_quantity should be reduced by 50");
        rs.close();
        verifyStmt.close();
    }

    /* PH_REDUCE_STOCK_INT_02
    Objective: Verify that the reduceStock method returns true when no stock_id exists (no rows affected).
    Input: qt = 50, stkID = "stk0001"
           Pre-test state: Database is empty or has no matching stock_id.
    Expected output: true, and no stock is updated.
    Expected change: Database is rolled back after test.
     */
    @Test
    public void testReduceStock_NoStockID() throws SQLException {
        // Không chèn dữ liệu mẫu để bảng pharmacy_stock rỗng
        boolean result = pharmacistInstance.reduceStock(50, "stk0001");

        assertTrue(result, "The method should return true when no stock_id exists (no rows affected)");

        Statement verifyStmt = connection.createStatement();
        ResultSet rs = verifyStmt.executeQuery("SELECT * FROM pharmacy_stock WHERE stock_id = 'stk0001'");
        assertFalse(rs.next(), "No stock should be present in the database");
        rs.close();
        verifyStmt.close();
    }

    /* PH_REDUCE_STOCK_INT_03
    Objective: Verify that the reduceStock method returns true when the stock_id does not exist.
    Input: qt = 50, stkID = "stk9999"
           Pre-test state: Database contains a stock with stock_id = "stk0001" and a drug with drug_id = "DRUG001".
    Expected output: true, and no stock is updated.
    Expected change: Database is rolled back after test.
     */
    @Test
    public void testReduceStock_InvalidStockID() throws SQLException {
        Statement stmt = connection.createStatement();
        stmt.executeUpdate("INSERT INTO drug (drug_id, drug_name, dangerous_drug) " +
                "VALUES ('DRUG001', 'Paracetamol', 1) " +
                "ON DUPLICATE KEY UPDATE drug_name = 'Paracetamol', dangerous_drug = 1");
        stmt.executeUpdate("INSERT INTO pharmacy_stock (stock_id, drug_id, brand_id, stock, remaining_quantity, manufac_date, exp_date, supplier_id, date) " +
                "VALUES ('stk0001', 'DRUG001', 'br0001', 500, 450, '2016-08-10', '2017-09-01', 'sup0001', '2016-08-20') " +
                "ON DUPLICATE KEY UPDATE drug_id = 'DRUG001', brand_id = 'br0001', stock = 500, remaining_quantity = 450, " +
                "manufac_date = '2016-08-10', exp_date = '2017-09-01', supplier_id = 'sup0001', date = '2016-08-20'");
        stmt.close();

        boolean result = pharmacistInstance.reduceStock(50, "stk9999");

        assertTrue(result, "The method should return true when the stock_id does not exist (no rows affected)");

        Statement verifyStmt = connection.createStatement();
        ResultSet rs = verifyStmt.executeQuery("SELECT remaining_quantity FROM pharmacy_stock WHERE stock_id = 'stk0001'");
        assertTrue(rs.next(), "The original stock should be present in the database");
        assertEquals(450, rs.getInt("remaining_quantity"), "The remaining_quantity should remain unchanged");
        rs.close();
        verifyStmt.close();
    }
}