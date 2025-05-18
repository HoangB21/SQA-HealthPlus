package Cashier;

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

public class CashierBillIntegrationTest {

    private Cashier cashierInstance;
    private Connection connection;

    @BeforeEach
    public void setUp() throws SQLException {
        connection = DriverManager.getConnection(
                "jdbc:mysql://127.0.0.1:3306/test_HMS2?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                "root", "123456");
        connection.setAutoCommit(false);

        // Thêm hoặc cập nhật dữ liệu mẫu vào sys_user, person, patient, bill
        Statement stmt = connection.createStatement();
        // Chèn sys_user trước, để user_id tồn tại
        stmt.executeUpdate("INSERT INTO sys_user (user_id, user_name, user_type, password, profile_pic) " +
                "VALUES ('hms020', 'user020', 'cashier', '1234', 'user020ProfPic.png') " +
                "ON DUPLICATE KEY UPDATE user_name = 'user020', user_type = 'cashier', " +
                "password = '1234', profile_pic = 'user020ProfPic.png', person_id = NULL");
        // Chèn person, sử dụng user_id = 'hms020' đã tồn tại
        stmt.executeUpdate("INSERT INTO person (person_id, user_id, nic, gender, date_of_birth, address, mobile, " +
                "first_name, last_name, email, nationality, religion) " +
                "VALUES ('p020', 'hms020', '872984565V', 'M', '1987-10-24', '123/f Yanthampalawa Kurunegala', " +
                "'0713457779', 'Lakshitha', 'Rangana', 'lakshithaasd@gmail.com', 'Sri Lankan', 'Buddhism') " +
                "ON DUPLICATE KEY UPDATE user_id = 'hms020', nic = '872984565V', gender = 'M', " +
                "date_of_birth = '1987-10-24', address = '123/f Yanthampalawa Kurunegala', mobile = '0713457779', " +
                "first_name = 'Lakshitha', last_name = 'Rangana', email = 'lakshithaasd@gmail.com', " +
                "nationality = 'Sri Lankan', religion = 'Buddhism'");
        // Cập nhật sys_user để thêm person_id
        stmt.executeUpdate("UPDATE sys_user SET person_id = 'p020' WHERE user_id = 'hms020'");
        // Chèn patient
        stmt.executeUpdate("INSERT INTO patient (patient_id, person_id) " +
                "VALUES ('hms0001pa', 'p020') " +
                "ON DUPLICATE KEY UPDATE person_id = 'p020'");
        // Chèn bill
        stmt.executeUpdate("INSERT INTO bill (bill_id, bill_date, doctor_fee, hospital_fee, pharmacy_fee, " +
                "laboratory_fee, appointment_fee, vat, discount, total, payment_method, patient_id, refund) " +
                "VALUES ('hms0007b', '2016-08-30 14:30:00', 200, 150, 300, 0, 500, 60, 0, 1210, 'pending', 'hms0001pa', 0) " +
                "ON DUPLICATE KEY UPDATE bill_date = '2016-08-30 14:30:00', doctor_fee = 200, hospital_fee = 150, " +
                "pharmacy_fee = 300, laboratory_fee = 0, appointment_fee = 500, vat = 60, discount = 0, total = 1210, " +
                "payment_method = 'pending', patient_id = 'hms0001pa', refund = 0");
        stmt.close();

        cashierInstance = new Cashier("hms020");
        cashierInstance.dbOperator = new DatabaseOperator() {
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

    /* CA_BILL_INT_01
    Objective: Verify that the bill method correctly inserts a new bill into the bill table and returns the new bill_id.
    Input: billInfo = "patient_id hms0001pa,doctor_fee 300,pharmacy_fee 400"
           Pre-test state: Database contains a bill with bill_id = "hms0007b".
    Expected output: New bill_id (e.g., "hms0008b"), and the new bill is present in the bill table.
    Expected change: Database is rolled back after test.
     */
    @Test
    public void testBill_SuccessfulInsertion() throws SQLException {
        String billInfo = "patient_id hms0001pa,doctor_fee 300,pharmacy_fee 400";
        String result = cashierInstance.bill(billInfo);

        assertNotNull(result, "The result should not be null");
        assertNotEquals("0", result, "The result should be a valid bill_id");
        assertEquals("hms0008b", result, "The bill_id should be 'hms0008b'");

        Statement verifyStmt = connection.createStatement();
        ResultSet rs = verifyStmt.executeQuery("SELECT * FROM bill WHERE bill_id = 'hms0008b'");
        assertTrue(rs.next(), "The new bill should be present in the database");
        assertEquals("hms0001pa", rs.getString("patient_id"), "The patient_id should match");
        assertEquals(300, rs.getInt("doctor_fee"), "The doctor_fee should match");
        assertEquals(400, rs.getInt("pharmacy_fee"), "The pharmacy_fee should match");
        assertNotNull(rs.getString("bill_date"), "The bill_date should not be null");
        rs.close();
        verifyStmt.close();
    }

    /* CA_BILL_INT_02
    Objective: Verify that the bill method returns "0" when an SQLException occurs (e.g., invalid column in billInfo).
    Input: billInfo = "invalid_column 100"
           Pre-test state: Database contains a bill with bill_id = "hms0007b".
    Expected output: "0", and no new bill is added.
    Expected change: Database is rolled back after test.
     */
    @Test
    public void testBill_ThrowsSQLException() throws SQLException {
        String billInfo = "invalid_column 100";
        String result = cashierInstance.bill(billInfo);

        assertEquals("0", result, "The method should return '0' when an SQLException occurs");

        Statement verifyStmt = connection.createStatement();
        ResultSet rs = verifyStmt.executeQuery("SELECT * FROM bill WHERE bill_id = 'hms0008b'");
        assertFalse(rs.next(), "No new bill should be added to the database");
        rs.close();
        verifyStmt.close();
    }

    /* CA_BILL_INT_03
    Objective: Verify that the bill method returns "0" when billInfo is invalid (e.g., empty string).
    Input: billInfo = ""
           Pre-test state: Database contains a bill with bill_id = "hms0007b".
    Expected output: "0", and no new bill is added.
    Expected change: Database is rolled back after test.
     */
    @Test
    public void testBill_InvalidBillInfo() throws SQLException {
        String billInfo = "";
        String result = cashierInstance.bill(billInfo);

        assertEquals("0", result, "The method should return '0' when billInfo is invalid");

        Statement verifyStmt = connection.createStatement();
        ResultSet rs = verifyStmt.executeQuery("SELECT * FROM bill WHERE bill_id = 'hms0008b'");
        assertFalse(rs.next(), "No new bill should be added to the database");
        rs.close();
        verifyStmt.close();
    }
}