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
import java.util.Arrays;

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

        Statement stmt = connection.createStatement();
        stmt.executeUpdate("INSERT INTO sys_user (user_id, user_name, user_type, password, profile_pic) " +
                "VALUES ('hms0020u', 'user020', 'cashier', '1234', 'user020ProfPic.png') " +
                "ON DUPLICATE KEY UPDATE user_name = 'user020', user_type = 'cashier', " +
                "password = '1234', profile_pic = 'user020ProfPic.png', person_id = NULL");
        stmt.executeUpdate("INSERT INTO person (person_id, user_id) " +
                "VALUES ('hms00001', 'hms0020u') " +
                "ON DUPLICATE KEY UPDATE user_id = 'hms0020u'");
        stmt.executeUpdate("UPDATE sys_user SET person_id = 'hms00001' WHERE user_id = 'hms0020u'");
        stmt.executeUpdate("INSERT INTO patient (patient_id, person_id) " +
                "VALUES ('hms0001pa', 'hms00001') " +
                "ON DUPLICATE KEY UPDATE person_id = 'hms00001'");
        stmt.executeUpdate("INSERT INTO bill (bill_id, bill_date, doctor_fee, hospital_fee, pharmacy_fee, " +
                "laboratory_fee, appointment_fee, vat, discount, total, payment_method, patient_id, refund) " +
                "VALUES ('hms0007b', '2016-08-30 14:30:00', 200, 150, 300, 0, 500, 60, 0, 1210, 'pending', 'hms0001pa', 0) " +
                "ON DUPLICATE KEY UPDATE bill_date = '2016-08-30 14:30:00', doctor_fee = 200, hospital_fee = 150, " +
                "pharmacy_fee = 300, laboratory_fee = 0, appointment_fee = 500, vat = 60, discount = 0, total = 1210, " +
                "payment_method = 'pending', patient_id = 'hms0001pa', refund = 0");
        stmt.close();

        cashierInstance = new Cashier("user020");
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
                ArrayList<ArrayList<String>> result = new ArrayList<>();
                ArrayList<String> columnNames = new ArrayList<>(Arrays.asList(columns.split(",")));
                result.add(columnNames);
                if (table.equals("sys_user") && columns.equals("user_id,user_type") && condition.contains("user_name = 'user020'")) {
                    ArrayList<String> row = new ArrayList<>();
                    row.add("hms0020u");
                    row.add("cashier");
                    result.add(row);
                }
                return result;
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

    @Test
    public void testBill_Success() throws SQLException {
        String billInfo = "patient_id hms0001pa,doctor_fee 300,pharmacy_fee 400";
        String result = cashierInstance.bill(billInfo);

        assertEquals("hms0008b", result, "Bill ID should be 'hms0008b'");
        Statement verifyStmt = connection.createStatement();
        ResultSet rs = verifyStmt.executeQuery("SELECT * FROM bill WHERE bill_id = 'hms0008b'");
        assertTrue(rs.next(), "New bill should be present");
        assertEquals("hms0001pa", rs.getString("patient_id"), "Patient ID should match");
        assertEquals(300, rs.getInt("doctor_fee"), "Doctor fee should match");
        rs.close();
        verifyStmt.close();
    }

    @Test
    public void testBill_InvalidBillInfo() {
        String billInfo = "";
        String result = cashierInstance.bill(billInfo);

        assertEquals("0", result, "Should return '0' for empty billInfo");
    }

    @Test
    public void testBill_InvalidPatient() {
        String billInfo = "patient_id invalid_patient,doctor_fee 300,pharmacy_fee 400";
        String result = cashierInstance.bill(billInfo);

        assertEquals("0", result, "Should return '0' for invalid patient_id");
    }

    @Test
    public void testBill_MissingRequiredField() {
        String billInfo = "doctor_fee 300,pharmacy_fee 400";
        String result = cashierInstance.bill(billInfo);

        assertEquals("0", result, "Should return '0' for missing patient_id");
    }

    @Test
    public void testBill_DatabaseError() throws SQLException {
        Statement stmt = connection.createStatement();
        stmt.executeUpdate("DROP TABLE bill");
        stmt.close();

        String billInfo = "patient_id hms0001pa,doctor_fee 300,pharmacy_fee 400";
        String result = cashierInstance.bill(billInfo);

        assertEquals("0", result, "Should return '0' on database error");
    }
}