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

public class CashierGetPaymentHistoryIntegrationTest {

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
                "VALUES ('hms020', 'user020', 'cashier', '1234', 'user020ProfPic.png') " +
                "ON DUPLICATE KEY UPDATE user_name = 'user020', user_type = 'cashier', " +
                "password = '1234', profile_pic = 'user020ProfPic.png', person_id = NULL");
        stmt.executeUpdate("INSERT INTO person (person_id, user_id, nic, gender, date_of_birth, address, mobile, " +
                "first_name, last_name, email, nationality, religion) " +
                "VALUES ('p020', 'hms020', '872984565V', 'M', '1987-10-24', '123/f Yanthampalawa Kurunegala', " +
                "'0713457779', 'Lakshitha', 'Rangana', 'lakshithaasd@gmail.com', 'Sri Lankan', 'Buddhism') " +
                "ON DUPLICATE KEY UPDATE user_id = 'hms020', nic = '872984565V', gender = 'M', " +
                "date_of_birth = '1987-10-24', address = '123/f Yanthampalawa Kurunegala', mobile = '0713457779', " +
                "first_name = 'Lakshitha', last_name = 'Rangana', email = 'lakshithaasd@gmail.com', " +
                "nationality = 'Sri Lankan', religion = 'Buddhism'");
        stmt.executeUpdate("UPDATE sys_user SET person_id = 'p020' WHERE user_id = 'hms020'");
        stmt.executeUpdate("INSERT INTO patient (patient_id, person_id) " +
                "VALUES ('hms0001pa', 'p020') " +
                "ON DUPLICATE KEY UPDATE person_id = 'p020'");
        stmt.executeUpdate("INSERT INTO bill (bill_id, bill_date, doctor_fee, hospital_fee, pharmacy_fee, " +
                "laboratory_fee, appointment_fee, vat, discount, total, payment_method, patient_id, refund) " +
                "VALUES ('hms0007b', '2016-08-30 14:30:00', 200, 150, 300, 0, 500, 60, 0, 1210, 'pending', 'hms0001pa', 0) " +
                "ON DUPLICATE KEY UPDATE bill_date = '2016-08-30 14:30:00', doctor_fee = 200, hospital_fee = 150, " +
                "pharmacy_fee = 300, laboratory_fee = 0, appointment_fee = 500, vat = 60, discount = 0, total = 1210, " +
                "payment_method = 'pending', patient_id = 'hms0001pa', refund = 0");
        stmt.executeUpdate("INSERT INTO bill (bill_id, bill_date, doctor_fee, hospital_fee, pharmacy_fee, " +
                "laboratory_fee, appointment_fee, vat, discount, total, payment_method, patient_id, refund) " +
                "VALUES ('hms0008b', '2016-09-01 10:00:00', 250, 100, 400, 50, 600, 70, 10, 1460, 'paid', 'hms0001pa', 0) " +
                "ON DUPLICATE KEY UPDATE bill_date = '2016-09-01 10:00:00', doctor_fee = 250, hospital_fee = 100, " +
                "pharmacy_fee = 400, laboratory_fee = 50, appointment_fee = 600, vat = 70, discount = 10, total = 1460, " +
                "payment_method = 'paid', patient_id = 'hms0001pa', refund = 0");
        stmt.close();

        cashierInstance = new Cashier("user020");
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
    public void testGetPaymentHistory_SuccessfulRetrieval() throws SQLException {
        ArrayList<ArrayList<String>> result = cashierInstance.getPaymentHistory(2);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("patient_id", result.get(0).get(0));
        assertEquals("bill_date", result.get(0).get(1));
        assertEquals("doctor_fee", result.get(0).get(2));
        assertEquals("hospital_fee", result.get(0).get(3));
        assertEquals("pharmacy_fee", result.get(0).get(4));
        assertEquals("laboratory_fee", result.get(0).get(5));
        assertEquals("appointment_fee", result.get(0).get(6));
        assertEquals("total", result.get(0).get(7));
        assertEquals("bill_id", result.get(0).get(8));
        assertEquals("hms0001pa", result.get(1).get(0));
        assertEquals("2016-09-01 10:00:00", result.get(1).get(1));
        assertEquals("250", result.get(1).get(2));
        assertEquals("100", result.get(1).get(3));
        assertEquals("400", result.get(1).get(4));
        assertEquals("50", result.get(1).get(5));
        assertEquals("600", result.get(1).get(6));
        assertEquals("1460", result.get(1).get(7));
        assertEquals("hms0008b", result.get(1).get(8));
        assertEquals("hms0001pa", result.get(2).get(0));
        assertEquals("2016-08-30 14:30:00", result.get(2).get(1));
        assertEquals("200", result.get(2).get(2));
        assertEquals("150", result.get(2).get(3));
        assertEquals("300", result.get(2).get(4));
        assertEquals("0", result.get(2).get(5));
        assertEquals("500", result.get(2).get(6));
        assertEquals("1210", result.get(2).get(7));
        assertEquals("hms0007b", result.get(2).get(8));
    }

    @Test
    public void testGetPaymentHistory_NoData() throws SQLException {
        Statement stmt = connection.createStatement();
        stmt.executeUpdate("DELETE FROM bill WHERE bill_id IN ('hms0007b', 'hms0008b')");
        stmt.close();

        ArrayList<ArrayList<String>> result = cashierInstance.getPaymentHistory(2);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("patient_id", result.get(0).get(0));
        assertEquals("bill_date", result.get(0).get(1));
        assertEquals("doctor_fee", result.get(0).get(2));
        assertEquals("hospital_fee", result.get(0).get(3));
        assertEquals("pharmacy_fee", result.get(0).get(4));
        assertEquals("laboratory_fee", result.get(0).get(5));
        assertEquals("appointment_fee", result.get(0).get(6));
        assertEquals("total", result.get(0).get(7));
        assertEquals("bill_id", result.get(0).get(8));
    }

    @Test
    public void testGetPaymentHistory_NegativeRows() throws SQLException {
        ArrayList<ArrayList<String>> result = cashierInstance.getPaymentHistory(-1);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("patient_id", result.get(0).get(0));
        assertEquals("bill_date", result.get(0).get(1));
        assertEquals("doctor_fee", result.get(0).get(2));
        assertEquals("hospital_fee", result.get(0).get(3));
        assertEquals("pharmacy_fee", result.get(0).get(4));
        assertEquals("laboratory_fee", result.get(0).get(5));
        assertEquals("appointment_fee", result.get(0).get(6));
        assertEquals("total", result.get(0).get(7));
        assertEquals("bill_id", result.get(0).get(8));
    }

    @Test
    public void testGetPaymentHistory_LargeRows() throws SQLException {
        ArrayList<ArrayList<String>> result = cashierInstance.getPaymentHistory(10);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("hms0001pa", result.get(1).get(0));
        assertEquals("2016-09-01 10:00:00", result.get(1).get(1));
        assertEquals("hms0001pa", result.get(2).get(0));
        assertEquals("2016-08-30 14:30:00", result.get(2).get(1));
    }

    @Test
    public void testGetPaymentHistory_ZeroRows() throws SQLException {
        ArrayList<ArrayList<String>> result = cashierInstance.getPaymentHistory(0);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("patient_id", result.get(0).get(0));
        assertEquals("bill_date", result.get(0).get(1));
        assertEquals("doctor_fee", result.get(0).get(2));
        assertEquals("hospital_fee", result.get(0).get(3));
        assertEquals("pharmacy_fee", result.get(0).get(4));
        assertEquals("laboratory_fee", result.get(0).get(5));
        assertEquals("appointment_fee", result.get(0).get(6));
        assertEquals("total", result.get(0).get(7));
        assertEquals("bill_id", result.get(0).get(8));
    }
}